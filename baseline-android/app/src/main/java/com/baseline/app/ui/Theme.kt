package com.baseline.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.baseline.app.data.Grade

object Palette {
    val Bg = Color(0xFF0E1116)
    val Surface = Color(0xFF161B22)
    val SurfaceAlt = Color(0xFF1C232C)
    val Line = Color(0xFF2A323D)
    val Text = Color(0xFFE6EDF3)
    val TextDim = Color(0xFF8B98A5)
    val Accent = Color(0xFF6EE7B7)
    val AccentDim = Color(0xFF2F6F58)
    val Warn = Color(0xFFF0B429)
    val Bad = Color(0xFFF26D6D)
    val Info = Color(0xFF6EA8FE)
}

fun gradeColor(grade: Grade): Color = when (grade) {
    Grade.A -> Palette.Accent
    Grade.B -> Palette.Info
    Grade.C -> Palette.Warn
    Grade.D -> Palette.Bad
}

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp)
)

@Composable
fun BaselineTheme(content: @Composable () -> Unit) {
    // Dark-only by design: this is an instrument, and the palette is part of it.
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Accent,
            onPrimary = Color(0xFF06231A),
            secondary = Palette.Info,
            background = Palette.Bg,
            onBackground = Palette.Text,
            surface = Palette.Surface,
            onSurface = Palette.Text,
            surfaceVariant = Palette.SurfaceAlt,
            onSurfaceVariant = Palette.TextDim,
            outline = Palette.Line,
            error = Palette.Bad
        ),
        typography = AppTypography,
        content = content
    )
}
