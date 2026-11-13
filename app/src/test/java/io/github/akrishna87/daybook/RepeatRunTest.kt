package io.github.akrishna87.daybook

import io.github.akrishna87.daybook.model.Data
import io.github.akrishna87.daybook.model.Dates
import io.github.akrishna87.daybook.model.Note
import io.github.akrishna87.daybook.model.QuickAdd
import io.github.akrishna87.daybook.model.Recording
import io.github.akrishna87.daybook.model.Repeat
import io.github.akrishna87.daybook.model.Task
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/** Repeating tasks with a start and end date, their week numbers, and voice notes on notes. */
class RepeatRunTest {
    private val today = LocalDate.of(2026, 11, 9) // a Monday
    private val run = Task(
        title = "Gym",
        due = today,
        repeat = Repeat.WEEKLY,
        repeatStart = today,
        repeatEnd = today.plusWeeks(6).minusDays(1),
    )

    @Test
    fun aRunStopsAtItsEndDate() {
        assertEquals(today.plusWeeks(1), Dates.nextOccurrence(run, today))
        val last = run.copy(due = today.plusWeeks(5))
        assertNull(Dates.nextOccurrence(last, today.plusWeeks(5)))
        // Without an end it carries on.
        assertEquals(today.plusWeeks(6), Dates.nextOccurrence(last.copy(repeatEnd = null), today.plusWeeks(5)))
        assertNull(Dates.nextOccurrence(run.copy(repeat = Repeat.NONE), today))
    }

    @Test
    fun weeksAreCountedFromTheStart() {
        assertEquals(1, Dates.weekNumber(today, today))
        assertEquals(1, Dates.weekNumber(today, today.plusDays(6)))
        assertEquals(2, Dates.weekNumber(today, today.plusDays(7)))
        assertNull(Dates.weekNumber(today, today.minusDays(1)))
        assertEquals(6, Dates.totalWeeks(today, today.plusWeeks(6).minusDays(1)))
        assertEquals("Week 1 of 6", Dates.weekLabel(run))
        assertEquals("Week 3 of 6", Dates.weekLabel(run.copy(due = today.plusWeeks(2))))
        assertEquals("Week 3", Dates.weekLabel(run.copy(due = today.plusWeeks(2), repeatEnd = null)))
        // Completed copies of an occurrence keep their week; plain tasks have none.
        assertEquals("Week 2 of 6", Dates.weekLabel(run.copy(due = today.plusWeeks(1), repeat = Repeat.NONE, seriesId = "x", done = true)))
        assertNull(Dates.weekLabel(Task(title = "x", due = today)))
        assertNull(Dates.weekLabel(run.copy(repeatStart = null)))
    }

    @Test
    fun upcomingNumbersItsWeeks() {
        assertEquals("Week 1 · This week · 9–15 Nov", Dates.weekHeading(today, today, Locale.ENGLISH))
        assertEquals("Week 2 · Next week · 16–22 Nov", Dates.weekHeading(today.plusWeeks(1), today, Locale.ENGLISH))
        assertEquals("Week 4 · 30 Nov – 6 Dec", Dates.weekHeading(today.plusWeeks(3), today, Locale.ENGLISH))
    }

    @Test
    fun quickAddReadsTheEnd() {
        val p = QuickAdd.parse("Gym every monday for 6 weeks", today)
        assertEquals("Gym", p.title)
        assertEquals(Repeat.WEEKLY, p.repeat)
        assertEquals(today, p.due)
        assertEquals(today.plusWeeks(6).minusDays(1), p.repeatEnd)

        val u = QuickAdd.parse("Vitamins every day until dec 31", today)
        assertEquals("Vitamins", u.title)
        assertEquals(Repeat.DAILY, u.repeat)
        assertEquals(LocalDate.of(2026, 12, 31), u.repeatEnd)

        val d = QuickAdd.parse("Stretch daily for 10 days", today)
        assertEquals(today.plusDays(9), d.repeatEnd)

        // Without a repeat, "for 2 weeks" is just part of the title.
        val plain = QuickAdd.parse("Book hotel for 2 weeks", today)
        assertEquals("Book hotel for 2 weeks", plain.title)
        assertNull(plain.repeatEnd)
    }

    @Test
    fun startEndAndVoiceNotesAreSaved() {
        val history = run.copy(id = "h", repeat = Repeat.NONE, seriesId = run.id, done = true, doneAt = 5L)
        val note = Note(title = "Standup", recordings = listOf(Recording(id = "r1", file = "voice-1.m4a", durationMs = 12_000L, createdAt = 7L)))
        val back = Data.fromJson(JSONObject(Data(tasks = listOf(run, history), notes = listOf(note)).toJson().toString()))
        assertEquals(run, back.task(run.id))
        assertEquals(history, back.task("h"))
        assertEquals(note.recordings, back.note(note.id)!!.recordings)
        // A note with only a voice note isn't empty.
        assertTrue(!Note(recordings = note.recordings).isBlank)
    }
}
