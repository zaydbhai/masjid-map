package com.example.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

data class TajwidPalette(
    val ghunnah: Color,
    val qalqalah: Color,
    val idgham: Color,
    val ikhfa: Color,
    val madd: Color,
    val lafzJalalah: Color
) {
    fun colorFor(rule: TajwidRule): Color = when (rule) {
        TajwidRule.GHUNNAH -> ghunnah
        TajwidRule.QALQALAH -> qalqalah
        TajwidRule.IDGHAM -> idgham
        TajwidRule.IKHFA -> ikhfa
        TajwidRule.MADD -> madd
        TajwidRule.LAFZ_JALALAH -> lafzJalalah
    }

    companion object {
        fun forTheme(isDark: Boolean): TajwidPalette = if (isDark) {
            TajwidPalette(
                ghunnah = Color(0xFF4ADE80),      // Vibrant Emerald
                qalqalah = Color(0xFFFB7185),     // Coral Rose
                idgham = Color(0xFF38BDF8),       // Sky Blue
                ikhfa = Color(0xFFFBBF24),        // Warm Amber
                madd = Color(0xFFC084FC),         // Soft Violet
                lafzJalalah = Color(0xFFF59E0B)   // Gold Accent
            )
        } else {
            TajwidPalette(
                ghunnah = Color(0xFF047857),      // Deep Emerald
                qalqalah = Color(0xFFBE123C),     // Crimson
                idgham = Color(0xFF0369A1),       // Ocean Blue
                ikhfa = Color(0xFFB45309),        // Deep Amber
                madd = Color(0xFF7E22CE),         // Royal Violet
                lafzJalalah = Color(0xFF9A3412)   // Warm Ochre
            )
        }
    }
}

/**
 * Offline rule-based Tajwid colorizer that annotates Uthmani Arabic script without mutating a
 * single character of the original Qur'anic text.
 */
object QuranTajwidEngine {

    private const val SHADDAH = '\u0651'
    private const val SUKUN = '\u0652'
    private const val SMALL_HIGH_ROUNDED_ZERO = '\u06DF'
    private const val SMALL_HIGH_UPRIGHT_RECT = '\u06E1' // Uthmani sukun glyph
    private const val MADDAH = '\u0653'
    private const val MADDAH_UTHMANI = '\u06E4'
    private const val SUPERSCRIPT_ALIF = '\u0670'

    private val TANWIN_CHARS = setOf('\u064B', '\u064C', '\u064D')
    private val QALQALAH_LETTERS = setOf('ق', 'ط', 'ب', 'ج', 'د')
    private val IDGHAM_LETTERS = setOf('ي', 'ر', 'م', 'ل', 'و', 'ن')
    private val IZHAR_THROAT_LETTERS = setOf('ء', 'أ', 'إ', 'آ', 'ه', 'ع', 'ح', 'غ', 'خ')
    private val HAMZAH_CHARS = setOf('ء', 'أ', 'إ', 'ؤ', 'ئ')
    private val LONG_VOWEL_LETTERS = setOf('ا', 'و', 'ي', 'ى')

    private val WAQF_MARKS = setOf(
        '\u06D6', '\u06D7', '\u06D8', '\u06D9', '\u06DA', '\u06DB', '\u06DC', '\u06DD', '\u06DE', '\u06E9'
    )

    private fun isDiacritic(c: Char): Boolean {
        val code = c.code
        return (code in 0x064B..0x065F) || c == SUPERSCRIPT_ALIF || (code in 0x06D6..0x06ED)
    }

    private fun isSukunMark(c: Char): Boolean =
        c == SUKUN || c == SMALL_HIGH_UPRIGHT_RECT || c == SMALL_HIGH_ROUNDED_ZERO

    fun buildTajwidAnnotatedString(text: String, palette: TajwidPalette): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        return buildAnnotatedString {
            append(text)
            val n = text.length

            // 1. Highlight Lafz al-Jalalah (الله / لله)
            var idx = 0
            while (idx < n) {
                val nextBaseIndices = ArrayList<Int>(6)
                var scan = idx
                while (scan < n && nextBaseIndices.size < 4 && !text[scan].isWhitespace()) {
                    if (!isDiacritic(text[scan])) {
                        nextBaseIndices.add(scan)
                    }
                    scan++
                }
                if (nextBaseIndices.size == 4) {
                    val b0 = text[nextBaseIndices[0]]
                    val b1 = text[nextBaseIndices[1]]
                    val b2 = text[nextBaseIndices[2]]
                    val b3 = text[nextBaseIndices[3]]
                    val isAllah = (b0 == 'ا' || b0 == 'ٱ' || b0 == 'ل') && b1 == 'ل' && b2 == 'ل' && b3 == 'ه'
                    if (isAllah && (scan >= n || text[scan].isWhitespace() || isDiacritic(text[scan]))) {
                        val wordEnd = findWordEnd(text, idx)
                        addStyle(
                            SpanStyle(color = palette.lafzJalalah, fontWeight = FontWeight.SemiBold),
                            start = idx,
                            end = wordEnd
                        )
                    }
                }
                idx = findNextWordStart(text, idx)
            }

            // 2. Character-level Tajwid rules (Ghunnah, Qalqalah, Madd, Idgham, Ikhfa/Iqlab)
            var i = 0
            while (i < n) {
                val ch = text[i]
                if (ch.isWhitespace() || isDiacritic(ch)) {
                    i++
                    continue
                }

                // Collect diacritics attached to base letter at i
                var j = i + 1
                var hasShaddah = false
                var hasSukun = false
                var hasMaddah = false
                var hasTanwin = false
                while (j < n && isDiacritic(text[j])) {
                    val d = text[j]
                    if (d == SHADDAH) hasShaddah = true
                    if (isSukunMark(d)) hasSukun = true
                    if (d == MADDAH || d == MADDAH_UTHMANI || d == SUPERSCRIPT_ALIF) hasMaddah = true
                    if (d in TANWIN_CHARS) hasTanwin = true
                    j++
                }

                // Rule A: Ghunnah (نّ or مّ)
                if ((ch == 'ن' || ch == 'م') && hasShaddah) {
                    addStyle(
                        SpanStyle(color = palette.ghunnah, fontWeight = FontWeight.SemiBold),
                        start = i,
                        end = j
                    )
                    i = j
                    continue
                }

                // Rule B: Maddah mark or vowel letter followed by Hamzah in same word
                if (hasMaddah || (ch in LONG_VOWEL_LETTERS && nextBaseCharInWord(text, j) in HAMZAH_CHARS)) {
                    addStyle(
                        SpanStyle(color = palette.madd),
                        start = i,
                        end = j
                    )
                    i = j
                    continue
                }

                // Rule C: Qalqalah (ق ط ب ج د with sukun or at end of word before pause)
                if (ch in QALQALAH_LETTERS && (hasSukun || (j >= n || text[j].isWhitespace() || text[j] in WAQF_MARKS))) {
                    // Only highlight end-of-word Qalqalah when preceded by sukun or stop, or explicit sukun mid-word
                    if (hasSukun || j >= n || text[j] in WAQF_MARKS) {
                        addStyle(
                            SpanStyle(color = palette.qalqalah, fontWeight = FontWeight.SemiBold),
                            start = i,
                            end = j
                        )
                        i = j
                        continue
                    }
                }

                // Rule D: Noon Sakinah or Tanwin -> Idgham / Ikhfa / Iqlab
                if ((ch == 'ن' && hasSukun) || hasTanwin) {
                    val nextCons = nextBaseCharSkippingSpaces(text, j)
                    when {
                        nextCons == null || nextCons in IZHAR_THROAT_LETTERS -> Unit
                        nextCons in IDGHAM_LETTERS -> {
                            addStyle(SpanStyle(color = palette.idgham), start = i, end = j)
                        }
                        else -> {
                            // Ikhfa or Iqlab (ب)
                            addStyle(SpanStyle(color = palette.ikhfa), start = i, end = j)
                        }
                    }
                }

                i = j
            }
        }
    }

    private fun findWordEnd(text: String, start: Int): Int {
        var k = start
        while (k < text.length && !text[k].isWhitespace()) k++
        return k
    }

    private fun findNextWordStart(text: String, from: Int): Int {
        var k = findWordEnd(text, from)
        while (k < text.length && text[k].isWhitespace()) k++
        return k
    }

    private fun nextBaseCharInWord(text: String, start: Int): Char? {
        var k = start
        while (k < text.length) {
            val c = text[k]
            if (c.isWhitespace()) return null
            if (!isDiacritic(c)) return c
            k++
        }
        return null
    }

    private fun nextBaseCharSkippingSpaces(text: String, start: Int): Char? {
        var k = start
        while (k < text.length) {
            val c = text[k]
            if (!c.isWhitespace() && !isDiacritic(c) && c !in WAQF_MARKS) return c
            k++
        }
        return null
    }

    /**
     * Tokenizes Uthmani verse text into indexable tokens with exact character offsets so
     * RTL Arabic ligatures remain unbroken while every individual word can be tapped.
     */
    fun tokenizeArabicWords(arabicText: String): List<QuranWordToken> {
        if (arabicText.isBlank()) return emptyList()
        val tokens = ArrayList<QuranWordToken>()
        val n = arabicText.length
        var i = 0
        var wordIdx = 0
        while (i < n) {
            while (i < n && arabicText[i].isWhitespace()) i++
            if (i >= n) break
            val start = i
            while (i < n && !arabicText[i].isWhitespace()) i++
            val end = i
            val slice = arabicText.substring(start, end)
            val clean = stripDiacriticsAndMarks(slice)
            if (clean.isNotBlank() && clean.any { it in '\u0621'..'\u064A' || it == 'ٱ' }) {
                tokens.add(
                    QuranWordToken(
                        wordIndex = wordIdx++,
                        startOffset = start,
                        endOffset = end,
                        surfaceWord = slice
                    )
                )
            }
        }
        return tokens
    }

    fun stripDiacriticsAndMarks(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (c in raw) {
            if (c.isWhitespace() || isDiacritic(c) || c in WAQF_MARKS) continue
            val normalized = when (c) {
                'ٱ', 'أ', 'إ', 'آ' -> 'ا'
                'ى' -> 'ي'
                else -> c
            }
            if (normalized in '\u0621'..'\u064A') {
                sb.append(normalized)
            }
        }
        return sb.toString()
    }

    fun analyzeArabicWordOffline(
        word: String,
        wordIndex: Int,
        apiGloss: String? = null,
        apiTranslit: String? = null
    ): QuranWordAnalysis {
        val clean = stripDiacriticsAndMarks(word)
        val morphology = detectMorphology(word, clean)
        val dominantRule = detectDominantTajwidRule(word)

        // Direct curated exact/stem lookup first
        val exactMatch = CURATED_WORDS[clean]
        if (exactMatch != null) {
            return QuranWordAnalysis(
                wordIndex = wordIndex,
                surfaceWord = word,
                cleanArabic = clean,
                rootLetters = exactMatch.root,
                morphologyNote = exactMatch.morphology ?: morphology,
                coreMeaning = exactMatch.meaning,
                contextualGloss = apiGloss,
                transliteration = apiTranslit,
                dominantTajwidRule = dominantRule
            )
        }

        // Stem stripping prefixes/suffixes to match curated root dictionary or derive 3-letter root
        val stripped = stripPrefixesAndSuffixes(clean)
        val stemMatch = CURATED_STEMS[stripped]
            ?: CURATED_WORDS[stripped]
            ?: matchCuratedSubstring(stripped)

        if (stemMatch != null) {
            return QuranWordAnalysis(
                wordIndex = wordIndex,
                surfaceWord = word,
                cleanArabic = clean,
                rootLetters = stemMatch.root,
                morphologyNote = stemMatch.morphology ?: morphology,
                coreMeaning = stemMatch.meaning,
                contextualGloss = apiGloss,
                transliteration = apiTranslit,
                dominantTajwidRule = dominantRule
            )
        }

        val derivedRoot = extractTrilateralRoot(stripped)
        val rootLookup = ROOT_MEANINGS[derivedRoot]
        return QuranWordAnalysis(
            wordIndex = wordIndex,
            surfaceWord = word,
            cleanArabic = clean,
            rootLetters = derivedRoot,
            morphologyNote = morphology,
            coreMeaning = rootLookup ?: "Derived Arabic stem ($stripped)",
            contextualGloss = apiGloss,
            transliteration = apiTranslit,
            dominantTajwidRule = dominantRule
        )
    }

    private fun detectDominantTajwidRule(word: String): TajwidRule? {
        if (word.contains("للّٰه") || word.contains("الله") || word.contains("لله")) {
            return TajwidRule.LAFZ_JALALAH
        }
        if (word.contains("ن$SHADDAH") || word.contains("م$SHADDAH")) {
            return TajwidRule.GHUNNAH
        }
        if (word.any { it == MADDAH || it == MADDAH_UTHMANI }) {
            return TajwidRule.MADD
        }
        for (idx in word.indices) {
            if (word[idx] in QALQALAH_LETTERS) {
                val next = word.getOrNull(idx + 1)
                if (next != null && isSukunMark(next)) return TajwidRule.QALQALAH
            }
        }
        return null
    }

    private fun detectMorphology(surface: String, clean: String): String {
        val tags = mutableListOf<String>()
        if (clean.startsWith("ال") || clean.startsWith("لل")) tags.add("Definite Noun (ال)")
        if (clean.endsWith("ون") || clean.endsWith("ين")) tags.add("Plural / Sound Masculine")
        if (clean.endsWith("ات")) tags.add("Feminine Plural")
        if (clean.startsWith("و") && clean.length > 3) tags.add("Prefixed 'And' (وَ)")
        if (clean.startsWith("ب") && clean.length > 3) tags.add("Prep. 'By/With' (بِـ)")
        if (clean.startsWith("ف") && clean.length > 3) tags.add("Conjunction 'Then/So' (فَـ)")
        if (surface.endsWith("ة") || clean.endsWith("ة")) tags.add("Feminine Ta-Marbutah")
        return if (tags.isEmpty()) "Qur'anic Word Form" else tags.joinToString(" · ")
    }

    private fun stripPrefixesAndSuffixes(clean: String): String {
        var s = clean
        if (s.length <= 3) return s
        val prefixes = listOf("وبال", "فبال", "ولل", "فلل", "وال", "فال", "بال", "كال", "ال", "لل", "سي", "ست", "سن", "و", "ف", "ب", "ل", "ك")
        for (p in prefixes) {
            if (s.startsWith(p) && s.length - p.length >= 3) {
                s = s.removePrefix(p)
                break
            }
        }
        val suffixes = listOf("كموه", "تمون", "ونهم", "ينهم", "هما", "هم", "كم", "هن", "نا", "ها", "ون", "ين", "ان", "ات", "وا", "تم", "تن", "ة", "ه", "ي", "ك")
        for (suf in suffixes) {
            if (s.endsWith(suf) && s.length - suf.length >= 3) {
                s = s.removeSuffix(suf)
                break
            }
        }
        return s
    }

    private fun extractTrilateralRoot(stem: String): String {
        if (stem.length <= 3) {
            return stem.toCharArray().joinToString("-")
        }
        val weakOrExtra = setOf('ا', 'و', 'ي', 'م', 'ت', 'ن', 'س')
        val strongChars = stem.filter { it !in weakOrExtra }
        val selected = when {
            strongChars.length == 3 -> strongChars
            strongChars.length > 3 -> strongChars.take(3)
            else -> {
                val merged = StringBuilder()
                for (c in stem) {
                    if (merged.length < 3 && (c !in setOf('ا', 'ت', 'م', 'ن') || merged.isNotEmpty())) {
                        merged.append(c)
                    }
                }
                if (merged.length < 3) stem.take(3) else merged.toString()
            }
        }
        return selected.toCharArray().joinToString("-")
    }

    private fun matchCuratedSubstring(stem: String): CuratedRootInfo? {
        return CURATED_STEMS.entries
            .firstOrNull { (key, _) -> key.length >= 3 && stem.contains(key) }
            ?.value
    }

    private data class CuratedRootInfo(
        val root: String,
        val meaning: String,
        val morphology: String? = null
    )

    private val CURATED_WORDS: Map<String, CuratedRootInfo> = mapOf(
        "بسم" to CuratedRootInfo("س-م-و", "In the name of (Prep. ب + اسم)", "Preposition + Noun"),
        "اسم" to CuratedRootInfo("س-م-و", "Name, designation, attribute", "Noun"),
        "الله" to CuratedRootInfo("أ-ل-ه", "Allah, The One True God worthy of all worship", "Proper Noun (Lafz al-Jalalah)"),
        "لله" to CuratedRootInfo("أ-ل-ه", "To/For Allah alone", "Prep. ل + Proper Noun"),
        "الرحمن" to CuratedRootInfo("ر-ح-م", "The Entirely & Universally Merciful", "Intensive Attribute (فَعْلَان)"),
        "الرحيم" to CuratedRootInfo("ر-ح-م", "The Especially & Continually Merciful", "Intensive Attribute (فَعِيل)"),
        "رحمة" to CuratedRootInfo("ر-ح-م", "Mercy, compassion, grace", "Noun"),
        "الحمد" to CuratedRootInfo("ح-م-د", "All praise and gratitude", "Definite Verbal Noun"),
        "رب" to CuratedRootInfo("ر-ب-ب", "Lord, Sustainer, Cherisher, Master", "Noun"),
        "العالمين" to CuratedRootInfo("ع-ل-م", "All the worlds & creations", "Sound Plural Noun"),
        "مالك" to CuratedRootInfo("م-ل-ك", "Master, Owner, Sovereign", "Active Participle"),
        "ملك" to CuratedRootInfo("م-ل-ك", "King, Sovereign / Dominion", "Noun"),
        "يوم" to CuratedRootInfo("ي-و-م", "Day, era, appointed time", "Noun"),
        "الدين" to CuratedRootInfo("د-ي-ن", "Recompense, Judgment, Way of life", "Definite Noun"),
        "اياك" to CuratedRootInfo("إ-ي-ا", "You alone (emphatic object pronoun)", "Detached Pronoun"),
        "نعبد" to CuratedRootInfo("ع-ب-د", "We worship & serve humbly", "Present Verb (1st Pl.)"),
        "نستعين" to CuratedRootInfo("ع-و-ن", "We seek help and assistance", "Form X Verb (اسْتَفْعَلَ)"),
        "اهدنا" to CuratedRootInfo("ه-د-ي", "Guide us (Imperative + 'us')", "Imperative Verb + Pronoun"),
        "الهدي" to CuratedRootInfo("ه-د-ي", "The true guidance", "Definite Noun"),
        "الصراط" to CuratedRootInfo("ص-ر-ط", "The clear wide path", "Definite Noun"),
        "المستقيم" to CuratedRootInfo("ق-و-م", "The straight, upright, balanced", "Form X Active Participle"),
        "انعمت" to CuratedRootInfo("ن-ع-م", "You bestowed favor/blessing", "Form IV Past Verb"),
        "المغضوب" to CuratedRootInfo("غ-ض-ب", "Those who earned wrath", "Passive Participle"),
        "الضالين" to CuratedRootInfo("ض-ل-ل", "Those who went astray", "Active Participle Plural"),
        "قل" to CuratedRootInfo("ق-و-ل", "Say! Proclaim!", "Imperative Verb"),
        "قال" to CuratedRootInfo("ق-و-ل", "He said / spoke", "Past Verb"),
        "قالوا" to CuratedRootInfo("ق-و-ل", "They said", "Past Verb Plural"),
        "هو" to CuratedRootInfo("هـ-و", "He / It (detached pronoun)", "Pronoun"),
        "احد" to CuratedRootInfo("أ-ح-د", "One, Unique, Indivisible", "Noun / Adjective"),
        "الصمد" to CuratedRootInfo("ص-م-د", "The Eternal Refuge, Self-Sufficient", "Divine Attribute"),
        "يلد" to CuratedRootInfo("و-ل-د", "Beget / give birth", "Present Jussive Verb"),
        "يولد" to CuratedRootInfo("و-ل-د", "Be born / begotten", "Passive Jussive Verb"),
        "كفوا" to CuratedRootInfo("ك-ف-أ", "Equal, comparable, equivalent", "Noun"),
        "اعوذ" to CuratedRootInfo("ع-و-ذ", "I seek refuge & protection", "Present Verb"),
        "الفلق" to CuratedRootInfo("ف-ل-ق", "The daybreak / splitting dawn", "Definite Noun"),
        "خلق" to CuratedRootInfo("خ-ل-ق", "He created / creation", "Past Verb / Noun"),
        "الناس" to CuratedRootInfo("ن-و-س / أ-ن-س", "Mankind, humanity", "Definite Noun"),
        "شر" to CuratedRootInfo("ش-ر-ر", "Evil, harm, mischief", "Noun"),
        "وسواس" to CuratedRootInfo("و-س-و-س", "Repeated whispering temptation", "Quadrilateral Noun"),
        "لا" to CuratedRootInfo("Particle", "No / Not (negation particle)", "Negation Particle"),
        "ما" to CuratedRootInfo("Particle", "What / That which / Not", "Relative / Negation Particle"),
        "من" to CuratedRootInfo("Particle", "From, of, among / Whoever", "Preposition / Pronoun"),
        "في" to CuratedRootInfo("Particle", "In, within, concerning", "Preposition"),
        "علي" to CuratedRootInfo("ع-ل-و", "Upon, over, against", "Preposition"),
        "الي" to CuratedRootInfo("Particle", "To, toward, until", "Preposition"),
        "ان" to CuratedRootInfo("Particle", "Indeed, truly / If", "Emphasis / Conditional Particle"),
        "الذين" to CuratedRootInfo("Relative", "Those who (masculine plural)", "Relative Pronoun"),
        "الذي" to CuratedRootInfo("Relative", "He who / That which", "Relative Pronoun")
    )

    private val CURATED_STEMS: Map<String, CuratedRootInfo> = mapOf(
        "رحم" to CuratedRootInfo("ر-ح-م", "Mercy, womb-kindness, compassion"),
        "حمد" to CuratedRootInfo("ح-م-د", "Praise, commendation, gratitude"),
        "علم" to CuratedRootInfo("ع-ل-م", "Knowledge, knowing, sign, worlds"),
        "عبد" to CuratedRootInfo("ع-ب-د", "Worship, devotion, servitude to Allah"),
        "هدي" to CuratedRootInfo("ه-د-ي", "Guidance, showing the right way"),
        "ملك" to CuratedRootInfo("م-ل-ك", "Sovereignty, ownership, dominion"),
        "قول" to CuratedRootInfo("ق-و-ل", "Speech, saying, word"),
        "كتب" to CuratedRootInfo("ك-ت-ب", "Writing, Book, scripture, decree"),
        "امن" to CuratedRootInfo("أ-م-ن", "Faith (Iman), trust, safety, peace"),
        "صلو" to CuratedRootInfo("ص-ل-و", "Prayer (Salah), connection, blessing"),
        "صلا" to CuratedRootInfo("ص-ل-و", "Prayer (Salah), devotion"),
        "زكو" to CuratedRootInfo("ز-ك-و", "Purification, growth, Zakah"),
        "صبر" to CuratedRootInfo("ص-ب-ر", "Patience, steadfast perseverance"),
        "شكر" to CuratedRootInfo("ش-ك-ر", "Gratitude, thankfulness, appreciation"),
        "غفر" to CuratedRootInfo("غ-ف-ر", "Forgiveness, covering over faults"),
        "توب" to CuratedRootInfo("ت-و-ب", "Repentance, returning to Allah"),
        "نزل" to CuratedRootInfo("ن-ز-ل", "Sending down revelation, descending"),
        "خلق" to CuratedRootInfo("خ-ل-ق", "Creation, shaping, moral character"),
        "سمو" to CuratedRootInfo("س-م-و", "Heavens, height, lofty name"),
        "ارض" to CuratedRootInfo("أ-ر-ض", "Earth, land, ground"),
        "نفس" to CuratedRootInfo("ن-ف-س", "Soul, self, inner being"),
        "قلب" to CuratedRootInfo("ق-ل-ب", "Heart, turning, innermost mind"),
        "نور" to CuratedRootInfo("ن-و-ر", "Light, illumination, clarity"),
        "حقق" to CuratedRootInfo("ح-ق-ق", "Truth, reality, right, obligation"),
        "حق" to CuratedRootInfo("ح-ق-ق", "Truth, right, genuine"),
        "بين" to CuratedRootInfo("ب-ي-ن", "Clear evidence, making distinct, between"),
        "جعل" to CuratedRootInfo("ج-ع-ل", "Making, appointing, placing"),
        "فعل" to CuratedRootInfo("ف-ع-ل", "Doing, action, deed"),
        "ذكر" to CuratedRootInfo("ذ-ك-ر", "Remembrance (Dhikr), reminder, mention"),
        "سبح" to CuratedRootInfo("س-ب-ح", "Glorification (Tasbih), floating/extolling perfection"),
        "قرا" to CuratedRootInfo("ق-ر-أ", "Reciting, reading, gathering (Qur'an)"),
        "فلح" to CuratedRootInfo("ف-ل-ح", "True success, thriving, flourishing"),
        "تقو" to CuratedRootInfo("و-ق-ي", "Taqwa, God-consciousness, protection"),
        "وقي" to CuratedRootInfo("و-ق-ي", "Guarding, mindfulness (Taqwa)"),
        "قوم" to CuratedRootInfo("ق-و-م", "Standing upright, people, establishing"),
        "عظم" to CuratedRootInfo("ع-ظ-م", "Magnificence, greatness, mighty"),
        "كرم" to CuratedRootInfo("ك-ر-م", "Generosity, nobility, honor"),
        "حكم" to CuratedRootInfo("ح-ك-م", "Wisdom, judgment, wise decree"),
        "خير" to CuratedRootInfo("خ-ي-ر", "Goodness, blessing, what is better"),
        "رسل" to CuratedRootInfo("ر-س-ل", "Sending messengers, message"),
        "نبي" to CuratedRootInfo("ن-ب-أ", "Prophet, bringing momentous news"),
        "جنت" to CuratedRootInfo("ج-ن-ن", "Paradise, lush garden"),
        "جنة" to CuratedRootInfo("ج-ن-ن", "Paradise, lush sheltered garden"),
        "نار" to CuratedRootInfo("ن-و-ر", "Fire, flame"),
        "موت" to CuratedRootInfo("م-و-ت", "Death, passing away"),
        "حيي" to CuratedRootInfo("ح-ي-ي", "Life, living, modesty"),
        "عقل" to CuratedRootInfo("ع-ق-ل", "Reasoning, intellect, understanding"),
        "فكر" to CuratedRootInfo("ف-ك-ر", "Reflection, deep contemplation"),
        "بصر" to CuratedRootInfo("ب-ص-ر", "Sight, insight, perceiving clearly"),
        "سمع" to CuratedRootInfo("س-م-ع", "Hearing, listening attentively"),
        "يدع" to CuratedRootInfo("د-ع-و", "Calling upon, supplicating (Dua)"),
        "دعو" to CuratedRootInfo("د-ع-و", "Calling, inviting, Dua"),
        "خوف" to CuratedRootInfo("خ-و-ف", "Fear, awe, caution"),
        "رجو" to CuratedRootInfo("ر-ج-و", "Hope, longing, expectation"),
        "ظلم" to CuratedRootInfo("ظ-ل-م", "Wrongdoing, oppression, darkness"),
        "عدل" to CuratedRootInfo("ع-د-ل", "Justice, fairness, balance"),
        "صدق" to CuratedRootInfo("ص-د-ق", "Truthfulness, sincerity, charity (Sadaqah)"),
        "رزق" to CuratedRootInfo("ر-ز-ق", "Provision, sustenance, livelihood"),
        "نصر" to CuratedRootInfo("ن-ص-ر", "Victory, decisive help, support"),
        "فتح" to CuratedRootInfo("ف-ت-ح", "Opening, victory, clear resolution")
    )

    private val ROOT_MEANINGS: Map<String, String> = CURATED_STEMS.values.associate { it.root to it.meaning } + mapOf(
        "ر-ب-ب" to "Lordship, nurturing care, sustaining",
        "أ-ل-ه" to "Divinity, worship of the One True God",
        "س-م-و" to "Name / elevation / heavens",
        "ي-و-م" to "Day, time, period",
        "د-ي-ن" to "Judgment, religion, owed accountability",
        "ع-و-ن" to "Aid, assistance, support",
        "ص-ر-ط" to "Clear highway / straight path",
        "ن-ع-م" to "Blessing, favor, ease, grace",
        "ض-ل-ل" to "Straying, losing the path",
        "غ-ض-ب" to "Anger, displeasure",
        "و-ل-د" to "Offspring, birth, lineage",
        "ف-ل-ق" to "Splitting open, breaking of dawn"
    )
}
