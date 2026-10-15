package com.revolic.tscprint

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.revolic.tscprint.core.PaperSize
import com.revolic.tscprint.core.PrinterProfile

private val destinations = listOf("พิมพ์", "Print Test", "ตั้งค่า")
private val destinationIcons = listOf(PrintIcons.Printer, PrintIcons.Test, PrintIcons.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintApp(printer: PrinterProfile, printers: List<PrinterProfile>, serviceEnabled: Boolean, networkAllowed: Boolean, busy: Boolean,
             status: String, statusError: Boolean, paper: PaperSize, papers: List<PaperSize>,
             onSelectPaper: (PaperSize) -> Unit, onAddPaper: (PaperSize) -> String?,
             onSelectPrinter: (String) -> Unit, onSavePrinter: (String?, String, String, Int, PaperSize) -> String?,
             onDeletePrinter: (String) -> String?, onProbe: (PrinterProfile) -> Unit, onClearFeedback: () -> Unit,
             onChoosePdf: () -> Unit, onPrintTest: (PrinterProfile) -> Unit, onServiceSettings: () -> Unit,
             onRequestNetwork: () -> Unit, onAppSettings: () -> Unit, updateContent: @Composable () -> Unit) {
    var destination by rememberSaveable { mutableIntStateOf(0) }
    var settingsRoute by rememberSaveable { mutableStateOf("list") }
    fun openSettings(route: String) {
        settingsRoute = route; destination = 2
        if (route != "list") onClearFeedback()
    }
    BackHandler(destination != 0) {
        if (destination == 2 && settingsRoute != "list") { settingsRoute = "list"; onClearFeedback() }
        else destination = 0
    }
    PrintTheme {
        BoxWithConstraints {
            val expanded = maxWidth >= 600.dp
            Scaffold(
                topBar = {
                    TopAppBar(title = { Text("TSC Direct Print", style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = { Icon(PrintIcons.Printer, null, Modifier.padding(start = 24.dp, end = 12.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary) })
                },
                bottomBar = {
                    if (!expanded) NavigationBar {
                        destinations.forEachIndexed { index, title ->
                            NavigationBarItem(selected = destination == index, onClick = { destination = index },
                                icon = { Icon(destinationIcons[index], null) }, label = { Text(title) },
                                colors = NavigationBarItemDefaults.colors(selectedTextColor = MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            ) { insets ->
                // The footer and IME overlap; consume Scaffold padding before applying IME insets.
                Row(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).imePadding()) {
                    if (expanded) NavigationRail {
                        destinations.forEachIndexed { index, title ->
                            NavigationRailItem(selected = destination == index, onClick = { destination = index },
                                icon = { Icon(destinationIcons[index], null) }, label = { Text(title) }, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        // Each destination starts at its title; long fonts and landscape can scroll.
                        key(destination, if (destination == 2) settingsRoute else "workspace") {
                            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().fillMaxHeight()
                                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                when (destination) {
                                    0 -> {
                                        PageTitle("พิมพ์ฉลาก", "จากมือถือ ตรงถึงเครื่องพิมพ์")
                                        PrinterPanel(printer, printers, serviceEnabled, paper, !busy, onSelectPrinter)
                                        PaperSelector(paper, papers, !busy, onSelectPaper)
                                        TextButton(onClick = { openSettings("printer:${printer.id}") }) { Text("จัดการขนาดกระดาษ") }
                                        Requirements(serviceEnabled, networkAllowed, onServiceSettings, onRequestNetwork, onAppSettings)
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            ActionButton("เลือก PDF เพื่อพิมพ์", PrintIcons.Printer,
                                                !busy && serviceEnabled && networkAllowed, onChoosePdf)
                                            OutlinedButton(onClick = { destination = 1 }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                                Icon(PrintIcons.Test, null, Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Text("เปิดหน้า Print Test")
                                            }
                                        }
                                        Feedback(busy, status, statusError)
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Text("พิมพ์จากแอปอื่น", style = MaterialTheme.typography.titleMedium)
                                            Instruction("1", "เปิดเอกสาร", "ใช้แอปที่มีเมนู Print หรือพิมพ์")
                                            Instruction("2", "เลือกเครื่องพิมพ์ตามชื่อที่บันทึก", "เลือกขนาดให้ตรงกระดาษ · ตรวจหน้าและจำนวนสำเนา")
                                            Instruction("3", "ยืนยันการพิมพ์", "มือถือส่งตรงผ่านเครือข่าย ไม่ต้องเปิด Mac")
                                        }
                                    }
                                    1 -> {
                                        PageTitle("ทดสอบการพิมพ์", "ตรวจขอบ ขนาด และความคมชัดในฉลากเดียว")
                                        PrinterSelector(printer, printers, !busy, onSelectPrinter)
                                        PaperSelector(paper, papers, !busy, onSelectPaper)
                                        TextButton(onClick = { openSettings("printer:${printer.id}") }) { Text("จัดการขนาดกระดาษ") }
                                        TestPreview(printer.host, paper)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(paper.label(), style = MaterialTheme.typography.labelLarge)
                                            Text("203 DPI · ขาวดำ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("เครื่องพิมพ์ปลายทาง", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${printer.name} · ${printer.endpoint()}", style = MaterialTheme.typography.bodyLarge)
                                            Text("ฉลาก 1 หน้า · เลือก 1 สำเนาในหน้าต่าง Print", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Requirements(serviceEnabled, networkAllowed, onServiceSettings, onRequestNetwork, onAppSettings)
                                        ActionButton("พิมพ์ฉลากทดสอบ", PrintIcons.Printer, !busy && serviceEnabled && networkAllowed) { onPrintTest(printer) }
                                        Text("จะเปิดหน้าต่าง Print ให้ตรวจและยืนยันก่อนพิมพ์", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Feedback(busy, status, statusError)
                                        HorizontalDivider()
                                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Text("ตรวจอะไรบนฉลาก", style = MaterialTheme.typography.titleMedium)
                                            CheckRow("ขอบครบ 4 ด้าน", "กรอบอยู่ห่างขอบฉลาก 2 มม.")
                                            CheckRow("เส้นวัดยาว ${paper.rulerMm()} มม.", "วัดระหว่างขีดปลายทั้งสองด้าน")
                                            CheckRow("QR อ่านได้", "สแกนแล้วได้ TSC-TEST-001")
                                            CheckRow("เลข 4 สีขาวบนพื้นดำ", "พื้นดำต้องอยู่ครบและตัวเลขอ่านชัด")
                                        }
                                        Text("ภาพตัวอย่างใช้เนื้อหาเดียวกับงานพิมพ์ ตำแหน่งจริงยังขึ้นกับค่า Shift และการป้อนฉลากที่เครื่อง", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    else -> SettingsPage(settingsRoute, { openSettings(it) }, printer, printers,
                                        serviceEnabled, networkAllowed, busy, status, statusError,
                                        onSavePrinter, onDeletePrinter, onProbe, onPrintTest,
                                        onServiceSettings, onRequestNetwork, onAppSettings, paper, papers, onAddPaper, updateContent)
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun PageTitle(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun PrinterPanel(printer: PrinterProfile, printers: List<PrinterProfile>, enabled: Boolean, paper: PaperSize, selectable: Boolean, onSelect: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(PrintIcons.Printer, null, Modifier.size(32.dp))
            PrinterSelector(printer, printers, selectable, onSelect,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                supportingColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f))
            Text("${paper.label()}   ·   203 DPI", style = MaterialTheme.typography.bodyMedium)
            Text(if (enabled) "บริการพิมพ์เปิดอยู่" else "ยังไม่ได้เปิดบริการพิมพ์", style = MaterialTheme.typography.labelLarge)
        }
    }
}
@Composable private fun Requirements(serviceEnabled: Boolean, networkAllowed: Boolean, onService: () -> Unit,
                                     onPermission: () -> Unit, onAppSettings: () -> Unit) {
    if (!serviceEnabled || !networkAllowed) Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ตั้งค่าก่อนเริ่มพิมพ์", style = MaterialTheme.typography.titleSmall)
            if (!serviceEnabled) {
                Text("เปิดบริการ TSC Direct Print ในการตั้งค่าการพิมพ์ของ Android", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onService, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("เปิดบริการพิมพ์") }
            }
            if (!networkAllowed) {
                Text("อนุญาตเครือข่ายภายใน เพื่อให้มือถือเข้าถึงเครื่องพิมพ์", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onPermission, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("อนุญาตเครือข่ายภายใน") }
                TextButton(onClick = onAppSettings, modifier = Modifier.heightIn(min = 48.dp)) { Text("เปิดการตั้งค่าสิทธิ์แอป") }
            }
        }
    }
}
@Composable private fun ActionButton(label: String, icon: ImageVector, enabled: Boolean, action: () -> Unit) {
    Button(onClick = action, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(12.dp)) {
        Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(10.dp)); Text(label)
    }
}
@Composable private fun Instruction(number: String, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { Text(number, style = MaterialTheme.typography.labelLarge) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable private fun CheckRow(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun TestPreview(host: String, paper: PaperSize) {
    val bitmap = remember(host, paper) { TestLabel.preview(host, paper).asImageBitmap() }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(16.dp)) {
            val previewHeight = (maxWidth / (paper.widthDots().toFloat() / paper.heightDots())).coerceAtMost(440.dp)
            Image(bitmap, "ฉลากทดสอบ ${paper.label()} มีกรอบ 4 ด้าน QR เลข 4 สีขาวบนพื้นดำ และเส้นวัด ${paper.rulerMm()} มม.",
                modifier = Modifier.fillMaxWidth().height(previewHeight), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
        }
    }
}
@Composable private fun Feedback(busy: Boolean, status: String, error: Boolean) {
    if (busy || status.isNotBlank()) Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodyMedium,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
    }
}
@Composable private fun SettingsPage(route: String, onRoute: (String) -> Unit,
                                    selectedPrinter: PrinterProfile, printers: List<PrinterProfile>, serviceEnabled: Boolean,
                                    networkAllowed: Boolean, busy: Boolean, status: String, statusError: Boolean,
                                    onSavePrinter: (String?, String, String, Int, PaperSize) -> String?,
                                    onDeletePrinter: (String) -> String?, onProbe: (PrinterProfile) -> Unit,
                                    onPrintTest: (PrinterProfile) -> Unit, onService: () -> Unit,
                                    onPermission: () -> Unit, onAppSettings: () -> Unit,
                                    paper: PaperSize, papers: List<PaperSize>, onAddPaper: (PaperSize) -> String?, updateContent: @Composable () -> Unit) {
    if (route != "list") {
        val profile = printers.find { "printer:${it.id}" == route }
        TextButton(onClick = { onRoute("list") }, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(PrintIcons.ChevronLeft, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("รายการเครื่องพิมพ์")
        }
        if (route != "add" && profile == null) {
            Text("ไม่พบเครื่องพิมพ์นี้", style = MaterialTheme.typography.titleLarge)
        } else {
            PageTitle(if (profile == null) "เพิ่มเครื่องพิมพ์" else "ตั้งค่าเครื่องพิมพ์", profile?.name ?: "บันทึก IP และกระดาษของเครื่องใหม่")
            Requirements(serviceEnabled, networkAllowed, onService, onPermission, onAppSettings)
            PrinterEditor(profile, paper, papers, !busy, onSavePrinter, onAddPaper, onDeletePrinter, onProbe, onPrintTest,
                networkAllowed, serviceEnabled, printers.size > 1,
                if (profile?.id == selectedPrinter.id) printers.firstOrNull { it.id != profile.id }?.name else null,
                onBack = { onRoute("list") }, feedback = { Feedback(busy, status, statusError) })
        }
    } else {
        PageTitle("ตั้งค่าเครื่องพิมพ์", "จัดการ IP และกระดาษแยกแต่ละเครื่อง")
        PrinterList(printers, !busy, onOpen = { onRoute("printer:$it") }, onAdd = { onRoute("add") })
        Feedback(busy, status, statusError)
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("บริการพิมพ์ Android", style = MaterialTheme.typography.titleMedium)
            if (serviceEnabled && networkAllowed) {
                Text("บริการพิมพ์เปิดอยู่", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = onService, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("จัดการบริการพิมพ์") }
            } else Requirements(serviceEnabled, networkAllowed, onService, onPermission, onAppSettings)
        }
        HorizontalDivider()
        updateContent()
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ฉลากและการจัดหน้า", style = MaterialTheme.typography.titleMedium)
            CheckRow("203 DPI · ขาวดำ", "เลือกเครื่องและกระดาษในหน้า พิมพ์ / Print Test")
            CheckRow("ตำแหน่งพิมพ์", "ใช้ค่า Shift X/Y ที่ตั้งไว้ในเครื่องพิมพ์")
            Text("TSC Direct Print ${BuildConfig.VERSION_NAME}\nAndroid 13+ · แอปอิสระ ไม่ใช่แอปทางการของ TSC", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
