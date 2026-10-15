package com.revolic.tscprint

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.revolic.tscprint.core.PaperSize
import com.revolic.tscprint.core.PrinterProfile

@Composable fun PrinterSelector(printer: PrinterProfile, printers: List<PrinterProfile>, enabled: Boolean,
                                onSelect: (String) -> Unit,
                                contentColor: Color = MaterialTheme.colorScheme.primary,
                                supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("เลือกเครื่องพิมพ์ · ${printers.size} เครื่อง", style = MaterialTheme.typography.titleSmall)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor,
                    disabledContentColor = contentColor.copy(alpha = 0.38f)),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { testTagsAsResourceId = true }.testTag("printer-selector")) {
                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                    Text(printer.name, style = MaterialTheme.typography.titleMedium)
                    Text(printer.endpoint(), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(12.dp))
                Icon(PrintIcons.ChevronDown, null, Modifier.size(24.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                printers.forEach { option ->
                    DropdownMenuItem(text = {
                        Column {
                            Text(option.name)
                            Text(option.endpoint(), style = MaterialTheme.typography.bodySmall)
                            Text(PaperSize.fromId(option.paperId).label(), style = MaterialTheme.typography.bodySmall)
                        }
                    }, onClick = { expanded = false; onSelect(option.id) })
                }
            }
        }
        Text("แตะเพื่อเปลี่ยนเครื่อง · ตรวจเครื่องปลายทางในหน้าต่าง Print อีกครั้ง", style = MaterialTheme.typography.bodySmall,
            color = supportingColor)
    }
}

/** Settings navigation is independent of the printer selected for printing. */
@Composable fun PrinterList(printers: List<PrinterProfile>, enabled: Boolean, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("เครื่องพิมพ์ที่บันทึก", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${printers.size}/20 เครื่อง", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("แตะเครื่องเพื่อเปิดการตั้งค่า · เพิ่มได้หลาย IP", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onAdd, enabled = enabled && printers.size < 20, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Icon(PrintIcons.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("เพิ่มเครื่องพิมพ์")
        }
        if (printers.size == 20) Text("บันทึกครบ 20 เครื่องแล้ว · ลบเครื่องที่ไม่ใช้ก่อนเพิ่มเครื่องใหม่", style = MaterialTheme.typography.bodySmall)
        printers.forEach { option ->
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }.testTag("saved-printer-${option.id}")
                    .clickable(enabled = enabled, onClickLabel = "ตั้งค่า ${option.name}") { onOpen(option.id) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(PrintIcons.Printer, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(option.name, style = MaterialTheme.typography.titleMedium)
                        Text(option.endpoint(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("กระดาษเริ่มต้น ${PaperSize.fromId(option.paperId).label()}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(PrintIcons.ChevronRight, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text("เลือกเครื่องที่จะพิมพ์ได้ในหน้า พิมพ์ / Print Test", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun PrinterEditor(profile: PrinterProfile?, defaultPaper: PaperSize, papers: List<PaperSize>, enabled: Boolean,
                              onSave: (String?, String, String, Int, PaperSize) -> String?, onAddPaper: (PaperSize) -> String?,
                              onDelete: (String) -> String?, onProbe: (PrinterProfile) -> Unit, onPrintTest: (PrinterProfile) -> Unit,
                              networkAllowed: Boolean, serviceEnabled: Boolean, canDelete: Boolean,
                              deletionFallback: String?, onBack: () -> Unit, feedback: @Composable () -> Unit) {
    var name by rememberSaveable { mutableStateOf(profile?.name ?: "") }
    var ip by rememberSaveable { mutableStateOf(profile?.host ?: "") }
    var port by rememberSaveable { mutableStateOf(profile?.port?.toString() ?: "9100") }
    var paperId by rememberSaveable { mutableStateOf(profile?.paperId ?: defaultPaper.id()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val dirty = profile == null || name != profile.name || ip != profile.host || port != profile.port.toString() || paperId != profile.paperId
    fun close() { keyboard?.hide(); focus.clearFocus(); onBack() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it; error = null }, label = { Text("ชื่อเครื่องพิมพ์") },
            singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }.testTag("printer-name"),
            supportingText = { Text("1–48 ตัวอักษร เช่น จุดแพ็กสินค้า") })
        OutlinedTextField(value = ip, onValueChange = { ip = it; error = null }, label = { Text("IP เครื่องพิมพ์") },
            singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }.testTag("printer-ip"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), supportingText = { Text("IPv4 เช่น 192.168.70.37") })
        OutlinedTextField(value = port, onValueChange = { port = it; error = null }, label = { Text("พอร์ต") },
            singleLine = true, enabled = enabled, modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }.testTag("printer-port"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), supportingText = { Text("ปกติใช้ 9100 · รองรับ 1–65535") })
        Text("กระดาษเริ่มต้นของเครื่องนี้", style = MaterialTheme.typography.titleSmall)
        PaperManager(PaperSize.fromId(paperId), papers, enabled, onSelect = { paperId = it.id() }, onAdd = {
            val result = onAddPaper(it)
            if (result == null) paperId = it.id()
            result
        })
        Text("การเปลี่ยนแปลงมีผลเมื่อกดบันทึกเครื่องพิมพ์", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = {
            val number = port.toIntOrNull()
            error = if (number == null) "พอร์ตต้องอยู่ระหว่าง 1–65535"
                else onSave(profile?.id, name, ip.trim(), number, PaperSize.fromId(paperId))
            if (error == null) close()
        }, enabled = enabled && dirty, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("บันทึกเครื่องพิมพ์") }
        TextButton(onClick = { close() }, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) { Text("ยกเลิกแก้ไขเครื่องพิมพ์") }
        if (profile != null) {
            HorizontalDivider()
            Text("ทดสอบ ${profile.name}", style = MaterialTheme.typography.titleMedium)
            Text("${profile.endpoint()} · ${PaperSize.fromId(profile.paperId).label()}", style = MaterialTheme.typography.bodyMedium)
            if (dirty) Text("บันทึกการเปลี่ยนแปลงก่อนตรวจการเชื่อมต่อหรือพิมพ์ทดสอบ", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { onProbe(profile) }, enabled = enabled && networkAllowed && !dirty,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("ตรวจการเชื่อมต่อ") }
            OutlinedButton(onClick = { onPrintTest(profile) }, enabled = enabled && networkAllowed && serviceEnabled && !dirty,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("พิมพ์ทดสอบเครื่องนี้") }
            Text("เลือก ${profile.name} ในหน้าต่าง Print ก่อนยืนยัน · ทดสอบ 1 หน้า", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            feedback()
            HorizontalDivider()
            TextButton(onClick = { deleting = true }, enabled = enabled && canDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error), modifier = Modifier.heightIn(min = 48.dp)) { Text("ลบเครื่องพิมพ์นี้") }
            if (!canDelete) Text("เพิ่มเครื่องใหม่ก่อนลบเครื่องสุดท้าย", style = MaterialTheme.typography.bodySmall)
        }
    }
    if (deleting && profile != null) AlertDialog(onDismissRequest = { deleting = false }, title = { Text("ลบ ${profile.name}?") },
        text = { Text("นำ ${profile.endpoint()} ออกจากรายการเครื่องพิมพ์" +
            (deletionFallback?.let { "\nเครื่องนี้ถูกเลือกพิมพ์อยู่ หลังลบจะเปลี่ยนเป็น $it" } ?: "")) },
        confirmButton = { TextButton(onClick = {
            error = onDelete(profile.id); deleting = false
            if (error == null) close()
        }) { Text("ลบเครื่องพิมพ์") } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text("ยกเลิก") } })
}
