package com.revolic.tscprint

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable fun UpdatePanel(state: UpdateUiState, canInstall: Boolean, onCheck: () -> Unit, onDownload: () -> Unit,
                           onInstall: () -> Unit, onAllowInstall: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("อัปเดตแอป", style = MaterialTheme.typography.titleMedium)
        Text("เวอร์ชันปัจจุบัน ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
        Text("GitHub · FlintsLabs/tsc-android-print", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(state.message, style = MaterialTheme.typography.bodyMedium,
            color = if (state.phase == UpdatePhase.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        if (state.phase == UpdatePhase.DOWNLOADING) {
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
            Text("ดาวน์โหลด ${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("ยกเลิกดาวน์โหลด") }
        } else if (state.working) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedButton(onClick = onCheck, enabled = !state.working,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { testTagsAsResourceId = true }.testTag("check-update")) { Text("ตรวจสอบอัปเดต") }
        if (!state.working && state.release != null) {
            if (state.phase == UpdatePhase.READY) {
                Button(onClick = if (canInstall) onInstall else onAllowInstall,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { testTagsAsResourceId = true }.testTag("install-update")) {
                    Text(if (canInstall) "ติดตั้งอัปเดต ${state.release.version}" else "อนุญาตติดตั้งจากแอปนี้")
                }
                Text("ติดตั้งเมื่องานพิมพ์เสร็จแล้ว · Android จะให้กดยืนยันอัปเดต ข้อมูลเครื่องพิมพ์ยังอยู่", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { testTagsAsResourceId = true }.testTag("download-update")) {
                    Text("ดาวน์โหลด ${state.release.version} (${String.format(Locale.US,"%.1f",state.release.size / 1048576.0)} MB)")
                }
                Text("ตรวจ checksum และลายเซ็นก่อนติดตั้ง ใช้เฉพาะรุ่นปกติที่เผยแพร่บน GitHub", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
