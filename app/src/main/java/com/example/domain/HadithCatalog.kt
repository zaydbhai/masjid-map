package com.example.domain

/**
 * Top 21 authentic Hadith collections backed by verified open-source GitHub repositories
 * and HadeethEnc.
 */
object HadithCatalog {

    val top21Books: List<HadithBookInfo> = listOf(
        HadithBookInfo(
            id = "bukhari",
            rank = 1,
            titleEnglish = "Sahih al-Bukhari",
            titleArabic = "صحيح البخاري",
            compiler = "Imam Muhammad al-Bukhari (d. 256 AH)",
            category = HadithBookCategory.KUTUB_AL_SITTAH,
            description = "The most authentic collection of Prophetic narrations across 97 books.",
            sourceType = HadithSourceType.FAWAZ_SECTIONS,
            fawazSlug = "bukhari",
            ahmedBasetPath = "the_9_books/bukhari"
        ),
        HadithBookInfo(
            id = "muslim",
            rank = 2,
            titleEnglish = "Sahih Muslim",
            titleArabic = "صحيح مسلم",
            compiler = "Imam Muslim ibn al-Hajjaj (d. 261 AH)",
            category = HadithBookCategory.KUTUB_AL_SITTAH,
            description = "Second of the Sahihain, renowned for strict isnad precision.",
            sourceType = HadithSourceType.FAWAZ_SECTIONS,
            fawazSlug = "muslim",
            ahmedBasetPath = "the_9_books/muslim"
        ),
        HadithBookInfo(
            id = "nasai",
            rank = 3,
            titleEnglish = "Sunan an-Nasa'i",
            titleArabic = "سنن النسائي",
            compiler = "Imam Ahmad an-Nasa'i (d. 303 AH)",
            category = HadithBookCategory.KUTUB_AL_SITTAH,
            description = "Al-Sunan al-Sughra with rigorous narrator grading.",
            sourceType = HadithSourceType.FAWAZ_SECTIONS,
            fawazSlug = "nasai",
            ahmedBasetPath = "the_9_books/nasai"
        ),
        HadithBookInfo(
            id = "abudawud",
            rank = 4,
            titleEnglish = "Sunan Abi Dawud",
            titleArabic = "سنن أبي داود",
            compiler = "Imam Abu Dawud al-Sijistani (d. 275 AH)",
            category = HadithBookCategory.KUTUB_AL_SITTAH,
            description = "Primary collection of juristic (ahkam) hadiths.",
            sourceType = HadithSourceType.FAWAZ_SECTIONS,
            fawazSlug = "abudawud",
            ahmedBasetPath = "the_9_books/abudawud"
        ),
        HadithBookInfo(
            id = "tirmidhi",
            rank = 5,
            titleEnglish = "Jami' at-Tirmidhi",
            titleArabic = "جامع الترمذي",
            compiler = "Imam Abu 'Isa at-Tirmidhi (d. 279 AH)",
            category = HadithBookCategory.KUTUB_AL_SITTAH,
            description = "Includes chain grades and scholarly legal commentary.",
            sourceType = HadithSourceType.FAWAZ_SECTIONS,
            fawazSlug = "tirmidhi",
            ahmedBasetPath = "the_9_books/tirmidhi"
        ),
        HadithBookInfo(
            id = "ibnmajah",
            rank = 6,
            titleEnglish = "Sunan Ibn Majah",
            titleArabic = "سنن ابن ماجه",
            compiler = "Imam Ibn Majah al-Qazwini (d. 273 AH)",
            category = HadithBookCategory.KUTUB_AL_SITTAH,
            description = "Sixth canonical book of Kutub al-Sittah.",
            sourceType = HadithSourceType.FAWAZ_SECTIONS,
            fawazSlug = "ibnmajah",
            ahmedBasetPath = "the_9_books/ibnmajah"
        ),
        HadithBookInfo(
            id = "malik",
            rank = 7,
            titleEnglish = "Muwatta Imam Malik",
            titleArabic = "موطأ مالك",
            compiler = "Imam Malik ibn Anas (d. 179 AH)",
            category = HadithBookCategory.PRIMARY_SUNAN,
            description = "Earliest written collection of hadith and Madinan practice.",
            sourceType = HadithSourceType.FAWAZ_SECTIONS,
            fawazSlug = "malik",
            ahmedBasetPath = "the_9_books/malik"
        ),
        HadithBookInfo(
            id = "ahmad",
            rank = 8,
            titleEnglish = "Musnad Ahmad",
            titleArabic = "مسند أحمد",
            compiler = "Imam Ahmad ibn Hanbal (d. 241 AH)",
            category = HadithBookCategory.PRIMARY_SUNAN,
            description = "Monumental encyclopedic collection organized by Companion.",
            sourceType = HadithSourceType.AHMED_BASET_BOOK,
            ahmedBasetPath = "the_9_books/ahmad"
        ),
        HadithBookInfo(
            id = "darimi",
            rank = 9,
            titleEnglish = "Sunan ad-Darimi",
            titleArabic = "سنن الدارمي",
            compiler = "Imam 'Abdullah ad-Darimi (d. 255 AH)",
            category = HadithBookCategory.PRIMARY_SUNAN,
            description = "Classical Sunan work completing the nine primary books.",
            sourceType = HadithSourceType.AHMED_BASET_BOOK,
            ahmedBasetPath = "the_9_books/darimi"
        ),
        HadithBookInfo(
            id = "riyadussalihin",
            rank = 10,
            titleEnglish = "Riyad as-Salihin",
            titleArabic = "رياض الصالحين",
            compiler = "Imam Yahya an-Nawawi (d. 676 AH)",
            category = HadithBookCategory.VIRTUES_AND_ADHKAR,
            description = "The Gardens of the Righteous — ethics, devotion, and character.",
            sourceType = HadithSourceType.AHMED_BASET_BOOK,
            ahmedBasetPath = "other_books/riyadussalihin"
        ),
        HadithBookInfo(
            id = "mishkat",
            rank = 11,
            titleEnglish = "Mishkat al-Masabih",
            titleArabic = "مشكاة المصابيح",
            compiler = "Al-Khatib at-Tabrizi (d. 741 AH)",
            category = HadithBookCategory.PRIMARY_SUNAN,
            description = "Comprehensive classical manual of faith, worship, and conduct.",
            sourceType = HadithSourceType.AHMED_BASET_BOOK,
            ahmedBasetPath = "other_books/mishkat"
        ),
        HadithBookInfo(
            id = "bulughulmaram",
            rank = 12,
            titleEnglish = "Bulugh al-Maram",
            titleArabic = "بلوغ المرام",
            compiler = "Al-Hafiz Ibn Hajar al-Asqalani (d. 852 AH)",
            category = HadithBookCategory.PRIMARY_SUNAN,
            description = "Essential hadith evidences for Islamic jurisprudence.",
            sourceType = HadithSourceType.AHMED_BASET_BOOK,
            ahmedBasetPath = "other_books/bulughulmaram"
        ),
        HadithBookInfo(
            id = "adabulmufrad",
            rank = 13,
            titleEnglish = "Al-Adab al-Mufrad",
            titleArabic = "الأدب المفرد",
            compiler = "Imam Muhammad al-Bukhari (d. 256 AH)",
            category = HadithBookCategory.VIRTUES_AND_ADHKAR,
            description = "Dedicated collection on Islamic manners, family, and kindness.",
            sourceType = HadithSourceType.AHMED_BASET_BOOK,
            ahmedBasetPath = "other_books/adabulmufrad"
        ),
        HadithBookInfo(
            id = "shamail",
            rank = 14,
            titleEnglish = "Ash-Shama'il al-Muhammadiyyah",
            titleArabic = "الشمائل المحمدية",
            compiler = "Imam Abu 'Isa at-Tirmidhi (d. 279 AH)",
            category = HadithBookCategory.VIRTUES_AND_ADHKAR,
            description = "The noble character, appearance, and daily habits of the Prophet ﷺ.",
            sourceType = HadithSourceType.AHMED_BASET_BOOK,
            ahmedBasetPath = "other_books/shamail"
        ),
        HadithBookInfo(
            id = "nawawi40",
            rank = 15,
            titleEnglish = "40 Hadith an-Nawawi",
            titleArabic = "الأربعون النووية",
            compiler = "Imam Yahya an-Nawawi (d. 676 AH)",
            category = HadithBookCategory.FORTIES,
            description = "Foundational 42 hadiths summarizing the pillars of Islam.",
            sourceType = HadithSourceType.FAWAZ_SECTIONS,
            fawazSlug = "nawawi",
            ahmedBasetPath = "forties/nawawi40"
        ),
        HadithBookInfo(
            id = "qudsi40",
            rank = 16,
            titleEnglish = "40 Hadith Qudsi",
            titleArabic = "الأحاديث القدسية",
            compiler = "Sacred Prophetic Narrations",
            category = HadithBookCategory.FORTIES,
            description = "Narrations whose meaning is directly from Allah.",
            sourceType = HadithSourceType.FAWAZ_SECTIONS,
            fawazSlug = "qudsi",
            ahmedBasetPath = "forties/qudsi40"
        ),
        HadithBookInfo(
            id = "shahwaliullah40",
            rank = 17,
            titleEnglish = "40 Hadith of Shah Waliullah",
            titleArabic = "أربعون الشاه ولي الله",
            compiler = "Shah Waliullah Dehlawi (d. 1176 AH)",
            category = HadithBookCategory.FORTIES,
            description = "Concise authentic narrations transmitted with short chains.",
            sourceType = HadithSourceType.AHMED_BASET_BOOK,
            ahmedBasetPath = "forties/shahwaliullah40"
        ),
        HadithBookInfo(
            id = "hisn",
            rank = 18,
            titleEnglish = "Hisn al-Muslim",
            titleArabic = "حصن المسلم",
            compiler = "Sa'id ibn Ali al-Qahtani",
            category = HadithBookCategory.VIRTUES_AND_ADHKAR,
            description = "Fortress of the Muslim — authentic daily duas & remembrance.",
            sourceType = HadithSourceType.HISN_MUSLIM_JSON
        ),
        HadithBookInfo(
            id = "hadeethenc_creed",
            rank = 19,
            titleEnglish = "Al-Aqidah (The Creed)",
            titleArabic = "العقيدة الإسلامية",
            compiler = "HadeethEnc Verified Corpus",
            category = HadithBookCategory.ENCYCLOPEDIC,
            description = "Authenticated narrations on Islamic creed and tawhid.",
            sourceType = HadithSourceType.HADEETH_ENC,
            hadeethEncCategoryId = "3"
        ),
        HadithBookInfo(
            id = "hadeethenc_fiqh",
            rank = 20,
            titleEnglish = "Al-Fiqh & Worship",
            titleArabic = "الفقه وأصوله",
            compiler = "HadeethEnc Verified Corpus",
            category = HadithBookCategory.ENCYCLOPEDIC,
            description = "Authenticated narrations on prayer, charity, fasting, and rulings.",
            sourceType = HadithSourceType.HADEETH_ENC,
            hadeethEncCategoryId = "4"
        ),
        HadithBookInfo(
            id = "hadeethenc_quran",
            rank = 21,
            titleEnglish = "Virtues of the Qur'an",
            titleArabic = "القرآن وعلومه",
            compiler = "HadeethEnc Verified Corpus",
            category = HadithBookCategory.ENCYCLOPEDIC,
            description = "Authenticated narrations on reciting and reflecting on the Qur'an.",
            sourceType = HadithSourceType.HADEETH_ENC,
            hadeethEncCategoryId = "1"
        )
    )

    // Authentic fallback seeds used while live GitHub JSON loads or when offline
    val authenticDailySeeds: List<HadithEntry> = listOf(
        HadithEntry(
            id = "seed_1",
            numberLabel = "Hadith 1",
            bookTitle = "Sahih al-Bukhari",
            chapterTitle = "Revelation",
            arabicText = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى",
            narrator = "Narrated 'Umar bin Al-Khattab:",
            englishText = "The reward of deeds depends upon the intentions and every person will get the reward according to what he has intended.",
            grade = "Sahih",
            reference = "Sahih al-Bukhari 1"
        ),
        HadithEntry(
            id = "seed_2",
            numberLabel = "Hadith 256",
            bookTitle = "Riyad as-Salihin",
            chapterTitle = "Good Manners",
            arabicText = "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ",
            narrator = "Narrated 'Uthman bin 'Affan:",
            englishText = "The best among you (Muslims) are those who learn the Qur'an and teach it.",
            grade = "Sahih",
            reference = "Sahih al-Bukhari 5027"
        ),
        HadithEntry(
            id = "seed_3",
            numberLabel = "Hadith 647",
            bookTitle = "Sahih al-Bukhari",
            chapterTitle = "Call to Prayers",
            arabicText = "صَلاَةُ الْجَمَاعَةِ تَفْضُلُ صَلاَةَ الْفَذِّ بِسَبْعٍ وَعِشْرِينَ دَرَجَةً",
            narrator = "Narrated 'Abdullah bin 'Umar:",
            englishText = "The prayer in congregation is twenty-seven times superior to the prayer offered by a person alone.",
            grade = "Sahih",
            reference = "Sahih al-Bukhari 645"
        ),
        HadithEntry(
            id = "seed_4",
            numberLabel = "Hadith 13",
            bookTitle = "40 Hadith an-Nawawi",
            chapterTitle = "Brotherhood",
            arabicText = "لاَ يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ",
            narrator = "Narrated Anas bin Malik:",
            englishText = "None of you truly believes until he loves for his brother what he loves for himself.",
            grade = "Sahih",
            reference = "Bukhari & Muslim"
        ),
        HadithEntry(
            id = "seed_5",
            numberLabel = "Hadith 2699",
            bookTitle = "Sahih Muslim",
            chapterTitle = "Remembrance & Dua",
            arabicText = "مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ بِهِ طَرِيقًا إِلَى الْجَنَّةِ",
            narrator = "Narrated Abu Hurairah:",
            englishText = "Whoever treads a path in search of knowledge, Allah will make easy for him the path to Paradise.",
            grade = "Sahih",
            reference = "Sahih Muslim 2699"
        ),
        HadithEntry(
            id = "seed_6",
            numberLabel = "Hadith 668",
            bookTitle = "Sahih Muslim",
            chapterTitle = "Prayer",
            arabicText = "مَنْ غَدَا إِلَى الْمَسْجِدِ أَوْ رَاحَ أَعَدَّ اللَّهُ لَهُ فِي الْجَنَّةِ نُزُلًا كُلَّمَا غَدَا أَوْ رَاحَ",
            narrator = "Narrated Abu Hurairah:",
            englishText = "He who goes to the masjid in the morning or in the evening, Allah prepares for him a hospitable place in Paradise every time he goes in the morning or evening.",
            grade = "Sahih",
            reference = "Sahih al-Bukhari 662 · Sahih Muslim 669"
        ),
        HadithEntry(
            id = "seed_7",
            numberLabel = "Hadith 223",
            bookTitle = "Sahih Muslim",
            chapterTitle = "Purification",
            arabicText = "الطُّهُورُ شَطْرُ الإِيمَانِ وَالْحَمْدُ لِلَّهِ تَمْلأُ الْمِيزَانَ",
            narrator = "Narrated Abu Malik al-Ash'ari:",
            englishText = "Purity is half of faith, and saying Alhamdulillah (praise be to Allah) fills the scale.",
            grade = "Sahih",
            reference = "Sahih Muslim 223"
        ),
        HadithEntry(
            id = "seed_8",
            numberLabel = "Hadith 6018",
            bookTitle = "Sahih al-Bukhari",
            chapterTitle = "Good Manners",
            arabicText = "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ",
            narrator = "Narrated Abu Hurairah:",
            englishText = "Whoever believes in Allah and the Last Day should speak what is good or keep silent.",
            grade = "Sahih",
            reference = "Sahih al-Bukhari 6018"
        ),
        HadithEntry(
            id = "seed_9",
            numberLabel = "Hadith 18",
            bookTitle = "40 Hadith an-Nawawi",
            chapterTitle = "Taqwa & Character",
            arabicText = "اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا، وَخَالِقِ النَّاسَ بِخُلُقٍ حَسَنٍ",
            narrator = "Narrated Abu Dharr & Mu'adh ibn Jabal:",
            englishText = "Have taqwa (fear and mindfulness) of Allah wherever you may be, follow up a bad deed with a good deed which will wipe it out, and behave well towards the people.",
            grade = "Hasan Sahih",
            reference = "Jami' at-Tirmidhi 1987"
        ),
        HadithEntry(
            id = "seed_10",
            numberLabel = "Hadith 2516",
            bookTitle = "Jami' at-Tirmidhi",
            chapterTitle = "Trust in Allah",
            arabicText = "احْفَظِ اللَّهَ يَحْفَظْكَ، احْفَظِ اللَّهَ تَجِدْهُ تُجَاهَكَ",
            narrator = "Narrated 'Abdullah ibn 'Abbas:",
            englishText = "Be mindful of Allah and He will protect you. Be mindful of Allah and you will find Him before you. When you ask, ask Allah; and when you seek help, seek help from Allah.",
            grade = "Sahih",
            reference = "Jami' at-Tirmidhi 2516"
        ),
        HadithEntry(
            id = "seed_11",
            numberLabel = "Hadith 6405",
            bookTitle = "Sahih al-Bukhari",
            chapterTitle = "Invocations",
            arabicText = "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ ثَقِيلَتَانِ فِي الْمِيزَانِ حَبِيبَتَانِ إِلَى الرَّحْمَنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            narrator = "Narrated Abu Hurairah:",
            englishText = "Two words are light on the tongue, heavy on the Scale, and beloved to the Most Merciful: 'SubhanAllahi wa bihamdihi, SubhanAllahil-'Azim.'",
            grade = "Sahih",
            reference = "Sahih al-Bukhari 6406"
        ),
        HadithEntry(
            id = "seed_12",
            numberLabel = "Hadith 2588",
            bookTitle = "Sahih Muslim",
            chapterTitle = "Charity",
            arabicText = "مَا نَقَصَتْ صَدَقَةٌ مِنْ مَالٍ، وَمَا زَادَ اللَّهُ عَبْدًا بِعَفْوٍ إِلَّا عِزًّا",
            narrator = "Narrated Abu Hurairah:",
            englishText = "Charity does not decrease wealth, no one forgives except that Allah increases him in honor, and no one humbles himself for the sake of Allah except that Allah raises him in status.",
            grade = "Sahih",
            reference = "Sahih Muslim 2588"
        )
    )
}
