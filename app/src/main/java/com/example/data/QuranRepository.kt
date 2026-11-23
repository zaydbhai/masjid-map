package com.example.data

import android.content.Context
import com.example.domain.QuranTranslationOption
import com.example.domain.QuranVerse
import com.example.domain.SurahCatalog
import com.example.domain.SurahContent
import com.example.domain.SurahInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class QuranRepository(private val context: Context) {

    private val cacheDir: File by lazy {
        File(context.filesDir, "quran_cache").apply { mkdirs() }
    }

    private val arabicEdition = "ara-quranuthmani"

    suspend fun loadSurah(
        surahNumber: Int,
        translation: QuranTranslationOption
    ): SurahContent = withContext(Dispatchers.IO) {
        val surah = SurahCatalog.byNumber(surahNumber)

        val arabicMap = fetchEditionChapter(
            edition = arabicEdition,
            chapter = surah.number,
            expectedAyahCount = surah.ayahCount,
            requireArabicScript = true
        )

        val englishMap = fetchEditionChapter(
            edition = translation.editionId,
            chapter = surah.number,
            expectedAyahCount = surah.ayahCount,
            requireArabicScript = false
        )

        val verses = (1..surah.ayahCount).map { verseNum ->
            QuranVerse(
                chapter = surah.number,
                verse = verseNum,
                arabicText = arabicMap[verseNum].orEmpty(),
                englishText = englishMap[verseNum].orEmpty()
            )
        }

        val verified = verses.size == surah.ayahCount &&
            verses.all { it.arabicText.isNotBlank() && it.englishText.isNotBlank() }

        SurahContent(
            surah = surah,
            translation = translation,
            verses = verses,
            verifiedIntegrity = verified,
            sourceLabel = "Tanzil Uthmani · GitHub fawazahmed0/quran-api"
        )
    }

    /**
     * Downloads and verifies all 114 Surahs (6,236 ayahs) for Arabic Uthmani + chosen English
     * translation in a single batch HTTP request per edition, saving per-Surah files locally.
     */
    suspend fun downloadCompleteEditionOffline(
        translation: QuranTranslationOption,
        onProgress: (completedSurahs: Int, totalSurahs: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        var step = 0
        val totalSteps = 228 // 114 Arabic + 114 English

        if (getCachedSurahNumbers(arabicEdition).size < 114) {
            downloadAndSplitEdition(
                edition = arabicEdition,
                requireArabicScript = true
            ) { ch ->
                step = ch
                onProgress((step / 2).coerceIn(0, 114), 114)
            }
        }

        if (getCachedSurahNumbers(translation.editionId).size < 114) {
            downloadAndSplitEdition(
                edition = translation.editionId,
                requireArabicScript = false
            ) { ch ->
                step = 114 + ch
                onProgress((step / 2).coerceIn(0, 114), 114)
            }
        }

        onProgress(114, 114)
        getCachedCountBoth(translation.editionId)
    }

    fun getCachedSurahNumbers(editionId: String): Set<Int> {
        val arReady = mutableSetOf<Int>()
        for (ch in 1..114) {
            val arFile = File(cacheDir, "${arabicEdition}_$ch.json")
            val edFile = File(cacheDir, "${editionId}_$ch.json")
            if (arFile.exists() && edFile.exists()) {
                arReady.add(ch)
            } else if (editionId == arabicEdition && arFile.exists()) {
                arReady.add(ch)
            }
        }
        return arReady
    }

    fun getCachedCountBoth(editionId: String): Int = getCachedSurahNumbers(editionId).size

    fun getCacheSizeBytes(): Long =
        cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun clearCache() {
        runCatching { cacheDir.deleteRecursively() }
    }

    private fun downloadAndSplitEdition(
        edition: String,
        requireArabicScript: Boolean,
        onChapterProgress: (Int) -> Unit
    ) {
        val fullUrls = listOf(
            "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/$edition.min.json",
            "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/$edition.json",
            "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/$edition.min.json",
            "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/$edition.json"
        )

        var splitSuccess = false
        for (url in fullUrls) {
            try {
                val request = Request.Builder().url(url).build()
                NetworkClients.okHttpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                    val body = resp.body?.string() ?: throw IllegalStateException("Empty body")
                    val root = JSONObject(body)
                    val arr = root.optJSONArray("quran") ?: throw IllegalStateException("No quran array")

                    val grouped = HashMap<Int, MutableList<JSONObject>>(114)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val ch = obj.getInt("chapter")
                        grouped.getOrPut(ch) { mutableListOf() }.add(obj)
                    }

                    for (surah in SurahCatalog.surahs) {
                        val verses = grouped[surah.number]
                            ?: throw IllegalStateException("Missing chapter ${surah.number}")
                        val chJson = JSONObject().put("chapter", JSONArray(verses)).toString()
                        parseAndVerifyChapterJson(chJson, surah.ayahCount, requireArabicScript)
                        File(cacheDir, "${edition}_${surah.number}.json").writeText(chJson)
                        if (surah.number % 6 == 0 || surah.number == 114) {
                            onChapterProgress(surah.number)
                        }
                    }
                    splitSuccess = true
                }
                if (splitSuccess) break
            } catch (_: Throwable) {
                // Try next mirror or per-chapter fallback
            }
        }

        if (!splitSuccess) {
            for (surah in SurahCatalog.surahs) {
                fetchEditionChapter(
                    edition = edition,
                    chapter = surah.number,
                    expectedAyahCount = surah.ayahCount,
                    requireArabicScript = requireArabicScript
                )
                onChapterProgress(surah.number)
            }
        }
    }

    private fun fetchEditionChapter(
        edition: String,
        chapter: Int,
        expectedAyahCount: Int,
        requireArabicScript: Boolean
    ): Map<Int, String> {
        val cacheFile = File(cacheDir, "${edition}_${chapter}.json")
        if (cacheFile.exists()) {
            val cached = runCatching {
                parseAndVerifyChapterJson(cacheFile.readText(), expectedAyahCount, requireArabicScript)
            }.getOrNull()
            if (cached != null) return cached
        }

        val urls = listOf(
            "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/$edition/$chapter.json",
            "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/$edition/$chapter.json"
        )

        var lastError: Throwable? = null
        for (url in urls) {
            try {
                val request = Request.Builder().url(url).build()
                NetworkClients.okHttpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("HTTP ${resp.code} fetching $edition chapter $chapter")
                    }
                    val body = resp.body?.string() ?: throw IllegalStateException("Empty body")
                    val parsed = parseAndVerifyChapterJson(body, expectedAyahCount, requireArabicScript)
                    cacheFile.writeText(body)
                    return parsed
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: IllegalStateException("Offline: Surah $chapter is not downloaded yet.")
    }

    private fun parseAndVerifyChapterJson(
        rawJson: String,
        expectedAyahCount: Int,
        requireArabicScript: Boolean
    ): Map<Int, String> {
        val root = JSONObject(rawJson)
        val array = root.optJSONArray("chapter") ?: root.optJSONArray("quran")
            ?: throw IllegalStateException("Unexpected Quran JSON format")

        val map = LinkedHashMap<Int, String>(expectedAyahCount)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val verseNum = item.getInt("verse")
            val text = item.getString("text").trim()
            if (text.isBlank()) throw IllegalStateException("Blank verse $verseNum")
            if (requireArabicScript && !containsArabicScript(text)) {
                throw IllegalStateException("Arabic script verification failed on verse $verseNum")
            }
            map[verseNum] = text
        }

        if (map.size != expectedAyahCount) {
            throw IllegalStateException("Integrity check failed: expected $expectedAyahCount verses, got ${map.size}")
        }
        for (v in 1..expectedAyahCount) {
            if (!map.containsKey(v)) {
                throw IllegalStateException("Integrity check failed: missing verse $v")
            }
        }
        return map
    }

    private fun containsArabicScript(text: String): Boolean =
        text.any { ch -> ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F' }

    fun getAllSurahs(): List<SurahInfo> = SurahCatalog.surahs
}
