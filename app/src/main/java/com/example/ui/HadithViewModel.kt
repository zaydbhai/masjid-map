package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BookmarksAndMasjidExtrasRepository
import com.example.data.HadithRepository
import com.example.domain.HadithBookCategory
import com.example.domain.HadithBookInfo
import com.example.domain.HadithCatalog
import com.example.domain.HadithChapter
import com.example.domain.HadithEntry
import com.example.domain.HadithSearchHit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HadithUiState(
    val books: List<HadithBookInfo> = HadithCatalog.top21Books,
    val offlineReadyBookIds: Set<String> = emptySet(),
    val downloadingBookId: String? = null,
    val isDownloadingEssentialPack: Boolean = false,
    val selectedCategory: HadithBookCategory = HadithBookCategory.ALL,
    val showSavedBookmarks: Boolean = false,
    val bookmarkedHadiths: List<HadithEntry> = emptyList(),
    val bookSearchQuery: String = "",
    val globalSearchHits: List<HadithSearchHit> = emptyList(),
    val isSearchingBooks: Boolean = false,
    val selectedBook: HadithBookInfo? = null,
    val chapters: List<HadithChapter> = emptyList(),
    val selectedChapter: HadithChapter? = null,
    val hadiths: List<HadithEntry> = emptyList(),
    val hadithSearchQuery: String = "",
    val isLoadingChapters: Boolean = false,
    val isLoadingHadiths: Boolean = false,
    val errorMessage: String? = null
)

class HadithViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HadithRepository(application)
    private val extrasRepo = BookmarksAndMasjidExtrasRepository.getInstance(application)
    private var globalSearchJob: Job? = null

    private val _uiState = MutableStateFlow(
        HadithUiState(
            offlineReadyBookIds = repository.getOfflineReadyBookIds()
        )
    )
    val uiState: StateFlow<HadithUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            extrasRepo.hadithBookmarks.collect { saved ->
                _uiState.update { it.copy(bookmarkedHadiths = saved) }
            }
        }
    }

    fun refreshOfflineState() {
        _uiState.update {
            it.copy(offlineReadyBookIds = repository.getOfflineReadyBookIds())
        }
    }

    fun selectCategory(category: HadithBookCategory) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                showSavedBookmarks = false,
                books = filterBooks(category, it.bookSearchQuery)
            )
        }
    }

    fun toggleSavedBookmarksView() {
        _uiState.update { it.copy(showSavedBookmarks = !it.showSavedBookmarks) }
    }

    fun toggleBookmark(entry: HadithEntry) {
        extrasRepo.toggleHadithBookmark(entry)
    }

    fun onBookSearchChanged(query: String) {
        _uiState.update {
            it.copy(
                bookSearchQuery = query,
                books = filterBooks(it.selectedCategory, query)
            )
        }
        globalSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(globalSearchHits = emptyList(), isSearchingBooks = false) }
            return
        }
        globalSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchingBooks = true) }
            delay(220L)
            val hits = repository.searchOfflineCachedBooks(trimmed)
            _uiState.update { it.copy(globalSearchHits = hits, isSearchingBooks = false) }
        }
    }

    fun onHadithSearchChanged(query: String) {
        _uiState.update { it.copy(hadithSearchQuery = query) }
    }

    private fun filterBooks(
        category: HadithBookCategory,
        query: String
    ): List<HadithBookInfo> {
        val trimmed = query.trim()
        return HadithCatalog.top21Books.filter { book ->
            val matchesCat = category == HadithBookCategory.ALL || book.category == category
            val matchesQuery = trimmed.isEmpty() ||
                book.titleEnglish.contains(trimmed, ignoreCase = true) ||
                book.compiler.contains(trimmed, ignoreCase = true) ||
                book.titleArabic.contains(trimmed)
            matchesCat && matchesQuery
        }
    }

    fun downloadBookOffline(book: HadithBookInfo, onComplete: () -> Unit = {}) {
        if (_uiState.value.downloadingBookId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingBookId = book.id, errorMessage = null) }
            try {
                repository.downloadBookOffline(book)
                _uiState.update {
                    it.copy(
                        downloadingBookId = null,
                        offlineReadyBookIds = repository.getOfflineReadyBookIds()
                    )
                }
                onComplete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        downloadingBookId = null,
                        errorMessage = "Could not download ${book.titleEnglish}: ${e.message ?: "check internet"}"
                    )
                }
            }
        }
    }

    fun downloadEssentialHadithPack(onComplete: () -> Unit = {}) {
        if (_uiState.value.isDownloadingEssentialPack) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadingEssentialPack = true, errorMessage = null) }
            val essentialIds = setOf("nawawi40", "qudsi40", "riyadussalihin", "hisn", "adabulmufrad", "bulughulmaram")
            val targets = HadithCatalog.top21Books.filter { it.id in essentialIds }
            try {
                for (book in targets) {
                    if (!repository.isBookOfflineReady(book)) {
                        repository.downloadBookOffline(book)
                        _uiState.update {
                            it.copy(offlineReadyBookIds = repository.getOfflineReadyBookIds())
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        isDownloadingEssentialPack = false,
                        offlineReadyBookIds = repository.getOfflineReadyBookIds()
                    )
                }
                onComplete()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isDownloadingEssentialPack = false,
                        offlineReadyBookIds = repository.getOfflineReadyBookIds(),
                        errorMessage = "Essential Hadith download paused: ${e.message ?: "check internet"}"
                    )
                }
            }
        }
    }

    fun openBook(book: HadithBookInfo) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedBook = book,
                    chapters = emptyList(),
                    selectedChapter = null,
                    hadiths = emptyList(),
                    hadithSearchQuery = "",
                    isLoadingChapters = true,
                    errorMessage = null
                )
            }
            try {
                val chapters = repository.getChapters(book)
                _uiState.update {
                    it.copy(
                        isLoadingChapters = false,
                        chapters = chapters,
                        offlineReadyBookIds = repository.getOfflineReadyBookIds()
                    )
                }
                if (chapters.size == 1) {
                    openChapter(chapters.first())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingChapters = false,
                        errorMessage = "Not cached offline yet. Tap the download icon on ${book.titleEnglish} while online to read offline."
                    )
                }
            }
        }
    }

    fun openChapter(chapter: HadithChapter) {
        val book = _uiState.value.selectedBook ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedChapter = chapter,
                    hadiths = emptyList(),
                    hadithSearchQuery = "",
                    isLoadingHadiths = true,
                    errorMessage = null
                )
            }
            try {
                val hadiths = repository.getHadithsForChapter(book, chapter)
                _uiState.update {
                    it.copy(
                        isLoadingHadiths = false,
                        hadiths = hadiths,
                        offlineReadyBookIds = repository.getOfflineReadyBookIds()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingHadiths = false,
                        errorMessage = "Chapter not cached offline yet. Download ${book.titleEnglish} while online to read all chapters offline."
                    )
                }
            }
        }
    }

    fun openSearchHit(hit: HadithSearchHit) {
        val book = HadithCatalog.top21Books.firstOrNull { it.id == hit.bookId } ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedBook = book,
                    chapters = emptyList(),
                    selectedChapter = null,
                    hadiths = emptyList(),
                    isLoadingChapters = true,
                    errorMessage = null
                )
            }
            runCatching {
                val chapters = repository.getChapters(book)
                val targetChapter = chapters.firstOrNull { it.id == hit.chapterId } ?: chapters.firstOrNull()
                _uiState.update { it.copy(isLoadingChapters = false, chapters = chapters) }
                if (targetChapter != null) {
                    openChapter(targetChapter)
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoadingChapters = false,
                        errorMessage = "Unable to open matched Hadith."
                    )
                }
            }
        }
    }

    fun navigateBackInsideHadith(): Boolean {
        val state = _uiState.value
        return when {
            state.selectedChapter != null && state.chapters.size > 1 -> {
                _uiState.update {
                    it.copy(selectedChapter = null, hadiths = emptyList(), hadithSearchQuery = "")
                }
                true
            }
            state.selectedBook != null -> {
                _uiState.update {
                    it.copy(
                        selectedBook = null,
                        chapters = emptyList(),
                        selectedChapter = null,
                        hadiths = emptyList(),
                        hadithSearchQuery = "",
                        errorMessage = null
                    )
                }
                true
            }
            else -> false
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
