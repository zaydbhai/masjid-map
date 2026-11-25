package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.AppThemeMode
import com.example.domain.ArabicFontOption
import com.example.domain.EnglishFontOption

private val LightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = BrassSecondary,
    onSecondary = BrassOnSecondary,
    secondaryContainer = BrassSecondaryContainer,
    onSecondaryContainer = BrassOnSecondaryContainer,
    tertiary = SlateTertiary,
    onTertiary = SlateOnTertiary,
    tertiaryContainer = SlateTertiaryContainer,
    onTertiaryContainer = SlateOnTertiaryContainer,
    background = AlabasterBackground,
    onBackground = InkOnBackground,
    surface = AlabasterSurface,
    onSurface = InkOnSurface,
    surfaceVariant = MintSurfaceVariant,
    onSurfaceVariant = MutedOnSurfaceVariant,
    outline = SageOutline,
    outlineVariant = SageOutlineVariant,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    error = ErrorLight
)

private val HighContrastLightColors = LightColors.copy(
    primary = HighContrastPrimaryLight,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF1F2B26),
    outline = HighContrastOutlineLight,
    outlineVariant = HighContrastOutlineVariantLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF3F1EC),
    surfaceContainer = Color(0xFFEAE7DF)
)

private val DarkColors = darkColorScheme(
    primary = MintPrimaryDark,
    onPrimary = MintOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = MintOnContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = GoldOnSecondaryDark,
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = GoldOnContainerDark,
    tertiary = SkyTertiaryDark,
    onTertiary = SkyOnTertiaryDark,
    tertiaryContainer = SkyContainerDark,
    onTertiaryContainer = SkyOnContainerDark,
    background = PineBackgroundDark,
    onBackground = MistOnBackgroundDark,
    surface = PineSurfaceDark,
    onSurface = MistOnSurfaceDark,
    surfaceVariant = PineSurfaceVariantDark,
    onSurfaceVariant = MistOnSurfaceVariantDark,
    outline = PineOutlineDark,
    outlineVariant = PineOutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    error = ErrorDark
)

private val AmoledColors = darkColorScheme(
    primary = MintPrimaryDark,
    onPrimary = MintOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = MintOnContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = GoldOnSecondaryDark,
    secondaryContainer = GoldContainerDark,
    onSecondaryContainer = GoldOnContainerDark,
    tertiary = SkyTertiaryDark,
    onTertiary = SkyOnTertiaryDark,
    tertiaryContainer = SkyContainerDark,
    onTertiaryContainer = SkyOnContainerDark,
    background = AmoledBackground,
    onBackground = MistOnBackgroundDark,
    surface = AmoledSurface,
    onSurface = MistOnSurfaceDark,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = MistOnSurfaceVariantDark,
    outline = PineOutlineDark,
    outlineVariant = PineOutlineVariantDark,
    surfaceContainerLowest = AmoledContainerLowest,
    surfaceContainerLow = AmoledContainerLow,
    surfaceContainer = AmoledContainer,
    surfaceContainerHigh = AmoledContainerHigh,
    surfaceContainerHighest = AmoledContainerHighest,
    error = ErrorDark
)

private val HighContrastDarkColors = AmoledColors.copy(
    primary = HighContrastPrimaryDark,
    onPrimary = Color.Black,
    secondary = HighContrastSecondaryDark,
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFE3EBE6),
    outline = HighContrastOutlineDark,
    outlineVariant = HighContrastOutlineVariantDark,
    surfaceContainerLow = Color(0xFF0C1210),
    surfaceContainer = Color(0xFF141C19)
)

val MasjidShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MasjidMapTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    highContrastLargePrint: Boolean = false,
    arabicFont: ArabicFontOption = ArabicFontOption.AMIRI,
    arabicFontSizeSp: Float = 28f,
    englishFont: EnglishFontOption = EnglishFontOption.PLUS_JAKARTA,
    englishFontSizeSp: Float = 16f,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK, AppThemeMode.AMOLED -> true
    }
    val colorScheme = when {
        highContrastLargePrint && useDark -> HighContrastDarkColors
        highContrastLargePrint -> HighContrastLightColors
        themeMode == AppThemeMode.SYSTEM -> if (systemDark) DarkColors else LightColors
        themeMode == AppThemeMode.LIGHT -> LightColors
        themeMode == AppThemeMode.DARK -> DarkColors
        else -> AmoledColors
    }
    val chosenEnglishFamily = englishFontFamilyFor(englishFont)
    val chosenArabicFamily = arabicFontFamilyFor(arabicFont)
    val effectiveArabicSize = if (highContrastLargePrint) arabicFontSizeSp.coerceAtLeast(32f) else arabicFontSizeSp
    val effectiveEnglishSize = if (highContrastLargePrint) englishFontSizeSp.coerceAtLeast(19f) else englishFontSizeSp

    val typography = buildAppTypography(
        displayFamily = chosenEnglishFamily,
        bodyFamily = chosenEnglishFamily,
        baseBodyScale = (englishFontSizeSp / 16f).coerceIn(0.85f, 1.35f),
        highContrast = highContrastLargePrint
    )
    val readingConfig = ReadingTypographyConfig(
        arabicFontFamily = chosenArabicFamily,
        arabicFontSizeSp = effectiveArabicSize,
        englishFontFamily = chosenEnglishFamily,
        englishFontSizeSp = effectiveEnglishSize,
        highContrastLargePrint = highContrastLargePrint
    )

    CompositionLocalProvider(LocalReadingTypography provides readingConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = MasjidShapes,
            content = content
        )
    }
}
