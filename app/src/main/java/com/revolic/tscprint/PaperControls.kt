package com.revolic.tscprint

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.revolic.tscprint.core.PaperSize

@Composable fun PaperSelector(paper: PaperSize, papers: List<PaperSize>, enabled: Boolean, onSelect: (PaperSize) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("ขนาดกระดาษ", style = MaterialTheme.typography.titleSmall)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(paper.label(), style = MaterialTheme.typography.titleMedium)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                papers.forEach { option ->
                    DropdownMenuItem(text = { Text(option.label()) }, onClick = { expanded = false; onSelect(option) })
                }
            }
        }
        Text("กว้างตามหัวพิมพ์ × สูงตามแนวป้อน", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun PaperManager(paper: PaperSize, papers: List<PaperSize>, enabled: Boolean,
                             onSelect: (PaperSize) -> Unit, onAdd: (PaperSize) -> String?) {
    var adding by rememberSaveable { mutableStateOf(false) }
    var width by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PaperSelector(paper, papers, enabled, onSelect)
        if (!adding) OutlinedButton(onClick = { adding = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("เพิ่มขนาดกระดาษเอง") }
        else {
            OutlinedTextField(value = width, onValueChange = { width = it; error = null }, enabled = enabled, singleLine = true,
                label = { Text("กว้าง (มม.)") }, supportingText = { Text("20–168 มม. ตามแนวหัวพิมพ์") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }.testTag("paper-width"), isError = error != null)
            OutlinedTextField(value = height, onValueChange = { height = it; error = null }, enabled = enabled, singleLine = true,
                label = { Text("สูง (มม.)") }, supportingText = { Text("20–500 มม. ตามแนวป้อนกระดาษ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().semantics { testTagsAsResourceId = true }.testTag("paper-height"), isError = error != null)
            Text("กรอกทศนิยมได้ 1 ตำแหน่ง · บันทึกขนาดเองได้ 20 ขนาด", style = MaterialTheme.typography.bodySmall)
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = {
                try {
                    val added = PaperSize.parse(width, height)
                    error = onAdd(added)
                    if (error == null) { adding = false; width = ""; height = ""; keyboard?.hide(); focus.clearFocus() }
                } catch (e: IllegalArgumentException) { error = e.message }
            }, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("บันทึกและใช้ขนาดนี้") }
            TextButton(onClick = { adding = false; error = null; keyboard?.hide(); focus.clearFocus() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("ยกเลิกเพิ่มขนาด") }
        }
    }
}
