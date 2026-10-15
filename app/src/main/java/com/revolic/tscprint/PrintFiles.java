package com.revolic.tscprint;
import android.content.Context;
import java.io.File;
public final class PrintFiles {
    private PrintFiles() { }
    public static File directory(Context context) {
        File dir=new File(context.getCacheDir(),"print-jobs");
        if(!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) throw new IllegalStateException("สร้างพื้นที่ชั่วคราวไม่ได้");
        return dir;
    }
    /** Called once per app process, before any adapter/service can own a temporary file. */
    public static void cleanOnProcessStart(Context context) {
        File[] files=directory(context).listFiles();
        if(files!=null) for(File file:files) if(file.isFile()) file.delete();
    }
}
