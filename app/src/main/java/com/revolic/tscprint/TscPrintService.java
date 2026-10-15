package com.revolic.tscprint;

import android.content.Intent;
import android.app.PendingIntent;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.print.*;
import android.printservice.*;
import android.printservice.PrintJob;
import com.revolic.tscprint.core.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public final class TscPrintService extends PrintService {
    public static final String PRINTER_ID="mh261t";
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ExecutorService probes=Executors.newSingleThreadExecutor();
    private final Map<PrintJobId,Task> tasks=new HashMap<>();
    private boolean destroyed;
    private static final class Task {
        final PrintJob job;final Cancellation cancel=new Cancellation();
        Task(PrintJob job) { this.job=job; }
    }
    @Override protected void onConnected() {
        for(PrintJob job:getActivePrintJobs()) {
            if(tasks.containsKey(job.getId())) continue;
            if(job.isQueued()) onPrintJobQueued(job);
            else if(job.isStarted() || job.isBlocked()) job.fail("งานก่อนหน้าถูกขัดจังหวะ โปรดตรวจฉลากก่อนสั่งซ้ำ");
        }
    }
    @Override protected PrinterDiscoverySession onCreatePrinterDiscoverySession() { return new Discovery(); }
    @Override protected void onPrintJobQueued(PrintJob job) {
        if(tasks.containsKey(job.getId())) return;
        if(!PrinterSettings.canUseNetwork(this)) { job.fail("เปิด TSC Direct Print แล้วอนุญาตเครือข่ายภายในก่อนพิมพ์");return; }
        PrintJobInfo info=job.getInfo();
        if(info.getPrinterId()==null || !PRINTER_ID.equals(info.getPrinterId().getLocalId())) { job.fail("เครื่องพิมพ์ไม่ตรงกับ TSC MH261T");return; }
        final PaperSize paper;final boolean rotate;
        try {
            PrintAttributes.MediaSize media=info.getAttributes()==null?null:info.getAttributes().getMediaSize();
            paper=PaperSettings.fromMedia(media);rotate=PaperSettings.rotated(paper,media);
        } catch(IllegalArgumentException e) {job.fail(e.getMessage());return;}
        if(!job.start()) return;
        Task task=new Task(job);tasks.put(job.getId(),task);
        ParcelFileDescriptor data=job.getDocument().getData();
        if(data==null) { tasks.remove(job.getId());job.fail("อ่าน PDF จากระบบไม่ได้");return; }
        try { task.cancel.register(data); } catch(IOException e) { job.fail("อ่าน PDF ไม่ได้");tasks.remove(job.getId());return; }
        PageRange[] selected=info.getPages();int[][] ranges=null;
        if(selected!=null) { ranges=new int[selected.length][2];for(int i=0;i<selected.length;i++) {ranges[i][0]=selected[i].getStart();ranges[i][1]=selected[i].getEnd();} }
        final int[][] pageRanges=ranges;final int copies=info.getCopies();final String host=PrinterSettings.host(this);
        job.setStatus("กำลังเตรียมฉลาก");
        worker.execute(()->runTask(task,data,pageRanges,copies,paper,rotate,host));
    }
    private void runTask(Task task,ParcelFileDescriptor data,int[][] ranges,int copies,PaperSize paper,boolean rotate,String host) {
        File pdf=null,tspl=null;String error=null;boolean sending=false;
        try {
            try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(data)) { pdf=PdfSpooler.copyPdf(in,PrintFiles.directory(this),task.cancel); }
            finally { task.cancel.unregister(data); }
            task.cancel.check();tspl=File.createTempFile("tsc-job-",".tspl",PrintFiles.directory(this));
            int pages=PdfSpooler.stage(pdf,tspl,ranges,copies,paper,rotate,task.cancel);
            task.cancel.check();
            if(!PrinterSettings.canUseNetwork(this)) throw new SecurityException("ไม่ได้รับสิทธิ์เครือข่ายภายใน");
            main.post(()->{if(!destroyed && !task.cancel.isCancelled()) task.job.setStatus("กำลังส่ง "+(pages*copies)+" ฉลากไปยังเครื่องพิมพ์");});
            sending=true;JobTransport.send(tspl,copies,host,PrinterSettings.PORT,task.cancel,5000,15000);
        } catch(Exception e) {
            error=(sending ? "ส่งข้อมูลไม่ครบ บางฉลากอาจพิมพ์แล้ว โปรดตรวจก่อนสั่งซ้ำ: " : "เตรียมงานไม่ได้: ")+safeMessage(e);
        } finally { if(pdf!=null) pdf.delete();if(tspl!=null) tspl.delete(); }
        final String failure=error;
        main.post(()->{
            tasks.remove(task.job.getId());if(destroyed) return;
            if(task.cancel.isCancelled()) { if(!task.job.isCancelled()) task.job.cancel(); }
            else if(failure!=null) task.job.fail(failure);
            else { task.job.setStatus("ส่งข้อมูลแล้ว โปรดตรวจฉลากที่เครื่อง");task.job.complete(); }
        });
    }
    private static String safeMessage(Exception e) { String m=e.getMessage();return m==null ? e.getClass().getSimpleName() : m; }
    @Override protected void onRequestCancelPrintJob(PrintJob job) {
        Task task=tasks.get(job.getId());if(task!=null) task.cancel.cancel();job.cancel();
    }
    @Override public void onDestroy() {
        destroyed=true;for(Task task:tasks.values()) task.cancel.cancel();
        worker.shutdownNow();probes.shutdownNow();super.onDestroy();
    }
    public static PrintAttributes.MediaSize stock() { return PaperSettings.media(PaperSize.LABEL); }
    private final class Discovery extends PrinterDiscoverySession {
        private boolean closed;private int generation;private boolean discovering;
        private final android.content.SharedPreferences.OnSharedPreferenceChangeListener paperChanged=(prefs,key)->{if(discovering && !closed)refresh();};
        Discovery() {PaperSettings.preferences(TscPrintService.this).registerOnSharedPreferenceChangeListener(paperChanged);}
        private PrinterInfo info(String host,boolean ready) {
            PrinterId id=generatePrinterId(PRINTER_ID);
            PrinterCapabilitiesInfo.Builder builder=new PrinterCapabilitiesInfo.Builder(id);
            PaperSize selected=PaperSettings.selected(TscPrintService.this);
            for(PaperSize paper:PaperSettings.all(TscPrintService.this))builder.addMediaSize(PaperSettings.media(paper),paper.equals(selected));
            PrinterCapabilitiesInfo capabilities=builder
                .addResolution(new PrintAttributes.Resolution("203","203 DPI",203,203),true)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .setColorModes(PrintAttributes.COLOR_MODE_MONOCHROME,PrintAttributes.COLOR_MODE_MONOCHROME)
                .setDuplexModes(PrintAttributes.DUPLEX_MODE_NONE,PrintAttributes.DUPLEX_MODE_NONE).build();
            PendingIntent settings=PendingIntent.getActivity(TscPrintService.this,0,new Intent(TscPrintService.this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            return new PrinterInfo.Builder(id,"TSC MH261T",ready?PrinterInfo.STATUS_IDLE:PrinterInfo.STATUS_UNAVAILABLE)
                .setDescription(host+":9100 · 203 DPI").setCapabilities(capabilities).setInfoIntent(settings).build();
        }
        private void refresh() {
            String host=PrinterSettings.host(TscPrintService.this);int request=++generation;
            if(!PrinterSettings.canUseNetwork(TscPrintService.this)) {addPrinters(Collections.singletonList(info(host,false)));return;}
            // The fixed endpoint remains selectable while reachability is checked.
            addPrinters(Collections.singletonList(info(host,true)));
            probes.execute(()->{
                boolean online=false;
                try(Socket socket=new Socket()) {socket.connect(new InetSocketAddress(host,PrinterSettings.PORT),2500);online=true;}catch(IOException|SecurityException ignored) { }
                boolean result=online;
                main.post(()->{if(!closed && request==generation) addPrinters(Collections.singletonList(info(host,result)));});
            });
        }
        @Override public void onStartPrinterDiscovery(List<PrinterId> priorityList) {discovering=true;refresh();}
        @Override public void onStopPrinterDiscovery() {discovering=false;generation++;}
        @Override public void onValidatePrinters(List<PrinterId> ids) {refresh();}
        @Override public void onStartPrinterStateTracking(PrinterId id) {refresh();}
        @Override public void onStopPrinterStateTracking(PrinterId id) { }
        @Override public void onDestroy() {closed=true;generation++;PaperSettings.preferences(TscPrintService.this).unregisterOnSharedPreferenceChangeListener(paperChanged);}
    }
}
