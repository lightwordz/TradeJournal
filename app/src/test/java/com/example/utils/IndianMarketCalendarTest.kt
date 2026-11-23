package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IndianMarketCalendarTest {

    @Test
    fun parseDateParts_validAndInvalidDates() {
        assertEquals(MonthKey(2026, 11) to 23, IndianMarketCalendar.parseDateParts("2026-11-23"))
        assertEquals(MonthKey(2026, 2) to 28, IndianMarketCalendar.parseDateParts("2026-02-28"))
        assertNull(IndianMarketCalendar.parseDateParts("2026-02-29"))
        assertNull(IndianMarketCalendar.parseDateParts("2026-13-01"))
        assertNull(IndianMarketCalendar.parseDateParts("invalid"))
    }

    @Test
    fun utcMillisRoundTrip_preservesExactDateWithoutTimezoneShift() {
        val dates = listOf("2026-01-01", "2026-03-06", "2026-11-23", "2026-12-31")
        dates.forEach { date ->
            val millis = IndianMarketCalendar.isoDateToUtcMillis(date)!!
            assertEquals(date, IndianMarketCalendar.utcMillisToIsoDate(millis))
        }
    }

    @Test
    fun isWeekend_detectsSaturdayAndSundayCorrectly() {
        // 2026-11-21 = Saturday, 2026-11-22 = Sunday, 2026-11-23 = Monday
        val nov2026 = MonthKey(2026, 11)
        assertTrue(IndianMarketCalendar.isWeekend(nov2026, 21))
        assertTrue(IndianMarketCalendar.isWeekend(nov2026, 22))
        assertFalse(IndianMarketCalendar.isWeekend(nov2026, 23))
    }

    @Test
    fun formatReadableDate_formatsCleanly() {
        assertEquals("Nov 23, 2026", IndianMarketCalendar.formatReadableDate("2026-11-23"))
    }
}
