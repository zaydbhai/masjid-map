package com.example.domain

enum class AdhkarGroup(val displayName: String) {
    MORNING("Morning"),
    EVENING("Evening"),
    AFTER_PRAYER("After Prayer")
}

data class DhikrEntry(
    val id: String,
    val group: AdhkarGroup,
    val title: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val targetCount: Int,
    val reference: String
)

data class TasbeehPreset(
    val id: String,
    val arabic: String,
    val transliteration: String,
    val meaning: String,
    val defaultTarget: Int = 33
)

object AdhkarCatalog {

    val tasbeehPresets: List<TasbeehPreset> = listOf(
        TasbeehPreset("subhanallah", "سُبْحَانَ اللَّهِ", "SubhanAllah", "Glory be to Allah", 33),
        TasbeehPreset("alhamdulillah", "الْحَمْدُ لِلَّهِ", "Alhamdulillah", "All praise is for Allah", 33),
        TasbeehPreset("allahuakbar", "اللَّهُ أَكْبَرُ", "Allahu Akbar", "Allah is the Greatest", 34),
        TasbeehPreset("astaghfirullah", "أَسْتَغْفِرُ اللَّهَ", "Astaghfirullah", "I seek forgiveness from Allah", 100),
        TasbeehPreset("tahlil", "لَا إِلَٰهَ إِلَّا اللَّهُ", "La ilaha illallah", "There is no deity worthy of worship except Allah", 100),
        TasbeehPreset("salawat", "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ", "Allahumma salli 'ala Muhammad", "O Allah, send peace and blessings upon Muhammad", 10)
    )

    val adhkarEntries: List<DhikrEntry> = listOf(
        DhikrEntry(
            id = "morning_kursi",
            group = AdhkarGroup.MORNING,
            title = "Ayat al-Kursi",
            arabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ",
            transliteration = "Allahu la ilaha illa Huwal-Hayyul-Qayyum...",
            translation = "Allah — there is no deity except Him, the Ever-Living, the Sustainer of all existence. Neither drowsiness overtakes Him nor sleep. Whoever recites it in the morning is protected until evening.",
            targetCount = 1,
            reference = "Qur'an 2:255 · Al-Hakim"
        ),
        DhikrEntry(
            id = "morning_quls",
            group = AdhkarGroup.MORNING,
            title = "Al-Ikhlas, Al-Falaq & An-Nas",
            arabic = "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ قُلْ أَعُوذُ بِرَبِّ النَّاسِ",
            transliteration = "Qul Huwallahu Ahad · Qul a'udhu bi Rabbil-Falaq · Qul a'udhu bi Rabbin-Nas",
            translation = "Recite the three protective Surahs three times each morning to suffice you against everything.",
            targetCount = 3,
            reference = "Abu Dawud 5082 · At-Tirmidhi 3575"
        ),
        DhikrEntry(
            id = "morning_asbahna",
            group = AdhkarGroup.MORNING,
            title = "Entering the Morning",
            arabic = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            transliteration = "Asbahna wa asbahal-mulku lillah, walhamdulillahi, la ilaha illallahu wahdahu la sharika lah...",
            translation = "We have reached the morning and at this very time all sovereignty belongs to Allah, and all praise is for Allah. None has the right to be worshipped except Allah alone without partner; to Him belongs all sovereignty and praise, and He is over all things omnipotent.",
            targetCount = 1,
            reference = "Sahih Muslim 2723"
        ),
        DhikrEntry(
            id = "morning_sayyid",
            group = AdhkarGroup.MORNING,
            title = "Sayyidul Istighfar (Master Supplication for Forgiveness)",
            arabic = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            transliteration = "Allahumma Anta Rabbi la ilaha illa Anta, khalaqtani wa ana 'abduka...",
            translation = "O Allah, You are my Lord, none has the right to be worshipped except You. You created me and I am Your servant, and I abide to Your covenant and promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favor upon me and I acknowledge my sin, so forgive me, for verily none can forgive sins except You.",
            targetCount = 1,
            reference = "Sahih al-Bukhari 6306"
        ),
        DhikrEntry(
            id = "morning_bismillah",
            group = AdhkarGroup.MORNING,
            title = "Protection Against All Harm",
            arabic = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            transliteration = "Bismillahil-ladhi la yadurru ma'asmihi shay'un fil-ardi wa la fis-sama'i wa Huwas-Sami'ul-'Alim",
            translation = "In the name of Allah, with whose name nothing on the earth or in the heavens can cause harm, and He is the All-Hearing, the All-Knowing.",
            targetCount = 3,
            reference = "Abu Dawud 5088 · At-Tirmidhi 3388"
        ),
        DhikrEntry(
            id = "morning_tasbeeh",
            group = AdhkarGroup.MORNING,
            title = "Sins Forgiven Like Sea Foam",
            arabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            transliteration = "SubhanAllahi wa bihamdihi",
            translation = "Glory be to Allah and praise Him.",
            targetCount = 100,
            reference = "Sahih Muslim 2692"
        ),
        DhikrEntry(
            id = "evening_amsayna",
            group = AdhkarGroup.EVENING,
            title = "Entering the Evening",
            arabic = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            transliteration = "Amsayna wa amsal-mulku lillah, walhamdulillahi, la ilaha illallahu wahdahu la sharika lah...",
            translation = "We have reached the evening and at this very time all sovereignty belongs to Allah, and all praise is for Allah. None has the right to be worshipped except Allah alone without partner; to Him belongs all sovereignty and praise, and He is over all things omnipotent.",
            targetCount = 1,
            reference = "Sahih Muslim 2723"
        ),
        DhikrEntry(
            id = "evening_quls",
            group = AdhkarGroup.EVENING,
            title = "Al-Ikhlas, Al-Falaq & An-Nas",
            arabic = "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ قُلْ أَعُوذُ بِرَبِّ النَّاسِ",
            transliteration = "Qul Huwallahu Ahad · Qul a'udhu bi Rabbil-Falaq · Qul a'udhu bi Rabbin-Nas",
            translation = "Recite the three protective Surahs three times each evening to protect you until morning.",
            targetCount = 3,
            reference = "Abu Dawud 5082 · At-Tirmidhi 3575"
        ),
        DhikrEntry(
            id = "evening_kalimat",
            group = AdhkarGroup.EVENING,
            title = "Seeking Refuge in Allah's Perfect Words",
            arabic = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            transliteration = "A'udhu bikalimatillahit-tammati min sharri ma khalaq",
            translation = "I seek refuge in the perfect words of Allah from the evil of what He has created.",
            targetCount = 3,
            reference = "Sahih Muslim 2709"
        ),
        DhikrEntry(
            id = "evening_hasbi",
            group = AdhkarGroup.EVENING,
            title = "Allah Is Sufficient for Me",
            arabic = "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
            transliteration = "HasbiyAllahu la ilaha illa Huwa 'alayhi tawakkaltu wa Huwa Rabbul-'Arshil-'Azim",
            translation = "Sufficient for me is Allah; there is no deity except Him. On Him I have relied, and He is the Lord of the Great Throne.",
            targetCount = 7,
            reference = "Abu Dawud 5081 · Ibn as-Sunni"
        ),
        DhikrEntry(
            id = "salah_istighfar",
            group = AdhkarGroup.AFTER_PRAYER,
            title = "Istighfar & Peace After Salam",
            arabic = "أَسْتَغْفِرُ اللَّهَ (ثَلَاثًا) اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ",
            transliteration = "Astaghfirullah (3x). Allahumma Antas-Salam wa minkas-salam, tabarakta ya Dhal-Jalali wal-Ikram",
            translation = "I seek Allah's forgiveness (three times). O Allah, You are Peace and from You comes peace. Blessed are You, O Possessor of Majesty and Honor.",
            targetCount = 1,
            reference = "Sahih Muslim 591"
        ),
        DhikrEntry(
            id = "salah_33",
            group = AdhkarGroup.AFTER_PRAYER,
            title = "Tasbeeh Fatimah (33 / 33 / 33)",
            arabic = "سُبْحَانَ اللَّهِ (٣٣) · الْحَمْدُ لِلَّهِ (٣٣) · اللَّهُ أَكْبَرُ (٣٣)",
            transliteration = "SubhanAllah (33) · Alhamdulillah (33) · Allahu Akbar (33)",
            translation = "Glory be to Allah (33x), Praise be to Allah (33x), Allah is the Greatest (33x), completed with: La ilaha illallahu wahdahu la sharika lah...",
            targetCount = 33,
            reference = "Sahih Muslim 597"
        ),
        DhikrEntry(
            id = "salah_kursi",
            group = AdhkarGroup.AFTER_PRAYER,
            title = "Ayat al-Kursi After Every Obligatory Prayer",
            arabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...",
            transliteration = "Allahu la ilaha illa Huwal-Hayyul-Qayyum...",
            translation = "Whoever recites Ayat al-Kursi immediately after every prescribed prayer, nothing stands between them and entering Paradise except death.",
            targetCount = 1,
            reference = "An-Nasa'i al-Kubra 9928"
        ),
        DhikrEntry(
            id = "salah_help",
            group = AdhkarGroup.AFTER_PRAYER,
            title = "Asking Help in Remembrance & Gratitude",
            arabic = "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ، وَشُكْرِكَ، وَحُسْنِ عِبَادَتِكَ",
            transliteration = "Allahumma a'inni 'ala dhikrika, wa shukrika, wa husni 'ibadatika",
            translation = "O Allah, help me to remember You, to give thanks to You, and to worship You in the best of manners.",
            targetCount = 1,
            reference = "Abu Dawud 1522 · An-Nasa'i 1303"
        )
    )
}
