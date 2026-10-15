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
import com.revolic.tscprint.core.PrinterAddress

private val destinations = listOf("พิมพ์", "Print Test", "ตั้งค่า")
private val destinationIcons = listOf(PrintIcons.Printer, PrintIcons.Test, PrintIcons.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintApp(host: String, serviceEnabled: Boolean, networkAllowed: Boolean, busy: Boolean,
             status: String, statusError: Boolean, paper: PaperSize, papers: List<PaperSize>,
             onSelectPaper: (PaperSize) -> Unit, onAddPaper: (PaperSize) -> String?,
             onSave: (String) -> Unit, onProbe: () -> Unit,
             onChoosePdf: () -> Unit, onPrintTest: () -> Unit, onServiceSettings: () -> Unit,
             onRequestNetwork: () -> Unit, onAppSettings: () -> Unit) {
    var destination by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(destination != 0) { destination = 0 }
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
                Row(Modifier.fillMaxSize().padding(insets).imePadding()) {
                    if (expanded) NavigationRail {
                        destinations.forEachIndexed { index, title ->
                            NavigationRailItem(selected = destination == index, onClick = { destination = index },
                                icon = { Icon(destinationIcons[index], null) }, label = { Text(title) }, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        // Each destination starts at its title; long fonts and landscape can scroll.
                        key(destination) {
                            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth().fillMaxHeight()
                                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                when (destination) {
                                    0 -> {
                                        PageTitle("พิมพ์ฉลาก", "จากมือถือ ตรงถึงเครื่องพิมพ์")
                                        PrinterPanel(host, serviceEnabled, paper)
                                        PaperSelector(paper, papers, !busy, onSelectPaper)
                                        TextButton(onClick = { destination = 2 }) { Text("จัดการขนาดกระดาษ") }
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
                                            Instruction("2", "เลือก TSC MH261T", "เลือกขนาดให้ตรงกระดาษ · ตรวจหน้าและจำนวนสำเนา")
                                            Instruction("3", "ยืนยันการพิมพ์", "มือถือส่งตรงผ่านเครือข่าย ไม่ต้องเปิด Mac")
                                        }
                                    }
                                    1 -> {
                                        PageTitle("ทดสอบการพิมพ์", "ตรวจขอบ ขนาด และความคมชัดในฉลากเดียว")
                                        PaperSelector(paper, papers, !busy, onSelectPaper)
                                        TextButton(onClick = { destination = 2 }) { Text("จัดการขนาดกระดาษ") }
                                        TestPreview(host, paper)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(paper.label(), style = MaterialTheme.typography.labelLarge)
                                            Text("203 DPI · ขาวดำ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("เครื่องพิมพ์ปลายทาง", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("TSC MH261T · $host", style = MaterialTheme.typography.bodyLarge)
                                            Text("ฉลาก 1 หน้า · เลือก 1 สำเนาในหน้าต่าง Print", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Requirements(serviceEnabled, networkAllowed, onServiceSettings, onRequestNetwork, onAppSettings)
                                        ActionButton("พิมพ์ฉลากทดสอบ", PrintIcons.Printer, !busy && serviceEnabled && networkAllowed, onPrintTest)
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
                                    else -> SettingsPage(host, serviceEnabled, networkAllowed, busy, status, statusError,
                                        onSave, onProbe, onServiceSettings, onRequestNetwork, onAppSettings, paper, papers, onSelectPaper, onAddPaper)
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
@Composable private fun PrinterPanel(host: String, enabled: Boolean, paper: PaperSize) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(PrintIcons.Printer, null, Modifier.size(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("TSC MH261T", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(host, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace)
            }
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
@Composable private fun SettingsPage(host: String, serviceEnabled: Boolean, networkAllowed: Boolean, busy: Boolean,
                                    status: String, statusError: Boolean, onSave: (String) -> Unit, onProbe: () -> Unit,
                                    onService: () -> Unit, onPermission: () -> Unit, onAppSettings: () -> Unit,
                                    paper: PaperSize, papers: List<PaperSize>, onSelectPaper: (PaperSize) -> Unit, onAddPaper: (PaperSize) -> String?) {
    var ip by rememberSaveable(host) { mutableStateOf(host) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    PageTitle("ตั้งค่าเครื่องพิมพ์", "เชื่อมต่อ TSC MH261T ผ่าน Wi-Fi / LAN")
    PaperManager(paper, papers, !busy, onSelectPaper, onAddPaper)
    HorizontalDivider()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("เครือข่าย", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = ip, onValueChange = { ip = it; error = null }, singleLine = true,
            label = { Text("IP เครื่องพิมพ์") }, modifier = Modifier.fillMaxWidth(), isError = error != null, enabled = !busy,
            supportingText = { Text(error ?: "พอร์ต 9100 · มือถือต้องเข้าถึงเครือข่ายนี้ได้") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        ActionButton("บันทึก IP", PrintIcons.Settings, !busy) {
            try { val address = PrinterAddress.validate(ip.trim()); ip = address; onSave(address) }
            catch (_: IllegalArgumentException) { error = "ใส่ IPv4 ให้ถูกต้อง เช่น 192.168.70.37" }
        }
        OutlinedButton(onClick = onProbe, enabled = !busy && networkAllowed && ip.trim() == host,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("ตรวจการเชื่อมต่อ") }
        Text(if (ip.trim() != host) "บันทึก IP ก่อนตรวจการเชื่อมต่อ" else "ตรวจ $host:9100 โดยไม่พิมพ์ฉลาก",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Feedback(busy, status, statusError)
    }
    HorizontalDivider()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("บริการพิมพ์ Android", style = MaterialTheme.typography.titleMedium)
        if (serviceEnabled && networkAllowed) {
            Text("บริการพิมพ์เปิดอยู่", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            OutlinedButton(onClick = onService, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("จัดการบริการพิมพ์") }
        } else Requirements(serviceEnabled, networkAllowed, onService, onPermission, onAppSettings)
    }
    HorizontalDivider()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ฉลากและการจัดหน้า", style = MaterialTheme.typography.titleMedium)
        CheckRow("${paper.label()} · 203 DPI", "ขาวดำ · ปรับเอกสารให้พอดีและรักษาสัดส่วน")
        CheckRow("ตำแหน่งพิมพ์", "ใช้ค่า Shift X/Y ที่ตั้งไว้ในเครื่องพิมพ์")
        Text("TSC Direct Print ${BuildConfig.VERSION_NAME}\nAndroid 13+ · แอปอิสระ ไม่ใช่แอปทางการของ TSC", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
