package uz.azoff.launcher

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle

val accentColors = listOf(
    Color(0xFF4F7DF3),
    Color(0xFF00A86B),
    Color(0xFFE5484D),
    Color(0xFFF59E0B),
    Color(0xFF8B5CF6),
    Color(0xFFEC4899),
    Color(0xFF14B8A6),
    Color(0xFF64748B),
)

/** Fon rasmi ustidagi matn uchun soyali oq uslub */
val wallpaperText = TextStyle(
    color = Color.White,
    shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), offset = Offset(0f, 2f), blurRadius = 6f),
)

@Composable
fun isDark(settings: LauncherSettings): Boolean = when (settings.theme) {
    1 -> false
    2 -> true
    else -> isSystemInDarkTheme()
}

@Composable
fun AzoffTheme(settings: LauncherSettings, content: @Composable () -> Unit) {
    val dark = isDark(settings)
    val ctx = LocalContext.current
    val scheme = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        val a = accentColors[settings.accent.coerceIn(0, accentColors.size - 1)]
        if (dark) darkColorScheme(primary = a, secondary = a, tertiary = a)
        else lightColorScheme(primary = a, secondary = a, tertiary = a)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
