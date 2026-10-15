package com.revolic.tscprint

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// One 24dp outlined icon family, without a large icon dependency.
object PrintIcons {
    val Printer = ImageVector.Builder("Printer", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = androidx.compose.ui.graphics.SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(7f,8f);verticalLineTo(3f);horizontalLineTo(17f);verticalLineTo(8f)
            moveTo(7f,17f);horizontalLineTo(3f);verticalLineTo(8f);horizontalLineTo(21f);verticalLineTo(17f);horizontalLineTo(17f)
            moveTo(7f,14f);horizontalLineTo(17f);verticalLineTo(21f);horizontalLineTo(7f);close()
            moveTo(17f,11f);horizontalLineTo(19f)
        }
    }.build()
    val Test = ImageVector.Builder("TestLabel", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = androidx.compose.ui.graphics.SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(5f,3f);horizontalLineTo(19f);verticalLineTo(21f);horizontalLineTo(5f);close()
            moveTo(8f,7f);horizontalLineTo(16f);moveTo(8f,11f);horizontalLineTo(13f)
            moveTo(8f,16f);lineTo(11f,18f);lineTo(16f,14f)
        }
    }.build()
    val Settings = ImageVector.Builder("Settings", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = null, stroke = androidx.compose.ui.graphics.SolidColor(Color.Black), strokeLineWidth = 1.8f) {
            moveTo(4f,7f);horizontalLineTo(8f);moveTo(12f,7f);horizontalLineTo(20f)
            moveTo(4f,17f);horizontalLineTo(12f);moveTo(16f,17f);horizontalLineTo(20f)
            moveTo(8f,4f);horizontalLineTo(12f);verticalLineTo(10f);horizontalLineTo(8f);close()
            moveTo(12f,14f);horizontalLineTo(16f);verticalLineTo(20f);horizontalLineTo(12f);close()
        }
    }.build()
}
