package com.revolic.tscprint;

import android.os.*;
import android.print.*;
import com.revolic.tscprint.core.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Regenerates the diagnostic when system Print changes media or orientation. */
public final class TestPrintAdapter extends PrintDocumentAdapter {
    private final File directory;private final String host;private final PaperSize initialPaper;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final List<File> owned=new ArrayList<>();
    private final Object lock=new Object();
    private Cancellation active;private File current;private boolean closed;private int generation;
    public TestPrintAdapter(File directory,String host) {this(directory,host,PaperSize.LABEL);}
    public TestPrintAdapter(File directory,String host,PaperSize initialPaper) {
        this.directory=directory;this.host=host;this.initialPaper=Objects.requireNonNull(initialPaper);
    }
    @Override public void onLayout(PrintAttributes oldAttrs,PrintAttributes attrs,CancellationSignal signal,LayoutResultCallback callback,Bundle extras) {
        final PaperSize paper;final boolean rotate;
        try {
            PrintAttributes.MediaSize media=attrs.getMediaSize();
            // Android may request a generic Letter preview before selecting a printer.
            // Keep that preview usable; the service validates the actual queued TSC media.
            if(media==null || !media.getId().startsWith("TSC_")) {paper=initialPaper;rotate=false;}
            else {paper=PaperSettings.fromMedia(media);rotate=PaperSettings.rotated(paper,media);}
        }
        catch(IllegalArgumentException e){callback.onLayoutFailed(e.getMessage());return;}
        Cancellation cancel=new Cancellation();final int request;
        synchronized(lock) {
            if(closed){callback.onLayoutCancelled();return;}
            if(active!=null)active.cancel();active=cancel;request=++generation;current=null;
        }
        signal.setOnCancelListener(cancel::cancel);
        worker.execute(()->{
            File file=null;
            try {
                file=TestLabel.createPdf(directory,host,paper,rotate,cancel);
                synchronized(lock) {
                    if(closed || request!=generation || signal.isCanceled()){file.delete();callback.onLayoutCancelled();return;}
                    current=file;owned.add(file);
                }
                callback.onLayoutFinished(new PrintDocumentInfo.Builder("TSC Print Test.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(1).build(),true);
            } catch(Exception e) {
                if(file!=null)file.delete();
                if(cancel.isCancelled())callback.onLayoutCancelled();else callback.onLayoutFailed("เตรียมฉลากไม่ได้: "+e.getMessage());
            } finally {signal.setOnCancelListener(null);}
        });
    }
    @Override public void onWrite(PageRange[] pages,ParcelFileDescriptor destination,CancellationSignal signal,WriteResultCallback callback) {
        final File pdf;synchronized(lock){pdf=current;}
        signal.setOnCancelListener(()->{try{destination.close();}catch(IOException ignored){}});
        worker.execute(()->{
            try(OutputStream out=new ParcelFileDescriptor.AutoCloseOutputStream(destination)) {
                if(pdf==null)throw new IOException("กรุณาเลือกขนาดกระดาษใหม่");
                try(InputStream in=new FileInputStream(pdf)) {
                    byte[] bytes=new byte[32768];int n;
                    while((n=in.read(bytes))!=-1){if(signal.isCanceled()){callback.onWriteCancelled();return;}out.write(bytes,0,n);}
                }
                if(signal.isCanceled())callback.onWriteCancelled();else callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            } catch(IOException e){if(signal.isCanceled())callback.onWriteCancelled();else callback.onWriteFailed(e.getMessage());}
            finally {signal.setOnCancelListener(null);}
        });
    }
    @Override public void onFinish() {
        synchronized(lock){closed=true;generation++;if(active!=null)active.cancel();}
        worker.execute(()->{synchronized(lock){for(File f:owned)f.delete();owned.clear();current=null;}});
        worker.shutdown();
    }
}
