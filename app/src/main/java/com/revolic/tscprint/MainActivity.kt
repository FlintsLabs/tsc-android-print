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
import androidx.lifecycle.ViewModelProvider
import com.revolic.tscprint.core.PaperSize
import com.revolic.tscprint.core.PrinterProfile
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
    private var printer by mutableStateOf(PrinterProfile(PrinterSettings.LEGACY_ID,"TSC MH261T",PrinterSettings.DEFAULT_IP,9100,PaperSize.LABEL.id()))
    private var printers by mutableStateOf(listOf(printer))
    private var statusError by mutableStateOf(false)
    private var status by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private var serviceEnabled by mutableStateOf(false)
    private var networkAllowed by mutableStateOf(false)
    private var canInstallUpdates by mutableStateOf(false)
    private val updates by lazy { ViewModelProvider(this)[UpdateViewModel::class.java] }
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
        try { selectFromIntent(intent); refreshPrinters() }
        catch (e: IllegalStateException) {
            setContent { PrintTheme { androidx.compose.material3.Text(e.message ?: "อ่านรายการเครื่องพิมพ์ไม่ได้") } }
            return
        }
        setContent {
            val updater = updates
            LaunchedEffect(updater.installRequest) {
                val file = updater.installRequest ?: return@LaunchedEffect
                updater.consumeInstallRequest()
                try {
                    canInstallUpdates = packageManager.canRequestPackageInstalls()
                    if (canInstallUpdates) startActivity(UpdateInstaller.intent(this@MainActivity, file))
                    else updater.installError()
                } catch (_: Exception) { updater.installError() }
            }
            PrintApp(printer, printers, serviceEnabled, networkAllowed, busy, status, statusError, paper, papers,
                onSelectPaper = { selected ->
                    PaperSettings.select(this, selected); PrinterSettings.update(this, printer.withPaper(selected.id())); refreshPrinters()
                    statusError = false; status = "เลือกกระดาษ ${selected.label()} แล้ว"
                },
                onAddPaper = { added ->
                    try {
                        PaperSettings.add(this, added); papers = PaperSettings.all(this)
                        null
                    } catch (e: IllegalArgumentException) { e.message }
                },
                onSelectPrinter = { id ->
                    PrinterSettings.select(this,id); refreshPrinters()
                    statusError = false; status = "เลือก ${printer.name} แล้ว"
                },
                onSavePrinter = { id, name, address, port, selectedPaper ->
                    try {
                        val saved = PrinterProfile(id ?: java.util.UUID.randomUUID().toString(),name,address,port,selectedPaper.id())
                        PrinterSettings.update(this,saved)
                        refreshPrinters(); statusError = false; status = "บันทึก ${saved.name} แล้ว"; null
                    } catch (e: IllegalArgumentException) { e.message }
                },
                onDeletePrinter = { id ->
                    try { PrinterSettings.remove(this,id); refreshPrinters(); statusError = false; status = "ลบเครื่องพิมพ์แล้ว"; null }
                    catch (e: IllegalArgumentException) { e.message }
                },
                onProbe = { probe(it) },
                onClearFeedback = { status = ""; statusError = false },
                onChoosePdf = { choosePdf.launch(arrayOf("application/pdf")) },
                onPrintTest = { printTest(it) },
                onServiceSettings = { startActivity(Intent(Settings.ACTION_PRINT_SETTINGS)) },
                onRequestNetwork = { permission.launch(PrinterSettings.LAN_PERMISSION) },
                onAppSettings = { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) },
                updateContent = {
                    UpdatePanel(updater.state, canInstallUpdates, updater::check, updater::download, updater::prepareInstall,
                        onAllowInstall = {
                            try { startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))) }
                            catch (_: Exception) { updater.installError() }
                        }, onCancel = updater::cancel)
                })
        }
    }
    override fun onResume() {
        super.onResume()
        canInstallUpdates = packageManager.canRequestPackageInstalls()
        try { refreshPrinters() } catch (e: IllegalStateException) { statusError = true; status = e.message ?: "อ่านรายการเครื่องพิมพ์ไม่ได้" }
        networkAllowed = PrinterSettings.canUseNetwork(this)
        serviceEnabled = getSystemService(PrintManager::class.java).isPrintServiceEnabled(ComponentName(this, TscPrintService::class.java))
    }
    private fun selectFromIntent(source: Intent) {
        val uri = source.data ?: return
        if (uri.scheme == "tsc-printer") PrinterSettings.find(this,uri.schemeSpecificPart)?.let { PrinterSettings.select(this,it.id) }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        try { selectFromIntent(intent); refreshPrinters() }
        catch (e: IllegalStateException) { statusError = true; status = e.message ?: "อ่านรายการเครื่องพิมพ์ไม่ได้" }
    }
    private fun refreshPrinters() {
        val catalog = PrinterSettings.catalog(this)
        printers = catalog.all(); printer = catalog.selected()
        papers = PaperSettings.all(this); paper = PaperSize.fromId(printer.paperId)
        // Mirror the current profile for old paper-management clients and upgrades.
        if (!papers.contains(paper)) { PaperSettings.addAndSelect(this,paper); papers = PaperSettings.all(this) }
        else PaperSettings.select(this,paper)
    }
    private fun probe(target: PrinterProfile) {
        busy = true; statusError = false; status = "กำลังตรวจ ${target.name} · ${target.endpoint()}…"
        worker.execute {
            var failed = false
            val result = try {
                check(PrinterSettings.canUseNetwork(this)) { "ยังไม่ได้รับสิทธิ์เครือข่าย" }
                Socket().use { socket ->
                    activityIo.register(socket)
                    try { activityIo.check(); socket.connect(InetSocketAddress(target.host, target.port), 5000) }
                    finally { activityIo.unregister(socket) }
                }
                "เชื่อมต่อ ${target.endpoint()} ได้แล้ว (ยังไม่ได้ส่งฉลาก)"
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
                runOnUiThread { launchPrint(prepared, count, "TSC Label · ${printer.name}", selectedPaper) }
            } catch (e: Exception) {
                file?.delete()
                runOnUiThread { if (!isDestroyed) { busy = false; statusError = true; status = "อ่าน PDF ไม่ได้: ${e.message}" } }
            }
        }
    }
    private fun printTest(target: PrinterProfile) {
        if (busy) return
        status = ""; statusError = false
        try {
            val testPaper = PaperSize.fromId(target.paperId)
            getSystemService(PrintManager::class.java).print("TSC Print Test · ${target.name}",
                TestPrintAdapter(PrintFiles.directory(this), target.host, testPaper), printAttributes(testPaper))
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
