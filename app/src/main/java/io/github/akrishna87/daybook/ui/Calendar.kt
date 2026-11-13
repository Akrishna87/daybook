package io.github.akrishna87.daybook.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarViewMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewDay
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.akrishna87.daybook.Store
import io.github.akrishna87.daybook.model.Agenda
import io.github.akrishna87.daybook.model.CalendarView
import io.github.akrishna87.daybook.model.Data
import io.github.akrishna87.daybook.model.Dates
import io.github.akrishna87.daybook.model.Entry
import io.github.akrishna87.daybook.model.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * The Upcoming tab: a calendar of every task. Month shows the month with each day's tasks and the
 * chosen day's list underneath; Week and Day lay timed tasks out by the hour; List is the day-by-day
 * list of the next four weeks. The chosen day is shared by all four, and the view is remembered.
 */
@Composable
fun UpcomingScreen(data: Data, padding: PaddingValues) {
    val nav = LocalNav.current
    val clock = LocalClock.current
    val today = clock.today
    val view = data.settings.calendarView
    val ws = Dates.weekStart(data.settings)
    val selected = nav.calendarDay ?: today
    var picking by remember { mutableStateOf(false) }
    fun select(d: LocalDate) {
        nav.calendarDay = d
    }
    fun setView(v: CalendarView) {
        if (v != view) Store.updateSettings { it.copy(calendarView = v) }
    }

    val inPeriod = when (view) {
        CalendarView.MONTH -> YearMonth.from(selected) == YearMonth.from(today)
        CalendarView.WEEK -> Dates.startOfWeek(selected, ws) == Dates.startOfWeek(today, ws)
        CalendarView.DAY -> selected == today
        CalendarView.LIST -> !selected.isAfter(today.plusDays(6))
    }

    Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
        ScreenHeader(
            "Upcoming",
            if (view == CalendarView.LIST) selected.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()) + " " + selected.year else null,
        ) {
            if (!inPeriod) TextButton(onClick = { select(today) }, Modifier.semantics { contentDescription = "Go to today" }) { Text("Today") }
            IconButton(onClick = { picking = true }, Modifier.semantics { contentDescription = "Pick a date" }) { Icon(Icons.Rounded.CalendarMonth, null) }
            SearchAction { nav.push(Page.Search) }
        }
        ViewSwitcher(view, ::setView)
        when (view) {
            CalendarView.MONTH -> MonthView(data, padding, selected, today, ws, ::select, onOpenDay = { select(it); setView(CalendarView.DAY) })
            CalendarView.WEEK -> WeekView(data, selected, today, clock.now, ws, ::select, onOpenDay = { select(it); setView(CalendarView.DAY) })
            CalendarView.DAY -> DayView(data, selected, today, clock.now, ::select)
            CalendarView.LIST -> UpcomingList(data, padding, selected, ::select)
        }
    }
    if (picking) DatePickDialog(selected, onPick = { select(it) }, onDismiss = { picking = false })
}

@Composable
private fun ViewSwitcher(view: CalendarView, onPick: (CalendarView) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(3.dp),
    ) {
        for (v in CalendarView.entries) {
            val on = v == view
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable(role = Role.Tab) { onPick(v) }
                    .semantics {
                        contentDescription = "${v.label} view"
                        selected = on
                    }
                    .padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(v.icon(), null, Modifier.size(16.dp), tint = color)
                Spacer(Modifier.width(5.dp))
                Text(v.label, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
}

private fun CalendarView.icon(): ImageVector = when (this) {
    CalendarView.MONTH -> Icons.Rounded.CalendarViewMonth
    CalendarView.WEEK -> Icons.Rounded.ViewWeek
    CalendarView.DAY -> Icons.Rounded.ViewDay
    CalendarView.LIST -> Icons.Rounded.ViewAgenda
}

/** "‹  November 2026  ›" above the Month, Week and Day views. Tapping the title picks a date. */
@Composable
private fun PeriodBar(title: String, prevLabel: String, nextLabel: String, onPrev: () -> Unit, onNext: () -> Unit, onTitle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev, Modifier.semantics { contentDescription = prevLabel }) { Icon(Icons.Rounded.ChevronLeft, null) }
        Text(
            title,
            Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(onClick = onTitle).padding(vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
        )
        IconButton(onClick = onNext, Modifier.semantics { contentDescription = nextLabel }) { Icon(Icons.Rounded.ChevronRight, null) }
    }
}

/** Calls [onPrev] or [onNext] when the content is swiped right or left. */
private fun Modifier.swipeToPage(key: Any, onPrev: () -> Unit, onNext: () -> Unit): Modifier = this.then(
    Modifier.pointerInput(key) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                val threshold = 64.dp.toPx()
                if (total < -threshold) onNext() else if (total > threshold) onPrev()
            },
        ) { change, amount ->
            total += amount
            change.consume()
        }
    },
)

/** A task's colour on the calendar: its project's. */
private fun Data.entryColor(task: Task): Color = Color(project(task.projectId).color)

// Month

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MonthView(
    data: Data,
    padding: PaddingValues,
    selected: LocalDate,
    today: LocalDate,
    ws: DayOfWeek,
    onSelect: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val nav = LocalNav.current
    val month = YearMonth.from(selected)
    val grid = remember(month, ws) { Dates.monthGrid(month, ws) }
    val byDay = remember(data.tasks, month) { Agenda.range(data.tasks, month.atDay(1), month.atEndOfMonth()) }
    val dayEntries = byDay[selected].orEmpty()
    val locale = Locale.getDefault()
    val currentSelect by rememberUpdatedState(onSelect)
    val currentSelected by rememberUpdatedState(selected)

    fun moveMonth(by: Long) {
        val m = YearMonth.from(currentSelected).plusMonths(by)
        currentSelect(if (m == YearMonth.from(today)) today else m.atDay(currentSelected.dayOfMonth.coerceAtMost(m.lengthOfMonth())))
    }

    var picking by remember { mutableStateOf(false) }
    PeriodBar(
        month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale) + " " + month.year,
        "Previous month", "Next month",
        onPrev = { moveMonth(-1) }, onNext = { moveMonth(1) }, onTitle = { picking = true },
    )
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 88.dp),
    ) {
        item(key = "grid-$month") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp).swipeToPage(month, { moveMonth(-1) }, { moveMonth(1) })) {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    for (l in (0 until 7).map { ws.plus(it.toLong()).getDisplayName(TextStyle.SHORT, locale) }) {
                        Text(
                            l,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                for (week in grid) {
                    Row(Modifier.fillMaxWidth()) {
                        for (d in week) {
                            if (d == null) {
                                Spacer(Modifier.weight(1f).height(66.dp))
                            } else {
                                MonthCell(
                                    d, byDay[d].orEmpty(), data, d == today, d == selected, d.isBefore(today),
                                    Modifier
                                        .weight(1f)
                                        .combinedClickable(
                                            onClick = { if (d == selected) onOpenDay(d) else onSelect(d) },
                                            onLongClick = { onSelect(d); nav.quickAdd = QuickAddDefaults(due = d) },
                                        ),
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
        }
        item(key = "day-head") {
            AgendaHeader(selected, today, dayEntries, onAdd = { nav.quickAdd = QuickAddDefaults(due = selected) }, onOpenDay = { onOpenDay(selected) })
        }
        if (dayEntries.isEmpty()) {
            item(key = "day-empty") {
                Text(
                    if (selected.isBefore(today)) "Nothing was on this day." else "Nothing planned. Tap + to add a task, or long-press any day.",
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(dayEntries, key = { it.key }) { e -> AgendaRow(e, data, today, Modifier.animateItem()) }
    }
    if (picking) DatePickDialog(selected, onPick = onSelect, onDismiss = { picking = false })
}

@Composable
private fun MonthCell(day: LocalDate, entries: List<Entry>, data: Data, isToday: Boolean, isSelected: Boolean, past: Boolean, modifier: Modifier) {
    val open = entries.count { !it.task.done }
    val desc = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()).format(day) +
        (if (isToday) ", today" else "") +
        when (open) {
            0 -> ""
            1 -> ", 1 task"
            else -> ", $open tasks"
        }
    Column(
        modifier
            .height(66.dp)
            .padding(1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent)
            .clearAndSetSemantics {
                contentDescription = desc
                selected = isSelected
            }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday -> MaterialTheme.colorScheme.onPrimary
                    past -> MaterialTheme.colorScheme.outline
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
        val shown = if (entries.size > 3) entries.take(2) else entries.take(3)
        for (e in shown) MonthChip(e, data)
        if (entries.size > shown.size) {
            Text("+${entries.size - shown.size}", fontSize = 9.sp, lineHeight = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MonthChip(e: Entry, data: Data) {
    val color = data.entryColor(e.task)
    val done = e.task.done
    Text(
        e.task.title,
        Modifier
            .fillMaxWidth()
            .padding(top = 1.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (done) MaterialTheme.colorScheme.surfaceVariant else color.copy(alpha = if (e.projected) 0.12f else 0.24f))
            .padding(horizontal = 2.dp),
        fontSize = 9.sp,
        lineHeight = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        textDecoration = if (done) TextDecoration.LineThrough else null,
    )
}

/** "Today · Fri 13 Nov · 3 tasks", with + to add a task that day and a way into the Day view. */
@Composable
private fun AgendaHeader(day: LocalDate, today: LocalDate, entries: List<Entry>, onAdd: () -> Unit, onOpenDay: (() -> Unit)?) {
    val open = entries.count { !it.task.done }
    val done = entries.size - open
    val count = listOfNotNull(
        when (open) {
            0 -> null
            1 -> "1 task"
            else -> "$open tasks"
        },
        if (done > 0) "$done done" else null,
    ).joinToString(" · ")
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                Dates.dayHeading(day, today),
                style = MaterialTheme.typography.titleSmall,
                color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (count.isNotEmpty()) Text(count, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onOpenDay != null) TextButton(onClick = onOpenDay) { Text("Day view") }
        IconButton(onClick = onAdd, Modifier.semantics { contentDescription = "Add task on ${Dates.dayHeading(day, today)}" }) {
            Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** One task in a day's list: a normal task row, or a later occurrence of a repeating task. */
@Composable
private fun AgendaRow(e: Entry, data: Data, today: LocalDate, modifier: Modifier = Modifier) {
    val nav = LocalNav.current
    val t = e.task
    when {
        e.projected -> ProjectedRow(e, data, today, modifier) { nav.push(Page.TaskPage(t.id)) }
        t.done -> TaskRow(t, data, modifier, showDue = false) { nav.push(Page.TaskPage(t.id)) }
        else -> SwipeTask(t, modifier) {
            val overdue = t.due?.isBefore(today) == true
            TaskRow(t, data, showDue = t.time != null || overdue, timeOnly = !overdue) { nav.push(Page.TaskPage(t.id)) }
        }
    }
}

/** A repeating task on a later day it comes round: it can be opened, and is ticked off when it's the next one. */
@Composable
private fun ProjectedRow(e: Entry, data: Data, today: LocalDate, modifier: Modifier, onClick: () -> Unit) {
    val clock = LocalClock.current
    val t = e.task
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(22.dp).clip(CircleShape).border(1.5.dp, data.entryColor(t).copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Repeat, null, Modifier.size(13.dp), tint = data.entryColor(t))
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val parts = listOfNotNull(
                    t.time?.let { Dates.timeFormatter(clock.is24Hour).format(it) },
                    t.repeat.label,
                    t.due?.let { "Next: " + Dates.dayLabel(it, today) },
                )
                Text(parts.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Dates.weekLabel(t.copy(due = e.day))?.let { WeekTag(it) }
        }
        HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// Week and Day

@Composable
private fun WeekView(
    data: Data,
    selected: LocalDate,
    today: LocalDate,
    now: LocalDateTime,
    ws: DayOfWeek,
    onSelect: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
) {
    val start = Dates.startOfWeek(selected, ws)
    val days = (0 until 7).map { start.plusDays(it.toLong()) }
    val byDay = remember(data.tasks, start) { Agenda.range(data.tasks, start, start.plusDays(6)) }
    val currentSelect by rememberUpdatedState(onSelect)
    val currentSelected by rememberUpdatedState(selected)
    val prev = { currentSelect(currentSelected.minusWeeks(1)) }
    val next = { currentSelect(currentSelected.plusWeeks(1)) }
    var picking by remember { mutableStateOf(false) }
    PeriodBar(weekTitle(start, today), "Previous week", "Next week", prev, next, onTitle = { picking = true })
    Column(Modifier.fillMaxSize().swipeToPage(start, prev, next)) {
        Row(Modifier.fillMaxWidth().padding(end = 4.dp)) {
            Spacer(Modifier.width(GUTTER))
            for (d in days) {
                val isToday = d == today
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenDay(d) }
                        .semantics { contentDescription = "Open " + DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()).format(d) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            d.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        AllDayStrip(days, byDay, data, compact = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Timeline(days, byDay, data, today, now, hourHeight = 52.dp, modifier = Modifier.weight(1f))
    }
    if (picking) DatePickDialog(selected, onPick = onSelect, onDismiss = { picking = false })
}

@Composable
private fun DayView(data: Data, selected: LocalDate, today: LocalDate, now: LocalDateTime, onSelect: (LocalDate) -> Unit) {
    val nav = LocalNav.current
    val byDay = remember(data.tasks, selected) { Agenda.range(data.tasks, selected, selected) }
    val entries = byDay[selected].orEmpty()
    val currentSelect by rememberUpdatedState(onSelect)
    val currentSelected by rememberUpdatedState(selected)
    val prev = { currentSelect(currentSelected.minusDays(1)) }
    val next = { currentSelect(currentSelected.plusDays(1)) }
    var picking by remember { mutableStateOf(false) }
    PeriodBar(dayTitle(selected, today), "Previous day", "Next day", prev, next, onTitle = { picking = true })
    Column(Modifier.fillMaxSize().swipeToPage(selected, prev, next)) {
        AgendaHeader(selected, today, entries, onAdd = { nav.quickAdd = QuickAddDefaults(due = selected) }, onOpenDay = null)
        AllDayStrip(listOf(selected), byDay, data, compact = false)
        if (entries.none { it.time != null } && entries.isNotEmpty()) {
            Text(
                "Tap an hour to add a task at that time.",
                Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Timeline(listOf(selected), byDay, data, today, now, hourHeight = 60.dp, modifier = Modifier.weight(1f))
    }
    if (picking) DatePickDialog(selected, onPick = onSelect, onDismiss = { picking = false })
}

private val GUTTER = 46.dp

/** Tasks without a time, above the hours. Week view shows a few per day; Day view shows them all. */
@Composable
private fun AllDayStrip(days: List<LocalDate>, byDay: Map<LocalDate, List<Entry>>, data: Data, compact: Boolean) {
    val nav = LocalNav.current
    val untimed = days.associateWith { d -> byDay[d].orEmpty().filter { it.time == null } }
    if (untimed.values.all { it.isEmpty() }) return
    var expanded by remember(days.first()) { mutableStateOf(false) }
    val limit = if (expanded) Int.MAX_VALUE else if (compact) 2 else 4
    Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp, end = 4.dp)) {
        Text(
            "All day",
            Modifier.width(GUTTER).padding(top = 3.dp, start = 4.dp),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        for (d in days) {
            val list = untimed.getValue(d)
            Column(Modifier.weight(1f).padding(horizontal = 1.dp)) {
                for (e in list.take(limit)) {
                    EntryBlock(e, data, Modifier.fillMaxWidth().padding(bottom = 2.dp).heightIn(min = if (compact) 20.dp else 34.dp), compact) {
                        nav.push(Page.TaskPage(e.task.id))
                    }
                }
                if (list.size > limit) {
                    Text(
                        "+${list.size - limit}",
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).clickable { expanded = true }.padding(2.dp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** A task drawn on the calendar: a tinted block in its project's colour. */
@Composable
private fun EntryBlock(e: Entry, data: Data, modifier: Modifier, compact: Boolean, onClick: () -> Unit) {
    val clock = LocalClock.current
    val color = data.entryColor(e.task)
    val done = e.task.done
    val fg = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (done) MaterialTheme.colorScheme.surfaceVariant else color.copy(alpha = if (e.projected) 0.13f else 0.26f))
            .clickable(onClick = onClick)
            .alpha(if (e.projected) 0.85f else 1f),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (done) MaterialTheme.colorScheme.outline else color))
        Column(Modifier.padding(horizontal = 3.dp, vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (e.projected || e.task.repeat != io.github.akrishna87.daybook.model.Repeat.NONE) {
                    Icon(Icons.Rounded.Repeat, "Repeats", Modifier.size(if (compact) 9.dp else 12.dp).padding(end = 1.dp), tint = fg)
                }
                Text(
                    e.task.title,
                    fontSize = if (compact) 10.sp else 13.sp,
                    lineHeight = if (compact) 12.sp else 16.sp,
                    maxLines = if (compact) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    color = fg,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                )
            }
            if (!compact && e.task.time != null) {
                Text(Dates.timeFormatter(clock.is24Hour).format(e.task.time), fontSize = 11.sp, lineHeight = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Hours down the side and one column per day, with timed tasks placed at their time (each takes an
 * hour, side by side where they overlap) and a line at the current time. Tap an empty hour to add a
 * task then.
 */
@Composable
private fun Timeline(
    days: List<LocalDate>,
    byDay: Map<LocalDate, List<Entry>>,
    data: Data,
    today: LocalDate,
    now: LocalDateTime,
    hourHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val nav = LocalNav.current
    val clock = LocalClock.current
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val nowColor = LocalExtra.current.overdue
    val compact = days.size > 1
    val hourFormat = DateTimeFormatter.ofPattern(if (clock.is24Hour) "HH:mm" else "h a", Locale.getDefault())
    val laid = remember(byDay, days) { days.associateWith { Agenda.lanes(byDay[it].orEmpty()) } }

    LaunchedEffect(days.first(), days.size) {
        val firstTimed = laid.values.flatten().minOfOrNull { it.first.time!!.hour }
        val hour = when {
            today in days -> now.hour - 1
            firstTimed != null -> firstTimed - 1
            else -> 7
        }.coerceIn(0, 16)
        scroll.scrollTo(with(density) { (hourHeight * hour).roundToPx() })
    }

    Box(modifier.fillMaxWidth().verticalScroll(scroll)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(hourHeight * 24)
                .padding(end = 4.dp)
                .drawBehind {
                    val h = hourHeight.toPx()
                    val left = GUTTER.toPx()
                    for (i in 0..24) drawLine(lineColor, Offset(left, i * h), Offset(size.width, i * h), 1f)
                },
        ) {
            Column(Modifier.width(GUTTER)) {
                for (h in 0 until 24) {
                    Box(Modifier.height(hourHeight).fillMaxWidth()) {
                        if (h > 0) {
                            Text(
                                hourFormat.format(LocalTime.of(h, 0)).lowercase(Locale.getDefault()),
                                Modifier.offset(y = (-7).dp).padding(start = 4.dp),
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            for (d in days) {
                BoxWithConstraints(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .drawBehind { drawLine(lineColor, Offset(0f, 0f), Offset(0f, size.height), 1f) }
                        .background(if (d == today && compact) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f) else Color.Transparent)
                        .pointerInput(d) {
                            detectTapGestures { pos ->
                                val hour = (pos.y / hourHeight.toPx()).toInt().coerceIn(0, 23)
                                nav.calendarDay = d
                                nav.quickAdd = QuickAddDefaults(due = d, time = LocalTime.of(hour, 0))
                            }
                        },
                ) {
                    val colWidth = maxWidth
                    for ((e, lane, lanes) in laid.getValue(d)) {
                        val t = e.time!!
                        val y = hourHeight * (t.toSecondOfDay() / 3600f)
                        val w = colWidth / lanes
                        EntryBlock(
                            e, data,
                            Modifier
                                .offset(x = w * lane, y = y)
                                .width(w)
                                .height(hourHeight)
                                .padding(horizontal = 1.dp, vertical = 1.dp),
                            compact,
                        ) { nav.push(Page.TaskPage(e.task.id)) }
                    }
                    if (d == today) {
                        val y = hourHeight * (now.toLocalTime().toSecondOfDay() / 3600f)
                        Box(Modifier.offset(y = y - 1.dp).fillMaxWidth().height(2.dp).background(nowColor))
                        Box(Modifier.offset(x = (-4).dp, y = y - 5.dp).size(10.dp).clip(CircleShape).background(nowColor))
                    }
                }
            }
        }
    }
}

private fun weekTitle(start: LocalDate, today: LocalDate): String {
    val end = start.plusDays(6)
    val l = Locale.getDefault()
    val year = if (end.year != today.year || start.year != end.year) " ${end.year}" else ""
    return if (start.month == end.month) {
        "${start.dayOfMonth} – ${DateTimeFormatter.ofPattern("d MMM", l).format(end)}$year"
    } else {
        "${DateTimeFormatter.ofPattern("d MMM", l).format(start)} – ${DateTimeFormatter.ofPattern("d MMM", l).format(end)}$year"
    }
}

private fun dayTitle(day: LocalDate, today: LocalDate): String =
    DateTimeFormatter.ofPattern(if (day.year == today.year) "EEEE, d MMMM" else "EEEE, d MMMM yyyy", Locale.getDefault()).format(day)
