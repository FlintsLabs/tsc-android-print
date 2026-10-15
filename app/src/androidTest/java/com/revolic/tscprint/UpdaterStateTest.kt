package com.revolic.tscprint

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class UpdaterStateTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as Application
    private fun waitFor(model: UpdateViewModel, phase: UpdatePhase) {
        val deadline = SystemClock.uptimeMillis() + 10000
        while (model.state.phase != phase && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(25)
        assertEquals(model.state.message, phase, model.state.phase)
    }
    private fun isolate(action: () -> Unit) {
        val directory = File(app.filesDir,"updates")
        val backup = File(app.filesDir,"updater-test-backup")
        check(!backup.exists())
        if (directory.exists()) check(directory.renameTo(backup))
        try { action() } finally { directory.deleteRecursively(); if (backup.exists()) check(backup.renameTo(directory)) }
    }
    @Test fun downloadAndProcessRestoreRetainVerifiedCandidateWithoutNetwork() = isolate {
        val bytes = UpdaterTest.fixture("future")
        val release = UpdaterTest.release(bytes,"v1.3.5")
        val store = ViewModelStore()
        lateinit var first: UpdateViewModel
        instrumentation.runOnMainSync {
            first = UpdateViewModel(app,UpdaterTest.client(release,bytes)); store.put("first",first); first.check()
        }
        waitFor(first,UpdatePhase.AVAILABLE)
        instrumentation.runOnMainSync { first.download() }
        waitFor(first,UpdatePhase.READY)
        instrumentation.runOnMainSync { store.clear() }
        lateinit var restored: UpdateViewModel
        instrumentation.runOnMainSync {
            restored = UpdateViewModel(app,GitHubReleaseClient { _, _ -> throw IOException("Restore must be local") });store.put("restored",restored)
        }
        waitFor(restored,UpdatePhase.READY)
        assertEquals(release.sha256,restored.state.release!!.sha256)
        instrumentation.runOnMainSync { restored.prepareInstall() }
        waitFor(restored,UpdatePhase.READY)
        assertNotNull(restored.installRequest)
        instrumentation.runOnMainSync { store.clear() }
    }
    @Test fun cancellationPreventsLateDownloadCompletionAndCleansPartialFile() = isolate {
        val bytes=UpdaterTest.fixture("future");val release=UpdaterTest.release(bytes,"v1.3.5")
        val entered=CountDownLatch(1);val exited=CountDownLatch(1)
        val client=GitHubReleaseClient { url,cancel ->
            if(url==release.apkUrl) {
                entered.countDown()
                try { while(!cancel.isCancelled) Thread.sleep(10);cancel.check() } finally { exited.countDown() }
            }
            response(url,release,bytes)
        }
        val store=ViewModelStore();lateinit var model:UpdateViewModel
        instrumentation.runOnMainSync { model=UpdateViewModel(app,client);store.put("model",model);model.check() }
        waitFor(model,UpdatePhase.AVAILABLE)
        instrumentation.runOnMainSync { model.download() }
        assertTrue(entered.await(5,TimeUnit.SECONDS))
        instrumentation.runOnMainSync { model.cancel() }
        assertTrue(exited.await(5,TimeUnit.SECONDS));SystemClock.sleep(200)
        assertEquals(UpdatePhase.AVAILABLE,model.state.phase)
        assertFalse(File(app.filesDir,"updates/update.apk.part").exists())
        assertFalse(File(app.filesDir,"updates/update.apk").exists())
        instrumentation.runOnMainSync { store.clear() }
    }
    @Test fun offlineCheckCanBeRetriedAndAnOlderReleaseIsNotOffered() = isolate {
        var offline=true
        val release=UpdaterTest.release(UpdaterTest.fixture("old"),"v1.3.3")
        val store=ViewModelStore();lateinit var model:UpdateViewModel
        instrumentation.runOnMainSync {
            model=UpdateViewModel(app,GitHubReleaseClient { url,_ ->
                if(offline)throw java.net.UnknownHostException("offline")
                response(url,release,UpdaterTest.fixture("old"))
            });store.put("model",model)
            assertEquals(UpdatePhase.IDLE,model.state.phase);model.check()
        }
        waitFor(model,UpdatePhase.ERROR)
        instrumentation.runOnMainSync { offline=false;model.check() }
        waitFor(model,UpdatePhase.CURRENT)
        assertNull(model.state.release)
        instrumentation.runOnMainSync { store.clear() }
    }
    private fun response(url:String,release:UpdateRelease,bytes:ByteArray):GitHubReleaseClient.Response {
        val content=if(url==UpdateRelease.API)UpdaterTest.metadata(release).toByteArray()
            else if(url==release.checksumUrl)(release.sha256+"  "+release.filename+"\n").toByteArray() else bytes
        return GitHubReleaseClient.Response(content.inputStream(),content.size.toLong()) {}
    }
}
