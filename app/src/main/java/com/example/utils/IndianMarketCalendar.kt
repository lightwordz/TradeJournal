package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class MonthKey(val year: Int, val month: Int) { // month: 1..12
    fun previous(): MonthKey = if (month == 1) MonthKey(year - 1, 12) else MonthKey(year, month - 1)
    fun next(): MonthKey = if (month == 12) MonthKey(year + 1, 1) else MonthKey(year, month + 1)
    fun prefix(): String = String.format(Locale.US, "%04d-%02d-", year, month)
    fun dateString(day: Int): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
}

object IndianMarketCalendar {
    private val istZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private val utcZone: TimeZone = TimeZone.getTimeZone("UTC")

    private val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // 2026 NSE / BSE / MCX major trading holidays (YYYY-MM-DD -> label)
    val holidays2026: Map<String, String> = mapOf(
        "2026-01-26" to "Republic Day · Markets Closed",
        "2026-02-15" to "Mahashivratri · NSE/BSE Closed",
        "2026-03-03" to "Holi · NSE/BSE Closed",
        "2026-03-20" to "Id-Ul-Fitr · Markets Closed",
        "2026-03-26" to "Ram Navami · Markets Closed",
        "2026-03-31" to "Mahavir Jayanti · Markets Closed",
        "2026-04-03" to "Good Friday · Markets Closed",
        "2026-04-14" to "Ambedkar Jayanti · Markets Closed",
        "2026-05-01" to "Maharashtra Day · NSE/BSE Closed",
        "2026-05-27" to "Bakri Id · Markets Closed",
        "2026-06-26" to "Muharram · Markets Closed",
        "2026-08-15" to "Independence Day · Markets Closed",
        "2026-09-14" to "Ganesh Chaturthi · NSE/BSE Closed",
        "2026-10-02" to "Gandhi Jayanti · Markets Closed",
        "2026-10-20" to "Dussehra · Markets Closed",
        "2026-11-08" to "Diwali Laxmi Pujan · Muhurat Session",
        "2026-11-10" to "Diwali Balipratipada · Markets Closed",
        "2026-11-24" to "Guru Nanak Jayanti · Markets Closed",
        "2026-12-25" to "Christmas · Markets Closed"
    )

    fun monthLabel(key: MonthKey): String = "${monthNames[key.month - 1]} ${key.year}"

    fun shortMonthName(month: Int): String = monthNames[month - 1].take(3)

    fun daysInMonth(key: MonthKey): Int {
        val cal = Calendar.getInstance(istZone).apply {
            set(Calendar.YEAR, key.year)
            set(Calendar.MONTH, key.month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun leadingBlanksMonFirst(key: MonthKey): Int {
        val cal = Calendar.getInstance(istZone).apply {
            set(Calendar.YEAR, key.year)
            set(Calendar.MONTH, key.month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        // Calendar.DAY_OF_WEEK: SUN=1, MON=2, TUE=3, WED=4, THU=5, FRI=6, SAT=7
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 6 // Sunday
        }
    }

    fun isWeekend(key: MonthKey, day: Int): Boolean {
        val cal = Calendar.getInstance(istZone).apply {
            set(Calendar.YEAR, key.year)
            set(Calendar.MONTH, key.month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    fun parseDateParts(isoDate: String): Pair<MonthKey, Int>? {
        val parts = isoDate.trim().split("-")
        if (parts.size != 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null
        if (year !in 1990..2100 || month !in 1..12) return null
        val monthKey = MonthKey(year, month)
        if (day !in 1..daysInMonth(monthKey)) return null
        return monthKey to day
    }

    fun todayIstDateString(nowMillis: Long = System.currentTimeMillis()): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istZone }
        return fmt.format(Date(nowMillis))
    }

    fun yesterdayIstDateString(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(istZone).apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istZone }
        return fmt.format(cal.time)
    }

    fun todayIst(nowMillis: Long = System.currentTimeMillis()): Triple<MonthKey, Int, String> {
        val dateStr = todayIstDateString(nowMillis)
        val parsed = parseDateParts(dateStr) ?: (MonthKey(2026, 1) to 1)
        return Triple(parsed.first, parsed.second, dateStr)
    }

    fun utcMillisToIsoDate(utcMillis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = utcZone }
        return fmt.format(Date(utcMillis))
    }

    fun isoDateToUtcMillis(isoDate: String): Long? {
        val (key, day) = parseDateParts(isoDate) ?: return null
        val cal = Calendar.getInstance(utcZone).apply {
            clear()
            set(Calendar.YEAR, key.year)
            set(Calendar.MONTH, key.month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        return cal.timeInMillis
    }

    fun formatReadableDate(isoDate: String): String {
        val (key, day) = parseDateParts(isoDate) ?: return isoDate
        return "${shortMonthName(key.month)} $day, ${key.year}"
    }
}
