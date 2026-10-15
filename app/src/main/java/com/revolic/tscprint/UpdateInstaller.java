package com.revolic.tscprint;

import android.content.*;
import android.content.pm.*;
import androidx.core.content.FileProvider;
import com.revolic.tscprint.core.*;
import java.io.*;
import java.security.*;
import java.util.*;

public final class UpdateInstaller {
    private UpdateInstaller() { }
    public static void verify(Context context,File apk,UpdateRelease release,Cancellation cancel) throws IOException {
        if(release.sha256==null)throw new IOException("ยังไม่ได้ตรวจสอบ checksum ของไฟล์");
        String actual;
        try(InputStream in=new FileInputStream(apk)) {
            actual=UpdateIntegrity.copy(in,OutputStream.nullOutputStream(),release.size,UpdateRelease.MAX_APK,cancel,n->{});
        }
        if(!actual.equals(release.sha256))throw new IOException("ไฟล์อัปเดตเปลี่ยนแปลง กรุณาดาวน์โหลดใหม่");
        PackageManager manager=context.getPackageManager();
        try {
            PackageInfo installed=manager.getPackageInfo(context.getPackageName(),PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES));
            PackageInfo candidate=manager.getPackageArchiveInfo(apk.getAbsolutePath(),PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES));
            if(candidate==null || !context.getPackageName().equals(candidate.packageName))throw new IOException("APK ไม่ใช่แอป TSC Direct Print");
            if(candidate.applicationInfo==null || candidate.applicationInfo.minSdkVersion>android.os.Build.VERSION.SDK_INT)
                throw new IOException("APK รุ่นนี้ต้องใช้ Android ใหม่กว่าที่เครื่องรองรับ");
            if(!release.version.equals(candidate.versionName) || candidate.getLongVersionCode()<=installed.getLongVersionCode())
                throw new IOException("APK ไม่ใช่เวอร์ชันใหม่กว่าที่ติดตั้งอยู่");
            if(!signers(installed).equals(signers(candidate)))throw new IOException("ลายเซ็น APK ไม่ตรงกับแอปที่ติดตั้ง จึงอัปเดตทับไม่ได้");
        } catch(PackageManager.NameNotFoundException e) {throw new IOException("อ่านข้อมูลแอปที่ติดตั้งไม่ได้",e);}
    }
    private static Set<String> signers(PackageInfo info) throws IOException {
        if(info.signingInfo==null || info.signingInfo.getApkContentsSigners().length==0)throw new IOException("APK ไม่มีลายเซ็นที่ตรวจสอบได้");
        Set<String> result=new HashSet<>();
        for(android.content.pm.Signature signer:info.signingInfo.getApkContentsSigners()) {
            try {result.add(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(signer.toByteArray())));}
            catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
        }
        return result;
    }
    /** Only the private updates directory is exposed, with a temporary read grant. */
    public static Intent intent(Context context,File file) {
        android.net.Uri uri=FileProvider.getUriForFile(context,context.getPackageName()+".updates",file);
        Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("APK update",uri));return intent;
    }
}
