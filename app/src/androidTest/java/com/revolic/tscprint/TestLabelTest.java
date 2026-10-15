package com.revolic.tscprint;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.revolic.tscprint.core.Cancellation;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.file.Files;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TestLabelTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private boolean black(byte[] bytes,int x,int y){return (bytes[60+y*102+x/8]&(0x80>>>(x%8)))==0;}
    @Test public void printableTestHasFourBordersScaleBadgeAndDecodableQr() throws Exception {
        File pdf=TestLabel.createPdf(PrintFiles.directory(context),"192.168.70.37",new Cancellation());
        File out=File.createTempFile("test-label-",".tspl",context.getCacheDir());
        try {
            assertEquals(1,PdfSpooler.stage(pdf,out,null,1,false,new Cancellation()));
            byte[] bytes=Files.readAllBytes(out.toPath());
            assertEquals(60+102*607+13,bytes.length);
            // Two millimetres inset, in printer dots. Test final binary, not just canvas.
            assertTrue(black(bytes,16,300));assertTrue(black(bytes,798,300));
            assertTrue(black(bytes,400,16));assertTrue(black(bytes,400,590));
            assertTrue(black(bytes,88,540));assertTrue(black(bytes,727,540));
            assertFalse(black(bytes,80,540));assertFalse(black(bytes,735,540));
            int dark=0,white=0;
            for(int y=250;y<390;y++)for(int x=490;x<710;x++) {
                if(black(bytes,x,y))dark++;else white++;
            }
            assertTrue("Black badge survives",dark>20000);
            assertTrue("White numeral survives",white>1000);
            int[] pixels=new int[815*607];
            for(int y=0;y<607;y++)for(int x=0;x<815;x++)pixels[y*815+x]=black(bytes,x,y)?0xff000000:0xffffffff;
            Result decoded=new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(815,607,pixels))));
            assertEquals(TestLabel.QR_TEXT,decoded.getText());
            Bitmap image=Bitmap.createBitmap(pixels,815,607,Bitmap.Config.ARGB_8888);
            try(OutputStream stream=new FileOutputStream(new File(context.getExternalFilesDir(null),"test-label-raster.png"))){image.compress(Bitmap.CompressFormat.PNG,100,stream);}finally{image.recycle();}
        } finally {pdf.delete();out.delete();}
    }
    @Test public void cancelledGenerationLeavesNoTemporaryPdf() throws Exception {
        File dir=new File(context.getCacheDir(),"test-label-cancel");dir.mkdir();
        Cancellation cancel=new Cancellation();cancel.cancel();
        try {assertThrows(InterruptedIOException.class,()->TestLabel.createPdf(dir,"192.168.70.37",cancel));assertEquals(0,dir.list().length);}
        finally {dir.delete();}
    }
}
