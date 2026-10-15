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
        String original=PrinterSettings.host(context);
        try(ActivityScenario<MainActivity> scene=ActivityScenario.launch(MainActivity.class)) {
            device.wait(Until.findObject(By.text("ตั้งค่า")),5000).click();
            device.wait(Until.findObject(By.clazz("android.widget.EditText")),5000).setText("999.1.1.1");
            device.findObject(By.text("บันทึก IP")).click();
            assertTrue(device.wait(Until.hasObject(By.text("ใส่ IPv4 ให้ถูกต้อง เช่น 192.168.70.37")),5000));
            assertEquals(original,PrinterSettings.host(context));
            device.findObject(By.clazz("android.widget.EditText")).setText("127.0.0.2");
            device.findObject(By.text("บันทึก IP")).click();
            new UiScrollable(new UiSelector().scrollable(true)).scrollTextIntoView("บันทึก IP 127.0.0.2 แล้ว");
            assertTrue(device.wait(Until.hasObject(By.text("บันทึก IP 127.0.0.2 แล้ว")),5000));
            assertEquals("127.0.0.2",PrinterSettings.host(context));
            scene.recreate();
            new UiScrollable(new UiSelector().scrollable(true)).scrollTextIntoView("IP เครื่องพิมพ์");
            assertTrue(device.wait(Until.hasObject(By.clazz("android.widget.EditText").text("127.0.0.2")),5000));
            device.pressBack();
            assertTrue(device.wait(Until.hasObject(By.text("พิมพ์ฉลาก")),5000));
            assertTrue(device.hasObject(By.text("127.0.0.2")));
        } finally {PrinterSettings.save(context,original);}
    }
}
