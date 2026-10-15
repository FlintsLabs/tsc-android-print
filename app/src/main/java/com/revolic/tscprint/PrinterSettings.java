package com.revolic.tscprint;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import com.revolic.tscprint.core.PrinterAddress;

public final class PrinterSettings {
    public static final String DEFAULT_IP="192.168.70.37", LAN_PERMISSION="android.permission.ACCESS_LOCAL_NETWORK";
    public static final int PORT=9100;
    private PrinterSettings() { }
    public static String host(Context context) { return context.getSharedPreferences("printer",Context.MODE_PRIVATE).getString("host",DEFAULT_IP); }
    public static void save(Context context,String host) { context.getSharedPreferences("printer",Context.MODE_PRIVATE).edit().putString("host",PrinterAddress.validate(host)).apply(); }
    public static boolean canUseNetwork(Context context) { return Build.VERSION.SDK_INT<37 || context.checkSelfPermission(LAN_PERMISSION)==PackageManager.PERMISSION_GRANTED; }
}
