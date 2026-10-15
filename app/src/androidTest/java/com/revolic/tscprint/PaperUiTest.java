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
  String old=PaperSettings.selected(context).id();Set<String> custom=new HashSet<>(PaperSettings.preferences(context).getStringSet("custom",Collections.emptySet()));
  PaperSettings.select(context,PaperSize.LABEL);
  try(ActivityScenario<MainActivity> scene=ActivityScenario.launch(MainActivity.class)) {
   UiScrollable content=new UiScrollable(new UiSelector().scrollable(true));content.scrollTextIntoView(PaperSize.LABEL.label());
   device.wait(Until.findObject(By.text(PaperSize.LABEL.label())),5000).click();
   device.wait(Until.findObject(By.text(PaperSize.A5.label())),5000).click();
   assertEquals(PaperSize.A5,PaperSettings.selected(context));
   device.findObject(By.text("Print Test")).click();
   assertTrue(device.wait(Until.hasObject(By.text(PaperSize.A5.label())),5000));
   device.findObject(By.text("ตั้งค่า")).click();
   device.wait(Until.findObject(By.text("เพิ่มขนาดกระดาษเอง")),5000).click();
   content.scrollTextIntoView("กว้าง (มม.)");device.wait(Until.findObject(By.res("paper-width")),5000).setText("169");
   content.scrollTextIntoView("สูง (มม.)");device.wait(Until.findObject(By.res("paper-height")),5000).setText("150.2");
   content.scrollTextIntoView("บันทึกและใช้ขนาดนี้");device.findObject(By.text("บันทึกและใช้ขนาดนี้")).click();
   assertEquals(PaperSize.A5,PaperSettings.selected(context));
   content.scrollTextIntoView("กว้าง (มม.)");device.wait(Until.findObject(By.res("paper-width")),5000).setText("100.5");
   content.scrollTextIntoView("บันทึกและใช้ขนาดนี้");device.findObject(By.text("บันทึกและใช้ขนาดนี้")).click();
   PaperSize expected=PaperSize.parse("100.5","150.2");
   assertTrue(device.wait(Until.hasObject(By.text(expected.label())),5000));
   assertEquals(expected,PaperSettings.selected(context));
   scene.recreate();assertEquals(expected,PaperSettings.selected(context));
   device.wait(Until.findObject(By.text("Print Test")),5000).click();
   assertTrue(device.wait(Until.hasObject(By.text(expected.label())),5000));
   device.findObject(By.text("พิมพ์")).click();
   content.scrollTextIntoView(expected.label());assertTrue(device.hasObject(By.text(expected.label())));
  } finally {PaperSettings.preferences(context).edit().putString("selected",old).putStringSet("custom",custom).commit();}
 }
}
