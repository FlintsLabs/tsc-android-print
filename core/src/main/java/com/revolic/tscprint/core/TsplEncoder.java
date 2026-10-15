package com.revolic.tscprint.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class TsplEncoder {
    public static final int WIDTH = 815, HEIGHT = 607, STRIDE = 102;
    private TsplEncoder() { }
    @FunctionalInterface public interface RowSource { void read(int y, int[] pixels) throws IOException; }
    /** TSPL BITMAP mode 1: white=1, black=0, most significant dot first. */
    public static byte[] packRow(int[] argb) {
        byte[] packed = new byte[(argb.length + 7) / 8];
        Arrays.fill(packed, (byte) 0xff);
        for (int x = 0; x < argb.length; x++) {
            int p = argb[x], alpha = p >>> 24;
            int r = (((p >>> 16) & 255) * alpha + 255 * (255 - alpha) + 127) / 255;
            int g = (((p >>> 8) & 255) * alpha + 255 * (255 - alpha) + 127) / 255;
            int b = ((p & 255) * alpha + 255 * (255 - alpha) + 127) / 255;
            if (299 * r + 587 * g + 114 * b < 128000) packed[x / 8] &= ~(0x80 >>> (x % 8));
        }
        return packed;
    }
    public static void writeLabel(OutputStream out, RowSource rows, Cancellation cancellation) throws IOException {
        writeLabel(out,PaperSize.LABEL,rows,cancellation);
    }
    public static void writeLabel(OutputStream out, PaperSize paper, RowSource rows, Cancellation cancellation) throws IOException {
        cancellation.check();
        out.write(("SIZE "+paper.widthMm()+" mm,"+paper.heightMm()+" mm\r\nDIRECTION 1,0\r\nCLS\r\nBITMAP 0,0,"+paper.stride()+","+paper.heightDots()+",1,").getBytes(StandardCharsets.US_ASCII));
        int[] pixels = new int[paper.widthDots()];
        for (int y = 0; y < paper.heightDots(); y++) {
            cancellation.check();
            rows.read(y, pixels);
            out.write(packRow(pixels));
        }
        cancellation.check();
        out.write("\r\nPRINT 1,1\r\n".getBytes(StandardCharsets.US_ASCII));
    }
}
