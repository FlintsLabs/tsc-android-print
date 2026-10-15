package com.revolic.tscprint

import android.content.ComponentName
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.revolic.tscprint.core.PaperSize
import com.revolic.tscprint.core.Cancellation
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val activityIo = Cancellation()
    private var paper by mutableStateOf(PaperSize.LABEL)
    private var papers by mutableStateOf(listOf(PaperSize.LABEL, PaperSize.A5))
    private var host by mutableStateOf(PrinterSettings.DEFAULT_IP)
    private var statusError by mutableStateOf(false)
    private var status by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private var serviceEnabled by mutableStateOf(false)
    private var networkAllowed by mutableStateOf(false)
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        statusError = !granted
        networkAllowed = granted
        status = if (granted) "อนุญาตเครือข่ายแล้ว" else "ยังไม่ได้รับสิทธิ์ เปิดสิทธิ์อุปกรณ์ใกล้เคียงในการตั้งค่าแอป"
    }
    private val choosePdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) openPdf(uri)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        host = PrinterSettings.host(this)
        setContent {
            PrintApp(host, serviceEnabled, networkAllowed, busy, status, statusError, paper, papers,
                onSelectPaper = { selected ->
                    PaperSettings.select(this, selected); paper = selected
                    statusError = false; status = "เลือกกระดาษ ${selected.label()} แล้ว"
                },
                onAddPaper = { added ->
                    try {
                        PaperSettings.addAndSelect(this, added); papers = PaperSettings.all(this); paper = added
                        statusError = false; status = "บันทึกและเลือกกระดาษ ${added.label()} แล้ว"; null
                    } catch (e: IllegalArgumentException) { e.message }
                },
                onSave = { address ->
                    PrinterSettings.save(this, address); host = address
                    statusError = false; status = "บันทึก IP $address แล้ว"
                },
                onProbe = { probe(host) },
                onChoosePdf = { choosePdf.launch(arrayOf("application/pdf")) },
                onPrintTest = { printTest() },
                onServiceSettings = { startActivity(Intent(Settings.ACTION_PRINT_SETTINGS)) },
                onRequestNetwork = { permission.launch(PrinterSettings.LAN_PERMISSION) },
                onAppSettings = { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) })
        }
    }
    override fun onResume() {
        super.onResume()
        papers = PaperSettings.all(this); paper = PaperSettings.selected(this)
        networkAllowed = PrinterSettings.canUseNetwork(this)
        serviceEnabled = getSystemService(PrintManager::class.java).isPrintServiceEnabled(ComponentName(this, TscPrintService::class.java))
    }
    private fun probe(host: String) {
        busy = true; statusError = false; status = "กำลังตรวจ $host:9100…"
        worker.execute {
            var failed = false
            val result = try {
                check(PrinterSettings.canUseNetwork(this)) { "ยังไม่ได้รับสิทธิ์เครือข่าย" }
                Socket().use { socket ->
                    activityIo.register(socket)
                    try { activityIo.check(); socket.connect(InetSocketAddress(host, PrinterSettings.PORT), 5000) }
                    finally { activityIo.unregister(socket) }
                }
                "เชื่อมต่อพอร์ต 9100 ได้แล้ว (ยังไม่ได้ส่งฉลาก)"
            } catch (e: Exception) { failed = true; "เชื่อมต่อไม่ได้ ตรวจ IP / Wi-Fi / สิทธิ์เครือข่าย: ${e.message}" }
            runOnUiThread { if (!isDestroyed) { busy = false; statusError = failed; status = result } }
        }
    }
    private fun openPdf(uri: Uri) {
        busy = true; statusError = false; status = "กำลังเตรียม PDF…"
        val selectedPaper = paper
        worker.execute {
            var file: File? = null
            try {
                file = contentResolver.openInputStream(uri)?.use { PdfSpooler.copyPdf(it, PrintFiles.directory(this), activityIo) }
                    ?: error("เปิดไฟล์ไม่ได้")
                activityIo.check()
                val count = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { it.pageCount } }
                require(count in 1..100) { "รองรับ PDF ไม่เกิน 100 หน้า" }
                val prepared = file
                runOnUiThread { launchPrint(prepared, count, "TSC Label", selectedPaper) }
            } catch (e: Exception) {
                file?.delete()
                runOnUiThread { if (!isDestroyed) { busy = false; statusError = true; status = "อ่าน PDF ไม่ได้: ${e.message}" } }
            }
        }
    }
    private fun printTest() {
        if (busy) return
        status = ""; statusError = false
        try {
            getSystemService(PrintManager::class.java).print("TSC Print Test",
                TestPrintAdapter(PrintFiles.directory(this), host), printAttributes(paper))
        } catch (e: Exception) { statusError = true; status = "เปิดเมนู Print ไม่ได้: ${e.message}" }
    }
    private fun printAttributes(selectedPaper: PaperSize): PrintAttributes = PrintAttributes.Builder()
        .setMediaSize(PaperSettings.media(selectedPaper))
        .setResolution(PrintAttributes.Resolution("203", "203 DPI", 203, 203))
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME).build()
    private fun launchPrint(file: File, count: Int, title: String, selectedPaper: PaperSize) {
        busy = false; status = ""
        if (isFinishing || isDestroyed) { file.delete(); return }
        try {
            val attributes = printAttributes(selectedPaper)
            getSystemService(PrintManager::class.java).print(title, PdfPrintAdapter(file, count, "$title.pdf"), attributes)
        } catch (e: Exception) { file.delete(); statusError = true; status = "เปิดเมนู Print ไม่ได้: ${e.message}" }
    }
    override fun onDestroy() { activityIo.cancel(); worker.shutdownNow(); super.onDestroy() }
}
