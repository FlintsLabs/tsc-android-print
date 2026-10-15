package com.revolic.tscprint;

import android.os.*;
import android.print.*;
import java.io.*;
import java.util.concurrent.*;

/** Android's spooler prunes this full PDF to the requested pages. */
public final class PdfPrintAdapter extends PrintDocumentAdapter {
    private final File pdf;private final int pageCount;private final String name;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    public PdfPrintAdapter(File pdf,int pageCount,String name) { this.pdf=pdf;this.pageCount=pageCount;this.name=name; }
    @Override public void onLayout(PrintAttributes oldAttrs,PrintAttributes newAttrs,CancellationSignal signal,LayoutResultCallback callback,Bundle extras) {
        if(signal.isCanceled()) callback.onLayoutCancelled();
        else callback.onLayoutFinished(new PrintDocumentInfo.Builder(name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(pageCount).build(),!newAttrs.equals(oldAttrs));
    }
    @Override public void onWrite(PageRange[] pages,ParcelFileDescriptor destination,CancellationSignal signal,WriteResultCallback callback) {
        signal.setOnCancelListener(()->{try{destination.close();}catch(IOException ignored){}});
        worker.execute(()->{
            try(InputStream in=new FileInputStream(pdf);OutputStream out=new ParcelFileDescriptor.AutoCloseOutputStream(destination)) {
                byte[] buffer=new byte[32768];int n;
                while((n=in.read(buffer))!=-1) {if(signal.isCanceled()){callback.onWriteCancelled();return;}out.write(buffer,0,n);}
                if(signal.isCanceled()) callback.onWriteCancelled();else callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            }catch(IOException e){if(signal.isCanceled())callback.onWriteCancelled();else callback.onWriteFailed("อ่าน PDF ไม่ได้");}
            finally {signal.setOnCancelListener(null);}
        });
    }
    @Override public void onFinish() {worker.execute(()->pdf.delete());worker.shutdown();}
}
