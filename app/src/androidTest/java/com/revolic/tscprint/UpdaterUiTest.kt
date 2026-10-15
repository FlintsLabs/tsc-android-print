package com.revolic.tscprint

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UpdaterUiTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val device=UiDevice.getInstance(instrumentation)
    @Test fun settingsContainsManualUpdateCheckAndSurvivesRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scene ->
            UiTestControls.tap(device,"ตั้งค่า")
            UiTestControls.text(device,"ตรวจสอบอัปเดต")
            assertTrue(device.hasObject(By.res("check-update").enabled(true)))
            assertTrue(device.hasObject(By.text("เวอร์ชันปัจจุบัน ${BuildConfig.VERSION_NAME}")))
            scene.recreate()
            UiTestControls.text(device,"ตรวจสอบอัปเดต")
            assertFalse(device.hasObject(By.res("install-update")))
        }
    }
    @Test fun downloadProgressPermissionInstallAndRetryStatesAreActionable() {
        val release=UpdaterTest.release(UpdaterTest.fixture("future"),"v1.3.6")
        val state=mutableStateOf(UpdateUiState(UpdatePhase.AVAILABLE,release,"พบเวอร์ชันใหม่ 1.3.6"))
        val permission=mutableStateOf(false)
        var checks=0;var grants=0;var installs=0
        ActivityScenario.launch(MainActivity::class.java).use { scene ->
            scene.onActivity { activity -> activity.setContent {
                PrintTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                    UpdatePanel(state.value,permission.value,{ checks++ },
                        { state.value=UpdateUiState(UpdatePhase.DOWNLOADING,release,"กำลังดาวน์โหลด 1.3.6…",.42f) },
                        { installs++ }, { grants++;permission.value=true }, {})
                } }
            } }
            device.wait(Until.findObject(By.res("download-update")),5000).click()
            assertTrue(device.wait(Until.hasObject(By.text("ดาวน์โหลด 42%")),3000))
            assertTrue(device.hasObject(By.res("check-update").enabled(false)))
            device.takeScreenshot(File(instrumentation.targetContext.getExternalFilesDir(null),"update-downloading.png"))
            scene.onActivity { state.value=UpdateUiState(UpdatePhase.READY,release,"ตรวจไฟล์แล้ว พร้อมติดตั้ง 1.3.6") }
            UiTestControls.tap(device,"อนุญาตติดตั้งจากแอปนี้");assertEquals(1,grants)
            UiTestControls.tap(device,"ติดตั้งอัปเดต 1.3.6");assertEquals(1,installs)
            device.takeScreenshot(File(instrumentation.targetContext.getExternalFilesDir(null),"update-ready.png"))
            scene.onActivity { state.value=UpdateUiState(UpdatePhase.ERROR,message="ตรวจสอบอัปเดตไม่ได้: เชื่อมต่ออินเทอร์เน็ตไม่ได้") }
            UiTestControls.tap(device,"ตรวจสอบอัปเดต");assertEquals(1,checks)
            assertTrue(device.wait(Until.gone(By.res("install-update")),3000))
            device.takeScreenshot(File(instrumentation.targetContext.getExternalFilesDir(null),"update-error.png"))
        }
    }
}
