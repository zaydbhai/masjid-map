package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.ArabicFontOption
import com.example.domain.EnglishFontOption

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val DmSans = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    Font(R.font.dm_sans, FontWeight.Medium),
    Font(R.font.dm_sans, FontWeight.Bold)
)

val AmiriFontFamily = FontFamily(Font(R.font.amiri, FontWeight.Normal))
val ScheherazadeFontFamily = FontFamily(Font(R.font.scheherazade_new, FontWeight.Normal))
val NotoNaskhArabicFontFamily = FontFamily(Font(R.font.noto_naskh_arabic, FontWeight.Normal))
val MerriweatherFontFamily = FontFamily(Font(R.font.merriweather, FontWeight.Normal))
val InterFontFamily = FontFamily(Font(R.font.inter, FontWeight.Normal))

fun arabicFontFamilyFor(option: ArabicFontOption): FontFamily = when (option) {
    ArabicFontOption.AMIRI -> AmiriFontFamily
    ArabicFontOption.SCHEHERAZADE -> ScheherazadeFontFamily
    ArabicFontOption.NOTO_NASKH -> NotoNaskhArabicFontFamily
}

fun englishFontFamilyFor(option: EnglishFontOption): FontFamily = when (option) {
    EnglishFontOption.PLUS_JAKARTA -> PlusJakartaSans
    EnglishFontOption.DM_SANS -> DmSans
    EnglishFontOption.MERRIWEATHER -> MerriweatherFontFamily
    EnglishFontOption.INTER -> InterFontFamily
}

data class ReadingTypographyConfig(
    val arabicFontFamily: FontFamily = AmiriFontFamily,
    val arabicFontSizeSp: Float = 28f,
    val englishFontFamily: FontFamily = PlusJakartaSans,
    val englishFontSizeSp: Float = 16f,
    val highContrastLargePrint: Boolean = false
)

val LocalReadingTypography = staticCompositionLocalOf { ReadingTypographyConfig() }

fun buildAppTypography(
    displayFamily: FontFamily = PlusJakartaSans,
    bodyFamily: FontFamily = DmSans,
    baseBodyScale: Float = 1f,
    highContrast: Boolean = false
): Typography {
    val scale = if (highContrast) (baseBodyScale * 1.15f).coerceIn(1.1f, 1.45f) else baseBodyScale.coerceIn(0.85f, 1.35f)
    val bodySp = (15f * scale).coerceIn(13f, 23f)
    val headlineScale = if (highContrast) 1.12f else 1f
    val labelScale = if (highContrast) 1.12f else 1f

    return Typography(
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (32f * headlineScale).sp,
            lineHeight = (38f * headlineScale).sp,
            letterSpacing = (-0.25).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (26f * headlineScale).sp,
            lineHeight = (32f * headlineScale).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (22f * headlineScale).sp,
            lineHeight = (28f * headlineScale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20f * headlineScale).sp,
            lineHeight = (26f * headlineScale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16f * labelScale).sp,
            lineHeight = (22f * labelScale).sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14f * labelScale).sp,
            lineHeight = (20f * labelScale).sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = if (highContrast) FontWeight.Medium else FontWeight.Normal,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.52f).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = if (highContrast) FontWeight.Medium else FontWeight.Normal,
            fontSize = (bodySp - 1f).coerceAtLeast(13f).sp,
            lineHeight = ((bodySp - 1f).coerceAtLeast(13f) * 1.48f).sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = if (highContrast) FontWeight.Medium else FontWeight.Normal,
            fontSize = (12.5f * labelScale).sp,
            lineHeight = (17f * labelScale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14f * labelScale).sp,
            lineHeight = (20f * labelScale).sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (12f * labelScale).sp,
            lineHeight = (16f * labelScale).sp,
            letterSpacing = 0.25.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (11f * labelScale).sp,
            lineHeight = (14f * labelScale).sp,
            letterSpacing = 0.3.sp
        )
    )
}
