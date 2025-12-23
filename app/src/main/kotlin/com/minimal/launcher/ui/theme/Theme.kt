package com.minimal.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

// Global CLI Colors
val TerminalBlue = Color(0xFF00D4FF)
val DeepNavy = Color(0xFF0A0B10)

@Composable
fun LauncherTheme(darkText: Boolean = false, content: @Composable () -> Unit) {
    val dynamicColor = if (darkText) Color(0xFF1A1A1A) else TerminalBlue
    
    val colorScheme = darkColorScheme(
        primary = dynamicColor,
        background = Color.Transparent, // Let wallpaper show through
        onBackground = dynamicColor,
        surface = DeepNavy,
        onSurface = dynamicColor
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            displayLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 64.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
            labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        ),
        content = content
    )
}
