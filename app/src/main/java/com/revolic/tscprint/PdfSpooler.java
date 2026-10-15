package com.revolic.tscprint;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import com.revolic.tscprint.core.*;
import java.io.*;

public final class PdfSpooler {
    public static final long MAX_PDF_BYTES=100L*1024*1024;
    private PdfSpooler() { }
    public static File copyPdf(InputStream in,File directory,Cancellation cancel) throws IOException {
        File file=File.createTempFile("tsc-pdf-",".pdf",directory);
        boolean success=false;
        try(OutputStream out=new FileOutputStream(file)) {
            cancel.register(in);
            byte[] buffer=new byte[32768];long size=0;int n;
            while(true) { cancel.check();n=in.read(buffer);if(n==-1)break;cancel.check();size+=n;if(size>MAX_PDF_BYTES) throw new IOException("PDF ใหญ่เกิน 100 MB");out.write(buffer,0,n); }
            cancel.check();success=true;return file;
        } finally { cancel.unregister(in);if(!success) file.delete(); }
    }
    public static int stage(File pdf,File tspl,int[][] ranges,int copies,boolean portrait,Cancellation cancel) throws IOException {
        return stage(pdf,tspl,ranges,copies,PaperSize.LABEL,portrait,cancel);
    }
    public static int stage(File pdf,File tspl,int[][] ranges,int copies,PaperSize paper,boolean rotate,Cancellation cancel) throws IOException {
        try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY); PdfRenderer renderer=new PdfRenderer(fd); OutputStream out=new BufferedOutputStream(new FileOutputStream(tspl))) {
            int[] pages=PageSelection.select(renderer.getPageCount(),ranges);
            PageSelection.totalLabels(pages.length,copies);
            Bitmap bitmap=Bitmap.createBitmap(paper.widthDots(),paper.heightDots(),Bitmap.Config.ARGB_8888);
            try {
                for(int index:pages) {
                    cancel.check(); bitmap.eraseColor(Color.WHITE);
                    try(PdfRenderer.Page page=renderer.openPage(index)) {
                        PrintLayout layout=PrintLayout.fit(page.getWidth(),page.getHeight(),paper,rotate);
                        Matrix matrix=new Matrix();float s=layout.scale,x=layout.offsetX,y=layout.offsetY;
                        matrix.setValues(layout.rotate ? new float[]{0,-s,x+s*page.getHeight(),s,0,y,0,0,1} : new float[]{s,0,x,0,s,y,0,0,1});
                        page.render(bitmap,null,matrix,PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                    }
                    TsplEncoder.writeLabel(out,paper,(y,row)->bitmap.getPixels(row,0,paper.widthDots(),0,y,paper.widthDots(),1),cancel);
                }
            } finally { bitmap.recycle(); }
            return pages.length;
        }
    }
}
