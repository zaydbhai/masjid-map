package com.example.data

import android.content.Context
import com.example.domain.HadithEntry
import com.example.domain.HomeMasjidSnapshot
import com.example.domain.JumuahTimes
import com.example.domain.LatLngPoint
import com.example.domain.Masjid
import com.example.domain.PrayerName
import com.example.domain.QuranBookmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class BookmarksAndMasjidExtrasRepository private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "masjid_map_extras",
        Context.MODE_PRIVATE
    )

    private val _favoriteMasjidIds = MutableStateFlow(loadFavoriteIds())
    val favoriteMasjidIds: StateFlow<Set<String>> = _favoriteMasjidIds.asStateFlow()

    private val _iqamahByMasjid = MutableStateFlow(loadIqamahMap())
    val iqamahByMasjid: StateFlow<Map<String, Map<PrayerName, String>>> = _iqamahByMasjid.asStateFlow()

    private val _quranBookmarks = MutableStateFlow(loadQuranBookmarks())
    val quranBookmarks: StateFlow<List<QuranBookmark>> = _quranBookmarks.asStateFlow()

    private val _hadithBookmarks = MutableStateFlow(loadHadithBookmarks())
    val hadithBookmarks: StateFlow<List<HadithEntry>> = _hadithBookmarks.asStateFlow()

    private val _jumuahByMasjid = MutableStateFlow(loadJumuahMap())
    val jumuahByMasjid: StateFlow<Map<String, JumuahTimes>> = _jumuahByMasjid.asStateFlow()

    private val _homeMasjid = MutableStateFlow(loadHomeMasjid())
    val homeMasjid: StateFlow<HomeMasjidSnapshot?> = _homeMasjid.asStateFlow()

    fun setHomeMasjid(masjid: Masjid) {
        val snapshot = HomeMasjidSnapshot(
            id = masjid.id,
            name = masjid.name,
            address = masjid.address,
            location = masjid.location
        )
        val json = JSONObject().apply {
            put("id", snapshot.id)
            put("name", snapshot.name)
            put("address", snapshot.address)
            put("lat", snapshot.location.latitude)
            put("lng", snapshot.location.longitude)
        }
        prefs.edit().putString(KEY_HOME_MASJID, json.toString()).apply()
        _homeMasjid.value = snapshot
    }

    fun clearHomeMasjid() {
        prefs.edit().remove(KEY_HOME_MASJID).apply()
        _homeMasjid.value = null
    }

    fun toggleFavoriteMasjid(masjidId: String): Boolean {
        val next = _favoriteMasjidIds.value.toMutableSet()
        val added = if (next.contains(masjidId)) {
            next.remove(masjidId)
            false
        } else {
            next.add(masjidId)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITE_MASJIDS, next).apply()
        _favoriteMasjidIds.value = next
        return added
    }

    fun setIqamahTimes(masjidId: String, times: Map<PrayerName, String>) {
        val cleaned = times.filterValues { it.isNotBlank() }
        val updatedAll = _iqamahByMasjid.value.toMutableMap()
        if (cleaned.isEmpty()) {
            updatedAll.remove(masjidId)
        } else {
            updatedAll[masjidId] = cleaned
        }
        saveIqamahMap(updatedAll)
        _iqamahByMasjid.value = updatedAll
    }

    fun clearIqamahTimes(masjidId: String) {
        val updatedAll = _iqamahByMasjid.value.toMutableMap()
        updatedAll.remove(masjidId)
        saveIqamahMap(updatedAll)
        _iqamahByMasjid.value = updatedAll
    }

    fun setJumuahTimes(masjidId: String, firstKhutbah: String, secondKhutbah: String) {
        val times = JumuahTimes(firstKhutbah.trim(), secondKhutbah.trim())
        val updatedAll = _jumuahByMasjid.value.toMutableMap()
        if (!times.isConfigured) {
            updatedAll.remove(masjidId)
        } else {
            updatedAll[masjidId] = times
        }
        saveJumuahMap(updatedAll)
        _jumuahByMasjid.value = updatedAll
    }

    fun toggleQuranBookmark(
        chapter: Int,
        verse: Int,
        surahName: String,
        arabicText: String,
        englishText: String
    ): Boolean {
        val current = _quranBookmarks.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.chapter == chapter && it.verse == verse }
        val added = if (existingIdx >= 0) {
            current.removeAt(existingIdx)
            false
        } else {
            current.add(
                0,
                QuranBookmark(
                    chapter = chapter,
                    verse = verse,
                    surahName = surahName,
                    arabicSnippet = arabicText.take(120),
                    englishSnippet = englishText.take(160)
                )
            )
            true
        }
        saveQuranBookmarks(current)
        _quranBookmarks.value = current
        return added
    }

    fun toggleHadithBookmark(entry: HadithEntry): Boolean {
        val current = _hadithBookmarks.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.id == entry.id }
        val added = if (existingIdx >= 0) {
            current.removeAt(existingIdx)
            false
        } else {
            current.add(0, entry)
            true
        }
        saveHadithBookmarks(current)
        _hadithBookmarks.value = current
        return added
    }

    private fun loadFavoriteIds(): Set<String> =
        prefs.getStringSet(KEY_FAVORITE_MASJIDS, emptySet())?.toSet() ?: emptySet()

    private fun loadIqamahMap(): Map<String, Map<PrayerName, String>> {
        val raw = prefs.getString(KEY_IQAMAH_TIMES, null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            val result = LinkedHashMap<String, Map<PrayerName, String>>()
            val masjidKeys = root.keys()
            while (masjidKeys.hasNext()) {
                val mId = masjidKeys.next()
                val obj = root.getJSONObject(mId)
                val inner = LinkedHashMap<PrayerName, String>()
                PrayerName.entries.forEach { p ->
                    val v = obj.optString(p.name).trim()
                    if (v.isNotBlank()) inner[p] = v
                }
                if (inner.isNotEmpty()) result[mId] = inner
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun saveIqamahMap(map: Map<String, Map<PrayerName, String>>) {
        val root = JSONObject()
        map.forEach { (mId, times) ->
            val obj = JSONObject()
            times.forEach { (p, v) -> obj.put(p.name, v) }
            root.put(mId, obj)
        }
        prefs.edit().putString(KEY_IQAMAH_TIMES, root.toString()).apply()
    }

    private fun loadQuranBookmarks(): List<QuranBookmark> {
        val raw = prefs.getString(KEY_QURAN_BOOKMARKS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        QuranBookmark(
                            chapter = o.getInt("chapter"),
                            verse = o.getInt("verse"),
                            surahName = o.getString("surahName"),
                            arabicSnippet = o.optString("arabicSnippet"),
                            englishSnippet = o.optString("englishSnippet"),
                            savedAtMillis = o.optLong("savedAtMillis")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveQuranBookmarks(list: List<QuranBookmark>) {
        val arr = JSONArray()
        list.forEach { b ->
            arr.put(
                JSONObject().apply {
                    put("chapter", b.chapter)
                    put("verse", b.verse)
                    put("surahName", b.surahName)
                    put("arabicSnippet", b.arabicSnippet)
                    put("englishSnippet", b.englishSnippet)
                    put("savedAtMillis", b.savedAtMillis)
                }
            )
        }
        prefs.edit().putString(KEY_QURAN_BOOKMARKS, arr.toString()).apply()
    }

    private fun loadHadithBookmarks(): List<HadithEntry> {
        val raw = prefs.getString(KEY_HADITH_BOOKMARKS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        HadithEntry(
                            id = o.getString("id"),
                            numberLabel = o.getString("numberLabel"),
                            bookTitle = o.getString("bookTitle"),
                            chapterTitle = o.optString("chapterTitle"),
                            arabicText = o.optString("arabicText"),
                            narrator = o.optString("narrator"),
                            englishText = o.optString("englishText"),
                            grade = o.optString("grade").takeIf { it.isNotBlank() },
                            reference = o.optString("reference")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveHadithBookmarks(list: List<HadithEntry>) {
        val arr = JSONArray()
        list.forEach { h ->
            arr.put(
                JSONObject().apply {
                    put("id", h.id)
                    put("numberLabel", h.numberLabel)
                    put("bookTitle", h.bookTitle)
                    put("chapterTitle", h.chapterTitle)
                    put("arabicText", h.arabicText)
                    put("narrator", h.narrator)
                    put("englishText", h.englishText)
                    put("grade", h.grade.orEmpty())
                    put("reference", h.reference)
                }
            )
        }
        prefs.edit().putString(KEY_HADITH_BOOKMARKS, arr.toString()).apply()
    }

    private fun loadJumuahMap(): Map<String, JumuahTimes> {
        val raw = prefs.getString(KEY_JUMUAH_TIMES, null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            val result = LinkedHashMap<String, JumuahTimes>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val mId = keys.next()
                val obj = root.getJSONObject(mId)
                val times = JumuahTimes(
                    firstKhutbah = obj.optString("first").trim(),
                    secondKhutbah = obj.optString("second").trim()
                )
                if (times.isConfigured) result[mId] = times
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun saveJumuahMap(map: Map<String, JumuahTimes>) {
        val root = JSONObject()
        map.forEach { (mId, times) ->
            root.put(
                mId,
                JSONObject().apply {
                    put("first", times.firstKhutbah)
                    put("second", times.secondKhutbah)
                }
            )
        }
        prefs.edit().putString(KEY_JUMUAH_TIMES, root.toString()).apply()
    }

    private fun loadHomeMasjid(): HomeMasjidSnapshot? {
        val raw = prefs.getString(KEY_HOME_MASJID, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            val id = obj.getString("id")
            val name = obj.getString("name")
            val address = obj.optString("address")
            val lat = obj.getDouble("lat")
            val lng = obj.getDouble("lng")
            HomeMasjidSnapshot(id, name, address, LatLngPoint(lat, lng))
        }.getOrNull()
    }

    companion object {
        private const val KEY_FAVORITE_MASJIDS = "favorite_masjid_ids"
        private const val KEY_IQAMAH_TIMES = "iqamah_times_json"
        private const val KEY_JUMUAH_TIMES = "jumuah_times_json"
        private const val KEY_QURAN_BOOKMARKS = "quran_bookmarks_json"
        private const val KEY_HADITH_BOOKMARKS = "hadith_bookmarks_json"
        private const val KEY_HOME_MASJID = "home_masjid_json"

        @Volatile
        private var instance: BookmarksAndMasjidExtrasRepository? = null

        fun getInstance(context: Context): BookmarksAndMasjidExtrasRepository {
            return instance ?: synchronized(this) {
                instance ?: BookmarksAndMasjidExtrasRepository(context).also { instance = it }
            }
        }
    }
}
