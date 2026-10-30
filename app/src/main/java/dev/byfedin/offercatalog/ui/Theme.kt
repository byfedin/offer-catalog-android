package dev.byfedin.offercatalog.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors =
    lightColorScheme(
        secondary = Color(0xFF47604A),
        secondaryContainer = Color(0xFFDCE8C7),
        onSecondaryContainer = Color(0xFF193A2E),
        primary = Color(0xFF285C4D),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDCE8C7),
        onPrimaryContainer = Color(0xFF193A2E),
        background = Color(0xFFF5F4EF),
        onBackground = Color(0xFF202E29),
        surface = Color(0xFFF5F4EF),
        onSurface = Color(0xFF202E29),
        surfaceContainerLow = Color(0xFFFEFDF9),
        surfaceContainer = Color(0xFFECEDE5),
        surfaceVariant = Color(0xFFE4E8DD),
        onSurfaceVariant = Color(0xFF556258),
        outline = Color(0xFF778279),
        outlineVariant = Color(0xFFD6DCD1),
    )
private val DarkColors =
    darkColorScheme(
        secondary = Color(0xFFB9CFAE),
        secondaryContainer = Color(0xFF324B3C),
        onSecondaryContainer = Color(0xFFDCE8C7),
        primary = Color(0xFFC7DFA2),
        onPrimary = Color(0xFF193A2E),
        primaryContainer = Color(0xFF324B3C),
        onPrimaryContainer = Color(0xFFDCE8C7),
        background = Color(0xFF101B1A),
        onBackground = Color(0xFFE6EAE0),
        surface = Color(0xFF101B1A),
        onSurface = Color(0xFFE6EAE0),
        surfaceContainerLow = Color(0xFF1B2925),
        surfaceContainer = Color(0xFF273730),
        surfaceVariant = Color(0xFF34443B),
        onSurfaceVariant = Color(0xFFB5C2B5),
        outline = Color(0xFF8C9B8E),
        outlineVariant = Color(0xFF3E5146),
    )
private val CatalogTypography =
    Typography(
        headlineLarge =
            TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 40.sp),
        headlineMedium =
            TextStyle(fontFamily = FontFamily.Serif, fontSize = 30.sp, lineHeight = 36.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 23.sp, lineHeight = 29.sp),
        titleMedium =
            TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        labelSmall =
            TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 1.3.sp,
            ),
    )

@Composable
fun CatalogTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CatalogTypography,
        content = content,
    )
}
