package de.openkleinanzeigen.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val GreenPrimary = Color(0xFF1B8F4E)
private val GreenDark = Color(0xFF0F5C32)
private val GreenLight = Color(0xFF4CAF50)
private val SurfaceTint = Color(0xFFE8F5E9)

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8E6C8),
    onPrimaryContainer = GreenDark,
    secondary = Color(0xFF2E7D32),
    onSecondary = Color.White,
    tertiary = Color(0xFF558B2F),
    background = Color(0xFFF5F7F5),
    onBackground = Color(0xFF1A1C1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = SurfaceTint,
    onSurfaceVariant = Color(0xFF424942),
    outline = Color(0xFFBFC8BF),
)

private val DarkColors = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color(0xFF003910),
    primaryContainer = GreenDark,
    onPrimaryContainer = Color(0xFFB8E6C8),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF003910),
    background = Color(0xFF101410),
    onBackground = Color(0xFFE2E3DE),
    surface = Color(0xFF1A1F1A),
    onSurface = Color(0xFFE2E3DE),
    surfaceVariant = Color(0xFF2A322A),
    onSurfaceVariant = Color(0xFFC2C9C0),
    outline = Color(0xFF4A524A),
)

@Composable
fun OpenKleinanzeigenTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
