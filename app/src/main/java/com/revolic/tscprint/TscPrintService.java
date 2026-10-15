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
    public static final String PRINTER_ID=PrinterSettings.LEGACY_ID;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final PrinterJobQueue worker=new PrinterJobQueue(3,32);
    private final ExecutorService probes=Executors.newFixedThreadPool(3);
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
        final PrinterProfile printer;
        try {
            if(info.getPrinterId()==null || !generatePrinterId(info.getPrinterId().getLocalId()).equals(info.getPrinterId())) {
                job.fail("เครื่องพิมพ์ไม่ตรงกับบริการ TSC");return;
            }
            printer=PrinterSettings.find(this,info.getPrinterId().getLocalId());
            if(printer==null) {job.fail("เครื่องพิมพ์นี้ถูกลบแล้ว กรุณาเลือกเครื่องใหม่");return;}
        } catch(IllegalStateException e) {job.fail(e.getMessage());return;}
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
        final int[][] pageRanges=ranges;final int copies=info.getCopies();
        job.setStatus("รอคิว "+printer.name);
        try {worker.submit(printer.endpoint(),()->runTask(task,data,pageRanges,copies,paper,rotate,printer));}
        catch(RejectedExecutionException e) {tasks.remove(job.getId());task.cancel.cancel();job.fail(e.getMessage());}
    }
    private void runTask(Task task,ParcelFileDescriptor data,int[][] ranges,int copies,PaperSize paper,boolean rotate,PrinterProfile printer) {
        File pdf=null,tspl=null;String error=null;boolean sending=false;
        try {
            try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(data)) { pdf=PdfSpooler.copyPdf(in,PrintFiles.directory(this),task.cancel); }
            finally { task.cancel.unregister(data); }
            task.cancel.check();tspl=File.createTempFile("tsc-job-",".tspl",PrintFiles.directory(this));
            int pages=PdfSpooler.stage(pdf,tspl,ranges,copies,paper,rotate,task.cancel);
            task.cancel.check();
            if(!PrinterSettings.canUseNetwork(this)) throw new SecurityException("ไม่ได้รับสิทธิ์เครือข่ายภายใน");
            main.post(()->{if(!destroyed && !task.cancel.isCancelled()) task.job.setStatus("กำลังส่ง "+(pages*copies)+" ฉลากไปยังเครื่องพิมพ์");});
            sending=true;JobTransport.send(tspl,copies,printer.host,printer.port,task.cancel,5000,15000);
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
        worker.close();probes.shutdownNow();super.onDestroy();
    }
    public static PrintAttributes.MediaSize stock() { return PaperSettings.media(PaperSize.LABEL); }
    private final class Discovery extends PrinterDiscoverySession {
        private boolean closed,discovering;
        private final Set<String> tracked=new HashSet<>(),probing=new HashSet<>();
        private final Map<String,Boolean> reachable=new HashMap<>();
        private final Runnable poll=this::refresh;
        private final android.content.SharedPreferences.OnSharedPreferenceChangeListener changed=(prefs,key)->{if(active())refresh();};
        Discovery() {
            PaperSettings.preferences(TscPrintService.this).registerOnSharedPreferenceChangeListener(changed);
            PrinterSettings.preferences(TscPrintService.this).registerOnSharedPreferenceChangeListener(changed);
        }
        private boolean active() {return !closed && (discovering || !tracked.isEmpty());}
        private PrinterInfo info(PrinterProfile printer,boolean ready) {
            PrinterId id=generatePrinterId(printer.id);
            PrinterCapabilitiesInfo.Builder builder=new PrinterCapabilitiesInfo.Builder(id);
            PaperSize selected=PaperSize.fromId(printer.paperId);
            List<PaperSize> sizes=new ArrayList<>(PaperSettings.all(TscPrintService.this));
            if(!sizes.contains(selected))sizes.add(selected);
            for(PaperSize paper:sizes)builder.addMediaSize(PaperSettings.media(paper),paper.equals(selected));
            PrinterCapabilitiesInfo capabilities=builder
                .addResolution(new PrintAttributes.Resolution("203","203 DPI",203,203),true)
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .setColorModes(PrintAttributes.COLOR_MODE_MONOCHROME,PrintAttributes.COLOR_MODE_MONOCHROME)
                .setDuplexModes(PrintAttributes.DUPLEX_MODE_NONE,PrintAttributes.DUPLEX_MODE_NONE).build();
            Intent intent=new Intent(TscPrintService.this,MainActivity.class).setData(android.net.Uri.parse("tsc-printer:"+printer.id));
            PendingIntent settings=PendingIntent.getActivity(TscPrintService.this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            return new PrinterInfo.Builder(id,printer.name,ready?PrinterInfo.STATUS_IDLE:PrinterInfo.STATUS_UNAVAILABLE)
                .setDescription(printer.endpoint()+" · 203 DPI").setCapabilities(capabilities).setInfoIntent(settings).build();
        }
        private void refresh() {
            if(closed)return;
            main.removeCallbacks(poll);
            final PrinterCatalog catalog;
            try {catalog=PrinterSettings.catalog(TscPrintService.this);}
            catch(IllegalStateException e) {
                List<PrinterId> ids=new ArrayList<>();for(PrinterInfo p:getPrinters())ids.add(p.getId());removePrinters(ids);return;
            }
            List<PrinterId> removed=new ArrayList<>();
            for(PrinterInfo old:getPrinters())if(catalog.find(old.getId().getLocalId())==null)removed.add(old.getId());
            if(!removed.isEmpty())removePrinters(removed);
            boolean allowed=PrinterSettings.canUseNetwork(TscPrintService.this);
            List<PrinterInfo> infos=new ArrayList<>();
            for(PrinterProfile p:catalog.all())infos.add(info(p,allowed && reachable.getOrDefault(p.endpoint(),true)));
            addPrinters(infos);
            if(allowed)for(PrinterProfile p:catalog.all()) {
                if(!probing.add(p.endpoint()))continue;
                probes.execute(()->{
                    boolean online=false;
                    try(Socket socket=new Socket()) {socket.connect(new InetSocketAddress(p.host,p.port),2500);online=true;}
                    catch(IOException|SecurityException ignored) { }
                    boolean result=online;
                    main.post(()->{
                        probing.remove(p.endpoint());if(closed)return;
                        reachable.put(p.endpoint(),result);
                        if(!active())return;
                        try {
                            PrinterProfile current=PrinterSettings.find(TscPrintService.this,p.id);
                            if(current!=null && current.endpoint().equals(p.endpoint()))addPrinters(Collections.singletonList(info(current,result && PrinterSettings.canUseNetwork(TscPrintService.this))));
                        } catch(IllegalStateException ignored) { }
                    });
                });
            }
            if(active())main.postDelayed(poll,5000);
        }
        @Override public void onStartPrinterDiscovery(List<PrinterId> priorityList) {discovering=true;refresh();}
        @Override public void onStopPrinterDiscovery() {discovering=false;if(!active())main.removeCallbacks(poll);}
        @Override public void onValidatePrinters(List<PrinterId> ids) {refresh();}
        @Override public void onStartPrinterStateTracking(PrinterId id) {tracked.add(id.getLocalId());refresh();}
        @Override public void onStopPrinterStateTracking(PrinterId id) {tracked.remove(id.getLocalId());if(!active())main.removeCallbacks(poll);}
        @Override public void onDestroy() {
            closed=true;main.removeCallbacks(poll);
            PaperSettings.preferences(TscPrintService.this).unregisterOnSharedPreferenceChangeListener(changed);
            PrinterSettings.preferences(TscPrintService.this).unregisterOnSharedPreferenceChangeListener(changed);
        }
    }
}
