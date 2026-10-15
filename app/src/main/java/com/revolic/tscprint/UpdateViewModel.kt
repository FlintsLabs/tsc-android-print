package com.revolic.tscprint

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.revolic.tscprint.core.Cancellation
import com.revolic.tscprint.core.ReleaseVersion
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

enum class UpdatePhase { IDLE, CHECKING, AVAILABLE, DOWNLOADING, VERIFYING, READY, CURRENT, ERROR }
data class UpdateUiState(val phase: UpdatePhase = UpdatePhase.IDLE, val release: UpdateRelease? = null,
                         val message: String = "ตรวจสอบเมื่อคุณกดปุ่ม ใช้อินเทอร์เน็ตเชื่อมต่อ GitHub",
                         val progress: Float = 0f) {
    val working: Boolean get() = phase in listOf(UpdatePhase.CHECKING, UpdatePhase.DOWNLOADING, UpdatePhase.VERIFYING)
}

/** Activity-scoped worker survives rotation; a verified download also survives process restart. */
class UpdateViewModel @JvmOverloads constructor(app: Application, private val client: GitHubReleaseClient = GitHubReleaseClient()) : AndroidViewModel(app) {
    var state by mutableStateOf(UpdateUiState()); private set
    var installRequest by mutableStateOf<File?>(null); private set
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val directory = File(app.filesDir, "updates")
    private val apk = File(directory, "update.apk")
    private val ticket = AtomicFile(File(directory, "pending.json"))
    private var cancellation = Cancellation()
    private var generation = 0
    private var cleared = false

    init {
        if (ticket.baseFile.exists()) {
            state = UpdateUiState(UpdatePhase.VERIFYING, message = "กำลังตรวจไฟล์อัปเดตที่ดาวน์โหลดไว้…")
            val request = generation
            worker.execute {
                try {
                    val release = UpdateRelease.restore(JSONObject(ticket.openRead().use { it.reader().readText() }))
                    if (ReleaseVersion.parse(release.version).compareTo(ReleaseVersion.parse(BuildConfig.VERSION_NAME)) > 0) {
                        UpdateInstaller.verify(app, apk, release, cancellation)
                        deliver(request) { state = UpdateUiState(UpdatePhase.READY, release, "ไฟล์พร้อมติดตั้ง ${release.version}") }
                    } else { clearDownload(); deliver(request) { state = UpdateUiState() } }
                } catch (_: Exception) { clearDownload(); deliver(request) { state = UpdateUiState(message = "ไฟล์อัปเดตเดิมใช้ไม่ได้ กรุณาตรวจสอบและดาวน์โหลดใหม่") } }
            }
        } else { apk.delete(); File(apk.path + ".part").delete() }
    }
    private fun deliver(request: Int, action: () -> Unit) { main.post { if (!cleared && generation == request) action() } }
    private fun begin(phase: UpdatePhase, release: UpdateRelease?, message: String): Pair<Int, Cancellation> {
        generation++; cancellation = Cancellation(); state = UpdateUiState(phase, release, message)
        return generation to cancellation
    }
    fun check() {
        if (state.working) return
        val previous = state
        val (request, cancel) = begin(UpdatePhase.CHECKING, previous.release, "กำลังตรวจ GitHub Releases…")
        worker.execute {
            try {
                val release = client.latest(cancel)
                val newer = ReleaseVersion.parse(release.version).compareTo(ReleaseVersion.parse(BuildConfig.VERSION_NAME)) > 0
                if (!newer) clearDownload()
                deliver(request) {
                    state = when {
                        !newer -> UpdateUiState(UpdatePhase.CURRENT, message = "ยังไม่มีรุ่นใหม่กว่า ${BuildConfig.VERSION_NAME} · GitHub ล่าสุด ${release.version}")
                        previous.phase == UpdatePhase.READY && previous.release?.tag == release.tag && previous.release.sha256 == release.sha256 -> previous
                        else -> UpdateUiState(UpdatePhase.AVAILABLE, release, "พบเวอร์ชันใหม่ ${release.version}")
                    }
                }
            } catch (e: Exception) {
                deliver(request) { state = if (previous.phase == UpdatePhase.READY) previous.copy(message = "ตรวจ GitHub ไม่ได้ แต่ไฟล์ที่ตรวจสอบไว้ยังพร้อมติดตั้ง")
                    else UpdateUiState(UpdatePhase.ERROR, message = "ตรวจสอบอัปเดตไม่ได้: ${failure(e)}") }
            }
        }
    }
    fun download() {
        if (state.working) return
        val offered = state.release ?: return
        val (request, cancel) = begin(UpdatePhase.DOWNLOADING, offered, "กำลังดาวน์โหลด ${offered.version}…")
        worker.execute {
            try {
                clearDownload(); var last = 0L
                val release = client.download(offered, apk, cancel) { bytes ->
                    val now = android.os.SystemClock.uptimeMillis()
                    if (now - last > 150 || bytes == offered.size) {
                        last = now; deliver(request) { state = state.copy(progress = bytes.toFloat() / offered.size) }
                    }
                }
                deliver(request) { state = UpdateUiState(UpdatePhase.VERIFYING, release, "กำลังตรวจ checksum และลายเซ็น APK…") }
                UpdateInstaller.verify(getApplication(), apk, release, cancel)
                if (!directory.isDirectory) directory.mkdirs()
                val stream = ticket.startWrite()
                try { stream.write(release.json().toString().toByteArray()); ticket.finishWrite(stream) }
                catch (e: Exception) { ticket.failWrite(stream); throw e }
                deliver(request) { state = UpdateUiState(UpdatePhase.READY, release, "ตรวจไฟล์แล้ว พร้อมติดตั้ง ${release.version}") }
            } catch (e: Exception) {
                clearDownload()
                deliver(request) { state = UpdateUiState(UpdatePhase.ERROR, offered, "ดาวน์โหลดอัปเดตไม่ได้: ${failure(e)}") }
            }
        }
    }
    fun prepareInstall() {
        if (state.phase != UpdatePhase.READY) return
        val release = state.release ?: return
        val (request, cancel) = begin(UpdatePhase.VERIFYING, release, "กำลังตรวจไฟล์ก่อนติดตั้ง…")
        worker.execute {
            try {
                UpdateInstaller.verify(getApplication(), apk, release, cancel)
                deliver(request) { state = UpdateUiState(UpdatePhase.READY, release, "ยืนยันอัปเดตในหน้าต่าง Android"); installRequest = apk }
            } catch (e: Exception) {
                clearDownload(); deliver(request) { state = UpdateUiState(UpdatePhase.ERROR, release, "ติดตั้งไม่ได้: ${failure(e)}") }
            }
        }
    }
    fun consumeInstallRequest() { installRequest = null }
    fun installError() { state = state.copy(message = "เปิดหน้าติดตั้งไม่ได้ ตรวจสิทธิ์ติดตั้งแอปแล้วลองอีกครั้ง") }
    fun cancel() {
        if (state.phase != UpdatePhase.DOWNLOADING) return
        generation++; cancellation.cancel()
        state = UpdateUiState(UpdatePhase.AVAILABLE, state.release, "ยกเลิกดาวน์โหลดแล้ว")
    }
    private fun clearDownload() { ticket.delete(); apk.delete(); File(apk.path + ".part").delete() }
    private fun failure(e: Exception): String = when (e) {
        is java.net.UnknownHostException -> "เชื่อมต่ออินเทอร์เน็ตไม่ได้ ตรวจ Wi-Fi แล้วลองใหม่"
        is java.net.SocketTimeoutException -> "การเชื่อมต่อหมดเวลา กรุณาลองใหม่"
        is javax.net.ssl.SSLException -> "ยืนยัน HTTPS ไม่ได้ ตรวจวันเวลาและอินเทอร์เน็ตแล้วลองใหม่"
        is java.net.SocketException -> "การเชื่อมต่อขัดข้อง ตรวจอินเทอร์เน็ตแล้วลองใหม่"
        else -> if (e.message?.contains("ENOSPC") == true) "พื้นที่ในเครื่องไม่พอ กรุณาเพิ่มพื้นที่แล้วดาวน์โหลดใหม่"
            else e.message ?: "ตรวจอินเทอร์เน็ตแล้วลองใหม่"
    }
    override fun onCleared() { cleared = true; generation++; cancellation.cancel(); worker.shutdownNow() }
}
