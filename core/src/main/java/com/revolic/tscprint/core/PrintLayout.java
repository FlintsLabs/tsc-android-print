package com.revolic.tscprint.core;

public final class PrintLayout {
    public final float scale, offsetX, offsetY;
    public final boolean rotate;
    private PrintLayout(float s, float x, float y, boolean r) { scale=s; offsetX=x; offsetY=y; rotate=r; }
    public static PrintLayout fit(float width, float height, boolean portraitStock) {
        return fit(width,height,PaperSize.LABEL,portraitStock);
    }
    public static PrintLayout fit(float width, float height, PaperSize paper, boolean rotate) {
        if (!Float.isFinite(width) || !Float.isFinite(height) || width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid PDF page size");
        float w = rotate ? height : width, h = rotate ? width : height;
        float s = Math.min(paper.widthDots() / w, paper.heightDots() / h);
        return new PrintLayout(s, (paper.widthDots()-w*s)/2, (paper.heightDots()-h*s)/2, rotate);
    }
}
