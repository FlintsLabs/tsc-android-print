package com.revolic.tscprint.core;

import java.util.*;

/** Validated immutable catalog; routing only resolves exact printer IDs. */
public final class PrinterCatalog {
    public static final int LIMIT=20;
    private final List<PrinterProfile> profiles;
    private final String selectedId;
    public PrinterCatalog(List<PrinterProfile> profiles,String selectedId) {
        if(profiles.isEmpty() || profiles.size()>LIMIT)throw new IllegalArgumentException("บันทึกเครื่องพิมพ์ได้ 1–20 เครื่อง");
        Set<String> ids=new HashSet<>(),endpoints=new HashSet<>();
        for(PrinterProfile p:profiles) {
            if(!ids.add(p.id))throw new IllegalArgumentException("รหัสเครื่องพิมพ์ซ้ำ");
            if(!endpoints.add(p.endpoint()))throw new IllegalArgumentException("มี IP และพอร์ตนี้ในรายการแล้ว");
        }
        if(!ids.contains(selectedId))throw new IllegalArgumentException("ไม่พบเครื่องพิมพ์ที่เลือก");
        this.profiles=Collections.unmodifiableList(new ArrayList<>(profiles));this.selectedId=selectedId;
    }
    public List<PrinterProfile> all() {return profiles;}
    public PrinterProfile find(String id) {for(PrinterProfile p:profiles)if(p.id.equals(id))return p;return null;}
    public PrinterProfile selected() {return find(selectedId);}
    public PrinterCatalog select(String id) {return new PrinterCatalog(profiles,id);}
    public PrinterCatalog upsert(PrinterProfile profile) {
        List<PrinterProfile> copy=new ArrayList<>(profiles);int index=-1;
        for(int i=0;i<copy.size();i++)if(copy.get(i).id.equals(profile.id))index=i;
        if(index<0)copy.add(profile);else copy.set(index,profile);
        return new PrinterCatalog(copy,selectedId);
    }
    public PrinterCatalog remove(String id) {
        if(find(id)==null)throw new IllegalArgumentException("ไม่พบเครื่องพิมพ์");
        if(profiles.size()==1)throw new IllegalArgumentException("ต้องเหลือเครื่องพิมพ์อย่างน้อย 1 เครื่อง");
        List<PrinterProfile> copy=new ArrayList<>(profiles);copy.removeIf(p->p.id.equals(id));
        return new PrinterCatalog(copy,id.equals(selectedId)?copy.get(0).id:selectedId);
    }
}
