package com.revolic.tscprint;

import android.content.Context;
import android.content.SharedPreferences;
import android.print.PrintAttributes;
import com.revolic.tscprint.core.PaperSize;
import java.util.*;

/** Media IDs encode physical dimensions; a queued job never depends on a later default change. */
public final class PaperSettings {
    private PaperSettings() { }
    public static SharedPreferences preferences(Context context) {return context.getSharedPreferences("paper",Context.MODE_PRIVATE);}
    public static List<PaperSize> all(Context context) {
        List<PaperSize> sizes=new ArrayList<>(Arrays.asList(PaperSize.LABEL,PaperSize.A5));
        Set<String> stored=preferences(context).getStringSet("custom",Collections.emptySet());
        List<String> sorted=new ArrayList<>(stored);Collections.sort(sorted);
        for(String id:sorted)try {PaperSize p=PaperSize.fromId(id);if(!sizes.contains(p))sizes.add(p);}catch(IllegalArgumentException ignored) { }
        return Collections.unmodifiableList(sizes);
    }
    public static PaperSize selected(Context context) {
        try {PaperSize p=PaperSize.fromId(preferences(context).getString("selected",PaperSize.LABEL.id()));if(all(context).contains(p))return p;}catch(IllegalArgumentException ignored) { }
        return PaperSize.LABEL;
    }
    public static void select(Context context,PaperSize paper) {
        if(!all(context).contains(paper))throw new IllegalArgumentException("บันทึกขนาดกระดาษก่อนเลือกใช้");
        preferences(context).edit().putString("selected",paper.id()).apply();
    }
    public static void addAndSelect(Context context,PaperSize paper) {
        Set<String> ids=new HashSet<>(preferences(context).getStringSet("custom",Collections.emptySet()));
        if(!paper.equals(PaperSize.LABEL) && !paper.equals(PaperSize.A5)) {
            if(!ids.contains(paper.id()) && ids.size()>=20)throw new IllegalArgumentException("บันทึกขนาดกำหนดเองได้สูงสุด 20 ขนาด");
            ids.add(paper.id());
        }
        preferences(context).edit().putStringSet("custom",ids).putString("selected",paper.id()).apply();
    }
    public static PrintAttributes.MediaSize media(PaperSize paper) {
        return new PrintAttributes.MediaSize(paper.id(),paper.label(),paper.widthMils(),paper.heightMils());
    }
    public static PaperSize fromMedia(PrintAttributes.MediaSize media) {
        if(media==null)throw new IllegalArgumentException("กรุณาเลือกขนาดกระดาษก่อนพิมพ์");
        PaperSize paper=PaperSize.fromId(media.getId());
        int w=media.getWidthMils(),h=media.getHeightMils(),pw=paper.widthMils(),ph=paper.heightMils();
        if(!((Math.abs(w-pw)<=1 && Math.abs(h-ph)<=1)||(Math.abs(w-ph)<=1 && Math.abs(h-pw)<=1)))
            throw new IllegalArgumentException("ขนาดกระดาษไม่ตรงกับรายการ TSC กรุณาเลือกใหม่");
        return paper;
    }
    public static boolean rotated(PaperSize paper,PrintAttributes.MediaSize media) {return paper.widthTenths!=paper.heightTenths && media.isPortrait()!=paper.isPortrait();}
}
