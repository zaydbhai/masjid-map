package com.example.domain

data class JuzInfo(
    val number: Int,
    val nameEnglish: String,
    val nameArabic: String,
    val startSurah: Int,
    val startAyah: Int,
    val endSurah: Int,
    val endAyah: Int
)

object JuzCatalog {
    val allJuz: List<JuzInfo> = listOf(
        JuzInfo(1, "Alif Lam Mim", "الم", 1, 1, 2, 141),
        JuzInfo(2, "Sayaqul", "سيقول", 2, 142, 2, 252),
        JuzInfo(3, "Tilka ar-Rusul", "تلك الرسل", 2, 253, 3, 92),
        JuzInfo(4, "Lan Tanalu", "لن تنالوا", 3, 93, 4, 23),
        JuzInfo(5, "Wal-Muhsanat", "والمحصنات", 4, 24, 4, 147),
        JuzInfo(6, "La Yuhibbullah", "لا يحب الله", 4, 148, 5, 81),
        JuzInfo(7, "Wa Idha Sami'u", "وإذا سمعوا", 5, 82, 6, 110),
        JuzInfo(8, "Wa Law Annana", "ولو أننا", 6, 111, 7, 87),
        JuzInfo(9, "Qal al-Mala'u", "قال الملأ", 7, 88, 8, 40),
        JuzInfo(10, "Wa'lamu", "واعلموا", 8, 41, 9, 92),
        JuzInfo(11, "Ya'tadhiruna", "يعتذرون", 9, 93, 11, 5),
        JuzInfo(12, "Wa Ma Min Dabbah", "وما من دابة", 11, 6, 12, 52),
        JuzInfo(13, "Wa Ma Ubarri'u", "وما أبرئ", 12, 53, 14, 52),
        JuzInfo(14, "Rubama", "ربما", 15, 1, 16, 128),
        JuzInfo(15, "Subhanalladhi", "سبحان الذي", 17, 1, 18, 74),
        JuzInfo(16, "Qala Alam", "قال ألم", 18, 75, 20, 135),
        JuzInfo(17, "Iqtaraba", "اقترب", 21, 1, 22, 78),
        JuzInfo(18, "Qad Aflaha", "قد أفلح", 23, 1, 25, 20),
        JuzInfo(19, "Wa Qalalladhina", "وقال الذين", 25, 21, 27, 55),
        JuzInfo(20, "A'man Khalaqa", "أمن خلق", 27, 56, 29, 45),
        JuzInfo(21, "Utlu Ma Uhiya", "اتل ما أوحي", 29, 46, 33, 30),
        JuzInfo(22, "Wa Man Yaqnut", "ومن يقنت", 33, 31, 36, 27),
        JuzInfo(23, "Wa Ma Liya", "وما لي", 36, 28, 39, 31),
        JuzInfo(24, "Fa Man Azlamu", "فمن أظلم", 39, 32, 41, 46),
        JuzInfo(25, "Ilayhi Yuraddu", "إليه يرد", 41, 47, 45, 37),
        JuzInfo(26, "Ha Mim", "حم", 46, 1, 51, 30),
        JuzInfo(27, "Qala Fa Ma Khatbukum", "قال فما خطبكم", 51, 31, 57, 29),
        JuzInfo(28, "Qad Sami'a", "قد سمع", 58, 1, 66, 12),
        JuzInfo(29, "Tabarakalladhi", "تبارك الذي", 67, 1, 77, 50),
        JuzInfo(30, "Amma Yatasa'alun", "عم يتساءلون", 78, 1, 114, 6)
    )

    fun juzForAyah(chapter: Int, verse: Int): JuzInfo {
        val code = chapter * 1000 + verse
        return allJuz.firstOrNull { juz ->
            val startCode = juz.startSurah * 1000 + juz.startAyah
            val endCode = juz.endSurah * 1000 + juz.endAyah
            code in startCode..endCode
        } ?: allJuz.first()
    }
}
