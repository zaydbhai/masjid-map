package com.example.data

import android.content.Context
import com.example.domain.QuranTafsirFormatter
import com.example.domain.QuranTafsirOption
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

    private val networkMonitor by lazy { NetworkMonitor(context.applicationContext) }

    private val cacheDir: File by lazy {
        File(context.filesDir, "quran_cache").apply { mkdirs() }
    }

    private val tafsirCacheDir: File by lazy {
        File(cacheDir, "tafsir").apply { mkdirs() }
    }

    private val arabicEdition = DEFAULT_ARABIC_EDITION

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
            sourceLabel = "Uthmani Hafs · GitHub fawazahmed0/quran-api"
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

    private val audioCacheDir: File by lazy {
        File(context.filesDir, "quran_audio_cache")
    }

    fun getCacheSizeBytes(): Long {
        val textBytes = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val audioBytes = if (audioCacheDir.exists()) {
            audioCacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            0L
        }
        return textBytes + audioBytes
    }

    fun clearCache() {
        runCatching { cacheDir.deleteRecursively() }
        runCatching { audioCacheDir.deleteRecursively() }
    }

    private fun downloadAndSplitEdition(
        edition: String,
        requireArabicScript: Boolean,
        onChapterProgress: (Int) -> Unit
    ) {
        if (!networkMonitor.isCurrentlyOnline()) {
            throw IllegalStateException("Offline: Connect to the internet to download Qur'an text.")
        }
        val candidateEditions = if (edition == arabicEdition) {
            listOf(arabicEdition, "ara-quranuthmanienc", "ara-quranacademy")
        } else {
            listOf(edition)
        }
        val fullUrls = candidateEditions.flatMap { ed ->
            listOf(
                "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/$ed.min.json",
                "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/$ed.json",
                "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/$ed.min.json",
                "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/$ed.json"
            )
        }

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

        if (!networkMonitor.isCurrentlyOnline()) {
            throw IllegalStateException("Offline: Surah $chapter is not downloaded yet.")
        }

        val candidateEditions = if (edition == arabicEdition) {
            listOf(arabicEdition, "ara-quranuthmanienc", "ara-quranacademy")
        } else {
            listOf(edition)
        }
        val urls = candidateEditions.flatMap { ed ->
            listOf(
                "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/$ed/$chapter.min.json",
                "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions/$ed/$chapter.json",
                "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/$ed/$chapter.min.json",
                "https://raw.githubusercontent.com/fawazahmed0/quran-api/1/editions/$ed/$chapter.json"
            )
        }

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

    suspend fun searchOfflineVerses(
        query: String,
        translation: QuranTranslationOption,
        limit: Int = 40
    ): List<com.example.domain.QuranSearchHit> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val normQuery = normalizeArabicForSearch(trimmed)
        val hits = ArrayList<com.example.domain.QuranSearchHit>(minOf(limit, 24))

        for (surah in SurahCatalog.surahs) {
            if (hits.size >= limit) break
            val arFile = File(cacheDir, "${arabicEdition}_${surah.number}.json")
            val edFile = File(cacheDir, "${translation.editionId}_${surah.number}.json")
            if (!arFile.exists() || !edFile.exists()) continue

            val arMap = runCatching {
                parseAndVerifyChapterJson(arFile.readText(), surah.ayahCount, true)
            }.getOrNull() ?: continue
            val enMap = runCatching {
                parseAndVerifyChapterJson(edFile.readText(), surah.ayahCount, false)
            }.getOrNull() ?: continue

            for (v in 1..surah.ayahCount) {
                if (hits.size >= limit) break
                val arText = arMap[v].orEmpty()
                val enText = enMap[v].orEmpty()
                val matchEn = enText.contains(trimmed, ignoreCase = true)
                val matchAr = normQuery.isNotEmpty() &&
                    (arText.contains(trimmed) || normalizeArabicForSearch(arText).contains(normQuery))
                if (matchEn || matchAr) {
                    hits.add(
                        com.example.domain.QuranSearchHit(
                            chapter = surah.number,
                            verse = v,
                            surahName = surah.nameEnglish,
                            arabicText = arText,
                            englishText = enText
                        )
                    )
                }
            }
        }
        hits
    }

    /**
     * Fetches and caches per-word English glosses & transliterations from Quran.com v4 API when
     * online; silently returns cached data or empty map when offline so offline morphological
     * root lookup always works without errors.
     */
    suspend fun fetchWordByWordForChapter(
        surahNumber: Int
    ): Map<Int, List<Pair<String, String?>>> = withContext(Dispatchers.IO) {
        val cacheFile = File(cacheDir, "wbw_en_$surahNumber.json")
        if (cacheFile.exists()) {
            val parsed = runCatching { parseWbwCacheJson(cacheFile.readText()) }.getOrNull()
            if (!parsed.isNullOrEmpty()) return@withContext parsed
        }
        if (!networkMonitor.isCurrentlyOnline()) return@withContext emptyMap()

        runCatching {
            val url =
                "https://api.quran.com/api/v4/verses/by_chapter/$surahNumber?language=en&words=true&word_fields=text_uthmani&per_page=300"
            val req = Request.Builder().url(url).build()
            NetworkClients.okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyMap()
                val body = resp.body?.string() ?: return@withContext emptyMap()
                val parsed = parseWbwApiJson(body)
                if (parsed.isNotEmpty()) {
                    val serialized = JSONObject()
                    parsed.forEach { (verseNum, pairs) ->
                        val arr = JSONArray()
                        pairs.forEach { (gloss, translit) ->
                            arr.put(
                                JSONObject()
                                    .put("g", gloss)
                                    .put("t", translit.orEmpty())
                            )
                        }
                        serialized.put(verseNum.toString(), arr)
                    }
                    cacheFile.writeText(serialized.toString())
                }
                parsed
            }
        }.getOrDefault(emptyMap())
    }

    private fun parseWbwCacheJson(raw: String): Map<Int, List<Pair<String, String?>>> {
        val obj = JSONObject(raw)
        val out = LinkedHashMap<Int, List<Pair<String, String?>>>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val vNum = key.toIntOrNull() ?: continue
            val arr = obj.optJSONArray(key) ?: continue
            val list = ArrayList<Pair<String, String?>>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val gloss = item.optString("g")
                val translit = item.optString("t").takeIf { it.isNotBlank() }
                list.add(gloss to translit)
            }
            out[vNum] = list
        }
        return out
    }

    private fun parseWbwApiJson(raw: String): Map<Int, List<Pair<String, String?>>> {
        val root = JSONObject(raw)
        val verses = root.optJSONArray("verses") ?: return emptyMap()
        val out = LinkedHashMap<Int, List<Pair<String, String?>>>(verses.length())
        for (i in 0 until verses.length()) {
            val vObj = verses.getJSONObject(i)
            val vNum = vObj.optInt("verse_number", 0)
            if (vNum <= 0) continue
            val words = vObj.optJSONArray("words") ?: continue
            val list = ArrayList<Pair<String, String?>>()
            for (w in 0 until words.length()) {
                val wObj = words.getJSONObject(w)
                if (wObj.optString("char_type_name") == "end") continue
                val gloss = wObj.optJSONObject("translation")?.optString("text").orEmpty()
                val translit = wObj.optJSONObject("transliteration")?.optString("text")
                    ?.takeIf { it.isNotBlank() }
                list.add(gloss to translit)
            }
            out[vNum] = list
        }
        return out
    }

    // ---- Offline Tafsir Commentary Repository ----

    fun getCachedTafsirSurahNumbers(edition: QuranTafsirOption): Set<Int> {
        if (!tafsirCacheDir.exists()) return emptySet()
        val cached = HashSet<Int>()
        for (ch in 1..114) {
            val f = File(tafsirCacheDir, "${edition.id}_$ch.json")
            if (f.exists() && f.length() > 12L) {
                cached.add(ch)
            }
        }
        return cached
    }

    suspend fun loadTafsirSurah(
        surahNumber: Int,
        edition: QuranTafsirOption
    ): Map<Int, String> = withContext(Dispatchers.IO) {
        val safeSurah = surahNumber.coerceIn(1, 114)
        tafsirCacheDir.mkdirs()
        val cacheFile = File(tafsirCacheDir, "${edition.id}_$safeSurah.json")
        if (cacheFile.exists()) {
            val cached = runCatching {
                QuranTafsirFormatter.parseTafsirChapterJson(cacheFile.readText())
            }.getOrNull()
            if (!cached.isNullOrEmpty()) return@withContext cached
        }

        if (!networkMonitor.isCurrentlyOnline()) {
            throw IllegalStateException(
                "Offline: ${edition.shortLabel} for Surah $safeSurah isn't saved yet. Connect once to cache it offline."
            )
        }

        val cdnUrls = listOf(
            "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@main/tafsir/${edition.id}/$safeSurah.json",
            "https://cdn.jsdelivr.net/gh/spa5k/tafsir_api@v1.2.3/tafsir/${edition.id}/$safeSurah.json",
            "https://raw.githubusercontent.com/spa5k/tafsir_api/main/tafsir/${edition.id}/$safeSurah.json"
        )

        var lastError: Throwable? = null
        for (url in cdnUrls) {
            try {
                val req = Request.Builder().url(url).build()
                NetworkClients.okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                    val body = resp.body?.string() ?: throw IllegalStateException("Empty body")
                    val parsed = QuranTafsirFormatter.parseTafsirChapterJson(body)
                    if (parsed.isNotEmpty()) {
                        cacheFile.writeText(QuranTafsirFormatter.serializeNormalizedChapterJson(parsed))
                        return@withContext parsed
                    }
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }

        // Fallback: Quran.com v4 chapter tafsir endpoint (handles pagination if large Surah)
        try {
            val combined = LinkedHashMap<Int, String>()
            var page = 1
            while (page <= 35) {
                val url =
                    "https://api.quran.com/api/v4/quran/tafsirs/${edition.quranComResourceId}?chapter_number=$safeSurah&per_page=300&page=$page"
                val req = Request.Builder().url(url).build()
                var nextPage: Int? = null
                NetworkClients.okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                    val body = resp.body?.string() ?: throw IllegalStateException("Empty body")
                    val parsed = QuranTafsirFormatter.parseTafsirChapterJson(body)
                    parsed.forEach { (ayah, text) ->
                        val prev = combined[ayah]
                        combined[ayah] = if (prev.isNullOrBlank()) text else "$prev\n\n$text"
                    }
                    val pag = JSONObject(body).optJSONObject("pagination")
                    val np = if (pag != null && !pag.isNull("next_page")) pag.optInt("next_page", 0) else 0
                    nextPage = if (np > page) np else null
                }
                val next = nextPage ?: break
                page = next
            }
            if (combined.isNotEmpty()) {
                cacheFile.writeText(QuranTafsirFormatter.serializeNormalizedChapterJson(combined))
                return@withContext combined
            }
        } catch (t: Throwable) {
            lastError = t
        }

        throw lastError ?: IllegalStateException(
            "Unable to load ${edition.shortLabel} for Surah $safeSurah."
        )
    }

    suspend fun downloadCompleteTafsirOffline(
        edition: QuranTafsirOption,
        onProgress: (completedSurahs: Int, totalSurahs: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        if (!networkMonitor.isCurrentlyOnline() && getCachedTafsirSurahNumbers(edition).size < 114) {
            throw IllegalStateException("Offline: Connect to the internet to download ${edition.shortLabel} Tafsir.")
        }
        var done = 0
        for (surah in SurahCatalog.surahs) {
            val cacheFile = File(tafsirCacheDir, "${edition.id}_${surah.number}.json")
            if (!cacheFile.exists() || cacheFile.length() <= 12L) {
                loadTafsirSurah(surah.number, edition)
            }
            done++
            if (done % 2 == 0 || done == 114) {
                onProgress(done, 114)
            }
        }
        getCachedTafsirSurahNumbers(edition).size
    }

    companion object {
        const val DEFAULT_ARABIC_EDITION = "ara-quranuthmanihaf"

        internal fun normalizeArabicForSearch(input: String): String {
            val sb = StringBuilder(input.length)
            for (ch in input) {
                val isDiacritic = ch in '\u064B'..'\u065F' || ch == '\u0670' || ch in '\u06D6'..'\u06ED' || ch == '\u0640'
                if (!isDiacritic) {
                    val mapped = when (ch) {
                        '\u0622', '\u0623', '\u0625', '\u0671' -> '\u0627'
                        '\u0649' -> '\u064A'
                        '\u0629' -> '\u0647'
                        else -> ch
                    }
                    sb.append(mapped)
                }
            }
            return sb.toString()
        }
    }
}
