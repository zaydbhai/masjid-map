package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
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

val MasjidShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MasjidMapTheme(
    themeMode: com.example.domain.AppThemeMode = com.example.domain.AppThemeMode.SYSTEM,
    arabicFont: ArabicFontOption = ArabicFontOption.AMIRI,
    arabicFontSizeSp: Float = 28f,
    englishFont: EnglishFontOption = EnglishFontOption.PLUS_JAKARTA,
    englishFontSizeSp: Float = 16f,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        com.example.domain.AppThemeMode.SYSTEM -> if (systemDark) DarkColors else LightColors
        com.example.domain.AppThemeMode.LIGHT -> LightColors
        com.example.domain.AppThemeMode.DARK -> DarkColors
        com.example.domain.AppThemeMode.AMOLED -> AmoledColors
    }
    val chosenEnglishFamily = englishFontFamilyFor(englishFont)
    val chosenArabicFamily = arabicFontFamilyFor(arabicFont)
    val typography = buildAppTypography(
        displayFamily = chosenEnglishFamily,
        bodyFamily = chosenEnglishFamily,
        baseBodyScale = (englishFontSizeSp / 16f).coerceIn(0.85f, 1.35f)
    )
    val readingConfig = ReadingTypographyConfig(
        arabicFontFamily = chosenArabicFamily,
        arabicFontSizeSp = arabicFontSizeSp,
        englishFontFamily = chosenEnglishFamily,
        englishFontSizeSp = englishFontSizeSp
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
