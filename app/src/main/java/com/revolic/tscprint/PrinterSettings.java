package com.revolic.tscprint;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import com.revolic.tscprint.core.*;
import org.json.*;
import java.util.*;

public final class PrinterSettings {
    public static final String DEFAULT_IP="192.168.70.37", LEGACY_ID="mh261t", LAN_PERMISSION="android.permission.ACCESS_LOCAL_NETWORK";
    public static final int PORT=9100;
    private static final String CATALOG="catalog_v1";
    private PrinterSettings() { }
    public static SharedPreferences preferences(Context context) {return context.getSharedPreferences("printer",Context.MODE_PRIVATE);}
    public static synchronized PrinterCatalog catalog(Context context) {
        SharedPreferences prefs=preferences(context);String stored=prefs.getString(CATALOG,null);
        if(stored==null) {
            PrinterProfile legacy=new PrinterProfile(LEGACY_ID,"TSC MH261T",prefs.getString("host",DEFAULT_IP),PORT,PaperSettings.selected(context).id());
            PrinterCatalog migrated=new PrinterCatalog(Collections.singletonList(legacy),legacy.id);
            persist(context,migrated);return migrated;
        }
        try {
            JSONObject state=new JSONObject(stored);
            if(state.getInt("version")!=1)throw new IllegalArgumentException("Unsupported printer settings version");
            JSONArray items=state.getJSONArray("printers");List<PrinterProfile> profiles=new ArrayList<>();
            for(int i=0;i<items.length();i++) {
                JSONObject item=items.getJSONObject(i);
                profiles.add(new PrinterProfile(item.getString("id"),item.getString("name"),item.getString("host"),item.getInt("port"),item.getString("paper")));
            }
            return new PrinterCatalog(profiles,state.getString("selected"));
        } catch(JSONException|IllegalArgumentException e) {throw new IllegalStateException("อ่านรายการเครื่องพิมพ์ไม่ได้ กรุณาตรวจการตั้งค่าแอป",e);}
    }
    private static void persist(Context context,PrinterCatalog catalog) {
        try {
            JSONArray items=new JSONArray();
            for(PrinterProfile p:catalog.all())items.put(new JSONObject().put("id",p.id).put("name",p.name).put("host",p.host).put("port",p.port).put("paper",p.paperId));
            JSONObject state=new JSONObject().put("version",1).put("selected",catalog.selected().id).put("printers",items);
            // One value changes atomically; listeners never see a partial registry.
            preferences(context).edit().putString(CATALOG,state.toString()).putString("host",catalog.selected().host).apply();
        } catch(JSONException e) {throw new IllegalStateException("บันทึกเครื่องพิมพ์ไม่ได้",e);}
    }
    public static PrinterProfile selected(Context context) {return catalog(context).selected();}
    public static PrinterProfile find(Context context,String id) {return catalog(context).find(id);}
    public static synchronized void select(Context context,String id) {persist(context,catalog(context).select(id));}
    public static synchronized void update(Context context,PrinterProfile profile) {persist(context,catalog(context).upsert(profile));}
    public static synchronized PrinterProfile add(Context context,String name,String host,int port,String paperId) {
        PrinterProfile profile=new PrinterProfile(UUID.randomUUID().toString(),name,host,port,paperId);
        PrinterCatalog updated=catalog(context).upsert(profile).select(profile.id);persist(context,updated);return profile;
    }
    public static synchronized void remove(Context context,String id) {persist(context,catalog(context).remove(id));}
    /** Compatibility for pre-multi-printer callers; actual jobs resolve the system PrinterId. */
    public static String host(Context context) {return selected(context).host;}
    public static synchronized void save(Context context,String host) {
        PrinterProfile selected=selected(context);update(context,selected.withAddress(host,selected.port));
    }
    public static boolean canUseNetwork(Context context) {return Build.VERSION.SDK_INT<37 || context.checkSelfPermission(LAN_PERMISSION)==PackageManager.PERMISSION_GRANTED;}
}
