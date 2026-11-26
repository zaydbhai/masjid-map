package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.QuranPlaybackController
import com.example.data.BookmarksAndMasjidExtrasRepository
import com.example.data.QuranRepository
import com.example.data.UserPreferencesRepository
import com.example.domain.JuzCatalog
import com.example.domain.JuzInfo
import com.example.domain.QuranBookmark
import com.example.domain.QuranReciterOption
import com.example.domain.QuranSearchHit
import com.example.domain.QuranTajwidEngine
import com.example.domain.QuranTranslationOption
import com.example.domain.QuranVerse
import com.example.domain.QuranWordAnalysis
import com.example.domain.RecitationLanguage
import com.example.domain.SleepTimerOption
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
    val selectedReciter: QuranReciterOption = QuranReciterOption.MISHARY_ALAFASY,
    val cachedSurahNumbers: Set<Int> = emptySet(),
    val showOnlyOffline: Boolean = false,
    val arabicOnlyMode: Boolean = false,
    val tajwidEnabled: Boolean = false,
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
    val errorMessage: String? = null,
    // Dedicated Listening Page & Word-by-Word Lookup State
    val isListeningPageVisible: Boolean = false,
    val listeningSurahContent: SurahContent? = null,
    val selectedWordAnalysis: QuranWordAnalysis? = null,
    val selectedWordVerseRef: Pair<Int, Int>? = null,
    // Audio Recitation & Sleep Timer State
    val playingSurah: Int? = null,
    val playingAyah: Int? = null,
    val isPlayingAudio: Boolean = false,
    val isBufferingAudio: Boolean = false,
    val audioProgressFraction: Float = 0f,
    val audioPositionMs: Long = 0L,
    val audioDurationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val autoAdvanceAyah: Boolean = true,
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    val sleepTimerRemainingSec: Int = 0,
    val cachedAudioAyahsForActiveSurah: Int = 0,
    val cachedAudioSurahNumbers: Set<Int> = emptySet(),
    val cachedAudioReciterIds: Set<String> = emptySet(),
    val audioCacheBytes: Long = 0L,
    val isDownloadingSurahAudio: Boolean = false,
    val surahAudioDownloadProgress: Int = 0,
    val isDownloadingEssentialAudio: Boolean = false,
    val essentialAudioProgressText: String? = null,
    val essentialAudioProgressFraction: Float = 0f,
    val audioErrorMessage: String? = null
)

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuranRepository(application)
    private val playbackController = QuranPlaybackController.getInstance(application)
    private val audioPlayer = playbackController.audioPlayer
    private val prefs = UserPreferencesRepository(application)
    private val extrasRepo = BookmarksAndMasjidExtrasRepository.getInstance(application)
    private var verseSearchJob: Job? = null
    private var audioDownloadJob: Job? = null
    private var essentialAudioDownloadJob: Job? = null
    private var listeningLoadJob: Job? = null
    private val wbwCacheBySurah = mutableMapOf<Int, Map<Int, List<Pair<String, String?>>>>()

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
            playbackController.snapshot.collect { snap ->
                val activeSurah = _uiState.value.activeSurahContent?.surah
                val playingSurahNum = snap.playingSurah
                val cachedTargetSurah = playingSurahNum?.let { SurahCatalog.byNumber(it) } ?: activeSurah
                val cachedAudioCount = if (cachedTargetSurah != null) {
                    audioPlayer.getCachedAyahCount(_uiState.value.selectedReciter, cachedTargetSurah.number, cachedTargetSurah.ayahCount)
                } else {
                    _uiState.value.cachedAudioAyahsForActiveSurah
                }
                val needsListeningLoad = playingSurahNum != null &&
                    _uiState.value.listeningSurahContent?.surah?.number != playingSurahNum
                _uiState.update {
                    it.copy(
                        playingSurah = snap.playingSurah,
                        playingAyah = snap.playingAyah,
                        isPlayingAudio = snap.isPlaying,
                        isBufferingAudio = snap.isBuffering,
                        audioProgressFraction = snap.progressFraction,
                        audioPositionMs = snap.positionMs,
                        audioDurationMs = snap.durationMs,
                        playbackSpeed = snap.playbackSpeed,
                        autoAdvanceAyah = snap.autoAdvanceAyah,
                        sleepTimerOption = snap.sleepTimerOption,
                        sleepTimerRemainingSec = snap.sleepTimerRemainingSec,
                        cachedAudioAyahsForActiveSurah = cachedAudioCount,
                        audioErrorMessage = snap.errorMessage,
                        isListeningPageVisible = if (playingSurahNum == null) false else it.isListeningPageVisible,
                        listeningSurahContent = if (playingSurahNum == null) null else it.listeningSurahContent
                    )
                }
                if (needsListeningLoad && playingSurahNum != null) {
                    ensureListeningContentLoaded(playingSurahNum, _uiState.value.selectedTranslation)
                }
            }
        }

        viewModelScope.launch {
            prefs.settingsFlow.collect { settings ->
                val prev = _uiState.value.selectedTranslation
                val cached = repository.getCachedSurahNumbers(settings.quranTranslation.editionId)
                val activeSurah = _uiState.value.activeSurahContent?.surah
                val cachedAudioCount = if (activeSurah != null) {
                    audioPlayer.getCachedAyahCount(settings.quranReciter, activeSurah.number, activeSurah.ayahCount)
                } else {
                    0
                }
                playbackController.setReciter(settings.quranReciter)
                _uiState.update {
                    it.copy(
                        selectedTranslation = settings.quranTranslation,
                        selectedReciter = settings.quranReciter,
                        cachedSurahNumbers = cached,
                        cachedAudioAyahsForActiveSurah = cachedAudioCount,
                        cachedAudioSurahNumbers = audioPlayer.getFullAudioCachedSurahNumbers(settings.quranReciter),
                        cachedAudioReciterIds = audioPlayer.getCachedReciterIds(),
                        audioCacheBytes = audioPlayer.getCacheSizeBytes(),
                        arabicOnlyMode = settings.arabicOnlyQuran,
                        tajwidEnabled = settings.tajwidEnabled,
                        lastReadSurah = settings.lastReadSurah,
                        lastReadAyah = settings.lastReadAyah,
                        surahs = filterSurahs(it.searchQuery, it.showOnlyOffline, cached)
                    )
                }
                if (prev != settings.quranTranslation && activeSurah != null) {
                    openSurah(activeSurah.number, _uiState.value.targetScrollAyah, settings.quranTranslation)
                }
                val listeningSurah = _uiState.value.listeningSurahContent?.surah?.number
                if (prev != settings.quranTranslation && listeningSurah != null) {
                    ensureListeningContentLoaded(listeningSurah, settings.quranTranslation)
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

    fun toggleTajwidEnabled() {
        val next = !_uiState.value.tajwidEnabled
        viewModelScope.launch { prefs.setTajwidEnabled(next) }
    }

    fun openListeningPage() {
        if (_uiState.value.playingSurah != null) {
            _uiState.update { it.copy(isListeningPageVisible = true) }
        }
    }

    fun closeListeningPage() {
        _uiState.update { it.copy(isListeningPageVisible = false) }
    }

    private fun ensureListeningContentLoaded(
        surahNumber: Int,
        translation: QuranTranslationOption
    ) {
        val active = _uiState.value.activeSurahContent
        if (active != null && active.surah.number == surahNumber && active.translation == translation) {
            _uiState.update { it.copy(listeningSurahContent = active) }
            prefetchWordByWord(surahNumber)
            return
        }
        listeningLoadJob?.cancel()
        listeningLoadJob = viewModelScope.launch {
            runCatching {
                repository.loadSurah(surahNumber, translation)
            }.onSuccess { loaded ->
                _uiState.update { it.copy(listeningSurahContent = loaded) }
                prefetchWordByWord(surahNumber)
            }
        }
    }

    private fun prefetchWordByWord(surahNumber: Int) {
        if (wbwCacheBySurah.containsKey(surahNumber)) return
        viewModelScope.launch {
            val wbw = repository.fetchWordByWordForChapter(surahNumber)
            if (wbw.isNotEmpty()) {
                wbwCacheBySurah[surahNumber] = wbw
            }
        }
    }

    fun inspectArabicWord(verse: QuranVerse, wordIndex: Int, surfaceWord: String) {
        val chapterWbw = wbwCacheBySurah[verse.chapter]
        val wordPair = chapterWbw?.get(verse.verse)?.getOrNull(wordIndex)
        val analysis = QuranTajwidEngine.analyzeArabicWordOffline(
            word = surfaceWord,
            wordIndex = wordIndex,
            apiGloss = wordPair?.first?.takeIf { it.isNotBlank() },
            apiTranslit = wordPair?.second
        )
        _uiState.update {
            it.copy(
                selectedWordAnalysis = analysis,
                selectedWordVerseRef = verse.chapter to verse.verse
            )
        }
        if (chapterWbw == null) {
            viewModelScope.launch {
                val fetched = repository.fetchWordByWordForChapter(verse.chapter)
                if (fetched.isNotEmpty()) {
                    wbwCacheBySurah[verse.chapter] = fetched
                    val currentRef = _uiState.value.selectedWordVerseRef
                    val currentAnalysis = _uiState.value.selectedWordAnalysis
                    if (currentRef == (verse.chapter to verse.verse) &&
                        currentAnalysis?.wordIndex == wordIndex
                    ) {
                        val updatedPair = fetched[verse.verse]?.getOrNull(wordIndex)
                        if (updatedPair != null) {
                            _uiState.update {
                                it.copy(
                                    selectedWordAnalysis = QuranTajwidEngine.analyzeArabicWordOffline(
                                        word = surfaceWord,
                                        wordIndex = wordIndex,
                                        apiGloss = updatedPair.first.takeIf { g -> g.isNotBlank() },
                                        apiTranslit = updatedPair.second
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun dismissWordAnalysis() {
        _uiState.update {
            it.copy(selectedWordAnalysis = null, selectedWordVerseRef = null)
        }
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
        val activeSurah = current.activeSurahContent?.surah
        val cachedAudioCount = if (activeSurah != null) {
            audioPlayer.getCachedAyahCount(current.selectedReciter, activeSurah.number, activeSurah.ayahCount)
        } else {
            0
        }
        _uiState.update {
            it.copy(
                cachedSurahNumbers = cached,
                cachedAudioAyahsForActiveSurah = cachedAudioCount,
                cachedAudioSurahNumbers = audioPlayer.getFullAudioCachedSurahNumbers(current.selectedReciter),
                cachedAudioReciterIds = audioPlayer.getCachedReciterIds(),
                audioCacheBytes = audioPlayer.getCacheSizeBytes(),
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

    fun selectReciter(reciter: QuranReciterOption) {
        val currentState = _uiState.value
        if (currentState.selectedReciter == reciter) return
        val activeSurah = currentState.activeSurahContent?.surah
        val cachedCount = if (activeSurah != null) {
            audioPlayer.getCachedAyahCount(reciter, activeSurah.number, activeSurah.ayahCount)
        } else {
            0
        }
        _uiState.update {
            it.copy(
                selectedReciter = reciter,
                cachedAudioAyahsForActiveSurah = cachedCount,
                cachedAudioSurahNumbers = audioPlayer.getFullAudioCachedSurahNumbers(reciter),
                cachedAudioReciterIds = audioPlayer.getCachedReciterIds(),
                audioCacheBytes = audioPlayer.getCacheSizeBytes(),
                audioErrorMessage = null
            )
        }
        playbackController.setReciter(reciter)
        viewModelScope.launch { prefs.setQuranReciter(reciter) }
    }

    fun selectRecitationLanguage(language: RecitationLanguage) {
        val current = _uiState.value.selectedReciter
        if (current.language == language) return
        val target = QuranReciterOption.forLanguage(language).firstOrNull() ?: return
        selectReciter(target)
    }

    fun togglePlayAyah(surahNumber: Int, ayahNumber: Int) {
        val state = _uiState.value
        val isPausingSameAyah = state.playingSurah == surahNumber &&
            state.playingAyah == ayahNumber &&
            state.isPlayingAudio
        if (!isPausingSameAyah) {
            _uiState.update { it.copy(isListeningPageVisible = true) }
        }
        playbackController.togglePlayAyah(surahNumber, ayahNumber, state.selectedReciter)
    }

    fun togglePlayPauseSurah() {
        val state = _uiState.value
        val activeSurah = state.activeSurahContent?.surah ?: state.listeningSurahContent?.surah
        val fallbackSurah = activeSurah?.number ?: state.playingSurah
        val fallbackAyah = if (activeSurah != null && state.lastReadSurah == activeSurah.number && state.lastReadAyah in 1..activeSurah.ayahCount) {
            state.lastReadAyah
        } else {
            1
        }
        if (!state.isPlayingAudio && !state.isBufferingAudio && fallbackSurah != null) {
            _uiState.update { it.copy(isListeningPageVisible = true) }
        }
        playbackController.togglePlayPause(
            fallbackSurah = fallbackSurah,
            fallbackAyah = fallbackAyah,
            reciter = state.selectedReciter
        )
    }

    fun playNextAyah() {
        playbackController.playNextAyah()
    }

    fun playPreviousAyah() {
        playbackController.playPreviousAyah()
    }

    fun seekAudioFraction(fraction: Float) {
        playbackController.seekToFraction(fraction)
    }

    fun cyclePlaybackSpeed() {
        playbackController.cycleSpeed()
    }

    fun toggleAutoAdvance() {
        playbackController.toggleAutoAdvance()
    }

    fun setSleepTimer(option: SleepTimerOption) {
        playbackController.setSleepTimer(option)
    }

    fun stopAudio() {
        _uiState.update { it.copy(isListeningPageVisible = false) }
        playbackController.stopAudio()
    }

    fun clearAudioError() {
        playbackController.clearError()
    }

    fun downloadActiveSurahAudio() {
        val state = _uiState.value
        val surah = state.activeSurahContent?.surah
            ?: state.listeningSurahContent?.surah
            ?: state.playingSurah?.let { SurahCatalog.byNumber(it) }
            ?: return
        val reciter = state.selectedReciter
        if (state.isDownloadingSurahAudio) return

        audioDownloadJob?.cancel()
        audioDownloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloadingSurahAudio = true,
                    surahAudioDownloadProgress = 0,
                    audioErrorMessage = null
                )
            }
            try {
                audioPlayer.downloadSurahAudioOffline(
                    reciter = reciter,
                    surahNumber = surah.number,
                    ayahCount = surah.ayahCount
                ) { done, _ ->
                    _uiState.update {
                        it.copy(
                            surahAudioDownloadProgress = done,
                            cachedAudioAyahsForActiveSurah = maxOf(it.cachedAudioAyahsForActiveSurah, done)
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        isDownloadingSurahAudio = false,
                        cachedAudioAyahsForActiveSurah = surah.ayahCount,
                        cachedAudioSurahNumbers = audioPlayer.getFullAudioCachedSurahNumbers(reciter),
                        cachedAudioReciterIds = audioPlayer.getCachedReciterIds(),
                        audioCacheBytes = audioPlayer.getCacheSizeBytes()
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isDownloadingSurahAudio = false,
                        cachedAudioSurahNumbers = audioPlayer.getFullAudioCachedSurahNumbers(reciter),
                        cachedAudioReciterIds = audioPlayer.getCachedReciterIds(),
                        audioCacheBytes = audioPlayer.getCacheSizeBytes(),
                        audioErrorMessage = t.message ?: "Unable to download Surah audio. Check your connection."
                    )
                }
            }
        }
    }

    fun downloadEssentialAudioOffline(onComplete: () -> Unit = {}) {
        val state = _uiState.value
        val reciter = state.selectedReciter
        if (state.isDownloadingEssentialAudio) return

        essentialAudioDownloadJob?.cancel()
        essentialAudioDownloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloadingEssentialAudio = true,
                    essentialAudioProgressText = "Preparing ${reciter.shortLabel} recitations…",
                    essentialAudioProgressFraction = 0f,
                    audioErrorMessage = null
                )
            }
            try {
                audioPlayer.downloadEssentialSurahsAudioOffline(reciter) { done, total, surahName ->
                    val frac = if (total > 0) done.toFloat() / total.toFloat() else 0f
                    _uiState.update {
                        it.copy(
                            essentialAudioProgressText = "Downloading $surahName ($done / $total)…",
                            essentialAudioProgressFraction = frac,
                            cachedAudioSurahNumbers = audioPlayer.getFullAudioCachedSurahNumbers(reciter),
                            cachedAudioReciterIds = audioPlayer.getCachedReciterIds(),
                            audioCacheBytes = audioPlayer.getCacheSizeBytes()
                        )
                    }
                }
                val activeSurah = _uiState.value.activeSurahContent?.surah
                val activeCached = if (activeSurah != null) {
                    audioPlayer.getCachedAyahCount(reciter, activeSurah.number, activeSurah.ayahCount)
                } else {
                    _uiState.value.cachedAudioAyahsForActiveSurah
                }
                _uiState.update {
                    it.copy(
                        isDownloadingEssentialAudio = false,
                        essentialAudioProgressText = null,
                        essentialAudioProgressFraction = 1f,
                        cachedAudioAyahsForActiveSurah = activeCached,
                        cachedAudioSurahNumbers = audioPlayer.getFullAudioCachedSurahNumbers(reciter),
                        cachedAudioReciterIds = audioPlayer.getCachedReciterIds(),
                        audioCacheBytes = audioPlayer.getCacheSizeBytes()
                    )
                }
                onComplete()
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isDownloadingEssentialAudio = false,
                        essentialAudioProgressText = null,
                        cachedAudioSurahNumbers = audioPlayer.getFullAudioCachedSurahNumbers(reciter),
                        cachedAudioReciterIds = audioPlayer.getCachedReciterIds(),
                        audioCacheBytes = audioPlayer.getCacheSizeBytes(),
                        errorMessage = t.message ?: "Unable to download audio pack while offline."
                    )
                }
            }
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
                val cachedAudioCount = audioPlayer.getCachedAyahCount(
                    _uiState.value.selectedReciter,
                    surahNumber,
                    content.surah.ayahCount
                )
                prefs.setLastReadPosition(surahNumber, initialAyah.coerceAtLeast(1))
                _uiState.update {
                    it.copy(
                        isLoadingSurah = false,
                        activeSurahContent = content,
                        cachedSurahNumbers = cached,
                        cachedAudioAyahsForActiveSurah = cachedAudioCount,
                        lastReadSurah = surahNumber,
                        lastReadAyah = initialAyah.coerceAtLeast(1),
                        surahs = filterSurahs(it.searchQuery, it.showOnlyOffline, cached),
                        errorMessage = null
                    )
                }
                prefetchWordByWord(surahNumber)
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

    override fun onCleared() {
        super.onCleared()
        audioDownloadJob?.cancel()
        essentialAudioDownloadJob?.cancel()
        // Note: playbackController stays alive so background/lock-screen playback continues!
    }
}
