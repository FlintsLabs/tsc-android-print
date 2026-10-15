package com.revolic.tscprint;
import android.app.Application;
public final class TscApplication extends Application {
    @Override public void onCreate() {super.onCreate();PrintFiles.cleanOnProcessStart(this);}
}
