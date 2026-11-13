package io.github.akrishna87.daybook

import io.github.akrishna87.daybook.model.Agenda
import io.github.akrishna87.daybook.model.CalendarView
import io.github.akrishna87.daybook.model.Entry
import io.github.akrishna87.daybook.model.Repeat
import io.github.akrishna87.daybook.model.Settings
import io.github.akrishna87.daybook.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** What the calendar puts on each day, and where repeating tasks start. */
class AgendaTest {
    private val today = LocalDate.of(2026, 11, 13) // a Friday
    private val utc = ZoneOffset.UTC

    @Test
    fun aRepeatingTaskShowsOnEveryDayItComesRound() {
        val standup = Task(title = "Standup", due = today, repeat = Repeat.WEEKDAYS)
        val days = Agenda.occurrences(standup, today, today.plusDays(6))
        // Fri, then Mon to Thu: no weekend.
        assertEquals(listOf(13, 16, 17, 18, 19), days.map { it.dayOfMonth })
        // Not before its due date.
        assertEquals(emptyList<LocalDate>(), Agenda.occurrences(standup, today.minusDays(5), today.minusDays(1)))
    }

    @Test
    fun aRunStopsAtItsEndDateOnTheCalendar() {
        val gym = Task(title = "Gym", due = today, repeat = Repeat.WEEKLY, repeatStart = today, repeatEnd = today.plusWeeks(2))
        assertEquals(listOf(today, today.plusWeeks(1), today.plusWeeks(2)), Agenda.occurrences(gym, today, today.plusMonths(3)))
    }

    @Test
    fun laterOccurrencesAreMarkedProjected() {
        val rent = Task(title = "Rent", due = today, repeat = Repeat.MONTHLY)
        val map = Agenda.range(listOf(rent), today, today.plusMonths(2), zone = utc)
        assertEquals(3, map.size)
        assertFalse(map.getValue(today).single().projected)
        assertTrue(map.getValue(today.plusMonths(1)).single().projected)
    }

    @Test
    fun finishedTasksShowOnTheirDayAndUndatedOnesOnTheDayTheyWereDone() {
        val done = Task(title = "Paid", due = today.minusDays(2), done = true, doneAt = 1L)
        val undated = Task(title = "Called", done = true, doneAt = today.atStartOfDay(utc).toInstant().toEpochMilli() + 3_600_000)
        val map = Agenda.range(listOf(done, undated), today.minusDays(7), today, zone = utc)
        assertEquals("Paid", map.getValue(today.minusDays(2)).single().task.title)
        assertEquals("Called", map.getValue(today).single().task.title)
        assertTrue(Agenda.range(listOf(done), today.minusDays(7), today, includeDone = false, zone = utc).isEmpty())
    }

    @Test
    fun aDayListsOpenTasksFirstUntimedThenByTime() {
        val a = Task(title = "9am", due = today, time = LocalTime.of(9, 0), createdAt = 1)
        val b = Task(title = "untimed", due = today, createdAt = 2)
        val c = Task(title = "done", due = today, done = true, doneAt = 5, createdAt = 3)
        val d = Task(title = "7am", due = today, time = LocalTime.of(7, 0), createdAt = 4)
        assertEquals(listOf("untimed", "7am", "9am", "done"), Agenda.on(listOf(a, b, c, d), today, zone = utc).map { it.task.title })
    }

    @Test
    fun aStartDateCanBeInThePastOrTheFuture() {
        // A weekly meeting that started on a Monday three weeks ago is next due on Monday.
        val mondays = LocalDate.of(2026, 10, 26)
        assertEquals(LocalDate.of(2026, 11, 16), Agenda.firstOnOrAfter(mondays, Repeat.WEEKLY, today))
        // Starting later: the start itself.
        assertEquals(LocalDate.of(2026, 12, 1), Agenda.firstOnOrAfter(LocalDate.of(2026, 12, 1), Repeat.DAILY, today))
        // A daily task that started in the past is due today.
        assertEquals(today, Agenda.firstOnOrAfter(LocalDate.of(2026, 9, 1), Repeat.DAILY, today))
        // Weekdays starting on a Saturday begin on the Monday.
        assertEquals(LocalDate.of(2026, 11, 16), Agenda.firstOnOrAfter(LocalDate.of(2026, 11, 14), Repeat.WEEKDAYS, today))
    }

    @Test
    fun overlappingTimedTasksSitSideBySide() {
        fun at(h: Int, m: Int = 0) = Entry(Task(title = "$h:$m", due = today, time = LocalTime.of(h, m)), today)
        val laid = Agenda.lanes(listOf(at(9), at(9, 30), at(11), at(14)))
        val byTitle = laid.associate { it.first.task.title to (it.second to it.third) }
        assertEquals(0 to 2, byTitle["9:0"])
        assertEquals(1 to 2, byTitle["9:30"])
        assertEquals(0 to 1, byTitle["11:0"])
        assertEquals(0 to 1, byTitle["14:0"])
        // Untimed tasks aren't laid out by the hour.
        assertTrue(Agenda.lanes(listOf(Entry(Task(title = "x", due = today), today))).isEmpty())
    }

    @Test
    fun theCalendarViewIsRemembered() {
        val s = Settings(calendarView = CalendarView.WEEK)
        assertEquals(CalendarView.WEEK, Settings.fromJson(s.toJson()).calendarView)
        assertEquals(CalendarView.MONTH, Settings.fromJson(null).calendarView)
    }
}
