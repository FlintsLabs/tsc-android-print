package com.revolic.tscprint;

import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.revolic.tscprint.core.Cancellation;
import com.revolic.tscprint.core.PrinterAddress;
import com.revolic.tscprint.core.PaperSize;
import java.io.*;

/** One diagnostic page. Shared drawing keeps preview and printable content identical. */
public final class TestLabel {
    public static final String QR_TEXT="TSC-TEST-001";
    private TestLabel() { }
    public static Bitmap preview(String host) {
        return preview(host,PaperSize.LABEL);
    }
    public static Bitmap preview(String host,PaperSize paper) {
        float scale=Math.min(1f,1000f/Math.max(paper.widthDots(),paper.heightDots()));
        Bitmap bitmap=Bitmap.createBitmap(Math.round(paper.widthDots()*scale),Math.round(paper.heightDots()*scale),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);canvas.scale(scale,scale);
        draw(canvas,PrinterAddress.validate(host),paper);
        return bitmap;
    }
    public static File createPdf(File directory,String host,Cancellation cancel) throws IOException {
        return createPdf(directory,host,PaperSize.LABEL,false,cancel);
    }
    public static File createPdf(File directory,String host,PaperSize paper,boolean rotate,Cancellation cancel) throws IOException {
        cancel.check();
        String address=PrinterAddress.validate(host);
        File file=File.createTempFile("tsc-test-",".pdf",directory);
        boolean success=false;
        PdfDocument document=new PdfDocument();
        try {
            // Coordinates are physical printer dots. Undo the service rotation in the logical PDF.
            PdfDocument.Page page=document.startPage(new PdfDocument.PageInfo.Builder(rotate?paper.heightDots():paper.widthDots(),rotate?paper.widthDots():paper.heightDots(),1).create());
            try {
                if(rotate) {page.getCanvas().translate(0,paper.widthDots());page.getCanvas().rotate(-90);}
                draw(page.getCanvas(),address,paper);
            } finally {document.finishPage(page);}
            cancel.check();
            try(OutputStream out=new FileOutputStream(file)){document.writeTo(out);}
            cancel.check();success=true;return file;
        } finally {try {document.close();} finally {if(!success)file.delete();}}
    }
    private static void draw(Canvas canvas,String host,PaperSize paper) {
        if(paper.equals(PaperSize.LABEL)) {drawLegacy(canvas,host);return;}
        int w=paper.widthDots(),h=paper.heightDots();
        canvas.drawColor(Color.WHITE);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);canvas.drawRect(16,16,w-17,h-17,p);p.setStyle(Paint.Style.FILL);
        float margin=24,inner=w-2*margin;
        float title=Math.min(32,inner/9),small=Math.min(20,inner/28);
        text(canvas,p,"TSC MH261T",margin,30+title,title,true);
        text(canvas,p,paper.widthMm()+" x "+paper.heightMm()+" mm / 203 DPI",margin,34+title+small,small,false);
        float top=40+title+small;
        float footer=Math.min(110,h*.25f),bottom=h-footer;
        float area=Math.max(35,bottom-top-12),column=inner/2;
        try {
            BitMatrix qr=new QRCodeWriter().encode(QR_TEXT,BarcodeFormat.QR_CODE,0,0);
            int module=Math.max(1,(int)(Math.min(column-8,area*.75f)/qr.getWidth()));
            int side=module*qr.getWidth();float qx=margin+(column-side)/2,qy=top+(area-side)/2;
            p.setAntiAlias(false);
            for(int y=0;y<qr.getHeight();y++)for(int x=0;x<qr.getWidth();x++)if(qr.get(x,y))
                canvas.drawRect(qx+x*module,qy+y*module,qx+(x+1)*module,qy+(y+1)*module,p);
            p.setAntiAlias(true);
        } catch(WriterException e) {throw new IllegalStateException(e);}
        float badgeW=column-8,badgeH=Math.min(badgeW*.65f,area*.65f),bx=margin+column+4,by=top+(area-badgeH)/2;
        canvas.drawRect(bx,by,bx+badgeW,by+badgeH,p);
        p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);float number=badgeH*.85f;
        p.setTextSize(number);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        canvas.drawText("4",bx+badgeW/2,by+badgeH/2-(p.ascent()+p.descent())/2,p);
        p.setColor(Color.BLACK);p.setTextAlign(Paint.Align.LEFT);
        int ruler=paper.rulerMm(),dots=(int)Math.round(ruler*203/25.4);
        float x=(w-dots)/2f,y=h-45;
        text(canvas,p,ruler+" mm",x,y-12,small,false);
        canvas.drawRect(x,y-1,x+dots+1,y+1,p);
        for(int i=0;i<=ruler/10;i++){float xx=x+dots*(i*10f/ruler);canvas.drawRect(xx-1,y-6,xx+1,y+6,p);}
        text(canvas,p,host,24,h-23,Math.min(16,inner/20),false);
    }
    private static void drawLegacy(Canvas canvas,String host) {
        canvas.drawColor(Color.WHITE);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.BLACK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);
        canvas.drawRect(16,16,798,590,p);
        p.setStyle(Paint.Style.FILL);
        text(canvas,p,"TSC MH261T",40,67,30,true);
        text(canvas,p,"PRINT TEST",608,62,22,true);
        text(canvas,p,"102 x 76 mm  /  203 DPI  /  TOP",40,101,20,false);
        canvas.drawRect(40,121,775,123,p);
        text(canvas,p,"SCAN QR",64,162,18,true);
        try {
            BitMatrix qr=new QRCodeWriter().encode(QR_TEXT,BarcodeFormat.QR_CODE,0,0);
            // Integer modules and the encoder's four-module quiet zone preserve scanner edges.
            int module=8;
            p.setAntiAlias(false);
            for(int y=0;y<qr.getHeight();y++)for(int x=0;x<qr.getWidth();x++)if(qr.get(x,y))
                canvas.drawRect(64+x*module,168+y*module,64+(x+1)*module,168+(y+1)*module,p);
            p.setAntiAlias(true);
        } catch(WriterException e) {throw new IllegalStateException("Cannot create diagnostic QR",e);}
        text(canvas,p,QR_TEXT,64,435,20,false);
        text(canvas,p,"REVERSE TEXT",460,162,18,true);
        text(canvas,p,"SHELF",460,209,26,true);
        canvas.drawRect(460,232,743,406,p);
        p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);
        text(canvas,p,"4",601,375,150,true);
        p.setColor(Color.BLACK);p.setTextAlign(Paint.Align.LEFT);
        text(canvas,p,"WHITE ON BLACK",460,435,20,false);
        text(canvas,p,"80 mm - measure between end marks",88,510,20,false);
        canvas.drawRect(88,539,728,541,p);
        for(int i=0;i<=8;i++) {
            float x=88+i*(639f/8);
            canvas.drawRect(x-1,i==0||i==8?526:532,x+1,552,p);
        }
        text(canvas,p,host+"   |   Border inset: 2 mm",40,573,18,false);
    }
    private static void text(Canvas canvas,Paint p,String text,float x,float y,float size,boolean bold) {
        p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        canvas.drawText(text,x,y,p);
    }
}
