package com.m7mdabaza.xogame.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderScheduleTest {

    private val zone = TimeZone.getTimeZone("Africa/Cairo")

    /** Millis of [day] November 2026 at [hour]:[minute] in [zone]. */
    private fun at(day: Int, hour: Int, minute: Int = 0): Long = Calendar.getInstance(zone).apply {
        clear()
        set(2026, Calendar.NOVEMBER, day, hour, minute)
    }.timeInMillis

    @Test
    fun `daytime - exactly 24 hours later`() {
        assertEquals(at(6, 9), ReminderSchedule.nextAt(at(5, 9), zone))
        assertEquals(at(6, 14, 30), ReminderSchedule.nextAt(at(5, 14, 30), zone))
        assertEquals(at(6, 21, 59), ReminderSchedule.nextAt(at(5, 21, 59), zone))
    }

    @Test
    fun `late evening moves to the next morning`() {
        assertEquals(at(7, 9), ReminderSchedule.nextAt(at(5, 22), zone))
        assertEquals(at(7, 9), ReminderSchedule.nextAt(at(5, 23, 45), zone))
    }

    @Test
    fun `after midnight moves to the same morning`() {
        assertEquals(at(6, 9), ReminderSchedule.nextAt(at(5, 0, 10), zone))
        assertEquals(at(6, 9), ReminderSchedule.nextAt(at(5, 8, 59), zone))
    }

    @Test
    fun `a reminder at the morning time repeats at the same time`() {
        val first = ReminderSchedule.nextAt(at(5, 2), zone)
        assertEquals(at(6, 9), first)
        assertEquals(at(7, 9), ReminderSchedule.nextAt(first, zone))
    }
}
