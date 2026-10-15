package com.revolic.tscprint.core;

import java.io.*;
import java.security.*;
import java.util.Locale;
import java.util.function.LongConsumer;

public final class UpdateIntegrity {
    private UpdateIntegrity() { }
    public static String checksum(String manifest,String filename) {
        String found=null;
        for(String line:manifest.split("\\r?\\n")) {
            if(line.isBlank())continue;
            if(line.length()<67 || !line.substring(0,64).matches("[A-Fa-f0-9]{64}") || line.charAt(64)!=' '
                || (line.charAt(65)!=' ' && line.charAt(65)!='*'))throw new IllegalArgumentException("รูปแบบ SHA256SUMS.txt ไม่ถูกต้อง");
            if(line.substring(66).equals(filename)) {
                if(found!=null)throw new IllegalArgumentException("มี checksum ของ APK ซ้ำใน Release");
                found=line.substring(0,64).toLowerCase(Locale.ROOT);
            }
        }
        if(found==null)throw new IllegalArgumentException("ไม่พบ checksum ของ APK ใน Release");
        return found;
    }
    public static String copy(InputStream in,OutputStream out,long expected,long limit,Cancellation cancel,LongConsumer progress) throws IOException {
        if(expected<0 || expected>limit)throw new IOException("ขนาดไฟล์อัปเดตเกินช่วงที่รองรับ");
        MessageDigest digest;
        try {digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
        byte[] buffer=new byte[32768];long total=0;
        while(true) {
            cancel.check();int count=in.read(buffer);if(count<0)break;
            total+=count;if(total>limit || total>expected)throw new IOException("ขนาดไฟล์อัปเดตไม่ตรงกับ Release");
            digest.update(buffer,0,count);out.write(buffer,0,count);progress.accept(total);
        }
        cancel.check();
        if(total!=expected)throw new IOException("ดาวน์โหลดไฟล์ไม่ครบ กรุณาลองใหม่");
        StringBuilder result=new StringBuilder(64);
        for(byte b:digest.digest())result.append(String.format(Locale.ROOT,"%02x",b&255));
        return result.toString();
    }
}
