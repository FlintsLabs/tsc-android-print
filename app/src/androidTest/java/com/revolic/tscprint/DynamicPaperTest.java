package com.revolic.tscprint;
import android.content.*;
import android.graphics.*;
import android.print.PrintAttributes;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.revolic.tscprint.core.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DynamicPaperTest {
 private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
 @Test public void a5HasPhysicalPortraitBordersAndQr() throws Exception {check(PaperSize.A5,false);}
 @Test public void customDecimalHasExactSizeBordersAndQr() throws Exception {check(PaperSize.parse("100.5","150.2"),false);}
 @Test public void landscapeDocumentRotatesBackOntoA5Stock() throws Exception {check(PaperSize.A5,true);}
 @Test public void smallestCustomLabelFitsDiagnostic() throws Exception {check(PaperSize.parse("20","20"),false);}
 private void check(PaperSize paper,boolean rotate) throws Exception {
  File pdf=TestLabel.createPdf(PrintFiles.directory(context),"192.168.70.37",paper,rotate,new Cancellation());
  File out=File.createTempFile("dynamic-",".tspl",context.getCacheDir());
  try {
   assertEquals(1,PdfSpooler.stage(pdf,out,null,1,paper,rotate,new Cancellation()));
   byte[] data=Files.readAllBytes(out.toPath());
   String expected="SIZE "+paper.widthMm()+" mm,"+paper.heightMm()+" mm\r\nDIRECTION 1,0\r\nCLS\r\nBITMAP 0,0,"+paper.stride()+","+paper.heightDots()+",1,";
   assertTrue(new String(data,StandardCharsets.ISO_8859_1).startsWith(expected));
   int head=expected.length(),w=paper.widthDots(),h=paper.heightDots();
   assertEquals(head+paper.stride()*h+13,data.length);
   assertTrue(black(data,head,paper,16,h/2));assertTrue(black(data,head,paper,w-17,h/2));
   assertTrue(black(data,head,paper,w/2,16));assertTrue(black(data,head,paper,w/2,h-17));
   int[] pixels=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++)pixels[y*w+x]=black(data,head,paper,x,y)?Color.BLACK:Color.WHITE;
   Result qr=new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(w,h,pixels))));
   assertEquals(TestLabel.QR_TEXT,qr.getText());
   Bitmap image=Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);
   try(OutputStream stream=new FileOutputStream(new File(context.getExternalFilesDir(null),"paper-"+paper.id()+"-"+rotate+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,stream);}finally{image.recycle();}
  } finally {pdf.delete();out.delete();}
 }
 private boolean black(byte[] d,int head,PaperSize p,int x,int y){return (d[head+y*p.stride()+x/8]&(0x80>>>(x%8)))==0;}
 @Test public void androidMediaIdResolvesPhysicalStockRegardlessOfOrientation() {
  PrintAttributes.MediaSize media=PaperSettings.media(PaperSize.A5).asLandscape();
  assertEquals(PaperSize.A5,PaperSettings.fromMedia(media));assertTrue(PaperSettings.rotated(PaperSize.A5,media));
  assertFalse(PaperSettings.rotated(PaperSize.A5,media.asPortrait()));
  assertThrows(IllegalArgumentException.class,()->PaperSettings.fromMedia(new PrintAttributes.MediaSize(PaperSize.A5.id(),"bad",1000,2000)));
 }
}
