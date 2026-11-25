package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DuaSituationCategory
import com.example.domain.IslamicGuidesCatalog
import com.example.domain.SituationalDua
import com.example.ui.theme.LocalReadingTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuasAndGuidesSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val reading = LocalReadingTypography.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(DuaSituationCategory.ALL) }
    var selectedGuideId by remember { mutableStateOf(IslamicGuidesCatalog.practicalGuides.first().id) }

    val filteredDuas = remember(searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        IslamicGuidesCatalog.situationalDuas.filter { dua ->
            val catMatch = selectedCategory == DuaSituationCategory.ALL || dua.category == selectedCategory
            val qMatch = q.isEmpty() ||
                dua.title.lowercase().contains(q) ||
                dua.translation.lowercase().contains(q) ||
                dua.transliteration.lowercase().contains(q)
            catMatch && qMatch
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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .testTag("duas_guides_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Sunnah Duas & Practical Guides",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Offline everyday supplications + Wudu, Salah & Umrah steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Situational Duas") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Wudu · Salah · Umrah") }
                )
            }

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search travel, rain, masjid, istikhara…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DuaSituationCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDuas, key = { it.id }) { dua ->
                        DuaCard(
                            dua = dua,
                            arabicFont = reading.arabicFontFamily,
                            onShare = {
                                val text = "${dua.title}\n\n${dua.arabic}\n${dua.transliteration}\n\n\"${dua.translation}\"\n(${dua.reference})"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(intent, "Share Dua"))
                                }
                            }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IslamicGuidesCatalog.practicalGuides.forEach { guide ->
                        FilterChip(
                            selected = selectedGuideId == guide.id,
                            onClick = { selectedGuideId = guide.id },
                            label = {
                                Text(
                                    when (guide.id) {
                                        "wudu" -> "Wudu"
                                        "salah" -> "Salah"
                                        else -> "Umrah"
                                    }
                                )
                            }
                        )
                    }
                }

                val activeGuide = IslamicGuidesCatalog.practicalGuides.firstOrNull { it.id == selectedGuideId }
                    ?: IslamicGuidesCatalog.practicalGuides.first()

                Text(
                    text = activeGuide.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activeGuide.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeGuide.steps, key = { "${activeGuide.id}_${it.stepNumber}" }) { step ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = step.stepNumber.toString(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                    Text(
                                        text = step.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = step.instruction,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (step.arabic.isNotBlank()) {
                                    Text(
                                        text = step.arabic,
                                        fontFamily = reading.arabicFontFamily,
                                        fontSize = 20.sp,
                                        lineHeight = 32.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (step.transliteration.isNotBlank()) {
                                    Text(
                                        text = step.transliteration,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuaCard(
    dua: SituationalDua,
    arabicFont: androidx.compose.ui.text.font.FontFamily,
    onShare: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dua.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onShare, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share ${dua.title}",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = dua.arabic,
                fontFamily = arabicFont,
                fontSize = 21.sp,
                lineHeight = 34.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = dua.transliteration,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = dua.translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dua.reference,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
