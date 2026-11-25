package com.example.data

import android.content.Context
import com.example.domain.CurrencyPreset
import com.example.domain.DailyWorshipLog
import com.example.domain.NisabStandard
import com.example.domain.PrayerLogStatus
import com.example.domain.PrayerName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WorshipTrackerState(
    val logsByDate: Map<String, DailyWorshipLog> = emptyMap(),
    val todayKey: String = WorshipTrackerRepository.currentDateKey(),
    val currentStreakDays: Int = 0,
    val totalFastedDays: Int = 0,
    val khatamTargetDays: Int = 30,
    val khatamCompletedPages: Int = 0,
    val zakatCurrency: CurrencyPreset = CurrencyPreset.USD,
    val zakatNisabStandard: NisabStandard = NisabStandard.SILVER
) {
    val todayLog: DailyWorshipLog
        get() = logsByDate[todayKey] ?: DailyWorshipLog(dateKey = todayKey)

    val khatamPagesPerDay: Int
        get() = ((604 - khatamCompletedPages).coerceAtLeast(0) + khatamTargetDays - 1) /
            khatamTargetDays.coerceAtLeast(1)

    val khatamProgressFraction: Float
        get() = (khatamCompletedPages / 604f).coerceIn(0f, 1f)
}

class WorshipTrackerRepository private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "masjid_worship_tracker",
        Context.MODE_PRIVATE
    )

    private val _state = MutableStateFlow(loadAll())
    val state: StateFlow<WorshipTrackerState> = _state.asStateFlow()

    fun setPrayerStatus(prayer: PrayerName, status: PrayerLogStatus, dateKey: String = currentDateKey()) {
        if (prayer == PrayerName.SUNRISE) return
        val currentMap = _state.value.logsByDate.toMutableMap()
        val existing = currentMap[dateKey] ?: DailyWorshipLog(dateKey)
        val updatedStatuses = existing.prayerStatuses.toMutableMap()
        if (status == PrayerLogStatus.UNLOGGED) {
            updatedStatuses.remove(prayer)
        } else {
            updatedStatuses[prayer] = status
        }
        val nextLog = existing.copy(prayerStatuses = updatedStatuses)
        if (nextLog.prayerStatuses.isEmpty() && !nextLog.fasted) {
            currentMap.remove(dateKey)
        } else {
            currentMap[dateKey] = nextLog
        }
        persistAndPublish(logsMap = currentMap)
    }

    fun cyclePrayerStatus(prayer: PrayerName, dateKey: String = currentDateKey()) {
        val current = _state.value.logsByDate[dateKey]?.prayerStatuses?.get(prayer) ?: PrayerLogStatus.UNLOGGED
        setPrayerStatus(prayer, current.nextCycle(), dateKey)
    }

    fun toggleFastedToday(dateKey: String = currentDateKey()): Boolean {
        val currentMap = _state.value.logsByDate.toMutableMap()
        val existing = currentMap[dateKey] ?: DailyWorshipLog(dateKey)
        val nextFasted = !existing.fasted
        val nextLog = existing.copy(fasted = nextFasted)
        if (nextLog.prayerStatuses.isEmpty() && !nextLog.fasted) {
            currentMap.remove(dateKey)
        } else {
            currentMap[dateKey] = nextLog
        }
        persistAndPublish(logsMap = currentMap)
        return nextFasted
    }

    fun setKhatamGoalDays(days: Int) {
        val safeDays = days.coerceIn(7, 180)
        prefs.edit().putInt(KEY_KHATAM_DAYS, safeDays).apply()
        _state.value = _state.value.copy(khatamTargetDays = safeDays)
    }

    fun addKhatamPages(delta: Int) {
        val next = (_state.value.khatamCompletedPages + delta).coerceIn(0, 604)
        prefs.edit().putInt(KEY_KHATAM_PAGES, next).apply()
        _state.value = _state.value.copy(khatamCompletedPages = next)
    }

    fun resetKhatamProgress() {
        prefs.edit().putInt(KEY_KHATAM_PAGES, 0).apply()
        _state.value = _state.value.copy(khatamCompletedPages = 0)
    }

    fun saveZakatPreferences(currency: CurrencyPreset, nisab: NisabStandard) {
        prefs.edit()
            .putString(KEY_ZAKAT_CURRENCY, currency.code)
            .putString(KEY_ZAKAT_NISAB, nisab.id)
            .apply()
        _state.value = _state.value.copy(zakatCurrency = currency, zakatNisabStandard = nisab)
    }

    fun recentDateKeys(days: Int = 14): List<String> {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val list = ArrayList<String>(days)
        for (offset in (days - 1) downTo 0) {
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            list.add(fmt.format(c.time))
        }
        return list
    }

    private fun persistAndPublish(logsMap: Map<String, DailyWorshipLog>) {
        val root = JSONObject()
        logsMap.entries
            .sortedByDescending { it.key }
            .take(120)
            .forEach { (dateKey, log) ->
                val obj = JSONObject()
                obj.put("fasted", log.fasted)
                val pObj = JSONObject()
                log.prayerStatuses.forEach { (p, st) -> pObj.put(p.name, st.id) }
                obj.put("prayers", pObj)
                root.put(dateKey, obj)
            }
        prefs.edit().putString(KEY_WORSHIP_LOGS, root.toString()).apply()

        val today = currentDateKey()
        _state.value = _state.value.copy(
            logsByDate = logsMap,
            todayKey = today,
            currentStreakDays = computeStreak(logsMap),
            totalFastedDays = logsMap.values.count { it.fasted }
        )
    }

    private fun loadAll(): WorshipTrackerState {
        val logs = loadLogsMap()
        val today = currentDateKey()
        val days = prefs.getInt(KEY_KHATAM_DAYS, 30).coerceIn(7, 180)
        val pages = prefs.getInt(KEY_KHATAM_PAGES, 0).coerceIn(0, 604)
        val currency = CurrencyPreset.fromCode(prefs.getString(KEY_ZAKAT_CURRENCY, CurrencyPreset.USD.code))
        val nisabId = prefs.getString(KEY_ZAKAT_NISAB, NisabStandard.SILVER.id)
        val nisab = NisabStandard.entries.firstOrNull { it.id == nisabId } ?: NisabStandard.SILVER

        return WorshipTrackerState(
            logsByDate = logs,
            todayKey = today,
            currentStreakDays = computeStreak(logs),
            totalFastedDays = logs.values.count { it.fasted },
            khatamTargetDays = days,
            khatamCompletedPages = pages,
            zakatCurrency = currency,
            zakatNisabStandard = nisab
        )
    }

    private fun loadLogsMap(): Map<String, DailyWorshipLog> {
        val raw = prefs.getString(KEY_WORSHIP_LOGS, null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            val result = LinkedHashMap<String, DailyWorshipLog>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val dateKey = keys.next()
                val obj = root.getJSONObject(dateKey)
                val fasted = obj.optBoolean("fasted", false)
                val pObj = obj.optJSONObject("prayers")
                val statuses = LinkedHashMap<PrayerName, PrayerLogStatus>()
                DailyWorshipLog.trackedPrayers.forEach { p ->
                    val stId = pObj?.optString(p.name)
                    val parsed = PrayerLogStatus.fromId(stId)
                    if (parsed != PrayerLogStatus.UNLOGGED) statuses[p] = parsed
                }
                if (fasted || statuses.isNotEmpty()) {
                    result[dateKey] = DailyWorshipLog(dateKey, statuses, fasted)
                }
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun computeStreak(logs: Map<String, DailyWorshipLog>): Int {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val todayKey = fmt.format(cal.time)
        var streak = 0

        // Include today if completed all 5 prayers; otherwise start counting from yesterday
        val todayCompleted = (logs[todayKey]?.completedPrayersCount ?: 0) == 5
        if (todayCompleted) {
            streak = 1
        }
        var offset = -1
        while (offset > -365) {
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val key = fmt.format(c.time)
            if ((logs[key]?.completedPrayersCount ?: 0) == 5) {
                streak++
                offset--
            } else {
                break
            }
        }
        return streak
    }

    companion object {
        private const val KEY_WORSHIP_LOGS = "worship_logs_json"
        private const val KEY_KHATAM_DAYS = "khatam_target_days"
        private const val KEY_KHATAM_PAGES = "khatam_completed_pages"
        private const val KEY_ZAKAT_CURRENCY = "zakat_currency"
        private const val KEY_ZAKAT_NISAB = "zakat_nisab"

        @Volatile
        private var instance: WorshipTrackerRepository? = null

        fun getInstance(context: Context): WorshipTrackerRepository {
            return instance ?: synchronized(this) {
                instance ?: WorshipTrackerRepository(context).also { instance = it }
            }
        }

        fun currentDateKey(nowMillis: Long = System.currentTimeMillis()): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
    }
}
