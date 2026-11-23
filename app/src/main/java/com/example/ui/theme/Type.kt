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
    val englishFontSizeSp: Float = 16f
)

val LocalReadingTypography = staticCompositionLocalOf { ReadingTypographyConfig() }

fun buildAppTypography(
    displayFamily: FontFamily = PlusJakartaSans,
    bodyFamily: FontFamily = DmSans,
    baseBodyScale: Float = 1f
): Typography {
    val bodySp = (15f * baseBodyScale).coerceIn(12f, 22f)
    return Typography(
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.25).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp
        ),
        titleSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = bodySp.sp,
            lineHeight = (bodySp * 1.48f).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (bodySp - 1f).coerceAtLeast(12f).sp,
            lineHeight = ((bodySp - 1f).coerceAtLeast(12f) * 1.45f).sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.4.sp
        )
    )
}
