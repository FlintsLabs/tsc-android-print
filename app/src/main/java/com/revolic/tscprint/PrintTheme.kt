package com.revolic.tscprint

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warm paper surfaces and deep green ink; status always has a text label.
private val Light = lightColorScheme(
    primary = Color(0xFF245C49), onPrimary = Color.White,
    secondary = Color(0xFF245C49), onSecondary = Color.White,
    background = Color(0xFFFAFAF5), onBackground = Color(0xFF202822),
    primaryContainer = Color(0xFF163F32), onPrimaryContainer = Color(0xFFF0F8F2),
    secondaryContainer = Color(0xFFE1EBE3), onSecondaryContainer = Color(0xFF234135),
    surface = Color(0xFFFAFAF5), onSurface = Color(0xFF202822),
    surfaceContainer = Color(0xFFF0F1EB), surfaceContainerLow = Color(0xFFF3F4EE),
    surfaceContainerHighest = Color(0xFFE7EAE2), onSurfaceVariant = Color(0xFF505B52),
    outline = Color(0xFF707D72), outlineVariant = Color(0xFFC7CEC4)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFA3D6B7), onPrimary = Color(0xFF133A29),
    secondary = Color(0xFFA3D6B7), onSecondary = Color(0xFF133A29),
    background = Color(0xFF141A16), onBackground = Color(0xFFE4EAE2),
    primaryContainer = Color(0xFF244B39), onPrimaryContainer = Color(0xFFE6F5E9),
    secondaryContainer = Color(0xFF344A3C), onSecondaryContainer = Color(0xFFDCEBDD),
    surface = Color(0xFF141A16), onSurface = Color(0xFFE4EAE2),
    surfaceContainer = Color(0xFF202922), surfaceContainerLow = Color(0xFF1A231D),
    surfaceContainerHighest = Color(0xFF303B32), onSurfaceVariant = Color(0xFFB9C6BA),
    outline = Color(0xFF89998B), outlineVariant = Color(0xFF424F44)
)
@Composable fun PrintTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
