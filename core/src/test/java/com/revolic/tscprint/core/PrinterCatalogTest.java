package com.revolic.tscprint.core;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class PrinterCatalogTest {
    private PrinterProfile profile(String id,String host,int port,String paper) {
        return new PrinterProfile(id,"คลัง "+id,host,port,paper);
    }
    @Test public void selectionAndEditsPreserveOtherPrintersAndStableIds() {
        PrinterProfile a=profile("mh261t","192.168.70.37",9100,"TSC_102x76");
        PrinterProfile b=profile("packing","192.168.70.40",9101,PaperSize.A5.id());
        PrinterCatalog catalog=new PrinterCatalog(Arrays.asList(a,b),a.id).select(b.id);
        PrinterCatalog edited=catalog.upsert(b.withAddress("192.168.70.41",9102));
        assertEquals("packing",edited.selected().id);
        assertEquals("192.168.70.41",edited.selected().host);
        assertEquals(9102,edited.selected().port);
        assertEquals(PaperSize.A5.id(),edited.selected().paperId);
        assertEquals("192.168.70.37",edited.find("mh261t").host);
        assertEquals("192.168.70.40",catalog.selected().host);
    }
    @Test public void deletedPrinterNeverFallsBackForRouting() {
        PrinterCatalog c=new PrinterCatalog(Arrays.asList(profile("a","127.0.0.1",9100,PaperSize.LABEL.id()),profile("b","127.0.0.2",9100,PaperSize.A5.id())),"b");
        PrinterCatalog removed=c.remove("b");
        assertNull(removed.find("b"));assertEquals("a",removed.selected().id);
        assertThrows(IllegalArgumentException.class,()->removed.remove("a"));
        assertThrows(IllegalArgumentException.class,()->removed.select("missing"));
    }
    @Test public void duplicateNormalizedEndpointIsRejectedButDifferentPortsAreAllowed() {
        PrinterProfile a=profile("a","192.168.070.037",9100,PaperSize.LABEL.id());
        PrinterCatalog c=new PrinterCatalog(Collections.singletonList(a),"a");
        assertThrows(IllegalArgumentException.class,()->c.upsert(profile("b","192.168.70.37",9100,PaperSize.A5.id())));
        assertEquals(2,c.upsert(profile("b","192.168.70.37",9101,PaperSize.A5.id())).all().size());
    }
    @Test public void invalidProfilesDoNotEnterCatalog() {
        assertThrows(IllegalArgumentException.class,()->profile("a","999.1.1.1",9100,PaperSize.LABEL.id()));
        assertThrows(IllegalArgumentException.class,()->profile("a","127.0.0.1",0,PaperSize.LABEL.id()));
        assertThrows(IllegalArgumentException.class,()->new PrinterProfile("a"," ","127.0.0.1",9100,PaperSize.LABEL.id()));
        assertThrows(IllegalArgumentException.class,()->profile("a","127.0.0.1",9100,"NA_LETTER"));
        assertThrows(IllegalArgumentException.class,()->new PrinterCatalog(Collections.emptyList(),"a"));
    }
    @Test public void twentiethPrinterIsAcceptedAndTwentyFirstDoesNotChangeCatalog() {
        List<PrinterProfile> list=new ArrayList<>();
        for(int i=1;i<=20;i++)list.add(profile("p"+i,"127.0.0."+i,9100,PaperSize.LABEL.id()));
        PrinterCatalog c=new PrinterCatalog(list,"p1");
        assertEquals(20,c.all().size());
        assertThrows(IllegalArgumentException.class,()->c.upsert(profile("p21","127.0.0.21",9100,PaperSize.LABEL.id())));
        assertEquals(20,c.all().size());assertEquals("p1",c.selected().id);
    }

}
