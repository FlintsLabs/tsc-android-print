package com.revolic.tscprint;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.WindowInsets;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import static org.junit.Assert.*;

/** Reproduces the RS38 form obstruction using a real software keyboard. */
@RunWith(AndroidJUnit4.class)
public class KeyboardInsetsTest {
    @Test public void printerFieldsUseTheSpaceImmediatelyAboveKeyboard() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try(PrinterTestState saved=PrinterTestState.isolated(context);ActivityScenario<MainActivity> scene=ActivityScenario.launch(MainActivity.class)) {
            UiTestControls.tap(device,"ตั้งค่า");UiTestControls.tap(device,"เพิ่มเครื่องพิมพ์");
            String[] fields={"printer-name","printer-ip","printer-port"};
            String[] labels={"ชื่อเครื่องพิมพ์","IP เครื่องพิมพ์","พอร์ต"};
            for(int index=0;index<fields.length;index++) {
                UiTestControls.text(device,labels[index]);
                UiObject2 input=device.wait(Until.findObject(By.res(fields[index])),5000);
                assertNotNull(fields[index],input);input.click();
                int[] geometry=new int[3];
                long deadline=SystemClock.uptimeMillis()+10000;
                do {
                    scene.onActivity(activity->{
                        android.view.View decor=activity.getWindow().getDecorView();
                        WindowInsets insets=decor.getRootWindowInsets();
                        int[] origin=new int[2];decor.getLocationOnScreen(origin);
                        geometry[0]=insets!=null && insets.isVisible(WindowInsets.Type.ime())?1:0;
                        geometry[1]=insets==null?0:origin[1]+decor.getHeight()-insets.getInsets(WindowInsets.Type.ime()).bottom;
                        geometry[2]=Math.round(16*activity.getResources().getDisplayMetrics().density);
                    });
                    if(geometry[0]==1)break;
                    SystemClock.sleep(50);
                } while(SystemClock.uptimeMillis()<deadline);
                assertEquals("Software keyboard opened",1,geometry[0]);
                device.waitForIdle();SystemClock.sleep(300);
                UiObject2 scroll=device.wait(Until.findObject(By.pkg(context.getPackageName()).scrollable(true)),5000);
                assertNotNull("Scrollable form",scroll);Rect viewport=scroll.getVisibleBounds();
                device.takeScreenshot(new File(context.getExternalFilesDir(null),"keyboard-"+fields[index]+".png"));
                int gap=geometry[1]-viewport.bottom;
                assertTrue(fields[index]+": unused space above keyboard = "+gap+" px",gap<=geometry[2]);
                assertTrue("Form must stay above keyboard",gap>=-geometry[2]);
                assertTrue("Input remains focused",device.hasObject(By.res(fields[index]).focused(true)));
                device.pressBack();device.waitForIdle();
            }
        }
    }
}
