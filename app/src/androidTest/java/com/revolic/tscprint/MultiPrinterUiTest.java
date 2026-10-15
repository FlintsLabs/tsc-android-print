package com.revolic.tscprint;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import com.revolic.tscprint.core.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MultiPrinterUiTest {
 private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
 private final UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
 @Test public void viewingAndEditingOtherPrinterDoesNotChangePrintSelection() throws Exception {
  try(PrinterTestState state=PrinterTestState.isolated(context)) {
   PrinterProfile first=PrinterSettings.selected(context);
   PrinterProfile second=PrinterSettings.add(context,"Packing details","127.0.0.2",9101,PaperSize.A5.id());
   PrinterSettings.select(context,first.id);
   try(ActivityScenario<MainActivity> scene=ActivityScenario.launch(MainActivity.class)) {
    tap("ตั้งค่า");
    assertTrue(device.wait(Until.hasObject(By.res("saved-printer-"+first.id)),3000));
    assertTrue(device.hasObject(By.res("saved-printer-"+second.id)));
    assertFalse(device.hasObject(By.res("printer-selector")));
    device.takeScreenshot(new File(context.getExternalFilesDir(null),"saved-printers-settings.png"));
    device.findObject(By.res("saved-printer-"+second.id)).click();
    assertTrue("A list row opens its own settings",device.wait(Until.hasObject(By.res("printer-name")),3000));
    assertEquals(first.id,PrinterSettings.selected(context).id);
    assertEquals(PaperSize.LABEL.id(),PaperSettings.selected(context).id());
    field("printer-name","ชื่อเครื่องพิมพ์","Packing changed");
    scene.recreate();
    assertEquals("Packing changed",device.wait(Until.findObject(By.res("printer-name")),3000).getText());
    tap("บันทึกเครื่องพิมพ์");
    UiTestControls.text(device,"เครื่องพิมพ์ที่บันทึก");
    assertEquals("Packing changed",PrinterSettings.find(context,second.id).name);
    assertEquals(first.id,PrinterSettings.selected(context).id);
    device.findObject(By.res("saved-printer-"+second.id)).click();
    field("printer-name","ชื่อเครื่องพิมพ์","Unsaved draft");
    device.pressBack();device.waitForIdle();
    if(device.hasObject(By.res("printer-name")))device.pressBack();
    assertTrue(device.wait(Until.hasObject(By.text("เครื่องพิมพ์ที่บันทึก")),3000));
    assertEquals("Packing changed",PrinterSettings.find(context,second.id).name);
    device.pressBack();
    assertTrue(device.wait(Until.hasObject(By.text("พิมพ์ฉลาก")),3000));
    assertEquals(first.id,PrinterSettings.selected(context).id);
   }
  }
 }
 @Test public void bothWorkspacesSelectSamePrinterAndRestoreItsPaper() throws Exception {
  try(PrinterTestState state=PrinterTestState.isolated(context)) {
   PrinterProfile first=PrinterSettings.selected(context);
   PrinterProfile second=PrinterSettings.add(context,"Packing test","127.0.0.2",9101,PaperSize.A5.id());
   try(ActivityScenario<MainActivity> scene=ActivityScenario.launch(MainActivity.class)) {
    assertTrue(device.wait(Until.hasObject(By.text("127.0.0.2:9101")),5000));
    device.wait(Until.findObject(By.res("printer-selector")),5000).click();
    device.wait(Until.findObject(By.text(first.name)),5000).click();
    assertEquals(first.id,PrinterSettings.selected(context).id);
    UiTestControls.text(device,PaperSize.LABEL.label());
    device.findObject(By.text("Print Test")).click();
    device.wait(Until.findObject(By.res("printer-selector")),5000).click();
    device.wait(Until.findObject(By.text(second.name)),5000).click();
    UiTestControls.text(device,PaperSize.A5.label());
    assertEquals(second.id,PrinterSettings.selected(context).id);
    device.takeScreenshot(new File(context.getExternalFilesDir(null),"multi-printer-test.png"));
    scene.recreate();
    assertTrue(device.wait(Until.hasObject(By.text("127.0.0.2:9101")),5000));
   }
  }
 }
 private void tap(String text) throws Exception {
  UiTestControls.tap(device,text);
 }
 private PrinterProfile awaitSaved(String name,int port) throws Exception {
  long deadline=android.os.SystemClock.uptimeMillis()+5000;
  while(android.os.SystemClock.uptimeMillis()<deadline) {
   for(PrinterProfile profile:PrinterSettings.catalog(context).all())if(profile.name.equals(name) && profile.port==port)return profile;
   android.os.SystemClock.sleep(50);
  }
  throw new AssertionError("Saved profile "+name+":"+port);
 }
 private void field(String resource,String label,String value) throws Exception {
  UiObject2 node=device.wait(Until.findObject(By.res(resource)),1000);
  if(node==null) {UiTestControls.text(device,label);node=device.wait(Until.findObject(By.res(resource)),3000);}
  assertNotNull(resource,node);node.setText(value);
  device.waitForIdle();InstrumentationRegistry.getInstrumentation().getUiAutomation().clearCache();
 }
 @Test public void addEditAndDeletePrinterFromSettings() throws Exception {
  try(PrinterTestState state=PrinterTestState.isolated(context)) {
   String selected=PrinterSettings.selected(context).id;
   try(ActivityScenario<MainActivity> scene=ActivityScenario.launch(MainActivity.class)) {
    tap("ตั้งค่า");tap("เพิ่มเครื่องพิมพ์");
    field("printer-name","ชื่อเครื่องพิมพ์","Packing UI");field("printer-ip","IP เครื่องพิมพ์","127.0.0.2");field("printer-port","พอร์ต","9101");
    UiTestControls.text(device,"กระดาษเริ่มต้นของเครื่องนี้");
    tap(PaperSize.LABEL.label());tap(PaperSize.A5.label());
    tap("บันทึกเครื่องพิมพ์");PrinterProfile added=awaitSaved("Packing UI",9101);
    assertEquals(PaperSize.A5.id(),added.paperId);assertEquals(selected,PrinterSettings.selected(context).id);
    assertEquals(2,PrinterSettings.catalog(context).all().size());
    device.wait(Until.findObject(By.res("saved-printer-"+added.id)),3000).click();
    field("printer-name","ชื่อเครื่องพิมพ์","Packing edited");field("printer-port","พอร์ต","9200");tap("บันทึกเครื่องพิมพ์");awaitSaved("Packing edited",9200);
    scene.recreate();assertEquals(PaperSize.A5.id(),PrinterSettings.find(context,added.id).paperId);
    assertEquals("Packing edited",PrinterSettings.find(context,added.id).name);assertEquals(9200,PrinterSettings.find(context,added.id).port);
    device.wait(Until.findObject(By.res("saved-printer-"+added.id)),3000).click();
    tap("ลบเครื่องพิมพ์นี้");tap("ลบเครื่องพิมพ์");
    long deadline=android.os.SystemClock.uptimeMillis()+5000;
    while(PrinterSettings.catalog(context).all().size()!=1 && android.os.SystemClock.uptimeMillis()<deadline)android.os.SystemClock.sleep(50);
    assertEquals(1,PrinterSettings.catalog(context).all().size());assertNull(PrinterSettings.find(context,added.id));
    assertEquals(selected,PrinterSettings.selected(context).id);
   }
  }
 }
 @Test public void connectionProbeUsesViewedPrinterAndKeepsPrintSelection() throws Exception {
  try(PrinterTestState state=PrinterTestState.isolated(context);
      java.net.ServerSocket server=new java.net.ServerSocket(0,1,java.net.InetAddress.getByName("127.0.0.2"))) {
   server.setSoTimeout(8000);
   String selected=PrinterSettings.selected(context).id;
   PrinterProfile other=PrinterSettings.add(context,"Probe details","127.0.0.2",server.getLocalPort(),PaperSize.A5.id());
   PrinterSettings.select(context,selected);
   try(ActivityScenario<MainActivity> scene=ActivityScenario.launch(MainActivity.class)) {
    tap("ตั้งค่า");device.wait(Until.findObject(By.res("saved-printer-"+other.id)),3000).click();
    tap("ตรวจการเชื่อมต่อ");
    try(java.net.Socket socket=server.accept()) {assertEquals("127.0.0.2",socket.getLocalAddress().getHostAddress());}
    UiTestControls.text(device,"เชื่อมต่อ "+other.endpoint()+" ได้แล้ว (ยังไม่ได้ส่งฉลาก)");
    assertEquals(selected,PrinterSettings.selected(context).id);
   }
  }
 }
}
