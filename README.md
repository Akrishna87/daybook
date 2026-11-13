# 📒✅ Daybook for Android

Daybook is **notes and tasks in one app**: Evernote-style notebooks and notes, with
Todoist-style tasks and Quick Add. A note can hold the tasks that come out of it, so a
meeting's notes and its action items stay together. Everything stays on your phone:
there are no accounts, no ads and no internet access.

## Installing it

1. On your Android phone, open the
   [latest build](https://github.com/Akrishna87/daybook/releases/tag/daybook-latest)
   and tap **Daybook.apk** to download it.
2. Open the downloaded file. Android will ask you to allow installing apps
   from your browser (or Files app). Allow it, then tap **Install**.
   Google Play Protect may warn that it doesn't recognise the app, because
   it isn't from the Play Store. Tap **More details → Install anyway**.
3. Open **Daybook**.

To update, install a newer `Daybook.apk` the same way. It installs over the old one and
keeps everything. (Calendar events from the first version of Daybook become tasks with
the same date and time.)

## Tasks

- **Quick Add**: tap **+** and type a task in plain words. Daybook highlights what it
  understands as you type:

  | Type | For |
  |---|---|
  | `today`, `tomorrow`, `friday`, `next week`, `this weekend`, `in 3 days`, `dec 25`, `2026-12-25` | the due date |
  | `9am`, `6:30pm`, `21:00`, `at 5`, `noon`, `tonight` | the time (and a reminder) |
  | `p1` `p2` `p3` | priority, as in Todoist (red, orange, blue) |
  | `#Work` | an existing project |
  | `@errands` | labels (new ones are made as you go) |
  | `every day`, `every weekday`, `every monday`, `weekly`, `every month`, `yearly` | repeating tasks |
  | `for 6 weeks`, `for 10 days`, `for 3 months`, `until dec 31` | when a repeating task ends |

  For example: `Pay rent friday 9am p1 #Home @bills every month`, or
  `Gym every monday for 6 weeks`. The chips under the text set the same things by hand
  (including a **Starts** and an **End date** chip for repeating tasks), and the panel stays open for the next
  task. Tap the **microphone** to say the task instead of typing it.
- **Today** shows overdue tasks (with **Reschedule** to move them all to today), then
  today's tasks, and a progress bar for the day.
- **Upcoming is a calendar**, with four views at the top (Daybook remembers the one you used last):
  - **Month**: the whole month, with each day's tasks shown in the grid in their project's
    colour. Tap a day to see its full list of tasks underneath, tap it again (or **Day view**)
    to open that day, and long-press a day to add a task on it. Swipe or use the arrows to
    change month, and tap the month name to jump to any date.
  - **Week**: seven days side by side, with untimed tasks in an **All day** row and timed tasks
    placed at their hour. Tap a day's heading to open it.
  - **Day**: one day hour by hour, with a line at the current time. Swipe for the next or
    previous day.
  - **List**: the next four weeks day by day, with a week strip and a **+** on each day, and a
    heading per week: **Week 1 · This week**, **Week 2 · Next week**, **Week 3**, and so on.

  Repeating tasks show on every day they'll come round, not just their next date, and finished
  tasks stay on the day they were due, so past days show what you did. In Week and Day, tap an
  empty hour to add a task at that time; **+** adds one on the day you're looking at.
- **Inbox and projects**: tasks without a project go to the Inbox. Make projects with a
  colour in **Browse**. **Labels** collect tasks across projects.
- **Task details**: tap a task to change its title, description, date, time, repeat,
  priority, project and labels, switch its reminder on or off, and add **sub-tasks**.
- **Repeating tasks with a start and end date**: in Quick Add or a repeating task's details,
  set when it **Starts** and **Ends**. The start can be later on (the task is first due then)
  or in the past (the weeks count from then, and the task is next due on the first date in step
  with it, so a Monday meeting stays on Mondays). The task then shows which week of the run it's in, like
  **Week 2 of 6**, and stops coming round after the end date.
- **Swipe** a task right to complete it or left to delete it, with **Undo** either way.
  Every completed task is saved under **Completed** (in Browse, and **Completed today** at
  the bottom of Today). Completing a repeating task saves that occurrence, with its week, in
  Completed and moves the task to its next date; completing the last one before its end
  date finishes it for good.
- **Reminders** notify you at a task's time, with a **Complete** button on the
  notification. Android asks for notification permission the first time you add a task
  with a time.

## Notes

- **Notebooks**: every note lives in a notebook. Make, rename and delete notebooks from
  the Notes tab, and filter by notebook or **#tag**.
- **Writing**: notes open ready to read. Tap the text (or the pencil) to edit, with a
  toolbar for **checklists**, bulleted and numbered lists, headings, bold, italic,
  strikethrough and dividers. Lists carry on when you press enter, and pressing enter on
  an empty item ends the list. Notes save as you type.
- **Voice notes**: tap the microphone at the top of a note to record, then **Save**.
  Recordings are listed in the note to play back or delete, and the mic on the Notes tab
  starts a new voice note straight away. While editing, the mic in the toolbar types what
  you say into the note. Recordings stay on the phone and aren't part of backups.
- **Checklists** tick off right in the note, and the list shows progress like `2/5`.
- **Tasks from notes**: at the bottom of every note, **Add task** makes a task linked to
  the note. The task shows a note icon and opens the note from its details.
- **Pin** notes to the top, **sort** by date updated, date created or title, **share** a
  note as text, and **Trash** keeps deleted notes until you restore them or empty it.

## Everything else

- **Search** (the magnifier on any tab) finds tasks by title, description or label, and
  notes by title, text or tag. Tap the microphone to search by voice. (Speaking into
  search, Quick Add and notes uses the phone's speech recogniser, such as the Google app.)
- **Light and dark themes**, following the phone or chosen in Settings.
- **Backup**: Settings → Export backup saves everything to a JSON file, and Restore from
  backup reads one back.
- **Import**: Settings → Import tasks and notes adds the tasks, projects, notes and
  notebooks from a Daybook JSON file (such as a ready-made plan) without touching what you
  already have. Importing the same file twice doesn't add duplicates.
- **Home screen widget** ("Daybook: Today"): today's and overdue tasks. Tap a circle to
  complete a task, or + to open Quick Add.

## How it's built

Kotlin and Jetpack Compose with Material 3, and no libraries beyond AndroidX.

- `model/` holds the data and the logic, all plain Kotlin with unit tests:
  `QuickAdd.kt` reads plain-word tasks, `Dates.kt` handles due dates, repeats (with start
  and end dates and week numbers) and task order, `Agenda.kt` works out what's on each day of
  the calendar (including every occurrence of repeating tasks) and lays out the hours, and
  `Markdown.kt` parses notes and powers the editing toolbar.
- `Voice.kt` records and plays voice notes and asks the phone's speech recogniser for
  voice search and dictation.
- `Store.kt` keeps everything in memory and saves it to one JSON file, `Reminders.kt`
  schedules notifications, and `Backup.kt` exports and restores.
- `ui/` has the screens. `widget/TodayWidget.kt` is the home screen widget.
- `ci/smoke-test.sh` runs on an Android emulator for every build. It adds tasks with
  Quick Add, completes one, adds a sub-task and a project, writes a note with a checklist,
  goes through the calendar's
  month, week and day views, searches, records a voice note, completes a repeating task and checks it's in Completed
  with its week number, and switches to the dark theme. Screenshots from that run are attached to
  each release.

Builds are made by the [Daybook APK workflow](.github/workflows/daybook-apk.yml) on
every push to this repo. Builds of `main` are published as the latest release; other
branches keep their APK as a workflow artifact.
