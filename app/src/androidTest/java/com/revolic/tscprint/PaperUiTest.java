package com.revolic.tscprint;
import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import com.revolic.tscprint.core.PaperSize;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PaperUiTest {
 @Test public void presetAndCustomSizeAreSharedByPrintTestAndPersist() throws Exception {
  Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
  UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
  try(PrinterTestState state=PrinterTestState.isolated(context);ActivityScenario<MainActivity> scene=ActivityScenario.launch(MainActivity.class)) {
   UiScrollable content=new UiScrollable(new UiSelector().scrollable(true));UiTestControls.tap(device,PaperSize.LABEL.label());
   UiTestControls.tap(device,PaperSize.A5.label());
   assertEquals(PaperSize.A5,PaperSettings.selected(context));
   device.findObject(By.text("Print Test")).click();
   assertTrue(device.wait(Until.hasObject(By.text(PaperSize.A5.label())),5000));
   device.findObject(By.text("ตั้งค่า")).click();
   device.wait(Until.findObject(By.res("saved-printer-"+PrinterSettings.selected(context).id)),5000).click();
   UiTestControls.tap(device,"เพิ่มขนาดกระดาษเอง");
   UiTestControls.text(device,"กว้าง (มม.)");device.wait(Until.findObject(By.res("paper-width")),5000).setText("169");
   UiTestControls.text(device,"สูง (มม.)");device.wait(Until.findObject(By.res("paper-height")),5000).setText("150.2");
   UiTestControls.tap(device,"บันทึกและใช้ขนาดนี้");
   assertEquals(PaperSize.A5,PaperSettings.selected(context));
   UiTestControls.text(device,"กว้าง (มม.)");device.wait(Until.findObject(By.res("paper-width")),5000).setText("100.5");
   UiTestControls.tap(device,"บันทึกและใช้ขนาดนี้");
   PaperSize expected=PaperSize.parse("100.5","150.2");
   assertTrue(device.wait(Until.hasObject(By.text(expected.label())),5000));
   assertEquals("Custom stock is a draft until the profile is saved",PaperSize.A5,PaperSettings.selected(context));
   UiTestControls.tap(device,"บันทึกเครื่องพิมพ์");
   assertEquals(expected,PaperSettings.selected(context));
   scene.recreate();assertEquals(expected,PaperSettings.selected(context));
   device.wait(Until.findObject(By.text("Print Test")),5000).click();
   assertTrue(device.wait(Until.hasObject(By.text(expected.label())),5000));
   device.findObject(By.text("พิมพ์")).click();
   UiTestControls.text(device,expected.label());assertTrue(device.hasObject(By.text(expected.label())));
  }
 }
}
