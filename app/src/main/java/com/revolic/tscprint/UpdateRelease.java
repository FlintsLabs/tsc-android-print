package com.revolic.tscprint;

import com.revolic.tscprint.core.ReleaseVersion;
import org.json.*;
import java.util.Locale;

/** All download locations are pinned to this repository, never supplied by an intent. */
public final class UpdateRelease {
    public static final String REPO="FlintsLabs/tsc-android-print", API="https://api.github.com/repos/"+REPO+"/releases/latest";
    public static final long MAX_APK=100L*1024*1024;
    public final String tag,version,filename,apkUrl,checksumUrl,sha256;
    public final long size;
    public UpdateRelease(String tag,String apkUrl,String checksumUrl,long size,String sha256) {
        this.tag=tag;this.version=ReleaseVersion.parse(tag).toString();this.filename="TSC-Direct-Print-"+version+".apk";
        String prefix="https://github.com/"+REPO+"/releases/download/"+tag+"/";
        if(!apkUrl.equals(prefix+filename) || !checksumUrl.equals(prefix+"SHA256SUMS.txt"))throw new IllegalArgumentException("ตำแหน่งดาวน์โหลดไม่ใช่ Release ของแอปนี้");
        if(size<1 || size>MAX_APK)throw new IllegalArgumentException("APK ต้องมีขนาดไม่เกิน 100 MB");
        if(sha256!=null && !sha256.matches("[a-fA-F0-9]{64}"))throw new IllegalArgumentException("checksum ของ Release ไม่ถูกต้อง");
        this.apkUrl=apkUrl;this.checksumUrl=checksumUrl;this.size=size;this.sha256=sha256==null?null:sha256.toLowerCase(Locale.ROOT);
    }
    public UpdateRelease withChecksum(String checksum) {return new UpdateRelease(tag,apkUrl,checksumUrl,size,checksum);}
    public JSONObject json() throws JSONException {
        return new JSONObject().put("tag",tag).put("apk",apkUrl).put("checksums",checksumUrl).put("size",size).put("sha256",sha256);
    }
    public static UpdateRelease restore(JSONObject json) throws JSONException {
        return new UpdateRelease(json.getString("tag"),json.getString("apk"),json.getString("checksums"),json.getLong("size"),json.getString("sha256"));
    }
    public static UpdateRelease parse(String text) throws JSONException {
        JSONObject release=new JSONObject(text);
        if(release.getBoolean("draft") || release.getBoolean("prerelease"))throw new IllegalArgumentException("แอปอัปเดตจาก Release ปกติที่เผยแพร่แล้วเท่านั้น");
        String tag=release.getString("tag_name"),name="TSC-Direct-Print-"+ReleaseVersion.parse(tag)+".apk";
        JSONObject apk=null,checksum=null;JSONArray assets=release.getJSONArray("assets");
        for(int index=0;index<assets.length();index++) {
            JSONObject asset=assets.getJSONObject(index);String file=asset.getString("name");
            if(name.equals(file)) {if(apk!=null)throw new IllegalArgumentException("พบ APK ซ้ำใน Release");apk=asset;}
            if("SHA256SUMS.txt".equals(file)) {if(checksum!=null)throw new IllegalArgumentException("พบไฟล์ checksum ซ้ำ");checksum=asset;}
        }
        if(apk==null || checksum==null)throw new IllegalArgumentException("Release ยังไม่มี APK และ SHA256SUMS.txt ครบ กรุณาลองใหม่ภายหลัง");
        if(!"uploaded".equals(apk.getString("state")) || !"uploaded".equals(checksum.getString("state")))throw new IllegalArgumentException("ไฟล์ Release ยังอัปโหลดไม่เสร็จ");
        String hash=apk.isNull("digest")?null:apk.optString("digest",null);
        if(hash!=null) {
            if(!hash.startsWith("sha256:"))throw new IllegalArgumentException("ไม่รองรับชนิด checksum ของ Release");
            hash=hash.substring(7);
        }
        return new UpdateRelease(tag,apk.getString("browser_download_url"),checksum.getString("browser_download_url"),apk.getLong("size"),hash);
    }
}
