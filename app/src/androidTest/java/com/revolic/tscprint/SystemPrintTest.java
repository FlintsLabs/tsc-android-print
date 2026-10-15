package com.revolic.tscprint;

import android.content.*;
import com.revolic.tscprint.core.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.print.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SystemPrintTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final String spooler="com.android.printspooler";
    @Test public void setupStatusMatchesFramework() {
        try(ActivityScenario<MainActivity> scene=ActivityScenario.launch(MainActivity.class)) {
            boolean enabled=context.getSystemService(PrintManager.class).isPrintServiceEnabled(new ComponentName(context,TscPrintService.class));
            assertTrue(device.wait(Until.hasObject(By.text(enabled?"บริการพิมพ์เปิดอยู่":"ยังไม่ได้เปิดบริการพิมพ์")),5000));
        }
    }
    @Test public void systemPrintDispatchesPdfToLocalSocket() throws Exception { runPrint(false,false); }
    @Test public void selectedPagesAndCopiesReachSocketInOrder() throws Exception { runPrint(true,false); }
    @Test public void printTestScreenRequiresConfirmationAndSendsOneLabel() throws Exception { runPrint(false,true); }
    @Test public void a5PrintTestSendsPortraitPhysicalStock() throws Exception {runPrint(false,true,PaperSize.A5,null);}
    @Test public void customPrintTestSendsSavedDecimalSize() throws Exception {runPrint(false,true,PaperSize.parse("100.5","150.2"),null);}
    @Test public void a5NormalPdfUsesSelectedStock() throws Exception {runPrint(false,false,PaperSize.A5,null);}
    @Test public void changingSystemPaperSizeRegeneratesTestLabel() throws Exception {runPrint(false,true,PaperSize.LABEL,PaperSize.A5);}
    private void runPrint(boolean subset,boolean diagnostic) throws Exception {runPrint(subset,diagnostic,PaperSize.LABEL,null);}
    private void runPrint(boolean subset,boolean diagnostic,PaperSize initial,PaperSize override) throws Exception {
        String savedPaper=PaperSettings.selected(context).id();
        java.util.Set<String> savedCustom=new java.util.HashSet<>(PaperSettings.preferences(context).getStringSet("custom",java.util.Collections.emptySet()));
        PaperSettings.addAndSelect(context,initial);
        PaperSize paper=override==null?initial:override;
        String original=PrinterSettings.host(context);PrinterSettings.save(context,"127.0.0.1");
        ExecutorService listener=Executors.newSingleThreadExecutor();
        File pdf=fixture();
        try(ServerSocket server=new ServerSocket(9100,10,InetAddress.getByName("127.0.0.1"));ActivityScenario<MainActivity> scene=ActivityScenario.launch(MainActivity.class)) {
            server.setSoTimeout(40000);
            Future<byte[]> received=listener.submit(()->{
                while(true) try(Socket socket=server.accept()) {
                    socket.setSoTimeout(15000);ByteArrayOutputStream bytes=new ByteArrayOutputStream();socket.getInputStream().transferTo(bytes);
                    if(bytes.size()>0) return bytes.toByteArray();
                }
            });
            if (diagnostic) {
                device.wait(Until.findObject(By.text("Print Test")),5000).click();
                UiScrollable content=new UiScrollable(new UiSelector().scrollable(true));
                content.scrollTextIntoView("พิมพ์ฉลากทดสอบ");
                device.wait(Until.findObject(By.text("พิมพ์ฉลากทดสอบ")),5000).click();
            } else scene.onActivity(activity->{
                PrintAttributes attrs=new PrintAttributes.Builder().setMediaSize(PaperSettings.media(initial)).setResolution(new PrintAttributes.Resolution("203","203 DPI",203,203))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS).setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME).build();
                activity.getSystemService(PrintManager.class).print("TSC integration capture",new PdfPrintAdapter(pdf,3,"Fixture.pdf"),attrs);
            });
            UiObject2 destination=device.wait(Until.findObject(By.res(spooler,"destination_spinner")),10000);
            assertNotNull("System Print opened",destination);destination.click();
            UiObject2 printer=device.wait(Until.findObject(By.res(spooler,"title").text("TSC MH261T")),5000);
            if(printer==null) {
                UiObject2 all=device.wait(Until.findObject(By.textContains("All printers")),3000);assertNotNull("All printers entry",all);all.click();
                printer=device.wait(Until.findObject(By.text("TSC MH261T")),10000);
            }
            assertNotNull("TSC discovered",printer);
            boolean picked=false;
            for(int attempt=0;attempt<20 && !picked;attempt++) {
                try {
                    UiObject2 option=device.wait(Until.findObject(By.text("TSC MH261T")),500);
                    if(option!=null && option.getParent().getParent().isEnabled()) {option.click();picked=true;}
                } catch(StaleObjectException ignored) { }
                if(!picked)android.os.SystemClock.sleep(100);
            }
            assertTrue("Online printer selectable",picked);
            if(override!=null) {
                device.wait(Until.findObject(By.res(spooler,"expand_collapse_handle")),5000).click();
                device.dumpWindowHierarchy(new File(context.getExternalFilesDir(null),"paper-options.xml"));
                UiObject2 media=device.wait(Until.findObject(By.res(spooler,"paper_size_spinner")),5000);
                assertNotNull("Paper size selector",media);media.click();
                device.wait(Until.findObject(By.text(override.label())),5000).click();
                device.findObject(By.res(spooler,"expand_collapse_handle")).click();
            }
            if(subset) {
                device.wait(Until.findObject(By.res(spooler,"expand_collapse_handle")),5000).click();
                device.dumpWindowHierarchy(new File(context.getExternalFilesDir(null),"options.xml"));
                UiObject2 copies=device.wait(Until.findObject(By.res(spooler,"copies_edittext")),3000);
                assertNotNull("Copies field",copies);copies.setText("2");
                device.findObject(By.res(spooler,"range_options_spinner")).click();
                device.wait(Until.findObject(By.textContains("Range")),3000).click();
                device.wait(Until.findObject(By.res(spooler,"page_range_edittext")),3000).setText("2-3");
                device.findObject(By.res(spooler,"expand_collapse_handle")).click();
            }
            UiObject2 print=device.wait(Until.findObject(By.res(spooler,"print_button").enabled(true)),10000);
            device.dumpWindowHierarchy(new File(context.getExternalFilesDir(null),"before-print.xml"));
            device.takeScreenshot(new File(context.getExternalFilesDir(null),"before-print.png"));
            assertNotNull("Print button enabled",print);
            assertFalse("No label sent before user confirmation",received.isDone());
            if(diagnostic) {
                device.takeScreenshot(new File(context.getExternalFilesDir(null),"test-label-system-preview.png"));
            }
            print.click();
            UiObject2 allow=device.wait(Until.findObject(By.res("android","button1")),2000);
            if(allow!=null)allow.click();
            byte[] actual=received.get(30,TimeUnit.SECONDS);
            String stream=new String(actual,StandardCharsets.ISO_8859_1);
            assertEquals(diagnostic?1:subset?4:3,stream.split("PRINT 1,1",-1).length-1);
            assertFalse(stream.contains("SHIFT"));assertFalse(stream.contains("REFERENCE"));
            String header="SIZE "+paper.widthMm()+" mm,"+paper.heightMm()+" mm\r\nDIRECTION 1,0\r\nCLS\r\nBITMAP 0,0,"+paper.stride()+","+paper.heightDots()+",1,";
            assertTrue(stream.startsWith(header));
            int frame=header.length()+paper.stride()*paper.heightDots()+13;
            assertEquals((diagnostic?1:subset?4:3)*frame,actual.length);
            if (diagnostic) {
                File expectedPdf=TestLabel.createPdf(PrintFiles.directory(context),"127.0.0.1",paper,false,new com.revolic.tscprint.core.Cancellation());
                File expectedStream=File.createTempFile("diagnostic-expected-",".tspl",context.getCacheDir());
                try {
                    PdfSpooler.stage(expectedPdf,expectedStream,null,1,paper,false,new com.revolic.tscprint.core.Cancellation());
                    assertArrayEquals("System Print preserves diagnostic content",java.nio.file.Files.readAllBytes(expectedStream.toPath()),actual);
                } finally {expectedPdf.delete();expectedStream.delete();}
            }
            int[] expected=subset?new int[]{1,2,1,2}:new int[]{0,1,2};
            if (!diagnostic) for(int i=0;i<expected.length;i++)for(int marker=0;marker<3;marker++) {
                PrintLayout fit=PrintLayout.fit(289,216,paper,false);
                int x=(int)(fit.offsetX+(50+marker*60)*fit.scale),y=(int)(fit.offsetY+90*fit.scale);
                boolean black=(actual[i*frame+header.length()+y*paper.stride()+x/8]&(0x80>>>(x%8)))==0;
                assertEquals("Page identity/order "+i+" marker "+marker,marker==expected[i],black);
            }
            try(OutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),diagnostic?"test-label-"+paper.id()+".tspl":subset?"subset-capture.tspl":"all-capture.tspl"))){out.write(actual);}
        } catch(Exception|AssertionError e) {
            device.dumpWindowHierarchy(new File(context.getExternalFilesDir(null),"print-failure.xml"));
            device.takeScreenshot(new File(context.getExternalFilesDir(null),"print-failure.png"));throw e;
        } finally {PaperSettings.preferences(context).edit().putString("selected",savedPaper).putStringSet("custom",savedCustom).commit();PrinterSettings.save(context,original);listener.shutdownNow();pdf.delete();device.pressBack();}
    }
    private File fixture() throws Exception {
        File file=File.createTempFile("system-fixture-",".pdf",context.getCacheDir());PdfDocument doc=new PdfDocument();
        try {
            for(int i=0;i<3;i++) {
                PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(289,216,i).create());
                Paint p=new Paint();p.setColor(Color.BLACK);p.setTextSize(20);page.getCanvas().drawText("TSC capture page "+(i+1),20,45,p);
                page.getCanvas().drawRect(40+i*60,80,60+i*60,100,p);
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);page.getCanvas().drawRect(8,8,281,208,p);doc.finishPage(page);
            }
            try(OutputStream out=new FileOutputStream(file)){doc.writeTo(out);}
        } finally {doc.close();}
        return file;
    }
}
