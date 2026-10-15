package com.revolic.tscprint;

import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import static org.junit.Assert.*;

/** Use the same accessibility actions available to Android assistive controls.
 * Coordinate swipes can be intercepted by the profile form's text fields. */
final class UiTestControls {
    static {Configurator.getInstance().setWaitForIdleTimeout(500);}
    private static void refresh() {InstrumentationRegistry.getInstrumentation().getUiAutomation().clearCache();}
    private static AccessibilityNodeInfo root() {return InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();}
    private static AccessibilityNodeInfo scrollable(AccessibilityNodeInfo node) {
        if(node==null)return null;
        if(node.isScrollable())return node;
        for(int i=0;i<node.getChildCount();i++) {
            AccessibilityNodeInfo found=scrollable(node.getChild(i));if(found!=null)return found;
        }
        return null;
    }
    private static AccessibilityNodeInfo exactText(AccessibilityNodeInfo node,String text) {
        if(node==null)return null;
        if(node.getText()!=null && text.contentEquals(node.getText()))return node;
        for(int i=0;i<node.getChildCount();i++) {
            AccessibilityNodeInfo found=exactText(node.getChild(i),text);if(found!=null)return found;
        }
        return null;
    }
    static UiObject2 text(UiDevice device,String text) throws Exception {
        refresh();UiObject2 node=device.wait(Until.findObject(By.text(text)),1000);
        for(int action:new int[]{AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,AccessibilityNodeInfo.ACTION_SCROLL_FORWARD}) {
            for(int i=0;node==null && i<20;i++) {
                refresh();AccessibilityNodeInfo content=scrollable(root());
                if(content==null || !content.performAction(action))break;
                device.waitForIdle();refresh();node=device.wait(Until.findObject(By.text(text)),250);
            }
        }
        if(node==null) {
            android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
            device.takeScreenshot(new java.io.File(context.getExternalFilesDir(null),"ui-missing.png"));
            device.dumpWindowHierarchy(new java.io.File(context.getExternalFilesDir(null),"ui-missing.xml"));
        }
        assertNotNull(text,node);return node;
    }
    static void tap(UiDevice device,String text) throws Exception {
        text(device,text);refresh();AccessibilityNodeInfo target=exactText(root(),text);
        while(target!=null && !target.isClickable())target=target.getParent();
        assertNotNull("Clickable "+text,target);
        assertTrue("Click "+text,target.performAction(AccessibilityNodeInfo.ACTION_CLICK));
        device.waitForIdle();refresh();
    }
    private UiTestControls() { }
}
