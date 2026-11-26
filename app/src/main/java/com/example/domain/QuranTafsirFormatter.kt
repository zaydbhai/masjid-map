package com.example.domain

import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import okio.Buffer

/**
 * Pure Kotlin / Moshi streaming offline parser, HTML-to-paragraph formatter, and grouped-verse
 * resolver for cached and CDN-fetched Qur'an Tafsir chapters (works identically on Android & JVM).
 */
object QuranTafsirFormatter {

    private val blockBreakRegex = Regex(
        "</?(p|div|h[1-6]|blockquote|section|article|tr)[^>]*>",
        RegexOption.IGNORE_CASE
    )
    private val lineBreakRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val listItemStartRegex = Regex("<li[^>]*>", RegexOption.IGNORE_CASE)
    private val listItemEndRegex = Regex("</li>", RegexOption.IGNORE_CASE)
    private val anyTagRegex = Regex("<[^>]+>")
    private val numericEntityRegex = Regex("&#(x?[0-9a-fA-F]+);")
    private val multiBlankLinesRegex = Regex("\\n{3,}")
    private val trailingSpaceLineRegex = Regex("[ \\t]+\\n")
    private val leadingSpaceLineRegex = Regex("\\n[ \\t]+")

    fun cleanTafsirHtml(raw: String): String {
        if (raw.isBlank()) return ""
        var text = raw
            .replace("\r\n", "\n")
            .replace("\r", "\n")
        text = lineBreakRegex.replace(text, "\n")
        text = listItemStartRegex.replace(text, "\n• ")
        text = listItemEndRegex.replace(text, "\n")
        text = blockBreakRegex.replace(text, "\n\n")
        text = anyTagRegex.replace(text, "")
        text = decodeHtmlEntities(text)
        text = trailingSpaceLineRegex.replace(text, "\n")
        text = leadingSpaceLineRegex.replace(text, "\n")
        text = multiBlankLinesRegex.replace(text, "\n\n")
        return text.trim()
    }

    private fun decodeHtmlEntities(input: String): String {
        val named = input
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&rsquo;", "\u2019")
            .replace("&lsquo;", "\u2018")
            .replace("&rdquo;", "\u201D")
            .replace("&ldquo;", "\u201C")
            .replace("&mdash;", "\u2014")
            .replace("&ndash;", "\u2013")
            .replace("&hellip;", "\u2026")
        return numericEntityRegex.replace(named) { match ->
            val token = match.groupValues[1]
            val codePoint = if (token.startsWith("x", ignoreCase = true)) {
                token.substring(1).toIntOrNull(16)
            } else {
                token.toIntOrNull(10)
            }
            if (codePoint != null && Character.isValidCodePoint(codePoint)) {
                String(Character.toChars(codePoint))
            } else {
                match.value
            }
        }
    }

    /**
     * Parses Tafsir JSON from any supported format via streaming [JsonReader]:
     * 1) `spa5k/tafsir_api` root JSONArray `[{"ayah": 1, "text": "..."}]`
     * 2) Object with `"ayahs"` array `{"ayahs": [{"ayah": 1, "text": "..."}]}`
     * 3) `api.quran.com/api/v4/quran/tafsirs/{id}` object `{"tafsirs": [{"verse_number": 1, "text": "..."}]}`
     */
    fun parseTafsirChapterJson(rawJson: String): Map<Int, String> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return emptyMap()

        val out = LinkedHashMap<Int, String>()
        val buffer = Buffer().writeUtf8(trimmed)
        JsonReader.of(buffer).use { reader ->
            when (reader.peek()) {
                JsonReader.Token.BEGIN_ARRAY -> readItemsArray(reader, out)
                JsonReader.Token.BEGIN_OBJECT -> {
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "ayahs", "tafsirs", "chapter" -> {
                                if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                                    readItemsArray(reader, out)
                                } else {
                                    reader.skipValue()
                                }
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                else -> Unit
            }
        }
        return out
    }

    private fun readItemsArray(reader: JsonReader, out: MutableMap<Int, String>) {
        reader.beginArray()
        while (reader.hasNext()) {
            if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
                reader.skipValue()
                continue
            }
            var ayah = 0
            var verseNumber = 0
            var verse = 0
            var verseKey = ""
            var rawText = ""

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "ayah" -> ayah = readIntSafely(reader)
                    "verse_number" -> verseNumber = readIntSafely(reader)
                    "verse" -> verse = readIntSafely(reader)
                    "verse_key" -> verseKey = readStringSafely(reader)
                    "text" -> rawText = readStringSafely(reader)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            val resolvedAyah = when {
                ayah > 0 -> ayah
                verseNumber > 0 -> verseNumber
                verse > 0 -> verse
                verseKey.contains(":") -> verseKey.substringAfter(":").toIntOrNull() ?: 0
                else -> 0
            }
            if (resolvedAyah > 0) {
                val cleaned = cleanTafsirHtml(rawText)
                if (cleaned.isNotBlank()) {
                    val existing = out[resolvedAyah]
                    out[resolvedAyah] = if (existing.isNullOrBlank()) cleaned else "$existing\n\n$cleaned"
                }
            }
        }
        reader.endArray()
    }

    private fun readIntSafely(reader: JsonReader): Int = when (reader.peek()) {
        JsonReader.Token.NUMBER -> runCatching { reader.nextInt() }.getOrElse {
            reader.skipValue()
            0
        }
        JsonReader.Token.STRING -> reader.nextString().trim().toIntOrNull() ?: 0
        else -> {
            reader.skipValue()
            0
        }
    }

    private fun readStringSafely(reader: JsonReader): String = when (reader.peek()) {
        JsonReader.Token.STRING -> reader.nextString()
        JsonReader.Token.NUMBER -> reader.nextString()
        else -> {
            reader.skipValue()
            ""
        }
    }

    fun serializeNormalizedChapterJson(chapterMap: Map<Int, String>): String {
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer ->
            writer.beginObject()
            writer.name("ayahs")
            writer.beginArray()
            chapterMap.toSortedMap().forEach { (ayah, text) ->
                writer.beginObject()
                writer.name("ayah").value(ayah)
                writer.name("text").value(text)
                writer.endObject()
            }
            writer.endArray()
            writer.endObject()
        }
        return buffer.readUtf8()
    }

    /**
     * Resolves an Ayah's Tafsir commentary. Classic tafsirs (like Ibn Kathir & Ma'arif)
     * frequently combine consecutive verses under the opening verse of a passage; if the exact
     * verse key is omitted, falls back cleanly to the nearest preceding passage verse.
     */
    fun resolveVerseEntry(
        surahNumber: Int,
        verseNumber: Int,
        chapterMap: Map<Int, String>,
        edition: QuranTafsirOption
    ): TafsirEntry? {
        val direct = chapterMap[verseNumber]
        if (!direct.isNullOrBlank()) {
            return TafsirEntry(
                chapter = surahNumber,
                verse = verseNumber,
                commentary = direct,
                edition = edition,
                groupedSourceVerse = null
            )
        }
        val precedingKey = chapterMap.keys.filter { it < verseNumber }.maxOrNull()
        if (precedingKey != null) {
            val groupedText = chapterMap[precedingKey].orEmpty()
            if (groupedText.isNotBlank()) {
                return TafsirEntry(
                    chapter = surahNumber,
                    verse = verseNumber,
                    commentary = groupedText,
                    edition = edition,
                    groupedSourceVerse = precedingKey
                )
            }
        }
        val followingKey = chapterMap.keys.filter { it > verseNumber }.minOrNull()
        if (followingKey != null) {
            val groupedText = chapterMap[followingKey].orEmpty()
            if (groupedText.isNotBlank()) {
                return TafsirEntry(
                    chapter = surahNumber,
                    verse = verseNumber,
                    commentary = groupedText,
                    edition = edition,
                    groupedSourceVerse = followingKey
                )
            }
        }
        return null
    }
}
