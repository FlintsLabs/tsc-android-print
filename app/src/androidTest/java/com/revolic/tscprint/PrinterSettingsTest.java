package com.revolic.tscprint;

import android.content.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.revolic.tscprint.core.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PrinterSettingsTest {
 private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
 private Map<String,?> saved;
 private String savedPaper;
 @Before public void before() {savedPaper=PaperSettings.selected(context).id();PaperSettings.select(context,PaperSize.LABEL);saved=new HashMap<>(PrinterSettings.preferences(context).getAll());PrinterSettings.preferences(context).edit().clear().commit();}
 @After public void after() {
  SharedPreferences.Editor e=PrinterSettings.preferences(context).edit().clear();
  for(Map.Entry<String,?> entry:saved.entrySet())if(entry.getValue() instanceof String)e.putString(entry.getKey(),(String)entry.getValue());
  e.commit();PaperSettings.select(context,PaperSize.fromId(savedPaper));
 }
 @Test public void legacyIpAndPaperMigrateOnceKeepingPrinterIdentity() {
  String oldPaper=PaperSettings.selected(context).id();
  try {
   PaperSettings.select(context,PaperSize.A5);
   PrinterSettings.preferences(context).edit().putString("host","192.168.70.45").commit();
   PrinterProfile migrated=PrinterSettings.selected(context);
   assertEquals("mh261t",migrated.id);assertEquals("192.168.70.45",migrated.host);
   assertEquals(PaperSize.A5.id(),migrated.paperId);
   PrinterSettings.preferences(context).edit().putString("host","192.168.70.46").commit();
   assertEquals("192.168.70.45",PrinterSettings.selected(context).host);
  } finally {PaperSettings.select(context,PaperSize.fromId(oldPaper));}
 }
 @Test public void savedPrintersRetainIndependentMediaAndExactRouting() {
  PrinterProfile first=PrinterSettings.selected(context);
  PrinterProfile second=PrinterSettings.add(context,"จุดแพ็ก","127.0.0.2",9101,PaperSize.A5.id());
  assertEquals(2,PrinterSettings.catalog(context).all().size());
  assertEquals(second.id,PrinterSettings.selected(context).id);
  PrinterSettings.select(context,first.id);
  assertEquals(PaperSize.LABEL.id(),PrinterSettings.selected(context).paperId);
  assertEquals(9101,PrinterSettings.find(context,second.id).port);
  PrinterProfile snapshot=PrinterSettings.find(context,second.id);
  PrinterSettings.update(context,second.withAddress("127.0.0.3",9200));
  assertEquals("127.0.0.2",snapshot.host);
  PrinterSettings.remove(context,second.id);assertNull(PrinterSettings.find(context,second.id));
 }
 @Test public void duplicateEndpointDoesNotChangeSavedSelection() {
  PrinterProfile first=PrinterSettings.selected(context);
  assertThrows(IllegalArgumentException.class,()->PrinterSettings.add(context,"duplicate",first.host,first.port,first.paperId));
  assertEquals(1,PrinterSettings.catalog(context).all().size());assertEquals(first.id,PrinterSettings.selected(context).id);
 }
 @Test public void malformedStoredCatalogFailsClosedWithoutReplacingIt() {
  PrinterSettings.preferences(context).edit().putString("catalog_v1","broken").commit();
  assertThrows(IllegalStateException.class,()->PrinterSettings.find(context,"mh261t"));
  assertEquals("broken",PrinterSettings.preferences(context).getString("catalog_v1",null));
 }
}
