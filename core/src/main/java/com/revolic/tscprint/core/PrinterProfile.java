package com.revolic.tscprint.core;

/** Immutable endpoint snapshot. This TSPL pipeline supports 203 DPI MH261T printers. */
public final class PrinterProfile {
    public static final int DPI=203;
    public final String id,name,host,paperId;
    public final int port;
    public PrinterProfile(String id,String name,String host,int port,String paperId) {
        if(id==null || !id.matches("[A-Za-z0-9_-]{1,64}"))throw new IllegalArgumentException("รหัสเครื่องพิมพ์ไม่ถูกต้อง");
        if(name==null || name.trim().isEmpty() || name.trim().length()>48 || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("กรอกชื่อเครื่องพิมพ์ 1–48 ตัวอักษร");
        if(port<1 || port>65535)throw new IllegalArgumentException("พอร์ตต้องอยู่ระหว่าง 1–65535");
        PaperSize.fromId(paperId);
        this.id=id;this.name=name.trim();this.host=PrinterAddress.validate(host);this.port=port;this.paperId=paperId;
    }
    public String endpoint() {return host+":"+port;}
    public PrinterProfile withAddress(String host,int port) {return new PrinterProfile(id,name,host,port,paperId);}
    public PrinterProfile withPaper(String paperId) {return new PrinterProfile(id,name,host,port,paperId);}
}
