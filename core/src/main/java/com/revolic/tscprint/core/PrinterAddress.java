package com.revolic.tscprint.core;

public final class PrinterAddress {
    private PrinterAddress() { }
    public static String validate(String value) {
        if (value==null || !value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) throw new IllegalArgumentException("กรอก IPv4 เช่น 192.168.70.37");
        String[] parts=value.split("\\."); int[] n=new int[4];
        for(int i=0;i<4;i++) { n[i]=Integer.parseInt(parts[i]); if(n[i]>255) throw new IllegalArgumentException("IPv4 ต้องอยู่ระหว่าง 0–255"); }
        if(n[0]==0 || n[0]>=224 || n[3]==255) throw new IllegalArgumentException("กรอก IP ของเครื่องพิมพ์โดยตรง");
        return n[0]+"."+n[1]+"."+n[2]+"."+n[3];
    }
}
