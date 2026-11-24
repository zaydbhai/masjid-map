package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookmarksAndMasjidExtrasRepository
import com.example.data.QuranAudioPlayer
import com.example.data.QuranRepository
import com.example.data.UserPreferencesRepository
import com.example.domain.JuzCatalog
import com.example.domain.JuzInfo
import com.example.domain.QuranBookmark
import com.example.domain.QuranReciterOption
import com.example.domain.QuranSearchHit
import com.example.domain.QuranTranslationOption
import com.example.domain.QuranVerse
import com.example.domain.RecitationLanguage
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
    // Audio Recitation State
    val playingSurah: Int? = null,
    val playingAyah: Int? = null,
    val isPlayingAudio: Boolean = false,
    val isBufferingAudio: Boolean = false,
    val audioProgressFraction: Float = 0f,
    val playbackSpeed: Float = 1.0f,
    val autoAdvanceAyah: Boolean = true,
    val cachedAudioAyahsForActiveSurah: Int = 0,
    val isDownloadingSurahAudio: Boolean = false,
    val surahAudioDownloadProgress: Int = 0,
    val audioErrorMessage: String? = null
)

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuranRepository(application)
    private val audioPlayer = QuranAudioPlayer(application)
    private val prefs = UserPreferencesRepository(application)
    private val extrasRepo = BookmarksAndMasjidExtrasRepository.getInstance(application)
    private var verseSearchJob: Job? = null
    private var playJob: Job? = null
    private var audioProgressJob: Job? = null
    private var audioDownloadJob: Job? = null

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
                val activeSurah = _uiState.value.activeSurahContent?.surah
                val cachedAudioCount = if (activeSurah != null) {
                    audioPlayer.getCachedAyahCount(settings.quranReciter, activeSurah.number, activeSurah.ayahCount)
                } else {
                    0
                }
                _uiState.update {
                    it.copy(
                        selectedTranslation = settings.quranTranslation,
                        selectedReciter = settings.quranReciter,
                        cachedSurahNumbers = cached,
                        cachedAudioAyahsForActiveSurah = cachedAudioCount,
                        arabicOnlyMode = settings.arabicOnlyQuran,
                        lastReadSurah = settings.lastReadSurah,
                        lastReadAyah = settings.lastReadAyah,
                        surahs = filterSurahs(it.searchQuery, it.showOnlyOffline, cached)
                    )
                }
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
                audioErrorMessage = null
            )
        }
        viewModelScope.launch { prefs.setQuranReciter(reciter) }

        val playingS = currentState.playingSurah
        val playingA = currentState.playingAyah
        if (playingS != null && playingA != null && (currentState.isPlayingAudio || currentState.isBufferingAudio)) {
            playAyah(playingS, playingA, reciter)
        }
    }

    fun selectRecitationLanguage(language: RecitationLanguage) {
        val current = _uiState.value.selectedReciter
        if (current.language == language) return
        val target = QuranReciterOption.forLanguage(language).firstOrNull() ?: return
        selectReciter(target)
    }

    fun togglePlayAyah(surahNumber: Int, ayahNumber: Int) {
        val state = _uiState.value
        if (state.playingSurah == surahNumber && state.playingAyah == ayahNumber) {
            if (state.isBufferingAudio) return
            if (state.isPlayingAudio) {
                audioPlayer.pause()
                audioProgressJob?.cancel()
                _uiState.update { it.copy(isPlayingAudio = false) }
            } else {
                audioPlayer.resume(state.playbackSpeed)
                _uiState.update { it.copy(isPlayingAudio = true, audioErrorMessage = null) }
                startProgressTicker()
            }
        } else {
            playAyah(surahNumber, ayahNumber, state.selectedReciter)
        }
    }

    fun togglePlayPauseSurah() {
        val state = _uiState.value
        val activeSurah = state.activeSurahContent?.surah ?: return
        if (state.playingSurah == activeSurah.number && state.playingAyah != null) {
            togglePlayAyah(activeSurah.number, state.playingAyah)
        } else {
            val startAyah = if (state.lastReadSurah == activeSurah.number && state.lastReadAyah in 1..activeSurah.ayahCount) {
                state.lastReadAyah
            } else {
                1
            }
            playAyah(activeSurah.number, startAyah, state.selectedReciter)
        }
    }

    fun playNextAyah() {
        val state = _uiState.value
        val surahNum = state.playingSurah ?: state.activeSurahContent?.surah?.number ?: return
        val info = SurahCatalog.byNumber(surahNum)
        val currentAyah = state.playingAyah ?: 1
        if (currentAyah < info.ayahCount) {
            playAyah(surahNum, currentAyah + 1, state.selectedReciter)
        }
    }

    fun playPreviousAyah() {
        val state = _uiState.value
        val surahNum = state.playingSurah ?: state.activeSurahContent?.surah?.number ?: return
        val currentAyah = state.playingAyah ?: 1
        val prev = (currentAyah - 1).coerceAtLeast(1)
        playAyah(surahNum, prev, state.selectedReciter)
    }

    fun cyclePlaybackSpeed() {
        val nextSpeed = when (_uiState.value.playbackSpeed) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 0.75f
            else -> 1.0f
        }
        audioPlayer.setSpeed(nextSpeed)
        _uiState.update { it.copy(playbackSpeed = nextSpeed) }
    }

    fun toggleAutoAdvance() {
        _uiState.update { it.copy(autoAdvanceAyah = !it.autoAdvanceAyah) }
    }

    fun stopAudio() {
        playJob?.cancel()
        audioProgressJob?.cancel()
        audioPlayer.stop()
        _uiState.update {
            it.copy(
                playingSurah = null,
                playingAyah = null,
                isPlayingAudio = false,
                isBufferingAudio = false,
                audioProgressFraction = 0f,
                audioErrorMessage = null
            )
        }
    }

    fun clearAudioError() {
        _uiState.update { it.copy(audioErrorMessage = null) }
    }

    fun downloadActiveSurahAudio() {
        val state = _uiState.value
        val surah = state.activeSurahContent?.surah ?: return
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
                        cachedAudioAyahsForActiveSurah = surah.ayahCount
                    )
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isDownloadingSurahAudio = false,
                        audioErrorMessage = t.message ?: "Unable to download Surah audio. Check your connection."
                    )
                }
            }
        }
    }

    private fun playAyah(
        surahNumber: Int,
        ayahNumber: Int,
        reciter: QuranReciterOption
    ) {
        playJob?.cancel()
        audioProgressJob?.cancel()
        val surahInfo = SurahCatalog.byNumber(surahNumber)
        val safeAyah = ayahNumber.coerceIn(1, surahInfo.ayahCount)

        playJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    playingSurah = surahNumber,
                    playingAyah = safeAyah,
                    isPlayingAudio = false,
                    isBufferingAudio = true,
                    audioProgressFraction = 0f,
                    audioErrorMessage = null
                )
            }
            try {
                val localFile = audioPlayer.ensureAyahAudioCached(reciter, surahNumber, safeAyah)
                val cachedCount = _uiState.value.activeSurahContent?.surah?.let { active ->
                    audioPlayer.getCachedAyahCount(reciter, active.number, active.ayahCount)
                } ?: _uiState.value.cachedAudioAyahsForActiveSurah

                _uiState.update { it.copy(cachedAudioAyahsForActiveSurah = cachedCount) }

                audioPlayer.playLocalFile(
                    file = localFile,
                    speed = _uiState.value.playbackSpeed,
                    onPrepared = {
                        _uiState.update {
                            it.copy(
                                isBufferingAudio = false,
                                isPlayingAudio = true,
                                audioErrorMessage = null
                            )
                        }
                        startProgressTicker()
                    },
                    onCompletion = {
                        audioProgressJob?.cancel()
                        val st = _uiState.value
                        if (st.autoAdvanceAyah) {
                            if (safeAyah < surahInfo.ayahCount) {
                                playAyah(surahNumber, safeAyah + 1, st.selectedReciter)
                            } else {
                                _uiState.update {
                                    it.copy(
                                        isPlayingAudio = false,
                                        audioProgressFraction = 1f
                                    )
                                }
                            }
                        } else {
                            // Loop same ayah when auto-advance is disabled
                            playAyah(surahNumber, safeAyah, st.selectedReciter)
                        }
                    },
                    onError = { err ->
                        audioProgressJob?.cancel()
                        _uiState.update {
                            it.copy(
                                isBufferingAudio = false,
                                isPlayingAudio = false,
                                audioErrorMessage = err
                            )
                        }
                    }
                )
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isBufferingAudio = false,
                        isPlayingAudio = false,
                        audioErrorMessage = t.message ?: "Unable to stream recitation audio offline."
                    )
                }
            }
        }
    }

    private fun startProgressTicker() {
        audioProgressJob?.cancel()
        audioProgressJob = viewModelScope.launch {
            while (true) {
                val dur = audioPlayer.durationMs()
                val pos = audioPlayer.currentPositionMs()
                if (dur > 0) {
                    _uiState.update {
                        it.copy(audioProgressFraction = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f))
                    }
                }
                delay(150L)
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
        playJob?.cancel()
        audioProgressJob?.cancel()
        audioDownloadJob?.cancel()
        audioPlayer.release()
    }
}

