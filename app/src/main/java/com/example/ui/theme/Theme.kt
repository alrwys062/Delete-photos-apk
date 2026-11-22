package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.prefs.ThemeMode

@Immutable
data class TimeDeleteExtendedColors(
    val isDark: Boolean,
    val cardBorder: Color,
    val elevatedCard: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val videoAccent: Color
)

val LocalTimeDeleteColors = staticCompositionLocalOf {
    TimeDeleteExtendedColors(
        isDark = true,
        cardBorder = ObsidianBorder,
        elevatedCard = ObsidianSurfaceElevated,
        gradientStart = ElectricViolet,
        gradientEnd = Color(0xFF5B21B6),
        success = EmeraldSuccess,
        successContainer = EmeraldSuccessContainerDark,
        warning = AmberWarning,
        warningContainer = AmberWarningContainerDark,
        danger = RoseDanger,
        dangerContainer = RoseDangerContainerDark,
        videoAccent = CyanVideo
    )
}

private val TimeDeleteDarkScheme = darkColorScheme(
    primary = ElectricVioletBright,
    onPrimary = Color.White,
    primaryContainer = DeepVioletContainer,
    onPrimaryContainer = SoftLavender,
    secondary = SoftLavender,
    onSecondary = Color(0xFF1E1042),
    secondaryContainer = ObsidianSurfaceHigh,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = CyanVideo,
    onTertiary = Color(0xFF042F40),
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF23213C),
    error = RoseDanger,
    onError = Color.White,
    errorContainer = RoseDangerContainerDark,
    onErrorContainer = Color(0xFFFFD9E0)
)

private val TimeDeleteLightScheme = lightColorScheme(
    primary = RoyalViolet,
    onPrimary = Color.White,
    primaryContainer = RoyalVioletContainer,
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = ElectricViolet,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceElevated,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    outlineVariant = LightSurfaceHigh,
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = RoseDangerContainerLight,
    onErrorContainer = Color(0xFF881337)
)

val TimeDeleteShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TimeDeleteTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDark) TimeDeleteDarkScheme else TimeDeleteLightScheme
    val extendedColors = if (useDark) {
        TimeDeleteExtendedColors(
            isDark = true,
            cardBorder = ObsidianBorder,
            elevatedCard = ObsidianSurfaceElevated,
            gradientStart = ElectricVioletBright,
            gradientEnd = Color(0xFF5B21B6),
            success = EmeraldSuccess,
            successContainer = EmeraldSuccessContainerDark,
            warning = AmberWarning,
            warningContainer = AmberWarningContainerDark,
            danger = RoseDanger,
            dangerContainer = RoseDangerContainerDark,
            videoAccent = CyanVideo
        )
    } else {
        TimeDeleteExtendedColors(
            isDark = false,
            cardBorder = LightBorder,
            elevatedCard = LightSurface,
            gradientStart = RoyalViolet,
            gradientEnd = ElectricVioletBright,
            success = Color(0xFF059669),
            successContainer = EmeraldSuccessContainerLight,
            warning = Color(0xFFD97706),
            warningContainer = AmberWarningContainerLight,
            danger = Color(0xFFE11D48),
            dangerContainer = RoseDangerContainerLight,
            videoAccent = Color(0xFF0284C7)
        )
    }

    CompositionLocalProvider(LocalTimeDeleteColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TimeDeleteTypography,
            shapes = TimeDeleteShapes,
            content = content
        )
    }
}
