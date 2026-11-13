package io.github.akrishna87.daybook.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * One task on one day of the calendar.
 *
 * [projected] is a later occurrence of a repeating task: the task itself is due earlier, but it will
 * come round on [day] too. Those can be opened but not ticked off until they're the next one.
 */
data class Entry(val task: Task, val day: LocalDate, val projected: Boolean = false) {
    val key: String get() = "${task.id}@$day"
    val time: LocalTime? get() = task.time
}

/** The views the Upcoming tab can show. */
enum class CalendarView(val label: String) { MONTH("Month"), WEEK("Week"), DAY("Day"), LIST("List") }

/** What goes on each day of the calendar. Plain Kotlin, so it is unit tested. */
object Agenda {

    /** Safety stop for very long ranges of daily tasks. */
    private const val MAX_STEPS = 2000

    /**
     * The days a task is on between [from] and [to] (inclusive): its due date, and for an open
     * repeating task every later date it will come round on, up to its end date.
     */
    fun occurrences(task: Task, from: LocalDate, to: LocalDate): List<LocalDate> {
        val due = task.due ?: return emptyList()
        if (to.isBefore(from)) return emptyList()
        if (task.done || task.repeat == Repeat.NONE) return if (due in from..to) listOf(due) else emptyList()
        val end = task.repeatEnd?.let { if (it.isBefore(to)) it else to } ?: to
        val out = ArrayList<LocalDate>()
        var d = due
        var steps = 0
        while (!d.isAfter(end) && steps < MAX_STEPS) {
            if (!d.isBefore(from)) out.add(d)
            d = Dates.step(d, task.repeat)
            steps++
        }
        return out
    }

    /**
     * Every task on every day from [from] to [to]: open tasks on their due date, later occurrences of
     * repeating tasks, and finished tasks on the day they were due (or finished, if they had no date).
     * Days with nothing on them are left out. Each day is in [dayOrder].
     */
    fun range(
        tasks: List<Task>,
        from: LocalDate,
        to: LocalDate,
        includeDone: Boolean = true,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Map<LocalDate, List<Entry>> {
        val byDay = HashMap<LocalDate, MutableList<Entry>>()
        fun put(e: Entry) = byDay.getOrPut(e.day) { ArrayList() }.add(e)
        for (t in tasks) {
            if (t.done) {
                if (!includeDone) continue
                val day = t.due ?: t.doneAt.takeIf { it > 0 }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: continue
                if (day in from..to) put(Entry(t, day))
                continue
            }
            val due = t.due ?: continue
            for (d in occurrences(t, from, to)) put(Entry(t, d, projected = d != due))
        }
        return byDay.mapValues { (_, v) -> v.sortedWith(dayOrder) }
    }

    /** Everything on one day, in [dayOrder]. */
    fun on(tasks: List<Task>, day: LocalDate, includeDone: Boolean = true, zone: ZoneId = ZoneId.systemDefault()): List<Entry> =
        range(tasks, day, day, includeDone, zone)[day].orEmpty()

    /** Open tasks first, then by time (untimed ones before timed ones, like an all-day row), then priority. */
    val dayOrder: Comparator<Entry> = compareBy<Entry>(
        { it.task.done },
        { it.time != null },
        { it.time },
        { it.task.priority },
        { it.task.createdAt },
    )

    /**
     * Where a repeating task with a run starting on [start] first comes round on or after [from]:
     * [start] itself (moved to Monday if it's a weekend and the task repeats on weekdays), then stepping on.
     */
    fun firstOnOrAfter(start: LocalDate, repeat: Repeat, from: LocalDate): LocalDate {
        var d = start
        if (repeat == Repeat.WEEKDAYS) while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.plusDays(1)
        if (repeat == Repeat.NONE) return if (d.isBefore(from)) from else d
        var steps = 0
        while (d.isBefore(from) && steps < 100_000) {
            d = Dates.step(d, repeat)
            steps++
        }
        return d
    }

    /**
     * Timed entries laid out side by side where they overlap, like a calendar's day view. Each task
     * takes [slotMinutes] (tasks have no length). Returns, for each entry, its lane and how many lanes
     * its group of overlapping entries uses.
     */
    fun lanes(entries: List<Entry>, slotMinutes: Int = 60): List<Triple<Entry, Int, Int>> {
        val timed = entries.filter { it.time != null }.sortedWith(compareBy({ it.time }, { it.task.priority }, { it.task.createdAt }))
        val result = ArrayList<Triple<Entry, Int, Int>>()
        var group = ArrayList<Pair<Entry, Int>>()
        var laneEnds = ArrayList<Int>()
        var groupEnd = -1
        fun flush() {
            val n = laneEnds.size
            for ((e, lane) in group) result.add(Triple(e, lane, n))
            group = ArrayList()
            laneEnds = ArrayList()
        }
        for (e in timed) {
            val start = e.time!!.toSecondOfDay() / 60
            val end = start + slotMinutes
            if (group.isNotEmpty() && start >= groupEnd) flush()
            var lane = laneEnds.indexOfFirst { it <= start }
            if (lane < 0) {
                lane = laneEnds.size
                laneEnds.add(end)
            } else {
                laneEnds[lane] = end
            }
            group.add(e to lane)
            groupEnd = maxOf(if (group.size == 1) end else groupEnd, end)
        }
        flush()
        return result
    }
}
