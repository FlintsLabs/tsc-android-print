package com.revolic.tscprint;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.revolic.tscprint.core.Cancellation;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.file.Files;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PipelineTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private static final int HEADER=60;
    private File document(boolean portrait) throws Exception {
        File pdf=File.createTempFile("fixture-",".pdf",context.getCacheDir());
        PdfDocument doc=new PdfDocument();
        try {
            for(int i=0;i<3;i++) {
                PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(portrait?216:289,portrait?289:216,i).create());
                Paint paint=new Paint();paint.setColor(Color.BLACK);
                if(i==0) page.getCanvas().drawRect(10,10,30,30,paint);
                else if(i==1) page.getCanvas().drawRect(200,10,210,30,paint);
                else {paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);page.getCanvas().drawRect(8,8,page.getInfo().getPageWidth()-8,page.getInfo().getPageHeight()-8,paint);}
                doc.finishPage(page);
            }
            try(OutputStream out=new FileOutputStream(pdf)){doc.writeTo(out);}
        }
        finally {doc.close();}
        return pdf;
    }
    private int payload(byte[] b) {for(int i=0;i<b.length-2;i++)if(b[i]==',' && b[i+1]=='1' && b[i+2]==',')return i+3;throw new AssertionError("No header");}
    private boolean black(byte[] b,int head,int x,int y){return (b[head+y*102+x/8] & (0x80>>>(x%8)))==0;}
    @Test public void selectedPageOnlyAndFourBordersSurviveRaster() throws Exception {
        File pdf=document(false),out=File.createTempFile("output-",".tspl",context.getCacheDir());
        try {
            assertEquals(1,PdfSpooler.stage(pdf,out,new int[][]{{1,1}},1,false,new Cancellation()));
            byte[] bytes=Files.readAllBytes(out.toPath());int h=payload(bytes);
            assertEquals(h+102*607+13,bytes.length);
            assertFalse(black(bytes,h,50,50));assertTrue(black(bytes,h,575,50));
            PdfSpooler.stage(pdf,out,new int[][]{{2,2}},1,false,new Cancellation());bytes=Files.readAllBytes(out.toPath());h=payload(bytes);
            assertTrue(black(bytes,h,23,300));assertTrue(black(bytes,h,790,300));
            assertTrue(black(bytes,h,400,23));assertTrue(black(bytes,h,400,584));
        } finally {pdf.delete();out.delete();}
    }
    @Test public void portraitRotatesClockwiseAndPadsWhite() throws Exception {
        File pdf=document(true),out=File.createTempFile("rotate-",".tspl",context.getCacheDir());
        try { PdfSpooler.stage(pdf,out,new int[][]{{0,0}},1,true,new Cancellation());byte[] b=Files.readAllBytes(out.toPath());int h=payload(b);
            assertTrue(black(b,h,755,50));assertFalse(black(b,h,50,50));
            for(int y=0;y<607;y++)assertEquals(1,b[h+y*102+101]&1);
        } finally {pdf.delete();out.delete();}
    }
    @Test public void corruptPdfFailsBeforeNetwork() throws Exception {
        File pdf=File.createTempFile("bad-",".pdf",context.getCacheDir()),out=File.createTempFile("bad-",".tspl",context.getCacheDir());
        try {Files.write(pdf.toPath(),new byte[]{1,2,3});assertThrows(IOException.class,()->PdfSpooler.stage(pdf,out,null,1,false,new Cancellation()));assertEquals(0,out.length());}
        finally {pdf.delete();out.delete();}
    }
    @Test public void cancelledCopyDeletesTemporaryPdf() throws Exception {
        Cancellation cancel=new Cancellation();cancel.cancel();File dir=new File(context.getCacheDir(),"cancel-test");dir.mkdir();
        try {assertThrows(InterruptedIOException.class,()->PdfSpooler.copyPdf(new ByteArrayInputStream(new byte[100]),dir,cancel));assertEquals(0,dir.list().length);}finally{dir.delete();}
    }

    @Test public void cancellingCopyClosesBlockedProviderStream() throws Exception {
        Cancellation cancel=new Cancellation();java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1);
        class BlockedInput extends InputStream {
            boolean closed;
            @Override public synchronized int read() throws IOException {entered.countDown();while(!closed)try{wait();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException();}throw new IOException("closed");}
            @Override public synchronized void close(){closed=true;notifyAll();}
        }
        BlockedInput in=new BlockedInput();java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.Future<?> copying=worker.submit(()->{try{PdfSpooler.copyPdf(in,context.getCacheDir(),cancel);fail("Expected cancellation");}catch(IOException expected){}});
        try {assertTrue(entered.await(2,java.util.concurrent.TimeUnit.SECONDS));cancel.cancel();copying.get(2,java.util.concurrent.TimeUnit.SECONDS);assertTrue(in.closed);}
        finally{in.close();worker.shutdownNow();}
    }

    @Test public void newProcessCleanupRemovesOnlyOwnedTemporaryFiles() throws Exception {
        File dir=PrintFiles.directory(context),stale=new File(dir,"tsc-pdf-old.pdf"),other=new File(context.getCacheDir(),"unrelated.txt");
        Files.write(stale.toPath(),new byte[]{1});Files.write(other.toPath(),new byte[]{2});
        try {PrintFiles.cleanOnProcessStart(context);assertFalse(stale.exists());assertTrue(other.exists());}
        finally {stale.delete();other.delete();}
    }
}
