package com.example.domain

/**
 * Canonical 114 Surah metadata with exact Uthmani ayah counts (6,236 total)
 * used to verify fetched GitHub Quran chapters against alteration or truncation.
 */
object SurahCatalog {
    val surahs: List<SurahInfo> = listOf(
        SurahInfo(1, "Al-Fatihah", "The Opener", "الفاتحة", 7, "Makki"),
        SurahInfo(2, "Al-Baqarah", "The Cow", "البقرة", 286, "Madani"),
        SurahInfo(3, "Ali 'Imran", "Family of Imran", "آل عمران", 200, "Madani"),
        SurahInfo(4, "An-Nisa", "The Women", "النساء", 176, "Madani"),
        SurahInfo(5, "Al-Ma'idah", "The Table Spread", "المائدة", 120, "Madani"),
        SurahInfo(6, "Al-An'am", "The Cattle", "الأنعام", 165, "Makki"),
        SurahInfo(7, "Al-A'raf", "The Heights", "الأعراف", 206, "Makki"),
        SurahInfo(8, "Al-Anfal", "The Spoils of War", "الأنفال", 75, "Madani"),
        SurahInfo(9, "At-Tawbah", "The Repentance", "التوبة", 129, "Madani"),
        SurahInfo(10, "Yunus", "Jonah", "يونس", 109, "Makki"),
        SurahInfo(11, "Hud", "Hud", "هود", 123, "Makki"),
        SurahInfo(12, "Yusuf", "Joseph", "يوسف", 111, "Makki"),
        SurahInfo(13, "Ar-Ra'd", "The Thunder", "الرعد", 43, "Madani"),
        SurahInfo(14, "Ibrahim", "Abraham", "إبراهيم", 52, "Makki"),
        SurahInfo(15, "Al-Hijr", "The Rocky Tract", "الحجر", 99, "Makki"),
        SurahInfo(16, "An-Nahl", "The Bee", "النحل", 128, "Makki"),
        SurahInfo(17, "Al-Isra", "The Night Journey", "الإسراء", 111, "Makki"),
        SurahInfo(18, "Al-Kahf", "The Cave", "الكهف", 110, "Makki"),
        SurahInfo(19, "Maryam", "Mary", "مريم", 98, "Makki"),
        SurahInfo(20, "Taha", "Ta-Ha", "طه", 135, "Makki"),
        SurahInfo(21, "Al-Anbya", "The Prophets", "الأنبياء", 112, "Makki"),
        SurahInfo(22, "Al-Hajj", "The Pilgrimage", "الحج", 78, "Madani"),
        SurahInfo(23, "Al-Mu'minun", "The Believers", "المؤمنون", 118, "Makki"),
        SurahInfo(24, "An-Nur", "The Light", "النور", 64, "Madani"),
        SurahInfo(25, "Al-Furqan", "The Criterion", "الفرقان", 77, "Makki"),
        SurahInfo(26, "Ash-Shu'ara", "The Poets", "الشعراء", 227, "Makki"),
        SurahInfo(27, "An-Naml", "The Ant", "النمل", 93, "Makki"),
        SurahInfo(28, "Al-Qasas", "The Stories", "القصص", 88, "Makki"),
        SurahInfo(29, "Al-'Ankabut", "The Spider", "العنكبوت", 69, "Makki"),
        SurahInfo(30, "Ar-Rum", "The Romans", "الروم", 60, "Makki"),
        SurahInfo(31, "Luqman", "Luqman", "لقمان", 34, "Makki"),
        SurahInfo(32, "As-Sajdah", "The Prostration", "السجدة", 30, "Makki"),
        SurahInfo(33, "Al-Ahzab", "The Combined Forces", "الأحزاب", 73, "Madani"),
        SurahInfo(34, "Saba", "Sheba", "سبإ", 54, "Makki"),
        SurahInfo(35, "Fatir", "Originator", "فاطر", 45, "Makki"),
        SurahInfo(36, "Ya-Sin", "Ya Sin", "يس", 83, "Makki"),
        SurahInfo(37, "As-Saffat", "Those Who Set the Ranks", "الصافات", 182, "Makki"),
        SurahInfo(38, "Sad", "The Letter Sad", "ص", 88, "Makki"),
        SurahInfo(39, "Az-Zumar", "The Troops", "الزمر", 75, "Makki"),
        SurahInfo(40, "Ghafir", "The Forgiver", "غافر", 85, "Makki"),
        SurahInfo(41, "Fussilat", "Explained in Detail", "فصلت", 54, "Makki"),
        SurahInfo(42, "Ash-Shuraa", "The Consultation", "الشورى", 53, "Makki"),
        SurahInfo(43, "Az-Zukhruf", "The Ornaments of Gold", "الزخرف", 89, "Makki"),
        SurahInfo(44, "Ad-Dukhan", "The Smoke", "الدخان", 59, "Makki"),
        SurahInfo(45, "Al-Jathiyah", "The Crouching", "الجاثية", 37, "Makki"),
        SurahInfo(46, "Al-Ahqaf", "The Wind-Curved Sandhills", "الأحقاف", 35, "Makki"),
        SurahInfo(47, "Muhammad", "Muhammad", "محمد", 38, "Madani"),
        SurahInfo(48, "Al-Fath", "The Victory", "الفتح", 29, "Madani"),
        SurahInfo(49, "Al-Hujurat", "The Rooms", "الحجرات", 18, "Madani"),
        SurahInfo(50, "Qaf", "The Letter Qaf", "ق", 45, "Makki"),
        SurahInfo(51, "Adh-Dhariyat", "The Winnowing Winds", "الذاريات", 60, "Makki"),
        SurahInfo(52, "At-Tur", "The Mount", "الطور", 49, "Makki"),
        SurahInfo(53, "An-Najm", "The Star", "النجم", 62, "Makki"),
        SurahInfo(54, "Al-Qamar", "The Moon", "القمر", 55, "Makki"),
        SurahInfo(55, "Ar-Rahman", "The Beneficent", "الرحمن", 78, "Madani"),
        SurahInfo(56, "Al-Waqi'ah", "The Inevitable", "الواقعة", 96, "Makki"),
        SurahInfo(57, "Al-Hadid", "The Iron", "الحديد", 29, "Madani"),
        SurahInfo(58, "Al-Mujadila", "The Pleading Woman", "المجادلة", 22, "Madani"),
        SurahInfo(59, "Al-Hashr", "The Exile", "الحشر", 24, "Madani"),
        SurahInfo(60, "Al-Mumtahanah", "She That Is to Be Examined", "الممتحنة", 13, "Madani"),
        SurahInfo(61, "As-Saf", "The Ranks", "الصف", 14, "Madani"),
        SurahInfo(62, "Al-Jumu'ah", "The Congregation, Friday", "الجمعة", 11, "Madani"),
        SurahInfo(63, "Al-Munafiqun", "The Hypocrites", "المنافقون", 11, "Madani"),
        SurahInfo(64, "At-Taghabun", "The Mutual Disillusion", "التغابن", 18, "Madani"),
        SurahInfo(65, "At-Talaq", "The Divorce", "الطلاق", 12, "Madani"),
        SurahInfo(66, "At-Tahrim", "The Prohibition", "التحريم", 12, "Madani"),
        SurahInfo(67, "Al-Mulk", "The Sovereignty", "الملك", 30, "Makki"),
        SurahInfo(68, "Al-Qalam", "The Pen", "القلم", 52, "Makki"),
        SurahInfo(69, "Al-Haqqah", "The Reality", "الحاقة", 52, "Makki"),
        SurahInfo(70, "Al-Ma'arij", "The Ascending Stairways", "المعارج", 44, "Makki"),
        SurahInfo(71, "Nuh", "Noah", "نوح", 28, "Makki"),
        SurahInfo(72, "Al-Jinn", "The Jinn", "الجن", 28, "Makki"),
        SurahInfo(73, "Al-Muzzammil", "The Enshrouded One", "المزمل", 20, "Makki"),
        SurahInfo(74, "Al-Muddaththir", "The Cloaked One", "المدثر", 56, "Makki"),
        SurahInfo(75, "Al-Qiyamah", "The Resurrection", "القيامة", 40, "Makki"),
        SurahInfo(76, "Al-Insan", "The Man", "الإنسان", 31, "Madani"),
        SurahInfo(77, "Al-Mursalat", "The Emissaries", "المرسلات", 50, "Makki"),
        SurahInfo(78, "An-Naba", "The Tidings", "النبأ", 40, "Makki"),
        SurahInfo(79, "An-Nazi'at", "Those Who Drag Forth", "النازعات", 46, "Makki"),
        SurahInfo(80, "'Abasa", "He Frowned", "عبس", 42, "Makki"),
        SurahInfo(81, "At-Takwir", "The Overthrowing", "التكوير", 29, "Makki"),
        SurahInfo(82, "Al-Infitar", "The Cleaving", "الإنفطار", 19, "Makki"),
        SurahInfo(83, "Al-Mutaffifin", "The Defrauding", "المطففين", 36, "Makki"),
        SurahInfo(84, "Al-Inshiqaq", "The Sundering", "الإنشقاق", 25, "Makki"),
        SurahInfo(85, "Al-Buruj", "The Mansions of the Stars", "البروج", 22, "Makki"),
        SurahInfo(86, "At-Tariq", "The Nightcommer", "الطارق", 17, "Makki"),
        SurahInfo(87, "Al-A'la", "The Most High", "الأعلى", 19, "Makki"),
        SurahInfo(88, "Al-Ghashiyah", "The Overwhelming", "الغاشية", 26, "Makki"),
        SurahInfo(89, "Al-Fajr", "The Dawn", "الفجر", 30, "Makki"),
        SurahInfo(90, "Al-Balad", "The City", "البلد", 20, "Makki"),
        SurahInfo(91, "Ash-Shams", "The Sun", "الشمس", 15, "Makki"),
        SurahInfo(92, "Al-Layl", "The Night", "الليل", 21, "Makki"),
        SurahInfo(93, "Ad-Duhaa", "The Morning Hours", "الضحى", 11, "Makki"),
        SurahInfo(94, "Ash-Sharh", "The Relief", "الشرح", 8, "Makki"),
        SurahInfo(95, "At-Tin", "The Fig", "التين", 8, "Makki"),
        SurahInfo(96, "Al-'Alaq", "The Clot", "العلق", 19, "Makki"),
        SurahInfo(97, "Al-Qadr", "The Power", "القدر", 5, "Makki"),
        SurahInfo(98, "Al-Bayyinah", "The Clear Proof", "البينة", 8, "Madani"),
        SurahInfo(99, "Az-Zalzalah", "The Earthquake", "الزلزلة", 8, "Madani"),
        SurahInfo(100, "Al-'Adiyat", "The Courser", "العاديات", 11, "Makki"),
        SurahInfo(101, "Al-Qari'ah", "The Calamity", "القارعة", 11, "Makki"),
        SurahInfo(102, "At-Takathur", "The Rivalry in World Increase", "التكاثر", 8, "Makki"),
        SurahInfo(103, "Al-'Asr", "The Declining Day", "العصر", 3, "Makki"),
        SurahInfo(104, "Al-Humazah", "The Traducer", "الهمزة", 9, "Makki"),
        SurahInfo(105, "Al-Fil", "The Elephant", "الفيل", 5, "Makki"),
        SurahInfo(106, "Quraysh", "Quraysh", "قريش", 4, "Makki"),
        SurahInfo(107, "Al-Ma'un", "The Small Kindnesses", "الماعون", 7, "Makki"),
        SurahInfo(108, "Al-Kawthar", "The Abundance", "الكوثر", 3, "Makki"),
        SurahInfo(109, "Al-Kafirun", "The Disbelievers", "الكافرون", 6, "Makki"),
        SurahInfo(110, "An-Nasr", "The Divine Support", "النصر", 3, "Madani"),
        SurahInfo(111, "Al-Masad", "The Palm Fiber", "المسد", 5, "Makki"),
        SurahInfo(112, "Al-Ikhlas", "The Sincerity", "الإخلاص", 4, "Makki"),
        SurahInfo(113, "Al-Falaq", "The Daybreak", "الفلق", 5, "Makki"),
        SurahInfo(114, "An-Nas", "Mankind", "الناس", 6, "Makki")
    )

    fun byNumber(number: Int): SurahInfo =
        surahs.firstOrNull { it.number == number } ?: surahs.first()

    /**
     * Returns the 1-based global Ayah index (1..6236) used by Islamic Network Audio CDN.
     */
    fun globalAyahNumber(surahNumber: Int, ayahNumber: Int): Int {
        val safeSurah = surahNumber.coerceIn(1, 114)
        val info = byNumber(safeSurah)
        val safeAyah = ayahNumber.coerceIn(1, info.ayahCount)
        var offset = 0
        for (i in 0 until (safeSurah - 1)) {
            offset += surahs[i].ayahCount
        }
        return offset + safeAyah
    }

    /**
     * Returns 6-digit SSSAAA code (e.g., 002255) used by EveryAyah.com.
     */
    fun everyAyahCode(surahNumber: Int, ayahNumber: Int): String {
        val s = surahNumber.coerceIn(1, 114)
        val a = ayahNumber.coerceIn(1, byNumber(s).ayahCount)
        return "%03d%03d".format(java.util.Locale.US, s, a)
    }
}
