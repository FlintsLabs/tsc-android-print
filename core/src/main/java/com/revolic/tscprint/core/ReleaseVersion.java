package com.revolic.tscprint.core;

/** Stable release tags only; pre-release builds are never offered as an update. */
public final class ReleaseVersion implements Comparable<ReleaseVersion> {
    private final int major,minor,patch;
    private ReleaseVersion(int major,int minor,int patch) {this.major=major;this.minor=minor;this.patch=patch;}
    public static ReleaseVersion parse(String value) {
        if(value==null || !value.matches("v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"))
            throw new IllegalArgumentException("หมายเลขเวอร์ชันจาก GitHub ไม่ถูกต้อง");
        String[] parts=value.replaceFirst("^v","").split("\\.");
        try {return new ReleaseVersion(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2]));}
        catch(NumberFormatException e) {throw new IllegalArgumentException("หมายเลขเวอร์ชันจาก GitHub ไม่ถูกต้อง",e);}
    }
    @Override public int compareTo(ReleaseVersion other) {
        int result=Integer.compare(major,other.major);
        if(result==0)result=Integer.compare(minor,other.minor);
        return result==0?Integer.compare(patch,other.patch):result;
    }
    @Override public String toString() {return major+"."+minor+"."+patch;}
}
