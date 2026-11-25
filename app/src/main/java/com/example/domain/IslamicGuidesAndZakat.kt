package com.example.domain

import kotlin.math.max
import kotlin.math.roundToLong

enum class NisabStandard(
    val id: String,
    val label: String,
    val gramsRequired: Double,
    val note: String
) {
    SILVER(
        id = "silver",
        label = "Silver Nisab (612.36 g)",
        gramsRequired = 612.36,
        note = "Lower threshold · benefits more recipients (recommended)"
    ),
    GOLD(
        id = "gold",
        label = "Gold Nisab (87.48 g)",
        gramsRequired = 87.48,
        note = "Classic gold standard (~7.5 tola / 87.48 g)"
    )
}

enum class CurrencyPreset(
    val code: String,
    val symbol: String,
    val defaultSilverPerGram: Double,
    val defaultGoldPerGram: Double,
    val defaultFitrPerPerson: Double
) {
    USD("USD", "$", 1.05, 85.0, 15.0),
    PKR("PKR", "Rs ", 295.0, 24000.0, 450.0),
    GBP("GBP", "£", 0.85, 68.0, 10.0),
    EUR("EUR", "€", 0.98, 79.0, 12.0),
    SAR("SAR", "SAR ", 3.95, 320.0, 25.0),
    AED("AED", "AED ", 3.85, 312.0, 25.0),
    INR("INR", "₹", 92.0, 7400.0, 150.0),
    CAD("CAD", "CA$", 1.48, 118.0, 15.0);

    companion object {
        fun fromCode(code: String?): CurrencyPreset =
            entries.firstOrNull { it.code == code } ?: USD
    }
}

data class ZakatSummary(
    val totalAssets: Double,
    val totalDebts: Double,
    val netZakatableWealth: Double,
    val nisabThreshold: Double,
    val meetsNisab: Boolean,
    val zakatDue: Double
)

object ZakatCalculator {
    fun compute(
        cashAndBank: Double,
        goldAndSilverValue: Double,
        investmentsAndStocks: Double,
        businessInventory: Double,
        receivables: Double,
        shortTermDebts: Double,
        nisabStandard: NisabStandard,
        metalPricePerGram: Double
    ): ZakatSummary {
        val totalAssets = max(0.0, cashAndBank) +
            max(0.0, goldAndSilverValue) +
            max(0.0, investmentsAndStocks) +
            max(0.0, businessInventory) +
            max(0.0, receivables)
        val debts = max(0.0, shortTermDebts)
        val net = max(0.0, totalAssets - debts)
        val threshold = max(0.0, nisabStandard.gramsRequired * max(0.0, metalPricePerGram))
        val meets = threshold > 0.0 && net >= threshold
        val due = if (meets) ((net * 0.025) * 100.0).roundToLong() / 100.0 else 0.0
        return ZakatSummary(
            totalAssets = totalAssets,
            totalDebts = debts,
            netZakatableWealth = net,
            nisabThreshold = threshold,
            meetsNisab = meets,
            zakatDue = due
        )
    }

    fun computeFitrAndFidyah(
        householdMembers: Int,
        fitrRatePerPerson: Double,
        missedFastingDays: Int,
        fidyahRatePerDay: Double
    ): Pair<Double, Double> {
        val fitr = max(0, householdMembers) * max(0.0, fitrRatePerPerson)
        val fidyah = max(0, missedFastingDays) * max(0.0, fidyahRatePerDay)
        return fitr to fidyah
    }
}

enum class DuaSituationCategory(val label: String) {
    ALL("All"),
    MASJID_PRAYER("Masjid & Salah"),
    DAILY_HOME("Daily & Home"),
    TRAVEL_WEATHER("Travel & Weather"),
    HARDSHIP_GUIDANCE("Relief & Istikhara")
}

data class SituationalDua(
    val id: String,
    val title: String,
    val category: DuaSituationCategory,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val reference: String
)

data class GuideStep(
    val stepNumber: Int,
    val title: String,
    val instruction: String,
    val arabic: String = "",
    val transliteration: String = ""
)

data class PracticalGuide(
    val id: String,
    val title: String,
    val subtitle: String,
    val steps: List<GuideStep>
)

object IslamicGuidesCatalog {

    val situationalDuas: List<SituationalDua> = listOf(
        SituationalDua(
            id = "enter_masjid",
            title = "Entering the Masjid",
            category = DuaSituationCategory.MASJID_PRAYER,
            arabic = "بِسْمِ اللَّهِ وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللَّهِ، اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            transliteration = "Bismillahi was-salatu was-salamu 'ala Rasulillah, Allahumma-ftah li abwaba rahmatik.",
            translation = "In the name of Allah, and peace and blessings be upon the Messenger of Allah. O Allah, open for me the gates of Your mercy.",
            reference = "Sahih Muslim 713 / Ibn Majah"
        ),
        SituationalDua(
            id = "leave_masjid",
            title = "Leaving the Masjid",
            category = DuaSituationCategory.MASJID_PRAYER,
            arabic = "بِسْمِ اللَّهِ وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللَّهِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            transliteration = "Bismillahi was-salatu was-salamu 'ala Rasulillah, Allahumma inni as'aluka min fadlik.",
            translation = "In the name of Allah, and peace and blessings be upon the Messenger of Allah. O Allah, I ask You from Your bounty.",
            reference = "Sahih Muslim 713"
        ),
        SituationalDua(
            id = "after_adhan",
            title = "Upon Hearing the Adhan",
            category = DuaSituationCategory.MASJID_PRAYER,
            arabic = "اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ وَالصَّلَاةِ الْقَائِمَةِ آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ",
            transliteration = "Allahumma Rabba hadhihid-da'watit-tammah was-salatil-qa'imah, ati Muhammadanil-wasilata wal-fadilah, wab'ath-hu maqaman mahmudanilladhi wa'adtah.",
            translation = "O Allah, Lord of this perfect call and established prayer, grant Muhammad the station of nearness and excellence, and raise him to the praised station You promised him.",
            reference = "Sahih al-Bukhari 614"
        ),
        SituationalDua(
            id = "leave_home",
            title = "Leaving Home",
            category = DuaSituationCategory.DAILY_HOME,
            arabic = "بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            transliteration = "Bismillah, tawakkaltu 'alallah, wa la hawla wa la quwwata illa billah.",
            translation = "In the name of Allah, I place my trust in Allah, and there is no power nor strength except through Allah.",
            reference = "Abu Dawud 5095 · Tirmidhi 3426"
        ),
        SituationalDua(
            id = "iftar",
            title = "Breaking the Fast (Iftar)",
            category = DuaSituationCategory.DAILY_HOME,
            arabic = "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ، وَثَبَتَ الْأَجْرُ إِنْ شَاءَ اللَّهُ",
            transliteration = "Dhahaba az-zama'u wabtallatil-'uruqu wa thabatal-ajru in sha'Allah.",
            translation = "The thirst has gone, the veins are moistened, and the reward is confirmed, if Allah wills.",
            reference = "Abu Dawud 2357"
        ),
        SituationalDua(
            id = "travel",
            title = "Starting a Journey",
            category = DuaSituationCategory.TRAVEL_WEATHER,
            arabic = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَى رَبِّنَا لَمُنْقَلِبُونَ",
            transliteration = "Subhanalladhi sakhkhara lana hadha wa ma kunna lahu muqrinin, wa inna ila Rabbina lamunqalibun.",
            translation = "Glory to Him who has subjected this to us, and we could never have controlled it ourselves. Truly, to our Lord we shall return.",
            reference = "Qur'an 43:13-14 · Sahih Muslim 1342"
        ),
        SituationalDua(
            id = "rain",
            title = "When It Rains",
            category = DuaSituationCategory.TRAVEL_WEATHER,
            arabic = "اللَّهُمَّ صَيِّبًا نَافِعًا",
            transliteration = "Allahumma sayyiban nafi'an.",
            translation = "O Allah, make it a beneficial downpour.",
            reference = "Sahih al-Bukhari 1032"
        ),
        SituationalDua(
            id = "anxiety_relief",
            title = "Relief from Anxiety & Grief",
            category = DuaSituationCategory.HARDSHIP_GUIDANCE,
            arabic = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْجُبْنِ وَالْبُخْلِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ",
            transliteration = "Allahumma inni a'udhu bika minal-hammi wal-hazan, wal-'ajzi wal-kasal, wal-jubni wal-bukhl, wa dala'id-dayni wa ghalabatir-rijal.",
            translation = "O Allah, I seek refuge in You from worry and grief, helplessness and laziness, cowardice and stinginess, and from overwhelming debt and oppression.",
            reference = "Sahih al-Bukhari 6369"
        ),
        SituationalDua(
            id = "istikhara",
            title = "Seeking Guidance (Istikhara)",
            category = DuaSituationCategory.HARDSHIP_GUIDANCE,
            arabic = "اللَّهُمَّ إِنِّي أَسْتَخِيرُكَ بِعِلْمِكَ وَأَسْتَقْدِرُكَ بِقُدْرَتِكَ وَأَسْأَلُكَ مِنْ فَضْلِكَ الْعَظِيمِ",
            transliteration = "Allahumma inni astakhiruka bi-'ilmika wa astaqdiruka bi-qudratika wa as'aluka min fadlikal-'azim...",
            translation = "O Allah, I seek Your counsel by Your knowledge, and I ask You for strength by Your power, and I ask You from Your immense bounty, for You have power while I have none, and You know while I do not.",
            reference = "Sahih al-Bukhari 1162 (pray 2 non-obligatory rak'ahs first)"
        )
    )

    val practicalGuides: List<PracticalGuide> = listOf(
        PracticalGuide(
            id = "wudu",
            title = "Step-by-Step Wudu (Ablution)",
            subtitle = "Complete Prophetic purification sequence before Salah",
            steps = listOf(
                GuideStep(1, "Intention & Bismillah", "Make sincere intention in your heart to purify for prayer, then say Bismillah.", "بِسْمِ اللَّهِ", "Bismillah"),
                GuideStep(2, "Wash Hands (3x)", "Wash both hands up to the wrists thoroughly, passing water between fingers."),
                GuideStep(3, "Rinse Mouth & Nose (3x)", "Take water in your right hand to rinse the mouth, then sniff water gently into the nostrils and blow it out using your left hand."),
                GuideStep(4, "Wash Entire Face (3x)", "Wash from the hairline to the chin and from ear to ear."),
                GuideStep(5, "Wash Forearms to Elbows (3x)", "Wash the right forearm including the elbow three times, then the left forearm including the elbow."),
                GuideStep(6, "Wipe Head & Ears (1x)", "Wipe wet hands from the forehead to the nape and back once, then clean inside and behind the ears."),
                GuideStep(7, "Wash Feet to Ankles (3x)", "Wash the right foot thoroughly up to and including the ankle three times, passing water between toes, then the left foot."),
                GuideStep(
                    8,
                    "Closing Dua",
                    "Look up or raise your gaze slightly and recite the Shahadah after finishing.",
                    "أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ",
                    "Ashhadu an la ilaha illallahu wahdahu la sharika lah, wa ashhadu anna Muhammadan 'abduhu wa rasuluh."
                )
            )
        ),
        PracticalGuide(
            id = "salah",
            title = "How to Pray Salah (Core Steps)",
            subtitle = "Clear Sunnah posture and recitation walkthrough for each rak'ah",
            steps = listOf(
                GuideStep(1, "Face Qibla & Takbir", "Stand facing the Qibla with calm focus, raise hands to shoulders/ears, and say Allahu Akbar.", "اللَّهُ أَكْبَرُ", "Allahu Akbar"),
                GuideStep(2, "Opening & Al-Fatihah", "Place right hand over left on chest. Recite opening dua quietly, seek refuge, then recite Surah Al-Fatihah followed by another Surah in the first two rak'ahs."),
                GuideStep(3, "Ruku (Bowing)", "Say Allahu Akbar, bow with back straight and hands gripping knees, and repeat three times:", "سُبْحَانَ رَبِّيَ الْعَظِيمِ", "Subhana Rabbiyal-'Azim (3x)"),
                GuideStep(4, "Rising from Ruku", "Stand straight while saying:", "سَمِعَ اللَّهُ لِمَنْ حَمِدَهُ · رَبَّنَا وَلَكَ الْحَمْدُ", "Sami'allahu liman hamidah · Rabbana wa lakal-hamd"),
                GuideStep(5, "Sujood (Two Prostrations)", "Go down onto forehead, nose, palms, knees, and toes. Say three times, sit calmly between them, then prostrate a second time:", "سُبْحَانَ رَبِّيَ الْأَعْلَى", "Subhana Rabbiyal-A'la (3x)"),
                GuideStep(6, "Tashahhud & Salawat", "After 2nd rak'ah (and final rak'ah), sit calmly, recite At-Tahiyyat and Ibrahimiyah Salawat upon the Prophet ﷺ."),
                GuideStep(7, "Taslim (Ending Prayer)", "Turn your face right then left, saying:", "السَّلَامُ عَلَيْكُمْ وَرَحْمَةُ اللَّهِ", "As-salamu 'alaykum wa rahmatullah")
            )
        ),
        PracticalGuide(
            id = "umrah",
            title = "Umrah Rituals at a Glance",
            subtitle = "From Miqat Ihram through Tawaf, Sa'i, and completion",
            steps = listOf(
                GuideStep(1, "Ihram before Miqat", "Perform ghusl, put on Ihram garments (men), make Niyyah for Umrah, and begin the Talbiyah:", "لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ", "Labbayk Allahumma labbayk..."),
                GuideStep(2, "Tawaf (7 Circuits)", "Starting from the Black Stone corner with the Kaaba on your left, circle 7 times in remembrance and dua."),
                GuideStep(3, "Pray 2 Rak'ahs & Zamzam", "Pray two short rak'ahs behind Maqam Ibrahim (or anywhere in the Haram) and drink Zamzam water with dua."),
                GuideStep(4, "Sa'i between Safa & Marwah", "Begin at As-Safa facing the Kaaba, then walk 7 laps between Safa and Marwah, finishing at Marwah."),
                GuideStep(5, "Halq or Taqsir", "Men shave (preferred) or trim hair evenly; women trim a fingertip's length from hair ends. Your Umrah is complete.")
            )
        )
    )
}
