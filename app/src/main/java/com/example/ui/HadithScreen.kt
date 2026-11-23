package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.HadithBookCategory
import com.example.domain.HadithBookInfo
import com.example.domain.HadithChapter
import com.example.domain.HadithEntry
import com.example.ui.theme.LocalReadingTypography

@Composable
fun HadithScreen(
    onOpenSettings: () -> Unit,
    hadithViewModel: HadithViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by hadithViewModel.uiState.collectAsStateWithLifecycle()
    val reading = LocalReadingTypography.current
    val context = LocalContext.current

    BackHandler(enabled = state.selectedBook != null) {
        hadithViewModel.navigateBackInsideHadith()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val selectedBook = state.selectedBook
        val selectedChapter = state.selectedChapter

        if (selectedBook == null) {
            // Level 1: Top 21 Hadith Books Library
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hadith Library",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Top 21 Canonical & Classical Books",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(
                            onClick = { hadithViewModel.downloadEssentialHadithPack() },
                            enabled = !state.isDownloadingEssentialPack,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("hadith_essential_pack_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (state.isDownloadingEssentialPack) "Saving…" else "Save Pack")
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("hadith_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Font settings"
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.bookSearchQuery,
                    onValueChange = hadithViewModel::onBookSearchChanged,
                    placeholder = { Text("Search 21 books or compilers…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.bookSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { hadithViewModel.onBookSearchChanged("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hadith_book_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HadithBookCategory.entries.forEach { category ->
                        FilterChip(
                            selected = category == state.selectedCategory,
                            onClick = { hadithViewModel.selectCategory(category) },
                            label = { Text(category.displayName) },
                            modifier = Modifier.testTag("hadith_cat_${category.name}")
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("hadith_books_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.books, key = { it.id }) { book ->
                    HadithBookCard(
                        book = book,
                        isOfflineReady = state.offlineReadyBookIds.contains(book.id),
                        isDownloading = state.downloadingBookId == book.id,
                        arabicFontFamily = reading.arabicFontFamily,
                        onDownloadOffline = { hadithViewModel.downloadBookOffline(book) },
                        onClick = { hadithViewModel.openBook(book) }
                    )
                }
            }
        } else {
            // Top Bar for Chapters / Hadiths
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { hadithViewModel.navigateBackInsideHadith() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hadith_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedChapter?.titleEnglish ?: selectedBook.titleEnglish,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selectedChapter != null) selectedBook.titleEnglish else selectedBook.compiler,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = "Font settings")
                    }
                }
            }

            if (state.isLoadingChapters || state.isLoadingHadiths) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Loading ${selectedBook.titleEnglish}…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (state.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = state.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        FilledTonalButton(onClick = { hadithViewModel.navigateBackInsideHadith() }) {
                            Text("Go Back")
                        }
                    }
                }
            } else if (selectedChapter == null) {
                // Level 2: Chapters inside Selected Book
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("hadith_chapters_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.chapters, key = { it.id }) { chapter ->
                        HadithChapterCard(
                            chapter = chapter,
                            arabicFontFamily = reading.arabicFontFamily,
                            onClick = { hadithViewModel.openChapter(chapter) }
                        )
                    }
                }
            } else {
                // Level 3: Hadiths inside Selected Chapter
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = state.hadithSearchQuery,
                        onValueChange = hadithViewModel::onHadithSearchChanged,
                        placeholder = { Text("Filter narrations in this chapter…") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.hadithSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { hadithViewModel.onHadithSearchChanged("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear filter")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )

                    val query = state.hadithSearchQuery.trim()
                    val visibleHadiths = if (query.isEmpty()) {
                        state.hadiths
                    } else {
                        state.hadiths.filter {
                            it.englishText.contains(query, ignoreCase = true) ||
                                it.narrator.contains(query, ignoreCase = true) ||
                                it.arabicText.contains(query) ||
                                it.numberLabel.contains(query, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("hadiths_reader_list"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visibleHadiths, key = { it.id }) { hadith ->
                            HadithNarrationCard(
                                hadith = hadith,
                                onShare = { shareHadith(context, hadith) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithBookCard(
    book: HadithBookInfo,
    isOfflineReady: Boolean,
    isDownloading: Boolean,
    arabicFontFamily: androidx.compose.ui.text.font.FontFamily,
    onDownloadOffline: () -> Unit,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clickable(onClick = onClick)
            .testTag("hadith_book_${book.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = book.rank.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = book.titleEnglish,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = book.compiler,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = book.titleArabic,
                    fontFamily = arabicFontFamily,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = book.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = book.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (isOfflineReady) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OfflinePin,
                                    contentDescription = "Available offline",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                if (!isOfflineReady) {
                    IconButton(
                        onClick = onDownloadOffline,
                        enabled = !isDownloading,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("download_hadith_${book.id}")
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = "Save ${book.titleEnglish} for offline reading",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithChapterCard(
    chapter: HadithChapter,
    arabicFontFamily: androidx.compose.ui.text.font.FontFamily,
    onClick: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("hadith_chapter_${chapter.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = chapter.numberLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = chapter.titleEnglish,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (chapter.titleArabic.isNotBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = chapter.titleArabic,
                    fontFamily = arabicFontFamily,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun HadithNarrationCard(
    hadith: HadithEntry,
    onShare: () -> Unit
) {
    val reading = LocalReadingTypography.current

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hadith_entry_${hadith.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = hadith.numberLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    hadith.grade?.let { grade ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = grade,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share Hadith",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (hadith.arabicText.isNotBlank()) {
                Text(
                    text = hadith.arabicText,
                    fontFamily = reading.arabicFontFamily,
                    fontSize = (reading.arabicFontSizeSp * 0.9f).coerceAtLeast(18f).sp,
                    lineHeight = (reading.arabicFontSizeSp * 1.55f).sp,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (hadith.narrator.isNotBlank()) {
                Text(
                    text = hadith.narrator,
                    fontFamily = reading.englishFontFamily,
                    fontSize = (reading.englishFontSizeSp - 1f).coerceAtLeast(12f).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (hadith.englishText.isNotBlank()) {
                Text(
                    text = hadith.englishText,
                    fontFamily = reading.englishFontFamily,
                    fontSize = reading.englishFontSizeSp.sp,
                    lineHeight = (reading.englishFontSizeSp * 1.48f).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (hadith.reference.isNotBlank()) {
                Text(
                    text = hadith.reference,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
