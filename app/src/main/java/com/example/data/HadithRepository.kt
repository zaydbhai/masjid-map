package com.example.data

import android.content.Context
import com.example.domain.HadithBookInfo
import com.example.domain.HadithCatalog
import com.example.domain.HadithChapter
import com.example.domain.HadithEntry
import com.example.domain.HadithSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HadithRepository(private val context: Context) {

    private val networkMonitor by lazy { NetworkMonitor(context.applicationContext) }

    private val cacheDir: File by lazy {
        File(context.filesDir, "hadith_cache").apply { mkdirs() }
    }

    fun getTop21Books(): List<HadithBookInfo> = HadithCatalog.top21Books

    fun isBookOfflineReady(book: HadithBookInfo): Boolean {
        return when (book.sourceType) {
            HadithSourceType.AHMED_BASET_BOOK ->
                File(cacheDir, "ahmed_${book.id}.json").exists()
            HadithSourceType.HISN_MUSLIM_JSON ->
                File(cacheDir, "hisn_en.json").exists()
            HadithSourceType.FAWAZ_SECTIONS,
            HadithSourceType.HADEETH_ENC ->
                File(cacheDir, "book_complete_${book.id}.flag").exists()
        }
    }

    fun getOfflineReadyBookIds(): Set<String> =
        HadithCatalog.top21Books.filter { isBookOfflineReady(it) }.map { it.id }.toSet()

    fun getCacheSizeBytes(): Long =
        cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun clearCache() {
        runCatching { cacheDir.deleteRecursively() }
    }

    suspend fun downloadBookOffline(book: HadithBookInfo) = withContext(Dispatchers.IO) {
        when (book.sourceType) {
            HadithSourceType.AHMED_BASET_BOOK -> {
                getAhmedBasetBookJson(book)
            }
            HadithSourceType.HISN_MUSLIM_JSON -> {
                getHisnRootArray()
            }
            HadithSourceType.FAWAZ_SECTIONS -> {
                downloadFawazFullBook(book)
            }
            HadithSourceType.HADEETH_ENC -> {
                val chapters = getChapters(book)
                var totalEntries = 0
                chapters.forEach { ch ->
                    totalEntries += getHadithsForChapter(book, ch).size
                }
                if (totalEntries == 0) {
                    throw IllegalStateException("Could not download ${book.titleEnglish} right now. Please try again when online.")
                }
                File(cacheDir, "book_complete_${book.id}.flag").writeText("1")
            }
        }
    }

    suspend fun getHadithOfTheDay(): HadithEntry = withContext(Dispatchers.IO) {
        val dayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val dailyCache = File(cacheDir, "daily_$dayKey.json")
        if (dailyCache.exists()) {
            runCatching {
                val obj = JSONObject(dailyCache.readText())
                return@withContext HadithEntry(
                    id = obj.getString("id"),
                    numberLabel = obj.getString("numberLabel"),
                    bookTitle = obj.getString("bookTitle"),
                    chapterTitle = obj.getString("chapterTitle"),
                    arabicText = obj.getString("arabicText"),
                    narrator = obj.getString("narrator"),
                    englishText = obj.getString("englishText"),
                    grade = obj.optString("grade").takeIf { it.isNotBlank() },
                    reference = obj.optString("reference")
                )
            }
        }

        val cal = Calendar.getInstance()
        val seed = cal.get(Calendar.YEAR) * 366 + cal.get(Calendar.DAY_OF_YEAR)

        // Try locally cached or live 40 Nawawi / Riyad as-Salihin
        try {
            val nawawiBook = HadithCatalog.top21Books.first { it.id == "nawawi40" }
            val chapters = getChapters(nawawiBook)
            val firstChapter = chapters.firstOrNull()
            if (firstChapter != null) {
                val entries = getHadithsForChapter(nawawiBook, firstChapter)
                    .filter { it.englishText.isNotBlank() }
                if (entries.isNotEmpty()) {
                    val chosen = entries[seed % entries.size]
                    saveDailyCache(dailyCache, chosen)
                    return@withContext chosen
                }
            }
        } catch (_: Exception) {
        }

        // If offline and user downloaded Riyad as-Salihin, draw from it locally
        try {
            val riyad = HadithCatalog.top21Books.first { it.id == "riyadussalihin" }
            if (isBookOfflineReady(riyad)) {
                val ch = getAhmedBasetChapters(riyad).firstOrNull()
                if (ch != null) {
                    val entries = getAhmedBasetHadiths(riyad, ch).filter { it.englishText.isNotBlank() }
                    if (entries.isNotEmpty()) {
                        return@withContext entries[seed % entries.size]
                    }
                }
            }
        } catch (_: Exception) {
        }

        HadithCatalog.authenticDailySeeds[seed % HadithCatalog.authenticDailySeeds.size]
    }

    fun getCachedOrSeedDailyHadith(): HadithEntry {
        val dayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val dailyCache = File(cacheDir, "daily_$dayKey.json")
        if (dailyCache.exists()) {
            runCatching {
                val obj = JSONObject(dailyCache.readText())
                return HadithEntry(
                    id = obj.getString("id"),
                    numberLabel = obj.getString("numberLabel"),
                    bookTitle = obj.getString("bookTitle"),
                    chapterTitle = obj.getString("chapterTitle"),
                    arabicText = obj.getString("arabicText"),
                    narrator = obj.getString("narrator"),
                    englishText = obj.getString("englishText"),
                    grade = obj.optString("grade").takeIf { it.isNotBlank() },
                    reference = obj.optString("reference")
                )
            }
        }
        val cal = Calendar.getInstance()
        val seed = cal.get(Calendar.YEAR) * 366 + cal.get(Calendar.DAY_OF_YEAR)
        return HadithCatalog.authenticDailySeeds[seed % HadithCatalog.authenticDailySeeds.size]
    }

    private fun saveDailyCache(file: File, entry: HadithEntry) {
        runCatching {
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("numberLabel", entry.numberLabel)
                put("bookTitle", entry.bookTitle)
                put("chapterTitle", entry.chapterTitle)
                put("arabicText", entry.arabicText)
                put("narrator", entry.narrator)
                put("englishText", entry.englishText)
                put("grade", entry.grade.orEmpty())
                put("reference", entry.reference)
            }
            file.writeText(obj.toString())
        }
    }

    suspend fun getChapters(book: HadithBookInfo): List<HadithChapter> = withContext(Dispatchers.IO) {
        when (book.sourceType) {
            HadithSourceType.FAWAZ_SECTIONS -> getFawazChapters(book)
            HadithSourceType.AHMED_BASET_BOOK -> getAhmedBasetChapters(book)
            HadithSourceType.HISN_MUSLIM_JSON -> getHisnChapters()
            HadithSourceType.HADEETH_ENC -> listOf(
                HadithChapter(
                    id = book.hadeethEncCategoryId ?: "1",
                    numberLabel = "1",
                    titleEnglish = book.titleEnglish,
                    titleArabic = book.titleArabic
                )
            )
        }
    }

    suspend fun getHadithsForChapter(
        book: HadithBookInfo,
        chapter: HadithChapter
    ): List<HadithEntry> = withContext(Dispatchers.IO) {
        when (book.sourceType) {
            HadithSourceType.FAWAZ_SECTIONS -> getFawazHadiths(book, chapter)
            HadithSourceType.AHMED_BASET_BOOK -> getAhmedBasetHadiths(book, chapter)
            HadithSourceType.HISN_MUSLIM_JSON -> getHisnEntries(chapter)
            HadithSourceType.HADEETH_ENC -> getHadeethEncEntries(book, chapter)
        }
    }

    // ---- 1. fawazahmed0/hadith-api (Section-based fast loading + Full book offline cache) ----

    private fun downloadFawazFullBook(book: HadithBookInfo) {
        val slug = book.fawazSlug ?: return
        getFawazInfoJson()

        val engRaw = fetchHttpString(
            "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-$slug.min.json",
            "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-$slug.json"
        )
        splitAndCacheFawazSections(prefix = "fawaz_eng_$slug", rawJson = engRaw)

        runCatching {
            val araRaw = fetchHttpString(
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-$slug.min.json",
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-$slug.json"
            )
            splitAndCacheFawazSections(prefix = "fawaz_ara_$slug", rawJson = araRaw)
        }

        File(cacheDir, "book_complete_${book.id}.flag").writeText("1")
    }

    private fun splitAndCacheFawazSections(prefix: String, rawJson: String) {
        val root = JSONObject(rawJson)
        val arr = root.optJSONArray("hadiths") ?: return
        val metadata = root.optJSONObject("metadata")
        val sectionsObj = metadata?.optJSONObject("sections")

        if (sectionsObj == null || sectionsObj.length() <= 1) {
            File(cacheDir, "${prefix}_all.json").writeText(rawJson)
            File(cacheDir, "${prefix}_1.json").writeText(rawJson)
            sectionsObj?.keys()?.forEach { key ->
                if (key.isNotBlank() && key != "0") {
                    File(cacheDir, "${prefix}_$key.json").writeText(rawJson)
                }
            }
            return
        }

        val grouped = HashMap<String, MutableList<JSONObject>>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val secKey = item.optJSONObject("reference")?.opt("book")?.toString() ?: "1"
            grouped.getOrPut(secKey) { mutableListOf() }.add(item)
        }

        val keys = sectionsObj.keys()
        while (keys.hasNext()) {
            val secKey = keys.next()
            val list = grouped[secKey].orEmpty()
            val secJson = JSONObject().put("hadiths", JSONArray(list)).toString()
            File(cacheDir, "${prefix}_$secKey.json").writeText(secJson)
        }
    }

    private fun getFawazInfoJson(): JSONObject {
        val cacheFile = File(cacheDir, "fawaz_info.json")
        if (cacheFile.exists()) {
            runCatching { return JSONObject(cacheFile.readText()) }
        }
        val body = fetchHttpString(
            "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/info.min.json",
            "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/info.json"
        )
        cacheFile.writeText(body)
        return JSONObject(body)
    }

    private fun getFawazChapters(book: HadithBookInfo): List<HadithChapter> {
        val slug = book.fawazSlug ?: return emptyList()
        val info = getFawazInfoJson()
        val bookObj = info.optJSONObject(slug) ?: return emptyList()
        val metadata = bookObj.optJSONObject("metadata") ?: return emptyList()
        val sectionsObj = metadata.optJSONObject("sections") ?: JSONObject()

        val chapters = mutableListOf<HadithChapter>()
        val keys = sectionsObj.keys()
        while (keys.hasNext()) {
            val secKey = keys.next()
            val secTitle = sectionsObj.optString(secKey).trim()
            if (secKey == "0" && secTitle.isBlank()) continue
            val label = secTitle.ifBlank { "Chapter $secKey" }
            chapters.add(
                HadithChapter(
                    id = secKey,
                    numberLabel = secKey,
                    titleEnglish = label
                )
            )
        }
        val sorted = chapters.sortedBy { it.id.toIntOrNull() ?: Int.MAX_VALUE }
        if (sorted.isEmpty()) {
            return listOf(
                HadithChapter(
                    id = "all",
                    numberLabel = "1",
                    titleEnglish = book.titleEnglish,
                    titleArabic = book.titleArabic
                )
            )
        }
        return sorted
    }

    private fun getFawazHadiths(book: HadithBookInfo, chapter: HadithChapter): List<HadithEntry> {
        val slug = book.fawazSlug ?: return emptyList()
        val engJson = if (chapter.id == "all") {
            fetchCachedOrNetwork(
                "fawaz_eng_${slug}_all.json",
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-$slug.min.json",
                "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-$slug.json"
            )
        } else {
            val sectionCache = File(cacheDir, "fawaz_eng_${slug}_${chapter.id}.json")
            val allCache = File(cacheDir, "fawaz_eng_${slug}_all.json")
            if (!sectionCache.exists() && allCache.exists() && allCache.length() > 2) {
                allCache.readText()
            } else {
                fetchCachedOrNetwork(
                    sectionCache.name,
                    "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-$slug/sections/${chapter.id}.min.json",
                    "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/eng-$slug/sections/${chapter.id}.json"
                )
            }
        }

        val araMap = runCatching {
            val araJson = if (chapter.id == "all") {
                fetchCachedOrNetwork(
                    "fawaz_ara_${slug}_all.json",
                    "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-$slug.min.json",
                    "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-$slug.json"
                )
            } else {
                val araSectionCache = File(cacheDir, "fawaz_ara_${slug}_${chapter.id}.json")
                val araAllCache = File(cacheDir, "fawaz_ara_${slug}_all.json")
                if (!araSectionCache.exists() && araAllCache.exists() && araAllCache.length() > 2) {
                    araAllCache.readText()
                } else {
                    fetchCachedOrNetwork(
                        araSectionCache.name,
                        "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-$slug/sections/${chapter.id}.min.json",
                        "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/ara-$slug/sections/${chapter.id}.json"
                    )
                }
            }
            val arr = JSONObject(araJson).optJSONArray("hadiths") ?: JSONArray()
            val map = HashMap<String, String>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val num = obj.opt("hadithnumber")?.toString() ?: continue
                map[num] = obj.optString("text").trim()
            }
            map
        }.getOrDefault(emptyMap())

        val engArr = JSONObject(engJson).optJSONArray("hadiths") ?: return emptyList()
        val result = ArrayList<HadithEntry>(engArr.length())
        for (i in 0 until engArr.length()) {
            val obj = engArr.getJSONObject(i)
            val num = obj.opt("hadithnumber")?.toString() ?: "${i + 1}"
            val text = obj.optString("text").trim()
            if (text.isBlank()) continue
            val gradesArr = obj.optJSONArray("grades")
            val gradeStr = if (gradesArr != null && gradesArr.length() > 0) {
                gradesArr.getJSONObject(0).optString("grade").takeIf { it.isNotBlank() }
            } else {
                null
            }
            result.add(
                HadithEntry(
                    id = "${book.id}_${chapter.id}_$num",
                    numberLabel = "Hadith $num",
                    bookTitle = book.titleEnglish,
                    chapterTitle = chapter.titleEnglish,
                    arabicText = araMap[num].orEmpty(),
                    narrator = "",
                    englishText = text,
                    grade = gradeStr,
                    reference = "${book.titleEnglish} $num"
                )
            )
        }
        return result
    }

    // ---- 2. AhmedBaset/hadith-json (Complete books cached locally) ----

    private fun getAhmedBasetBookJson(book: HadithBookInfo): JSONObject {
        val path = book.ahmedBasetPath ?: throw IllegalStateException("Missing path")
        val raw = fetchCachedOrNetwork(
            "ahmed_${book.id}.json",
            "https://cdn.jsdelivr.net/gh/AhmedBaset/hadith-json@v1.2.0/db/by_book/$path.json",
            "https://raw.githubusercontent.com/AhmedBaset/hadith-json/v1.2.0/db/by_book/$path.json"
        )
        return JSONObject(raw)
    }

    private fun getAhmedBasetChapters(book: HadithBookInfo): List<HadithChapter> {
        val root = getAhmedBasetBookJson(book)
        val arr = root.optJSONArray("chapters") ?: return listOf(
            HadithChapter("0", "1", book.titleEnglish, book.titleArabic)
        )
        val chapters = ArrayList<HadithChapter>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.optInt("id", i + 1).toString()
            val en = obj.optString("english").trim().ifBlank { "Chapter $id" }
            val ar = obj.optString("arabic").trim()
            chapters.add(HadithChapter(id = id, numberLabel = id, titleEnglish = en, titleArabic = ar))
        }
        return chapters
    }

    private fun getAhmedBasetHadiths(book: HadithBookInfo, chapter: HadithChapter): List<HadithEntry> {
        val root = getAhmedBasetBookJson(book)
        val arr = root.optJSONArray("hadiths") ?: return emptyList()
        val chapterIdInt = chapter.id.toIntOrNull()
        val list = ArrayList<HadithEntry>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val cId = obj.optInt("chapterId", -1)
            if (chapterIdInt != null && cId != chapterIdInt) continue
            val idInBook = obj.optInt("idInBook", obj.optInt("id", i + 1))
            val arabic = obj.optString("arabic").trim()
            val engObj = obj.optJSONObject("english")
            val narrator = engObj?.optString("narrator")?.trim().orEmpty()
            val text = engObj?.optString("text")?.trim().orEmpty()
            if (text.isBlank() && arabic.isBlank()) continue
            list.add(
                HadithEntry(
                    id = "${book.id}_$idInBook",
                    numberLabel = "Hadith $idInBook",
                    bookTitle = book.titleEnglish,
                    chapterTitle = chapter.titleEnglish,
                    arabicText = arabic,
                    narrator = narrator,
                    englishText = text,
                    reference = "${book.titleEnglish} $idInBook"
                )
            )
        }
        return list
    }

    // ---- 3. Hisn al-Muslim (wafaaelmaandy/Hisn-Muslim-Json) ----

    private fun getHisnRootArray(): JSONArray {
        val raw = fetchCachedOrNetwork(
            "hisn_en.json",
            "https://cdn.jsdelivr.net/gh/wafaaelmaandy/Hisn-Muslim-Json@master/husn_en.json",
            "https://raw.githubusercontent.com/wafaaelmaandy/Hisn-Muslim-Json/master/husn_en.json"
        )
        val trimmed = raw.trim()
        if (trimmed.startsWith("[")) return JSONArray(trimmed)
        val obj = JSONObject(trimmed)
        val firstKey = obj.keys().asSequence().firstOrNull() ?: return JSONArray()
        return obj.optJSONArray(firstKey) ?: JSONArray()
    }

    private fun getHisnChapters(): List<HadithChapter> {
        val arr = getHisnRootArray()
        val chapters = ArrayList<HadithChapter>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val id = obj.opt("ID")?.toString() ?: "${i + 1}"
            val title = obj.optString("TITLE").trim().ifBlank { "Chapter $id" }
            chapters.add(HadithChapter(id = id, numberLabel = id, titleEnglish = title))
        }
        return chapters
    }

    private fun getHisnEntries(chapter: HadithChapter): List<HadithEntry> {
        val arr = getHisnRootArray()
        for (i in 0 until arr.length()) {
            val catObj = arr.getJSONObject(i)
            val id = catObj.opt("ID")?.toString() ?: "${i + 1}"
            if (id != chapter.id) continue
            val items = catObj.optJSONArray("TEXT") ?: return emptyList()
            val list = ArrayList<HadithEntry>(items.length())
            for (j in 0 until items.length()) {
                val item = items.getJSONObject(j)
                val dhikrId = item.opt("ID")?.toString() ?: "${j + 1}"
                val arabic = item.optString("ARABIC_TEXT").ifBlank { item.optString("Text") }.trim()
                val trans = item.optString("TRANSLATED_TEXT").trim()
                val translit = item.optString("LANGUAGE_ARABIC_TRANSLATED_TEXT").trim()
                val repeat = item.opt("REPEAT")?.toString()?.takeIf { it.isNotBlank() && it != "0" }
                list.add(
                    HadithEntry(
                        id = "hisn_${id}_$dhikrId",
                        numberLabel = "Dhikr ${j + 1}",
                        bookTitle = "Hisn al-Muslim",
                        chapterTitle = chapter.titleEnglish,
                        arabicText = arabic,
                        narrator = translit,
                        englishText = trans,
                        grade = repeat?.let { "Repeat ${it}x" },
                        reference = "Hisn al-Muslim #$dhikrId"
                    )
                )
            }
            return list
        }
        return emptyList()
    }

    // ---- 4. HadeethEnc API v1 (Books 19-21) ----

    private fun getHadeethEncEntries(book: HadithBookInfo, chapter: HadithChapter): List<HadithEntry> {
        val catId = book.hadeethEncCategoryId ?: chapter.id
        val listRaw = fetchCachedOrNetwork(
            "hadeethenc_cat_${catId}_p1.json",
            "https://hadeethenc.com/api/v1/hadeeths/list/?language=en&category_id=$catId&page=1&per_page=20"
        )
        val dataArr = JSONObject(listRaw).optJSONArray("data") ?: return emptyList()
        val result = ArrayList<HadithEntry>()
        val limit = minOf(dataArr.length(), 15)
        for (i in 0 until limit) {
            val summary = dataArr.getJSONObject(i)
            val hId = summary.optString("id")
            if (hId.isBlank()) continue
            runCatching {
                val enRaw = fetchCachedOrNetwork(
                    "hadeethenc_en_$hId.json",
                    "https://hadeethenc.com/api/v1/hadeeths/one/?language=en&id=$hId"
                )
                val enObj = JSONObject(enRaw)
                val arObj = runCatching {
                    val arRaw = fetchCachedOrNetwork(
                        "hadeethenc_ar_$hId.json",
                        "https://hadeethenc.com/api/v1/hadeeths/one/?language=ar&id=$hId"
                    )
                    JSONObject(arRaw)
                }.getOrNull()

                result.add(
                    HadithEntry(
                        id = "henc_$hId",
                        numberLabel = "Hadith #$hId",
                        bookTitle = book.titleEnglish,
                        chapterTitle = enObj.optString("title").ifBlank { chapter.titleEnglish },
                        arabicText = arObj?.optString("hadeeth")?.trim().orEmpty(),
                        narrator = "",
                        englishText = enObj.optString("hadeeth").trim(),
                        grade = enObj.optString("grade").takeIf { it.isNotBlank() },
                        reference = enObj.optString("attribution").ifBlank { "HadeethEnc #$hId" }
                    )
                )
            }
        }
        return result
    }

    private fun fetchCachedOrNetwork(cacheName: String, vararg urls: String): String {
        val file = File(cacheDir, cacheName)
        if (file.exists() && file.length() > 2) {
            return file.readText()
        }
        val content = fetchHttpString(*urls)
        file.writeText(content)
        return content
    }

    private fun fetchHttpString(vararg urls: String): String {
        if (!networkMonitor.isCurrentlyOnline()) {
            throw IllegalStateException("Offline: book chapter is not downloaded yet.")
        }
        var lastError: Throwable? = null
        for (url in urls) {
            try {
                val request = Request.Builder().url(url).build()
                NetworkClients.okHttpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("HTTP ${resp.code} for $url")
                    }
                    return resp.body?.string() ?: throw IllegalStateException("Empty response")
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: IllegalStateException("Offline: book chapter is not downloaded yet.")
    }

    suspend fun searchOfflineCachedBooks(
        query: String,
        limit: Int = 40
    ): List<com.example.domain.HadithSearchHit> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val hits = ArrayList<com.example.domain.HadithSearchHit>(minOf(limit, 24))

        for (book in HadithCatalog.top21Books) {
            if (hits.size >= limit) break
            when (book.sourceType) {
                HadithSourceType.AHMED_BASET_BOOK -> {
                    val file = File(cacheDir, "ahmed_${book.id}.json")
                    if (!file.exists()) continue
                    runCatching {
                        val root = JSONObject(file.readText())
                        val arr = root.optJSONArray("hadiths") ?: return@runCatching
                        for (i in 0 until arr.length()) {
                            if (hits.size >= limit) break
                            val obj = arr.getJSONObject(i)
                            val cId = obj.optInt("chapterId", 0).toString()
                            val idInBook = obj.optInt("idInBook", obj.optInt("id", i + 1))
                            val arabic = obj.optString("arabic").trim()
                            val engObj = obj.optJSONObject("english")
                            val narrator = engObj?.optString("narrator")?.trim().orEmpty()
                            val text = engObj?.optString("text")?.trim().orEmpty()
                            if (text.contains(trimmed, ignoreCase = true) ||
                                narrator.contains(trimmed, ignoreCase = true) ||
                                arabic.contains(trimmed)
                            ) {
                                hits.add(
                                    com.example.domain.HadithSearchHit(
                                        bookId = book.id,
                                        bookTitle = book.titleEnglish,
                                        chapterId = cId,
                                        entry = HadithEntry(
                                            id = "${book.id}_$idInBook",
                                            numberLabel = "Hadith $idInBook",
                                            bookTitle = book.titleEnglish,
                                            chapterTitle = "",
                                            arabicText = arabic,
                                            narrator = narrator,
                                            englishText = text,
                                            reference = "${book.titleEnglish} $idInBook"
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
                HadithSourceType.HISN_MUSLIM_JSON -> {
                    val file = File(cacheDir, "hisn_en.json")
                    if (!file.exists()) continue
                    runCatching {
                        val chapters = getHisnChapters()
                        for (ch in chapters) {
                            if (hits.size >= limit) break
                            getHisnEntries(ch).forEach { entry ->
                                if (hits.size < limit &&
                                    (entry.englishText.contains(trimmed, ignoreCase = true) ||
                                        entry.chapterTitle.contains(trimmed, ignoreCase = true) ||
                                        entry.arabicText.contains(trimmed))
                                ) {
                                    hits.add(
                                        com.example.domain.HadithSearchHit(
                                            bookId = book.id,
                                            bookTitle = book.titleEnglish,
                                            chapterId = ch.id,
                                            entry = entry
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                HadithSourceType.FAWAZ_SECTIONS -> {
                    val slug = book.fawazSlug ?: continue
                    val files = cacheDir.listFiles { f ->
                        f.name.startsWith("fawaz_eng_${slug}_") && f.name.endsWith(".json")
                    }.orEmpty()
                    val seenNumbers = HashSet<String>()
                    for (file in files) {
                        if (hits.size >= limit) break
                        val secKey = file.name.removePrefix("fawaz_eng_${slug}_").removeSuffix(".json")
                        runCatching {
                            val araMap = runCatching {
                                val araFile = File(cacheDir, "fawaz_ara_${slug}_$secKey.json")
                                    .takeIf { it.exists() }
                                    ?: File(cacheDir, "fawaz_ara_${slug}_all.json").takeIf { it.exists() }
                                val araArr = araFile?.let { JSONObject(it.readText()).optJSONArray("hadiths") }
                                    ?: JSONArray()
                                HashMap<String, String>(araArr.length()).apply {
                                    for (j in 0 until araArr.length()) {
                                        val obj = araArr.getJSONObject(j)
                                        val n = obj.opt("hadithnumber")?.toString() ?: continue
                                        put(n, obj.optString("text").trim())
                                    }
                                }
                            }.getOrDefault(emptyMap())

                            val engArr = JSONObject(file.readText()).optJSONArray("hadiths") ?: return@runCatching
                            for (i in 0 until engArr.length()) {
                                if (hits.size >= limit) break
                                val obj = engArr.getJSONObject(i)
                                val num = obj.opt("hadithnumber")?.toString() ?: "${i + 1}"
                                if (!seenNumbers.add(num)) continue
                                val text = obj.optString("text").trim()
                                val arabic = araMap[num].orEmpty()
                                if (text.contains(trimmed, ignoreCase = true) ||
                                    (arabic.isNotEmpty() && arabic.contains(trimmed))
                                ) {
                                    hits.add(
                                        com.example.domain.HadithSearchHit(
                                            bookId = book.id,
                                            bookTitle = book.titleEnglish,
                                            chapterId = secKey,
                                            entry = HadithEntry(
                                                id = "${book.id}_${secKey}_$num",
                                                numberLabel = "Hadith $num",
                                                bookTitle = book.titleEnglish,
                                                chapterTitle = if (secKey == "all") book.titleEnglish else "Section $secKey",
                                                arabicText = arabic,
                                                narrator = "",
                                                englishText = text,
                                                reference = "${book.titleEnglish} $num"
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                HadithSourceType.HADEETH_ENC -> Unit
            }
        }
        hits
    }
}
