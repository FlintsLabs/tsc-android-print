package com.revolic.tscprint.core;

import java.math.BigDecimal;

/** Physical stock: width across the print head, height along the feed. Tenths of a millimetre. */
public final class PaperSize {
    public static final int DPI=203, MIN_TENTHS=200, MAX_WIDTH_TENTHS=1680, MAX_HEIGHT_TENTHS=5000;
    public static final PaperSize LABEL=new PaperSize(1020,760), A5=new PaperSize(1480,2100);
    public final int widthTenths,heightTenths;
    public PaperSize(int widthTenths,int heightTenths) {
        if(widthTenths<MIN_TENTHS || widthTenths>MAX_WIDTH_TENTHS || heightTenths<MIN_TENTHS || heightTenths>MAX_HEIGHT_TENTHS)
            throw new IllegalArgumentException("กว้าง 20–168 มม. และสูง 20–500 มม. ตามช่วงที่แอปรองรับ");
        this.widthTenths=widthTenths;this.heightTenths=heightTenths;
    }
    public static PaperSize parse(String width,String height) { return new PaperSize(tenths(width),tenths(height)); }
    private static int tenths(String value) {
        String s=value==null?"":value.trim();
        if(!s.matches("[0-9]{1,3}(\\.[0-9])?")) throw new IllegalArgumentException("กรอกมิลลิเมตรเป็นตัวเลข ทศนิยมได้ 1 ตำแหน่ง");
        return new BigDecimal(s).movePointRight(1).intValueExact();
    }
    public static PaperSize fromId(String id) {
        if("TSC_102x76".equals(id))return LABEL;
        if(id==null || !id.matches("TSC_MM10_[0-9]{3,4}x[0-9]{3,4}"))throw new IllegalArgumentException("ไม่รู้จักขนาดกระดาษ กรุณาเลือกขนาดของ TSC ใหม่");
        String[] parts=id.substring(9).split("x");
        return new PaperSize(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]));
    }
    public String id() { return equals(LABEL)?"TSC_102x76":"TSC_MM10_"+widthTenths+"x"+heightTenths; }
    public String widthMm() { return mm(widthTenths); }
    public String heightMm() { return mm(heightTenths); }
    private static String mm(int n) { return BigDecimal.valueOf(n,1).stripTrailingZeros().toPlainString(); }
    public String label() { return widthMm()+" × "+heightMm()+" มม."; }
    public int widthDots() { return (int)Math.round(widthTenths*DPI/254.0); }
    public int heightDots() { return (int)Math.round(heightTenths*DPI/254.0); }
    public int stride() { return (widthDots()+7)/8; }
    public int widthMils() { return (int)Math.round(widthTenths*1000/254.0); }
    public int heightMils() { return (int)Math.round(heightTenths*1000/254.0); }
    public boolean isPortrait() { return widthTenths<=heightTenths; }
    public int rulerMm() { return Math.max(10,Math.min(80,(int)((widthTenths/10.0-8)/10)*10)); }
    @Override public boolean equals(Object other) { return other instanceof PaperSize && widthTenths==((PaperSize)other).widthTenths && heightTenths==((PaperSize)other).heightTenths; }
    @Override public int hashCode() { return 31*widthTenths+heightTenths; }
}
