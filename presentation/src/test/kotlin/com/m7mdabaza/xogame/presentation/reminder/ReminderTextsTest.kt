package com.m7mdabaza.xogame.presentation.reminder

import com.m7mdabaza.xogame.domain.model.ChallengeReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTextsTest {

    private val day = 20_400L

    private fun reminder(epochDay: Long = day, streak: Int = 0, number: Int = 1) = ChallengeReminder(epochDay, streak, number)

    @Test
    fun `every text is different`() {
        val all = ReminderTexts.daily + ReminderTexts.streak + ReminderTexts.comeback
        assertEquals(all.size, all.map { it.first }.toSet().size)
        assertEquals(all.size, all.map { it.second }.toSet().size)
        assertTrue("enough daily texts to stay fresh", ReminderTexts.daily.size >= 14)
    }

    @Test
    fun `a new text every day, and every text comes up before one repeats`() {
        for ((streak, number, size) in listOf(
            Triple(0, 1, ReminderTexts.daily.size),
            Triple(5, 1, ReminderTexts.streak.size),
            Triple(0, ReminderTexts.COMEBACK_FROM, ReminderTexts.comeback.size),
        )) {
            val texts = (0 until size).map { ReminderTexts.pick(reminder(day + it, streak, number)) }
            assertEquals("streak $streak, reminder $number", size, texts.toSet().size)
            texts.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
        }
    }

    @Test
    fun `a streak of 2 or more is named in the text, 1 day is not a streak yet`() {
        val text = ReminderTexts.pick(reminder(streak = 4))
        assertTrue(text is ReminderText.Streak)
        assertEquals(4, (text as ReminderText.Streak).days)
        assertTrue(ReminderTexts.pick(reminder(streak = 1)) is ReminderText.Plain)
        assertTrue(ReminderTexts.daily.any { it.first == ReminderTexts.pick(reminder(streak = 1)).title })
    }

    @Test
    fun `from the third reminder in a row the comeback texts are used, unless a streak is at stake`() {
        val second = ReminderTexts.pick(reminder(number = ReminderTexts.COMEBACK_FROM - 1))
        val third = ReminderTexts.pick(reminder(number = ReminderTexts.COMEBACK_FROM))
        assertTrue(ReminderTexts.daily.any { it.first == second.title })
        assertTrue(ReminderTexts.comeback.any { it.first == third.title })
        assertTrue(ReminderTexts.pick(reminder(streak = 3, number = ReminderTexts.COMEBACK_FROM)) is ReminderText.Streak)
    }
}
