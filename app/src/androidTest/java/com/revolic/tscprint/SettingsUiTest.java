package com.revolic.tscprint;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SettingsUiTest {
    @Test public void invalidIpDoesNotSaveAndValidIpSurvivesRecreation() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try(PrinterTestState state=PrinterTestState.isolated(context);ActivityScenario<MainActivity> scene=ActivityScenario.launch(MainActivity.class)) {
            String original=PrinterSettings.host(context);
            device.wait(Until.findObject(By.text("ตั้งค่า")),5000).click();
            device.wait(Until.findObject(By.res("saved-printer-"+PrinterSettings.selected(context).id)),5000).click();
            UiTestControls.text(device,"IP เครื่องพิมพ์");
            device.wait(Until.findObject(By.res("printer-ip")),5000).setText("999.1.1.1");
            UiTestControls.text(device,"บันทึกเครื่องพิมพ์");
            UiTestControls.tap(device,"บันทึกเครื่องพิมพ์");
            assertTrue(device.wait(Until.hasObject(By.text("IPv4 ต้องอยู่ระหว่าง 0–255")),5000));
            assertEquals(original,PrinterSettings.host(context));
            UiTestControls.text(device,"IP เครื่องพิมพ์");
            device.findObject(By.res("printer-ip")).setText("127.0.0.2");
            UiTestControls.text(device,"บันทึกเครื่องพิมพ์");
            UiTestControls.tap(device,"บันทึกเครื่องพิมพ์");
            assertTrue(device.wait(Until.hasObject(By.textContains("บันทึก TSC")),5000));
            assertEquals("127.0.0.2",PrinterSettings.host(context));
            scene.recreate();
            UiTestControls.text(device,"เครื่องพิมพ์ที่บันทึก");
            assertTrue(device.wait(Until.hasObject(By.text("127.0.0.2:9100")),5000));
            device.pressBack();
            assertTrue(device.wait(Until.hasObject(By.text("พิมพ์ฉลาก")),5000));
            assertTrue(device.hasObject(By.text("127.0.0.2:9100")));
        }
    }
}
