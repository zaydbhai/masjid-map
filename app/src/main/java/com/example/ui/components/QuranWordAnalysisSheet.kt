package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.QuranTajwidEngine
import com.example.domain.QuranWordAnalysis
import com.example.domain.TajwidPalette
import com.example.domain.TajwidRule

/**
 * Renders unbroken Uthmani RTL Arabic script with optional Tajwid color accents and accurate
 * tap-to-word-lookup via [TextLayoutResult.getOffsetForPosition], preserving Arabic ligatures.
 */
@Composable
fun TappableTajwidArabicText(
    arabicText: String,
    tajwidEnabled: Boolean,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.End,
    onWordTap: (wordIndex: Int, surfaceWord: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val palette = remember(isDark) { TajwidPalette.forTheme(isDark) }
    val annotated = remember(arabicText, tajwidEnabled, palette) {
        if (tajwidEnabled) {
            QuranTajwidEngine.buildTajwidAnnotatedString(arabicText, palette)
        } else {
            AnnotatedString(arabicText)
        }
    }
    val tokens = remember(arabicText) {
        QuranTajwidEngine.tokenizeArabicWords(arabicText)
    }
    var layoutResult by remember(arabicText) { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotated,
        fontFamily = fontFamily,
        fontSize = fontSize,
        lineHeight = lineHeight,
        textAlign = textAlign,
        color = textColor,
        onTextLayout = { layoutResult = it },
        modifier = modifier.pointerInput(arabicText, tokens) {
            detectTapGestures { offset ->
                val layout = layoutResult ?: return@detectTapGestures
                val charIdx = layout.getOffsetForPosition(offset)
                val tapped = tokens.firstOrNull { charIdx in it.startOffset..it.endOffset }
                    ?: tokens.minByOrNull { token ->
                        when {
                            charIdx < token.startOffset -> token.startOffset - charIdx
                            charIdx > token.endOffset -> charIdx - token.endOffset
                            else -> 0
                        }
                    }
                if (tapped != null) {
                    onWordTap(tapped.wordIndex, tapped.surfaceWord)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuranWordAnalysisSheet(
    analysis: QuranWordAnalysis,
    verseRef: Pair<Int, Int>?,
    arabicFontFamily: FontFamily,
    tajwidEnabled: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isSystemInDarkTheme()
    val palette = remember(isDark) { TajwidPalette.forTheme(isDark) }
    val tajwidWord = remember(analysis.surfaceWord, tajwidEnabled, palette) {
        if (tajwidEnabled) {
            QuranTajwidEngine.buildTajwidAnnotatedString(analysis.surfaceWord, palette)
        } else {
            AnnotatedString(analysis.surfaceWord)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .testTag("quran_word_analysis_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (verseRef != null) {
                            "Word Analysis · ${verseRef.first}:${verseRef.second}"
                        } else {
                            "Word & Root Analysis"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!analysis.transliteration.isNullOrBlank()) {
                        Text(
                            text = analysis.transliteration,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = tajwidWord,
                        fontFamily = arabicFontFamily,
                        fontSize = 30.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!analysis.contextualGloss.isNullOrBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "WORD MEANING",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = analysis.contextualGloss,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TRILATERAL ROOT (جذر)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = analysis.rootLetters,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = analysis.morphologyNote,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = analysis.coreMeaning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            analysis.dominantTajwidRule?.let { rule ->
                val ruleColor = palette.colorFor(rule)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ruleColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, ruleColor.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ruleColor,
                            modifier = Modifier.size(12.dp)
                        ) {}
                        Column {
                            Text(
                                text = "Tajwid Rule · ${rule.label} (${rule.arabicLabel})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = rule.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (tajwidEnabled) {
                Text(
                    text = "TAJWID COLOR KEY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TajwidRule.entries.forEach { rule ->
                        val c = palette.colorFor(rule)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = c.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, c.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = c,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Text(
                                    text = rule.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
