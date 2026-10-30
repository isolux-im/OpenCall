package im.isolux.opencall.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Palette « Silver Liquid Glass » : gris glace, reflets miroir, verre translucide. */
@Immutable
data class GlassColors(
    val backgroundTop: Color,
    val backgroundMiddle: Color,
    val backgroundBottom: Color,
    val highlight: Color,
    val glassFillTop: Color,
    val glassFillBottom: Color,
    val glassStrokeTop: Color,
    val glassStrokeBottom: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val callGreen: Color,
    val callRed: Color,
    val warning: Color,
    val isDark: Boolean,
)

private val LightGlass = GlassColors(
    backgroundTop = Color(0xFFF6F8FA),
    backgroundMiddle = Color(0xFFE4E8ED),
    backgroundBottom = Color(0xFFD3D9E0),
    highlight = Color(0x99FFFFFF),
    glassFillTop = Color(0xB3FFFFFF),
    glassFillBottom = Color(0x66F1F4F7),
    glassStrokeTop = Color(0xF2FFFFFF),
    glassStrokeBottom = Color(0x40A7B0BB),
    textPrimary = Color(0xFF1C1E22),
    textSecondary = Color(0xFF5F6772),
    accent = Color(0xFF3E4752),
    callGreen = Color(0xFF34C759),
    callRed = Color(0xFFFF3B30),
    warning = Color(0xFFB7791F),
    isDark = false,
)

private val DarkGlass = GlassColors(
    backgroundTop = Color(0xFF30343A),
    backgroundMiddle = Color(0xFF212429),
    backgroundBottom = Color(0xFF141619),
    highlight = Color(0x22FFFFFF),
    glassFillTop = Color(0x29FFFFFF),
    glassFillBottom = Color(0x0FFFFFFF),
    glassStrokeTop = Color(0x59FFFFFF),
    glassStrokeBottom = Color(0x14FFFFFF),
    textPrimary = Color(0xFFF2F4F7),
    textSecondary = Color(0xFFA9B1BC),
    accent = Color(0xFFD5DBE2),
    callGreen = Color(0xFF30D158),
    callRed = Color(0xFFFF453A),
    warning = Color(0xFFFFC266),
    isDark = true,
)

val LocalGlassColors = staticCompositionLocalOf { LightGlass }

object Glass {
    val colors: GlassColors
        @Composable get() = LocalGlassColors.current
}

@Composable
fun OpenCallTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val glass = if (dark) DarkGlass else LightGlass
    val scheme = if (dark) {
        darkColorScheme(
            primary = glass.accent, onPrimary = Color(0xFF15171A),
            background = glass.backgroundBottom, surface = Color(0xFF24272C),
            onSurface = glass.textPrimary, onBackground = glass.textPrimary,
            surfaceVariant = Color(0xFF2E3238), onSurfaceVariant = glass.textSecondary,
            error = glass.callRed,
        )
    } else {
        lightColorScheme(
            primary = glass.accent, onPrimary = Color.White,
            background = glass.backgroundTop, surface = Color(0xFFF7F9FB),
            onSurface = glass.textPrimary, onBackground = glass.textPrimary,
            surfaceVariant = Color(0xFFE6EAEE), onSurfaceVariant = glass.textSecondary,
            error = glass.callRed,
        )
    }
    val base = Typography()
    val typography = base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    )
    CompositionLocalProvider(LocalGlassColors provides glass) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
