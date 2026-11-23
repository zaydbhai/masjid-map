package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookmarksAndMasjidExtrasRepository
import com.example.data.QuranRepository
import com.example.data.UserPreferencesRepository
import com.example.domain.JuzCatalog
import com.example.domain.JuzInfo
import com.example.domain.QuranBookmark
import com.example.domain.QuranSearchHit
import com.example.domain.QuranTranslationOption
import com.example.domain.QuranVerse
import com.example.domain.SurahCatalog
import com.example.domain.SurahContent
import com.example.domain.SurahInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QuranBrowseMode(val label: String) {
    SURAHS("Surahs"),
    JUZ("Juz (30)"),
    BOOKMARKS("Bookmarks")
}

data class QuranUiState(
    val surahs: List<SurahInfo> = SurahCatalog.surahs,
    val juzList: List<JuzInfo> = JuzCatalog.allJuz,
    val browseMode: QuranBrowseMode = QuranBrowseMode.SURAHS,
    val searchQuery: String = "",
    val selectedTranslation: QuranTranslationOption = QuranTranslationOption.SAHIH_INTERNATIONAL,
    val cachedSurahNumbers: Set<Int> = emptySet(),
    val showOnlyOffline: Boolean = false,
    val arabicOnlyMode: Boolean = false,
    val bookmarks: List<QuranBookmark> = emptyList(),
    val lastReadSurah: Int = 1,
    val lastReadAyah: Int = 1,
    val targetScrollAyah: Int = 1,
    val verseSearchHits: List<QuranSearchHit> = emptyList(),
    val isSearchingVerses: Boolean = false,
    val isDownloadingAllOffline: Boolean = false,
    val offlineDownloadProgress: Int = 0,
    val activeSurahContent: SurahContent? = null,
    val isLoadingSurah: Boolean = false,
    val errorMessage: String? = null
)

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuranRepository(application)
    private val prefs = UserPreferencesRepository(application)
    private val extrasRepo = BookmarksAndMasjidExtrasRepository.getInstance(application)
    private var verseSearchJob: Job? = null

    private val _uiState = MutableStateFlow(
        QuranUiState(
            cachedSurahNumbers = repository.getCachedSurahNumbers(
                QuranTranslationOption.SAHIH_INTERNATIONAL.editionId
            )
        )
    )
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            extrasRepo.quranBookmarks.collect { saved ->
                _uiState.update { it.copy(bookmarks = saved) }
            }
        }

        viewModelScope.launch {
            prefs.settingsFlow.collect { settings ->
                val prev = _uiState.value.selectedTranslation
                val cached = repository.getCachedSurahNumbers(settings.quranTranslation.editionId)
                _uiState.update {
                    it.copy(
                        selectedTranslation = settings.quranTranslation,
                        cachedSurahNumbers = cached,
                        arabicOnlyMode = settings.arabicOnlyQuran,
                        lastReadSurah = settings.lastReadSurah,
                        lastReadAyah = settings.lastReadAyah,
                        surahs = filterSurahs(it.searchQuery, it.showOnlyOffline, cached)
                    )
                }
                val activeSurah = _uiState.value.activeSurahContent?.surah
                if (prev != settings.quranTranslation && activeSurah != null) {
                    openSurah(activeSurah.number, _uiState.value.targetScrollAyah, settings.quranTranslation)
                }
            }
        }
    }

    fun setBrowseMode(mode: QuranBrowseMode) {
        _uiState.update { it.copy(browseMode = mode) }
    }

    fun toggleArabicOnlyMode() {
        val next = !_uiState.value.arabicOnlyMode
        viewModelScope.launch { prefs.setArabicOnlyQuran(next) }
    }

    fun onSearchQueryChanged(query: String) {
        val current = _uiState.value
        _uiState.update {
            it.copy(
                searchQuery = query,
                surahs = filterSurahs(query, current.showOnlyOffline, current.cachedSurahNumbers)
            )
        }
        verseSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(verseSearchHits = emptyList(), isSearchingVerses = false) }
            return
        }
        verseSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchingVerses = true) }
            delay(220L)
            val hits = repository.searchOfflineVerses(trimmed, _uiState.value.selectedTranslation)
            _uiState.update { it.copy(verseSearchHits = hits, isSearchingVerses = false) }
        }
    }

    fun toggleOfflineFilter() {
        val current = _uiState.value
        val nextFilter = !current.showOnlyOffline
        _uiState.update {
            it.copy(
                showOnlyOffline = nextFilter,
                surahs = filterSurahs(current.searchQuery, nextFilter, current.cachedSurahNumbers)
            )
        }
    }

    fun refreshCachedState() {
        val current = _uiState.value
        val cached = repository.getCachedSurahNumbers(current.selectedTranslation.editionId)
        _uiState.update {
            it.copy(
                cachedSurahNumbers = cached,
                surahs = filterSurahs(current.searchQuery, current.showOnlyOffline, cached)
            )
        }
    }

    private fun filterSurahs(
        query: String,
        onlyOffline: Boolean,
        cachedSet: Set<Int>
    ): List<SurahInfo> {
        val trimmed = query.trim()
        return SurahCatalog.surahs.filter { surah ->
            val matchesOffline = !onlyOffline || cachedSet.contains(surah.number)
            val matchesQuery = trimmed.isEmpty() ||
                surah.nameEnglish.contains(trimmed, ignoreCase = true) ||
                surah.nameMeaning.contains(trimmed, ignoreCase = true) ||
                surah.nameArabic.contains(trimmed) ||
                surah.number.toString() == trimmed
            matchesOffline && matchesQuery
        }
    }

    fun selectTranslation(translation: QuranTranslationOption) {
        viewModelScope.launch {
            prefs.setQuranTranslation(translation)
        }
        val activeSurah = _uiState.value.activeSurahContent?.surah
        if (activeSurah != null) {
            openSurah(activeSurah.number, _uiState.value.targetScrollAyah, translation)
        }
    }

    fun downloadCompleteQuranOffline(
        translation: QuranTranslationOption = _uiState.value.selectedTranslation,
        onComplete: () -> Unit = {}
    ) {
        if (_uiState.value.isDownloadingAllOffline) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloadingAllOffline = true,
                    offlineDownloadProgress = 0,
                    errorMessage = null
                )
            }
            try {
                repository.downloadCompleteEditionOffline(translation) { done, _ ->
                    _uiState.update { it.copy(offlineDownloadProgress = done) }
                }
                val cached = repository.getCachedSurahNumbers(_uiState.value.selectedTranslation.editionId)
                _uiState.update {
                    it.copy(
                        isDownloadingAllOffline = false,
                        offlineDownloadProgress = 114,
                        cachedSurahNumbers = cached,
                        surahs = filterSurahs(it.searchQuery, it.showOnlyOffline, cached)
                    )
                }
                onComplete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDownloadingAllOffline = false,
                        errorMessage = e.message ?: "Unable to download complete Qur'an. Connect to the internet and try again."
                    )
                }
            }
        }
    }

    fun openSurah(
        surahNumber: Int,
        initialAyah: Int = 1,
        translation: QuranTranslationOption = _uiState.value.selectedTranslation
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingSurah = true,
                    targetScrollAyah = initialAyah.coerceAtLeast(1),
                    errorMessage = null
                )
            }
            try {
                val content = repository.loadSurah(surahNumber, translation)
                val cached = repository.getCachedSurahNumbers(translation.editionId)
                prefs.setLastReadPosition(surahNumber, initialAyah.coerceAtLeast(1))
                _uiState.update {
                    it.copy(
                        isLoadingSurah = false,
                        activeSurahContent = content,
                        cachedSurahNumbers = cached,
                        lastReadSurah = surahNumber,
                        lastReadAyah = initialAyah.coerceAtLeast(1),
                        surahs = filterSurahs(it.searchQuery, it.showOnlyOffline, cached),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingSurah = false,
                        errorMessage = e.message ?: "Surah not downloaded for offline yet. Tap 'Save All' while online."
                    )
                }
            }
        }
    }

    fun markAyahRead(surahNumber: Int, ayahNumber: Int) {
        val safeAyah = ayahNumber.coerceAtLeast(1)
        if (_uiState.value.lastReadSurah == surahNumber && _uiState.value.lastReadAyah == safeAyah) return
        viewModelScope.launch {
            prefs.setLastReadPosition(surahNumber, safeAyah)
        }
    }

    fun toggleBookmark(surah: SurahInfo, verse: QuranVerse) {
        extrasRepo.toggleQuranBookmark(
            chapter = surah.number,
            verse = verse.verse,
            surahName = surah.nameEnglish,
            arabicText = verse.arabicText,
            englishText = verse.englishText
        )
    }

    fun closeSurah() {
        _uiState.update { it.copy(activeSurahContent = null, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
