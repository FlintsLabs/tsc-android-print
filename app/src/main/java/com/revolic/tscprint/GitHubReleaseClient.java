package com.revolic.tscprint;

import com.revolic.tscprint.core.*;
import org.json.JSONException;
import javax.net.ssl.HttpsURLConnection;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.LongConsumer;

/** Manual, anonymous HTTPS requests; no credential or printer traffic is sent to GitHub. */
public final class GitHubReleaseClient {
    public interface Transport {Response open(String url,Cancellation cancel) throws IOException;}
    public static final class Response implements AutoCloseable {
        public final InputStream body;public final long length;private final Closeable cleanup;
        public Response(InputStream body,long length,Closeable cleanup) {this.body=body;this.length=length;this.cleanup=cleanup;}
        @Override public void close() throws IOException {try {body.close();}finally {cleanup.close();}}
    }
    private final Transport transport;
    public GitHubReleaseClient() {this(GitHubReleaseClient::https);}
    public GitHubReleaseClient(Transport transport) {this.transport=transport;}
    public UpdateRelease latest(Cancellation cancel) throws IOException {
        try {return UpdateRelease.parse(text(UpdateRelease.API,1024*1024,cancel));}
        catch(JSONException e) {throw new IOException("อ่านข้อมูล Release ไม่ได้ กรุณาลองใหม่ภายหลัง",e);}
        catch(IllegalArgumentException e) {throw new IOException(e.getMessage(),e);}
    }
    public UpdateRelease download(UpdateRelease release,File target,Cancellation cancel,LongConsumer progress) throws IOException {
        if(!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs())throw new IOException("สร้างพื้นที่ดาวน์โหลดไม่ได้");
        File partial=new File(target.getPath()+".part");boolean complete=false;
        try {
            String expected=UpdateIntegrity.checksum(text(release.checksumUrl,65536,cancel),release.filename);
            if(release.sha256!=null && !release.sha256.equals(expected))throw new IOException("checksum ใน GitHub กับ SHA256SUMS.txt ไม่ตรงกัน");
            String actual;
            try(Response response=transport.open(release.apkUrl,cancel);OutputStream out=new FileOutputStream(partial)) {
                if(response.length>=0 && response.length!=release.size)throw new IOException("ขนาด APK ไม่ตรงกับ Release");
                actual=UpdateIntegrity.copy(response.body,out,release.size,UpdateRelease.MAX_APK,cancel,progress);
            }
            if(!actual.equals(expected))throw new IOException("ไฟล์ APK ไม่ผ่านการตรวจ checksum กรุณาดาวน์โหลดใหม่");
            cancel.check();
            if(target.exists() && !target.delete())throw new IOException("แทนที่ไฟล์อัปเดตเดิมไม่ได้");
            if(!partial.renameTo(target))throw new IOException("บันทึกไฟล์อัปเดตไม่ได้");
            complete=true;return release.withChecksum(actual);
        } catch(IllegalArgumentException e) {throw new IOException(e.getMessage(),e);}
        finally {if(!complete)partial.delete();}
    }
    private String text(String url,int limit,Cancellation cancel) throws IOException {
        try(Response response=transport.open(url,cancel);ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
            if(response.length>limit)throw new IOException("ข้อมูล Release ใหญ่เกินกำหนด");
            byte[] buffer=new byte[8192];int count;
            while(true) {
                cancel.check();count=response.body.read(buffer);if(count<0)break;
                if(bytes.size()+count>limit)throw new IOException("ข้อมูล Release ใหญ่เกินกำหนด");
                bytes.write(buffer,0,count);
            }
            return bytes.toString(StandardCharsets.UTF_8.name());
        }
    }
    private static Response https(String source,Cancellation cancel) throws IOException {
        URL url=new URL(source);
        Set<String> hosts=Set.of("api.github.com","github.com","release-assets.githubusercontent.com","objects.githubusercontent.com");
        for(int redirects=0;redirects<=5;redirects++) {
            cancel.check();
            if(!"https".equals(url.getProtocol()) || !hosts.contains(url.getHost()) || url.getUserInfo()!=null || (url.getPort()!=-1 && url.getPort()!=443))
                throw new IOException("GitHub ส่งตำแหน่งดาวน์โหลดที่ไม่รองรับ");
            HttpsURLConnection connection=(HttpsURLConnection)url.openConnection();
            connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(30000);
            connection.setUseCaches(false);
            connection.setRequestProperty("User-Agent","TSC-Direct-Print/"+BuildConfig.VERSION_NAME);
            connection.setRequestProperty("Accept",url.getHost().equals("api.github.com")?"application/vnd.github+json":"application/octet-stream");
            if(url.getHost().equals("api.github.com"))connection.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
            Closeable close=connection::disconnect;cancel.register(close);
            boolean retained=false;
            try {
                int code=connection.getResponseCode();
                if(code==301 || code==302 || code==303 || code==307 || code==308) {
                    String location=connection.getHeaderField("Location");
                    if(location==null)throw new IOException("GitHub ไม่ระบุตำแหน่งดาวน์โหลด");
                    url=new URL(url,location);continue;
                }
                if(code==403 || code==429)throw new IOException("GitHub จำกัดการตรวจสอบชั่วคราว กรุณาลองใหม่ภายหลัง");
                if(code==404)throw new IOException("ไม่พบ Release หรือไฟล์อัปเดตบน GitHub");
                if(code!=200)throw new IOException("GitHub ไม่พร้อมใช้งาน กรุณาลองใหม่ภายหลัง");
                InputStream body=connection.getInputStream();retained=true;
                return new Response(body,connection.getContentLengthLong(),()->{cancel.unregister(close);connection.disconnect();});
            } finally {if(!retained) {cancel.unregister(close);connection.disconnect();}}
        }
        throw new IOException("GitHub เปลี่ยนตำแหน่งดาวน์โหลดหลายครั้งเกินกำหนด");
    }
}
