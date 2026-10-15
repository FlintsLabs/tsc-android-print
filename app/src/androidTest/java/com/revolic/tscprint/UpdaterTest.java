package com.revolic.tscprint;

import android.content.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.revolic.tscprint.core.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UpdaterTest {
    final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    static String hash(byte[] bytes) throws Exception {
        StringBuilder text=new StringBuilder();
        for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))text.append(String.format(Locale.ROOT,"%02x",b&255));
        return text.toString();
    }
    static byte[] fixture(String name) throws IOException {
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name+".apk")) {return in.readAllBytes();}
    }
    static UpdateRelease release(byte[] bytes,String tag) throws Exception {
        String version=ReleaseVersion.parse(tag).toString(),root="https://github.com/"+UpdateRelease.REPO+"/releases/download/"+tag+"/";
        return new UpdateRelease(tag,root+"TSC-Direct-Print-"+version+".apk",root+"SHA256SUMS.txt",bytes.length,hash(bytes));
    }
    static String metadata(UpdateRelease r) throws Exception {
        return new JSONObject().put("tag_name",r.tag).put("draft",false).put("prerelease",false).put("assets",new JSONArray()
            .put(new JSONObject().put("name",r.filename).put("state","uploaded").put("size",r.size).put("digest","sha256:"+r.sha256).put("browser_download_url",r.apkUrl))
            .put(new JSONObject().put("name","SHA256SUMS.txt").put("state","uploaded").put("browser_download_url",r.checksumUrl))).toString();
    }
    static GitHubReleaseClient client(UpdateRelease release,byte[] apk) {
        return new GitHubReleaseClient((url,cancel)->{
            byte[] body;
            try {
                if(url.equals(UpdateRelease.API))body=metadata(release).getBytes(StandardCharsets.UTF_8);
                else if(url.equals(release.checksumUrl))body=(release.sha256+"  "+release.filename+"\n").getBytes(StandardCharsets.UTF_8);
                else if(url.equals(release.apkUrl))body=apk;
                else throw new IOException("Unexpected URL");
            } catch(Exception e) {throw new IOException(e);}
            return new GitHubReleaseClient.Response(new ByteArrayInputStream(body),body.length,()->{});
        });
    }
    @Test public void releaseParserRejectsDraftMissingAssetsAndForeignDownloadOrigins() throws Exception {
        UpdateRelease valid=release(fixture("future"),"v1.3.6");String json=metadata(valid);
        assertEquals(valid.version,UpdateRelease.parse(json).version);
        assertThrows(IllegalArgumentException.class,()->UpdateRelease.parse(new JSONObject(json).put("draft",true).toString()));
        assertThrows(IllegalArgumentException.class,()->UpdateRelease.parse(new JSONObject(json).put("prerelease",true).toString()));
        assertThrows(IllegalArgumentException.class,()->UpdateRelease.parse(new JSONObject(json).put("assets",new JSONArray()).toString()));
        JSONObject insecure = new JSONObject(json);
        insecure.getJSONArray("assets").getJSONObject(0).put("browser_download_url",valid.apkUrl.replace("https:","http:"));
        assertThrows(IllegalArgumentException.class,()->UpdateRelease.parse(insecure.toString()));
        JSONObject foreign = new JSONObject(json);
        foreign.getJSONArray("assets").getJSONObject(0).put("browser_download_url",valid.apkUrl.replace("github.com","evil.example"));
        assertThrows(IllegalArgumentException.class,()->UpdateRelease.parse(foreign.toString()));
        assertThrows(IllegalArgumentException.class,()->UpdateRelease.parse(json.replace("v1.3.6","v1.3.6-beta")));
    }
    @Test public void downloaderVerifiesChecksumAndDeletesCorruptPartialFiles() throws Exception {
        byte[] bytes=fixture("future");UpdateRelease release=release(bytes,"v1.3.6");
        File target=new File(context.getCacheDir(),"test-update.apk");
        try {
            UpdateRelease verified=client(release,bytes).download(release,target,new Cancellation(),n->{});
            assertEquals(release.sha256,verified.sha256);assertArrayEquals(bytes,java.nio.file.Files.readAllBytes(target.toPath()));
            target.delete();byte[] corrupt=bytes.clone();corrupt[corrupt.length-1]^=1;
            assertThrows(IOException.class,()->client(release,corrupt).download(release,target,new Cancellation(),n->{}));
            assertFalse(target.exists());assertFalse(new File(target+".part").exists());
        } finally {target.delete();}
    }
    @Test public void verifierRejectsWrongPackageSignerDowngradeAndChangedBytes() throws Exception {
        File target=new File(context.getCacheDir(),"verify-update.apk");
        try {
            byte[] future=fixture("future");java.nio.file.Files.write(target.toPath(),future);
            UpdateInstaller.verify(context,target,release(future,"v1.3.6"),new Cancellation());
            for(String name:new String[]{"wrong-package","wrong-signer","old"}) {
                byte[] bytes=fixture(name);java.nio.file.Files.write(target.toPath(),bytes);
                UpdateRelease r=release(bytes,name.equals("old")?"v1.3.3":"v1.3.6");
                assertThrows(name,IOException.class,()->UpdateInstaller.verify(context,target,r,new Cancellation()));
            }
            java.nio.file.Files.write(target.toPath(),future);future[future.length-1]^=1;
            assertThrows(IOException.class,()->UpdateInstaller.verify(context,target,release(future,"v1.3.6"),new Cancellation()));
        } finally {target.delete();}
    }
    @Test public void installerSharesOnlyUpdateFilesAndRequiresNativeConfirmation() throws Exception {
        File dir=new File(context.getFilesDir(),"updates");dir.mkdirs();File file=new File(dir,"provider-test.apk");
        try {
            java.nio.file.Files.write(file.toPath(),fixture("future"));
            Intent intent=UpdateInstaller.intent(context,file);
            assertEquals(Intent.ACTION_VIEW,intent.getAction());assertEquals("content",intent.getData().getScheme());
            assertTrue((intent.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
            try(InputStream in=context.getContentResolver().openInputStream(intent.getData())) {assertArrayEquals(fixture("future"),in.readAllBytes());}
            assertThrows(IllegalArgumentException.class,()->UpdateInstaller.intent(context,new File(context.getFilesDir(),"printer.xml")));
        } finally {file.delete();}
    }
}
