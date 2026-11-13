# DailyToDoList — Product Requirements (canonical)

**This file is the single source of truth for the project.** Any session — human or AI —
working in this repository must read this file first and update the **Status matrix**
after changing behaviour. Do not keep requirements in chat history: if it is not in this
file, it is not agreed.

- Version: **1.1** (supersedes 1.0)
- Platform: Android, Java only (no Kotlin)
- Package: `com.example.dailytodolist`
- Min/target/compile SDK: 24 / 34 / 34
- Design language: Material 3, light + dark
- Languages: English (default), Simplified Chinese (`values-zh`)
- Last audited: 2026-11-07 against commit `ee481ea` + working tree

> **Status legend.** `DONE` = verified in code. `PARTIAL` = exists but misses a stated
> requirement. `OPEN` = not implemented. Every status carries a file reference so the
> next session can check it in seconds instead of re-auditing.

---

## 0. How sessions work in this repository

### 0.1 Ground rules

1. **Read this file before starting.** Re-read the Status matrix after finishing.
2. **One workstream per session.** Pick a stream from §4, do not "clean up" files owned
   by another stream in the same turn.
3. **Never claim a test passed unless it ran in this session.** Report the executed
   result, or say it was not run. A stale `app/build/test-results/` directory is
   evidence of a *past* run, not of a current one.
4. **Never regress an invariant in §3.** If a requirement and an invariant collide,
   stop and raise it — do not silently pick one.
5. **Update the matrix in the same change** that alters behaviour. A matrix row that
   disagrees with the code is a bug.

### 0.2 Build and test commands

The JBR bundled with Android Studio is broken on this machine (missing `lib/jvm.cfg`),
so Gradle needs the standalone JDK:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\openjdk\jdk-21.0.8'
$gradle = "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\bin\gradle.bat"
& $gradle --offline :app:testDebugUnitTest --console=plain   # unit tests
& $gradle --offline :app:assembleDebug --console=plain       # build
```

**Sandbox caveat (currently blocking).** Gradle needs to write
`%USERPROFILE%\.gradle\native\*\windows-amd64\native-platform.dll.lock`. Under the
default workspace-write sandbox that write is denied and every Gradle invocation fails
with `Failed to load native library 'native-platform.dll'` /
`Access is denied`. Overriding `TMP`, `-Dorg.gradle.native.dir`, `--no-daemon` or
`GRADLE_USER_HOME` does **not** help: the lock path is derived from the user home.
Running a build therefore requires a one-time `danger-full-access` approval. If a
session cannot get it, it must say "tests not run this session" rather than quote the
matrix.

`scripts/build.ps1` documents the same JDK workaround but cannot be executed directly on
this machine (execution policy: unsigned script). Invoke the Gradle command line above,
or run the script with `-ExecutionPolicy Bypass`.

### 0.3 Verification of the 123-test claim

Last recorded run: `app/build/test-results/testDebugUnitTest` holds 9 suites,
**123 tests, 0 failures, 0 errors**, written 2026-11-07 13:16. That satisfies the
"≥100 passing" bar for the *last* run, but it is not proof for the current tree, because
several source files were modified after that timestamp.

---

## 1. Product vision (1.0, unchanged)

A frictionless daily planner that combines a plain To-Do list with **time blocking**:
users do not merely list tasks, they place them on the day and protect focus time.
Audience: students, professionals, and anyone who wants a pleasant daily planner.

---

## 2. Functional requirements and status

### 2.1 Task management (list view)

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 3.1.1 | Create task with title, optional description, category | `DONE` | `AddTaskActivity`: `EXTRA_TITLE`, `EXTRA_DESCRIPTION`, `EXTRA_CATEGORY`; `TaskWithDate.description` |
| 3.1.2 | Colour-coded categories | `DONE` | `TimeCategory.getColorRes()` → `R.color.block_*`; `values/colors.xml` + `values-night/colors.xml` |
| 3.1.3 | Complete task via checkbox, strike-through | `DONE` | `MainActivity` strike-through flags on `taskText` |
| 3.1.4 | Delete with bottom-sheet confirmation | `DONE` | `MainActivity.showDeleteConfirmation()` → `BottomSheetDialog` |
| 3.1.5 | Completing awards +5 coins | `DONE` with defect | `MainActivity` (~L625) guards with `wasCompleted`; `CalendarActivity` (~L561) does **not** — see 7.5 |

### 2.2 Time blocking (timeline)

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 3.2.1 | Segmented List / Timeline toggle | `DONE` | `MaterialButtonToggleGroup` + `setTimelineView()` |
| 3.2.2 | Hourly grid 06:00–23:00 | `DONE` | `TimelineSlots.DEFAULT_FIRST_HOUR/LAST_HOUR` |
| 3.2.3 | Scheduled tasks appear as blocks in their hour | `DONE` | `TimelineSlots.add()` buckets on `startMinute / 60` |
| 3.2.4 | Out-of-window tasks clamped, never dropped | `DONE` | same method clamps index to `[0, slotCount-1]`; storage untouched |
| 3.2.5 | Same-hour tasks stacked | `DONE` | bucket is a `List<TimeBlockRow>`; `TimelineAdapter` inflates one block per task |
| 3.2.6 | All 18 rows 06:00–23:00 render inside the outer `ScrollView` | `DONE` | `WrapHeightRecyclerView` measures against `UNSPECIFIED`; verified on emulator: 23:00 visible above the bottom nav |
| 3.2.7 | No empty-state block in the Timeline view | `DONE` | `setTimelineView(true)` no longer touches `noTasksLayout`; the grid is a fixed frame and starts directly below the toggle |
| 3.2.8 | Muted inline hint when the day has no scheduled tasks | `DONE` | `timelineEmptyHint` above the grid; shown on `scheduled.isEmpty()`, grid stays visible below |
| 3.2.9 | Horizontal week day selector above the grid | `DONE` | `daySelectorRecyclerView` + `DaySelectorAdapter`; `WeekDay.weekOf()` builds Monday-first week |
| 3.2.10 | Selecting a day reloads the grid for that date | `DONE` | `OnDaySelectedListener` → `renderTimelineForSelectedDate()` → `TaskStore.loadScheduledForDate()` |
| 3.2.11 | Defaults to today on entering Timeline | `DONE` | `setTimelineView(true)` resets `selectedDateIso = todayIso()` then `bindDaySelector()` |
| 3.2.12 | Past blocks are visually dimmed | `DONE` | `TimelineEdits.isPast()` + `TimelineAdapter.PAST_ALPHA` (0.5); compared against the block's **end** minute, so a block still in progress is not dimmed |
| 3.2.13 | Swipe LEFT/RIGHT a block to delete it | `DONE` | `ItemTouchHelper.SimpleCallback(UP\|DOWN, LEFT\|RIGHT)` → `onSwiped()` resolves the row's task via `TimelineAdapter.taskAt()` → `confirmDeleteTask()` → `deleteTaskFromTimeline()`, which removes **by UUID**, not by index. The row is restored immediately so a cancelled dialog leaves no half-swiped block |
| 3.2.14 | Drag any block UP/DOWN to reschedule it | `DONE` | Long-press starts `startDrag()` for **past and future** blocks alike (the 50% alpha is visual only, never a gesture gate). `ItemTouchHelper` in `MainActivity.setupTimelineDrag()`; `onMove()` records the target hour, `clearView()` applies the delta. Rows are never reordered: the grid stays a fixed 06:00–23:00 frame |
| 3.2.15 | Empty hours are never dragged | `DONE` | `isLongPressDragEnabled()` returns false; drag starts only from a block's long-press, so an empty row has no entry point. Empty hours **are** valid drop targets |
| 3.2.16 | Drag preserves duration and stays inside the day | `DONE` | `TimelineEdits.shiftByHours()` clamps to `[0, 1440-duration]`; 13 tests in `TimelineEditsTest` |
| 3.2.17 | Rescheduling re-arms the reminder | `DONE` | `applyDraggedHour()` calls `ReminderScheduler.schedule()` after `saveTasks()` |

> **Layout landmine (3.2.6).** `android:layout_height="wrap_content"` plus
> `nestedScrollingEnabled="false"` on a RecyclerView nested in a `ScrollView` is **not
> enough**. A vertical `LinearLayout` passes an `AT_MOST` height spec, and RecyclerView's
> auto-measure clamps the result to it, so the grid silently truncates at whatever hour
> the viewport reaches (was ~14:00). `WrapHeightRecyclerView` overrides `onMeasure` to
> measure with `UNSPECIFIED`. Equally, the `tasksContainer` must not be `0dp` +
> `layout_weight="1"` inside a `wrap_content` card — that squeeze was the original
> five-hour cutoff. Only use this for short, fixed-length lists: `UNSPECIFIED` forces
> every row to be laid out.

### 2.3 Anytime tasks (backlog)

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 3.3.1 | Toggle "schedule specific time" off | `DONE` | `switchScheduleTime` in `activity_add_task.xml` |
| 3.3.2 | Stored with time `-1` | `DONE` | `TaskWithDate.NO_TIME`; `TaskStore` preserves it |
| 3.3.3 | List view hides the time text | `DONE` | `MainActivity` (~L573) sets `taskTimeText` `GONE` |
| 3.3.4 | Horizontal chips in Backlog above the grid | `DONE` | `item_backlog_task.xml`; `loadAnytimeForDate()` |
| 3.3.5 | Long-press a backlog chip and drop it on an hour to schedule it | `DONE` (verified on emulator) | Chip: `MainActivity.bindBacklog()` `OnLongClickListener` → `ClipData.newPlainText("task_id", id)` + `startDragAndDrop()`. Grid: `TimelineAdapter.SlotHolder.attachDropListener()` on `slotBlocks` (STARTED/ENTERED/EXITED/DROP) → `Listener.onTaskDropped(taskId, hour)` → `MainActivity.scheduleTaskAtHour()`. Emulator `emulator-5554` (API 36, 1440×3200): Anytime task dragged to the 11:00 row produced a `11:00 - 12:00` block, the backlog chip disappeared, and the state survived a cold restart |
| 3.3.6 | Hovering a row during a cross-view drag highlights it | `DONE` (verified on emulator) | `SlotHolder.hoverBackground` (accent at ~15% alpha, built once per holder); `ACTION_DRAG_ENTERED`/`EXITED` swap it, `bind()` restores idle. Confirmed by screenshotting mid-drag with the finger held over the 11:00 row |

### 2.3a Time block reminders

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 3.3a.1 | POST_NOTIFICATIONS + SCHEDULE_EXACT_ALARM declared | `DONE` | `AndroidManifest.xml`; runtime request via `AddTaskActivity.requestNotificationPermissionOnce()` |
| 3.3a.2 | Notification channel created | `DONE` | `ReminderScheduler.ensureChannel()`, id `time_block_reminders` |
| 3.3a.3 | Exact alarm 5 minutes before the block start | `DONE` | `ReminderScheduler.schedule()`; `setExactAndAllowWhileIdle`, falls back to inexact when `canScheduleExactAlarms()` is false |
| 3.3a.4 | Notification names the task and uses its category colour | `DONE` | `TaskReminderReceiver`; `builder.setColor(getColor(colorRes))` |
| 3.3a.5 | Delete or complete cancels the alarm | `DONE` | `MainActivity` delete + checkbox handlers call `ReminderScheduler.cancel()` |
| 3.3a.6 | Alarms survive a reboot | `DONE` | `BootReminderReceiver` re-arms from `TaskStore.loadAll()` |

> **Identity landmine (3.3a.3).** `TaskWithDate` mints a fresh UUID in its constructor, so
> the task `AddTaskActivity` arms an alarm for is *not* the same object `MainActivity`
> stores. The id is therefore handed forward in `EXTRA_TASK_ID` and adopted with
> `restoreId()`. Without that, cancelling on delete would silently miss and the alarm
> would fire for a deleted task.
>
> **Anytime tasks never get an alarm.** `startMinute == NO_TIME` is `-1`, and
> `-1 / 60 == 0` would schedule a 00:00 alarm; `schedule()` checks `isAnytime()` first.
> A task whose warning time has already passed is unscheduled rather than fired at once.

> **Landmine.** In Java `-1 / 60 == 0`, so any code that derives the hour from
> `startMinute` must test `isAnytime()` **first**, or an Anytime task will masquerade as
> a 00:00 task. A genuine 00:00 task must not be confused with the sentinel either.

### 2.4 Top 3 priorities

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 3.4.1 | Mark as Top Priority during creation | `DONE` | `checkTopPriority` → `EXTRA_TOP_PRIORITY` |
| 3.4.2 | Home section above the view toggle, max 3 | `DONE` | `prioritiesSection` precedes `viewToggleGroup` in `activity_main.xml`; `MAX_TOP_PRIORITIES = 3` |
| 3.4.3 | Accent border + ★ star | `DONE` | `item_priority_task.xml` (`★`), `setStrokeColor(accentColor)` |
| 3.4.4 | Anytime priority shows category, not a fake time | `DONE` | `MainActivity.bindPriorities()` → `getLabelRes()` |
| 3.4.5 | Prompt to replace when 3 slots are full | `OPEN` | cap is enforced by *truncation at display time* only — see 7.4 |

### 2.5 Theming and personalisation

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 3.5.1 | Follow the system light/dark setting | `DONE` (v1.1 upgraded) | `ThemeManager.getNightMode()` defaults to `MODE_NIGHT_FOLLOW_SYSTEM`; `setDefaultNightMode()`; tri-state selector `nightModeGroup` in `activity_profile.xml` |
| 3.5.2 | Theme store spends coins to unlock gradients | `DONE` | `ShopActivity.purchaseTheme()`; `theme_owned_<id>` persisted |
| 3.5.3 | No hardcoded hex in UI | `DONE` | 0 hex literals in layouts and UI components; colour now flows through Material 3 role attributes, `ThemeColors`, and night-qualified semantic `@color` resources — see 7.6 |

The legacy boolean `dark_mode` has been fully replaced by the tri-state `night_mode`
preference; zero `"dark_mode"` references remain. Do not reintroduce a boolean: it cannot
express "follow the system".

### 2.6 Architecture and storage

| # | Requirement | Status | Evidence |
|---|---|---|---|
| 4.1 | Java only | `DONE` | no Kotlin sources |
| 4.2 | SharedPreferences, UUID-keyed (`task.<uuid>.field`) | `DONE` | `util/TaskStore.java` |
| 4.3 | One-time migration with backup | `DONE` | `migrateIfNeeded()`, `tasks.migration_v2_done`, `tasks.legacy_backup` |
| 4.4 | MaterialTimePicker | `DONE` | `AddTaskActivity` + `values/styles_timepicker.xml` |
| 4.5 | MaterialAlertDialog for dialogs | `OPEN` | several screens still use `AlertDialog.Builder` |
| 4.6 | >100 unit tests | `DONE` for last run | 123 tests, see §0.3 |

> **Second storage format (technical debt, not yet a bug).**
> `CalendarActivity` keeps its own pipe-delimited blob under `calendar_tasks`
> (`date|name|time|isCompleted||`), completely separate from `TaskStore`. The two stores
> never reconcile. Any feature that must see "all tasks" (7.2 daily planning) has to
> decide explicitly whether the calendar participates — do not let a session silently
> merge or drop it.

---

## 3. Invariants every change must preserve

1. **Identity is the UUID.** `TaskWithDate.id` is stable for the life of the task. Editing
   must never mint a new id.
2. **No positional addressing.** Never store or look up a task by list index. Positional
   keys were the original data-corruption bug (see the `TaskStore` class comment).
3. **Write through one transaction.** `TaskStore.saveAll()` writes everything in a single
   `SharedPreferences.Editor` so a crash cannot persist half a list.
4. **`-1` means Anytime.** Never coerce it to `0`, never clamp it to a time.
5. **Display never mutates storage.** Timeline clamping and bucket placement are view
   concerns. Rendering a task outside 06:00–23:00 must not rewrite its times.
6. **Migration is idempotent and reversible.** `migrateIfNeeded()` returns early on the
   flag; legacy keys stay on disk; a backup blob is written before new keys.
7. **Coins are awarded at most once per task, ever.** Persist the fact on the task, not in
   view state — see 7.5.
8. **No `recreate()` loops and no hardcoded colours in new code.** New UI uses theme
   attributes (`?attr/colorSurface`, `?attr/colorOnSurface`, …) and colour resources.

---

## 4. Version 1.1 requirements — the current work front

This section is additive to 1.0. Where 1.1 contradicts 1.0, **1.1 wins**.

### 4.1 Task editing — `OPEN` (largest gap)

- Edit title, description, category, date, and schedule of an existing task.
- Switch a task between Scheduled and Anytime.
- **Preserve the original UUID.**
- Editing or completing must not mint extra coins.

**Current state.** `AddTaskActivity` is create-only: it has no `EXTRA_TASK_ID`, no edit
mode, and `MainActivity` has no edit entry point (no long-press, no edit affordance on the
row). `description` exists in the model, the store, and the create form, but a task can
never have it changed after creation.

**Implementation notes.**

- Prefer reusing `AddTaskActivity` with an optional `EXTRA_TASK_ID` over a second
  activity: the pickers, validation and category spinner already exist.
- On save in edit mode, mutate the existing object and call `TaskStore.saveAll()`; do not
  `remove` + `add`, because `saveAll(List)` deletes any id absent from the incoming list.
- Switching to Anytime means `startMinute = endMinute = NO_TIME`; switching back needs a
  fresh valid interval (default 09:00–10:00 is fine) — never persist `-1` as a real time.

**Acceptance.**
- Editing a title leaves `id`, coin history, and every other task untouched.
- Editing survives app restart.
- Scheduled↔Anytime round-trip returns to a consistent state in list, timeline, backlog.

### 4.2 Daily planning — `OPEN`

- Every task belongs to a specific date (already true: `TaskWithDate.dateIso`).
- **The Home list shows only the selected date.**
- Users navigate to previous/next date and return to Today.
- List, Timeline, Backlog and Top 3 all render the *same* selected date.
- New tasks default to the selected date.

**Current state.** There is no selected-date state. `MainActivity` has no
`selectedDate` field and `activity_main.xml` has no date navigation — `dateCard` / `dateText`
are a static display. Everything time-related hardcodes **today**:

- list: `displayTasks()` iterates all of `tasks` (`store.loadAll()`) with **no date
  filter**, so the list shows tasks from every date at once;
- timeline: `TaskStore.loadScheduledForDate(todayIso())`;
- backlog: `loadAnytimeForToday(todayIso())`;
- Top 3: `loadTopPriorities()` compares against `todayIso()`;
- new task: `AddTaskActivity` has a `MaterialDatePicker` (`btnSelectDate`, defaults to "Today") and stamps `EXTRA_DATE_ISO` with the **selected** date. If Today is selected with a time that has already passed, a Toast + confirm dialog offers to schedule for tomorrow instead. (The Home surfaces still hardcode today — that is the remaining 4.2 gap.)

So of the 1.1 requirements only "every task belongs to a date" currently holds.

**Implementation notes.** Introduce one `selectedDate` field in `MainActivity` and route
all four surfaces through it; the intent-extra plumbing for dates already exists
(`EXTRA_DATE_ISO`, `fromMinuteOfDay(title, description, dateIso, …)`). Persisting the
selection is optional; defaulting to today on cold start satisfies "return to Today".

**Acceptance.**
- Selecting tomorrow hides today's tasks in **all four** surfaces simultaneously.
- A task created while tomorrow is selected lands on tomorrow.
- "Today" restores the current date.
- Navigating dates does not modify any task.

### 4.3 Scheduling validation — `PARTIAL`

| Sub-requirement | Status | Notes |
|---|---|---|
| End later than start | `DONE` | `AddTaskActivity` rejects `endMinute <= startMinute` |
| No zero/negative duration | `DONE` | same guard |
| No cross-midnight tasks | `DONE` (implicitly) | minutes are 0..1439 within one day; `endMinute` may reach 1440 |
| Timeline visibility never rewrites times | `DONE` | `TimelineSlots` clamps only its own index |
| Out-of-window tasks stay accessible with real times | `DONE` | block shows `getTimeRangeLabel()`; clamped block still shows `05:30–06:30` |
| Same-hour tasks individually accessible | `DONE` | stacked blocks, each with its own title/time |

**Gap.** `IntervalRules` (validation + overlap + dangling-block rules) is fully
implemented and tested but **not wired into the UI** — `AddTaskActivity` mentions it only
in a comment. The save path uses a hand-rolled `endMinute <= startMinute` check. Wire the
shared rules in so UI and tests cannot drift apart.

Note the `endMinute` boundary: `IntervalRules` allows `endMinute` up to **1440**
(24:00). Decide deliberately whether a 23:30–24:00 task is valid ("does not cross
midnight") and make the rule explicit rather than incidental.

### 4.4 Top 3 rules — `PARTIAL`

| Sub-requirement | Status | Notes |
|---|---|---|
| Max 3 priorities per day | `PARTIAL` | enforced only by truncating the display (`MAX_TOP_PRIORITIES`) |
| Prompt to replace when full | `OPEN` | none |
| Priority independent of completion | `DONE` | `loadTopPriorities()` filters on `isTopPriority` + date only |
| Priority tasks belong to the selected date | `PARTIAL` | correct logic, but against hardcoded *today* (see 4.2) |

**Gap.** Because the cap lives in the renderer, storage can hold four flagged tasks; the
fourth simply never appears — silently losing the user's intent. The rule must be enforced
at the write path (on save/create), with a replace prompt (§7.4 of 1.1). Reusable pattern:
serve the "replace which one?" choice with a Material dialog listing the current three.

### 4.5 Gamification integrity — `PARTIAL`, and the most user-visible defect

| Sub-requirement | Status | Notes |
|---|---|---|
| First completion awards exactly 5 | `DONE` | `MainActivity` (~L625) |
| Re-completing awards nothing | `PARTIAL` | `MainActivity` guards with `wasCompleted`… |
| …same rule everywhere | `OPEN` | **`CalendarActivity` (~L561) awards +5 on every check** |
| Deleting a completed task refunds nothing | `DONE` | delete path never touches coins |
| Balance cannot go negative | `DONE` | `ShopActivity` requires `currentCoins >= price` |
| Purchased themes survive restart | `DONE` | persisted as `theme_owned_<id>` |

**Two defects.**

1. `CalendarActivity` has no `wasCompleted` guard, so uncheck→check there pays out **again**.
   Combined with `MainActivity`'s guard this makes the same task pay 5 or 10 depending on
   the screen used.
2. The guard is **view state**, not persisted. `wasCompleted` only protects a single tap
   within one live view. Uncheck, go to the calendar, come back, check again → rewarded
   twice. Restarting the app also resets nothing but the guard never existed on disk.

**Fix direction.** Add a persisted per-task flag (e.g. `rewarded` / `coins_awarded`,
mirroring the `.description` key pattern in `TaskStore`) and make award decisions from it.
Deleting a task may drop the flag with the task — the coins already banked stay banked
(that is the required behaviour). Centralise the award in one helper so both screens
cannot diverge again.

### 4.6 Theme system — `PARTIAL`

| Sub-requirement | Status | Notes |
|---|---|---|
| Default follows the system | `DONE` | `MODE_NIGHT_FOLLOW_SYSTEM` default |
| User may pick System / Light / Dark | `DONE` | tri-state selector in `activity_profile.xml`, `ThemeManager.saveNightMode()` |
| All colours via semantic attributes / resources | `DONE` | 0 hex literals in any layout or UI component; see 7.6 |
| No hardcoded colour in UI components | `DONE` | same |
| Purchased themes work in light and dark | `DONE` | 10 themes × 2 modes; surfaces, text and gradients all resolve from the active style |

**Hot spots** — cleared. Every layout, activity and adapter now resolves colour
from the active theme: layouts use `?attr/color*`, Java uses `ThemeColors` /
`MaterialColors`, and semantic status colours come from night-qualified
`@color` resources. The only remaining `0x…` constants are alpha masks in
`GradientContrast` and `TimelineAdapter`'s ripple maths, plus
`MaterialColors.getColor(..., fallback)` last-resort arguments — algorithm
constants, not UI colour choices.

**Policy for new code.** Move a colour to `colors.xml` (plus `values-night/colors.xml`
when it must differ) and read it with `ContextCompat.getColor` or a theme attribute.
Contrast decisions (white vs dark text on a category fill) belong in one helper.
`util/GradientContrast` is that helper for gradient backgrounds (WCAG relative
luminance, 10 tests in `GradientContrastTest`); `TimelineAdapter.isDark()` is an older,
simpler variant for a single fill colour and should be migrated to it rather than
extended.

### 4.7 Localization — `PARTIAL`

| Sub-requirement | Status | Notes |
|---|---|---|
| English + Simplified Chinese for all user-facing text | `PARTIAL` | 215 / 215 string keys match exactly (was 198 / 198 before the five timeline-interaction strings) |
| All strings in resources | `OPEN` | **129** hardcoded `android:text="…"` in layouts |
| Resource key sets match | `DONE` | verified equal, both directions |
| Date/time respect device locale | `PARTIAL` | `TimeCategory.formatMinuteOfDay()` is deliberately `Locale.US` 24-hour (canonical, not display-localised); `todayIso()` is `Locale.US` for storage (correct) |

Keep the 24-hour canonical format for **storage and comparison**, and localise only what
is shown. Any change here must keep `dateIso` parseable by `IntervalRules.isValidDateIso`.

### 4.8 Accessibility and usability — `OPEN`

| Sub-requirement | Status | Evidence |
|---|---|---|
| Meaningful accessibility descriptions | `OPEN` | **0** `contentDescription` attributes across 39 layouts; **101** clickable nodes |
| Category not conveyed by colour alone | `PARTIAL` | backlog and priority chips print `getLabelRes()`; plain list rows use only the tint |
| Readable at larger system font sizes | `PARTIAL` | no `dp` text sizes (good), but many rows use fixed heights / `maxLines` |
| Adequate touch targets | `PARTIAL` | only **3** `minHeight` declarations in all layouts; target ≥48dp |
| Clear empty states and validation errors | `PARTIAL` | empty states exist; `taskNameLayout.setError` and `durationText` cover title/interval only |

Add `contentDescription` to every icon-only control (back buttons, delete, edit, star),
and add a visible category label to list rows so colour is never the only channel.

### 4.9 Data integrity — `DONE` (keep it that way)

- Stable UUID per task — `UuidUtil`, `TaskWithDate.ensureId()`.
- Updating one task cannot corrupt others — `task.<uuid>.*` keys, no positional writes.
- Migration idempotent + legacy backup — `migrateIfNeeded()`, `tasks.legacy_backup`.
- Failed migration does not destroy originals — legacy keys are never deleted.
- Core task management works offline — SharedPreferences only; Firebase is auth-only.

**Known risk to watch.** `CalendarActivity` still uses the legacy pipe-delimited
`calendar_tasks` blob, where a `|` inside a task name corrupts the parse. If calendar
data is brought into scope, migrate it to `TaskStore` rather than parsing harder.

### 4.10 Testing and acceptance — `PARTIAL`

| Sub-requirement | Status |
|---|---|
| ≥100 passing unit tests | `DONE` (123 in last recorded run) |
| Cover create, edit, complete, delete, storage, migration, scheduling, priority, coins | `OPEN` — no tests at all for `TaskStore`, migration, coins, or priority |
| Boundary tests: 06:00, 23:00, Anytime (-1), invalid intervals, overlaps | `PARTIAL` — `TimelineSlotsTest`, `TaskWithDateAnytimeTest`, `IntervalRulesTest` cover boundaries and overlaps; `AddTaskActivity` is not unit-testable (`Activity`) |
| English/Chinese switching | `OPEN` — key parity checked manually; no test |
| System/Light/Dark behaviour | `OPEN` |
| Full suite run after each phase | process rule |
| Do not report unexecuted tests as passing | process rule |

**Highest-value missing tests** (pure logic, no emulator, cheap to add):

1. `TaskStore` — write/read round trip, delete-one-leaves-others, `saveAll` drops absent ids, migration idempotency, backup written once.
2. Coin policy — extract the award decision into a pure helper (`shouldAward(wasChecked, nowChecked, alreadyRewarded)`) and table-test it, including the Anytime and edit cases.
3. Top-3 policy — a pure "can this become a priority / which to replace" rule.
4. Daily selection — a pure "tasks for date" filter.

Keep new logic in `model/` or `util/` as plain Java precisely so it is testable without an
emulator; that is why `TimelineSlots` and `IntervalRules` live there.

---

## 5. Version 2.0 — explicitly out of scope for 1.1

Do not implement these while 1.1 is open, and do not remove their extension points.

- Daily energy tracking and reflection.
- Push notifications for upcoming time blocks.
- Cloud backup and synchronisation (Firebase / Drive).
- Recurring tasks.
- Optional AI-assisted daily scheduling.
- Calendar integration.

Already landed ahead of schedule (treat as 2.0 work in progress, keep it isolated):
`ReviewActivity`, `ReviewStore`, `DailyReview`, `activity_review.xml`,
`item_energy_row.xml`, and 11 tests in `DailyReviewTest`. It is registered in
`AndroidManifest.xml` under "End of day review". Any 1.1 work must not break it, and it
must not be extended further until 1.1 closes.

The AI auto-schedule flow is a second 2.0 preview: `AIService` (plain
`HttpURLConnection` + Gson, no Retrofit dependency), `util/AutoScheduleMapper` (the
matching/validation rules, plain Java, 12 tests in `AutoScheduleMapperTest`), the
`btnAutoSchedule` trigger in `activity_main.xml`, and the wiring in `MainActivity`
(worker thread + `runOnUiThread`, `TaskStore` save, reminder re-arm). It talks to a
local Python endpoint at `10.0.2.2:8000/ai-schedule` that is **not part of this repo**,
so the feature is inert until someone runs that server. It is a preview of §5, not 1.1
work: do not extend it further until 1.1 closes. `AIService` catches Gson
`JsonSyntaxException` (e.g. `Expected BEGIN_ARRAY` when the model returns a single object
instead of a list), logs the raw response body under tag `AIService`, and rethrows as
`IOException` so the existing failure Toast handles it. No user-facing strings added.

**AI Q&A chat ("DailyToDo-AI") is a third 2.0 preview**, landed on direct request
while 1.1 is still open (same "ahead of schedule" status as `ReviewActivity` above).
`AIService.ask(question, contextData)` POSTs `{"question":...,"context_data":...}`
to `10.0.2.2:8000/ai-chat` and returns the `answer` string; the shared
`postJson()` helper also serves `/ai-schedule`, with a 60 s read timeout for chat
because local LLM inference is slow. `ChatActivity` + `ChatAdapter` + the two chat
layouts render user bubbles on the right / AI bubbles on the left, and the context
is rebuilt on **every send** from `TaskStore.loadAll()`: one line per task with
date, time (`Anytime` when `isAnytime()` — never a fake 00:00), category label,
priority and completion. 10 new string keys were added to **both** locale files
(208 / 208 parity). The Home entry is `btnAskAi` in the date card header. No
hardcoded colours in the new UI: bubble fills resolve through `MaterialColors`.
Like the other previews, it is inert until the Python server runs, and must not be
extended further until 1.1 closes.

**Chat "add task" actions.** `/ai-chat` may return an action describing a task to
create, alongside `answer`. `AIService.ask()` now returns a `ChatResponse` carrying
both `answer` and an optional `ChatAction` (`task_name`, `start_hour`, `end_hour`,
`category`, `top_priority`, `date`). Both a nested `action` object and flat
top-level fields are accepted; a missing hour defaults to the `NO_TIME` sentinel,
never 0. `ChatActivity.applyAction()` is the single place that builds the
`TaskWithDate`: **`start_hour == -1` passes `NO_TIME` straight to
`fromMinuteOfDay`**, which clears `hasTime` and sets both minutes to -1 — so the
task lands in the Backlog and the list hides its time text (§3 invariant: `-1`
means Anytime, never coerced to 0). A real hour becomes `hour * 60`; out-of-range
hours fall back to Anytime rather than a broken interval. Saves are
read-modify-write through `TaskStore.saveAll` (UUID identity preserved), and only
scheduled tasks get a reminder. 2 new string keys in both locales (210 / 210).

---

## 6. Recommended work order for 1.1

Sequenced to unblock others and keep write scopes disjoint:

1. **Daily planning (§4.2)** — touches `MainActivity`, `AddTaskActivity`,
   `activity_main.xml`, strings. Everything else (Top 3 per selected date, editing dates)
   depends on this state existing, so do it first.
2. **Coin integrity (§4.5)** — touches `TaskStore` (new persisted flag), `MainActivity`,
   `CalendarActivity`, plus new tests. Small diff, removes a real over-payment bug.
3. **Task editing (§4.1)** — touches `AddTaskActivity`, `MainActivity`, `task_item.xml`.
   Build on (1) and (2) so the editor inherits date selection and safe coin handling.
4. **Top 3 write-path enforcement (§4.4)** — small, after (1).
5. **Validation wiring (§4.3)** — route save through `IntervalRules`.
6. **Accessibility (§4.8)** and **localization (§4.7)** — broad but mechanical.
7. **Hardcoded colours (§4.6)** — largest mechanical change; do it last, file by file,
   and never in the same turn as another stream.

Files are the collision surface. Suggested ownership for parallel sessions:

| Stream | Primary files |
|---|---|
| Daily planning | `MainActivity`, `AddTaskActivity`, `activity_main.xml` |
| Coins | `util/TaskStore`, `CalendarActivity`, new test files |
| Editing | `AddTaskActivity`, `task_item.xml`, `activity_add_task.xml` |
| Colours | `res/values*/colors.xml`, `res/values*/themes.xml`, one screen at a time |
| A11y/i18n | `res/layout/*`, `res/values*/strings.xml` |

`MainActivity.java` is the hot file — only one stream should edit it at a time.

---

## 7. Definition of done for 1.1

- Every `OPEN`/`PARTIAL` row in §2 and §4 is `DONE`, or explicitly deferred with a reason
  recorded here.
- `./gradlew :app:testDebugUnitTest` executed **in the closing session**, ≥100 tests, 0
  failures, and the count reported with the command's own output.
- New tests cover store/migration/coins/priority/date-selection (§4.10).
- No regression in §3 invariants.
- This Status matrix updated to match reality.

---

## 8. Changelog

| Version | Date | Change |
|---|---|---|
| 1.0 | — | Initial PRD: list + timeline, categories, Anytime, Top 3, theming, UUID storage, 100+ tests |
| 1.1 | 2026-11-07 | Added §4 requirements: task editing, daily planning, scheduling validation, Top 3 rules, gamification integrity, theme system (System/Light/Dark), localization, accessibility, data integrity, testing. Consolidated 1.0 + 1.1 into this canonical file with an evidence-backed status matrix. |
| 1.1 | 2026-11-11 | Added chat "add task" action handling: `AIService.ask()` now returns `ChatResponse` (answer + optional `ChatAction`), tolerating nested `action` objects and flat fields; `ChatActivity.applyAction()` maps `start_hour == -1` → `TaskWithDate.NO_TIME` via `fromMinuteOfDay` (Anytime → Backlog, time text hidden) and real hours → `hour*60` scheduled tasks, with read-modify-write `TaskStore.saveAll` and reminder only for scheduled tasks. 2 new EN/ZH strings (210/210). Verified by standalone `javac` + Gson parsing/conversion test covering both "write homework" (Anytime) and "9am" (Scheduled) cases; Gradle suite not run this session. |
| 1.1 | 2026-11-12 | **Redesigned the Timeline from a rigid table grid into a vertical spine, and removed the "TIME \| THU" band.** `item_timeline_slot.xml` is now a `ConstraintLayout`: the hour label sits top-left, a 1dp `?attr/colorOutlineVariant` `View` runs the full row height so consecutive rows join into one continuous line, a 9dp circular dot (`drawable/bg_timeline_dot.xml`, tinted `?attr/colorOnSurfaceVariant` — `colorPrimary` is too bright on several themes and this is a decorative marker, not a control) is centred on the spine at the top of the hour, and the task area is the `slotBlocks` container to the right. `item_timeline_block.xml` became a 12dp `MaterialCardView` holding a category dot, a bold title, the time range and a category pill on the far right; the pill fill is the category colour at `0x2E` alpha and its **label uses `getTextColorRes()` rather than `getColorRes()`**, because the fill ramp is not text-safe (Break `#7ED321` measures 1.87:1 on a light card). `TimelineAdapter.createBlockView()` now binds four views instead of two and builds `GradientDrawable`s for the dot and pill instead of one solid block background; the `slotTimeLabel` / `slotBlocks` / `blockTitle` / `blockTimeRange` / `blockDescription` ids are unchanged, so the drag contract and the `ItemTouchHelper` wiring still hold. **Two layout bugs found and fixed on device:** (a) the hour label was rendering `TimelineSlots.getSlotLabel()`'s `06:00 - 07:00` range and wrapping to two lines, so the adapter now formats its own single-tick `hourLabel()` — `getSlotLabel` itself is untouched because six model tests pin it; (b) every card collapsed to a 28px sliver because the inner `ConstraintLayout` had no bottom constraint to terminate its vertical chain, fixed by pinning `blockDescription` (GONE still resolves a chain) to the parent bottom. **`colorSurfaceVariant` was the wrong card fill:** it is `#F0F0F0`, only 15/255 from the white card behind it, so cards and unselected day pills both disappeared; cards now use `?attr/colorSurface` + a 1dp `?attr/colorOutline` hairline, and the day chips are `colorSurface` + outline with an 18dp radius. `activity_main.xml` gained a `FrameLayout` wrapper around the `ScrollView` so a new `@+id/fabAddTask` `FloatingActionButton` can float bottom-right and open `AddTaskActivity`; it is visible only in Timeline view and its fill/icon come from `GradientHelper.colorsFor()` + `GradientContrast.onColorFor()`, so a light accent (sunset `#FFE53B`) gets a dark plus. `bindTimelineHeader()` now formats `EEE, MMM d` through the locale's `SimpleDateFormat` ("Thu, Nov 12", "周四, 11月 12") instead of `WEEKDAY_LABELS`, and the header pill has a calendar glyph (`ic_calendar_small`, `ic_add`) — both vectors use a white placeholder fill overridden by `app:tint`. 0 new strings (the FAB reuses `add_task_title`), so strings stay **226 / 226**, diff 0; 0 hex literals in the new layouts. Verified this session with the standalone JDK 21: `:app:assembleDebug` BUILD SUCCESSFUL and `:app:testDebugUnitTest` → **183 tests, 0 failures, 0 errors** across 13 suites. On `emulator-5554`: the spine, dots, single-tick hour labels, "Thu, Nov 12" pill, category dot/title/time/pill card and the FAB all render (screenshots); the FAB tap opens Add New Task; and **drag-to-reschedule still works end to end** — `input draganddrop` moved `study` from `start=900` (15:00) to `840` (14:00) and back to `900`, each step confirmed by reading `shared_prefs/TodoAppPrefs.xml`. |
| 1.1 | 2026-11-12 | **Added a pre-permission dialog in front of the Android 13+ POST_NOTIFICATIONS prompt.** `AddTaskActivity` previously called `ActivityCompat.requestPermissions()` directly from the save path, which spent one of the OS's two lifetime prompts without explaining what it bought. New `res/layout/dialog_permission.xml` is a 24dp-cornered `MaterialCardView` (card = `?attr/colorSurface`) with a trailing `ImageButton` close X, a centred `ic_bell` hero tinted `?attr/colorPrimary`, a bold `?attr/colorOnSurface` title, a `?attr/colorOnSurfaceVariant` subtitle, a full-width gradient Allow button (`GradientHelper.applyPill` — a gradient cannot be an XML attribute) and a full-width outlined Don't Allow button (`Widget.Material3.Button.OutlinedButton`, `strokeColor=?attr/colorOutline`). `showPermissionRationale()` raises the OS prompt only on Allow; Don't Allow, the X, back press and outside tap all route through `finishPendingWithoutPermission()` so the task is still saved — the permission is a courtesy, never a precondition. **This matters because Android 13+ stops showing the prompt after two denials**, so a refusal that never reached the OS leaves the allowance intact for a later attempt. Two new vectors: `ic_bell` (the existing `ic_notification` is a check mark, not a bell) and `ic_close`; both use a white placeholder fill overridden by `app:tint`, so no palette literal reaches the UI. 5 new string keys in both locales (**226 / 226**, diff 0); `Don't Allow` needed the apostrophe escaped as `\'` or aapt fails with "Invalid unicode escape sequence". 0 hex literals in the new layout. Verified this session with the standalone JDK 21: `:app:assembleDebug` BUILD SUCCESSFUL and `:app:testDebugUnitTest` → **183 tests, 0 failures, 0 errors**. On `emulator-5554`, with POST_NOTIFICATIONS revoked, all three paths were exercised end to end: **Don't Allow** → task `PermissionTest` saved, `granted=false`, no OS prompt consumed; **Allow** → OS dialog appeared → granted → `granted=true`, task `AllowPath` saved; **X** → task saved, `granted=false`, no OS prompt. Light and dark mode both screenshotted; dark renders a purple card with white title and a legible outlined deny button. |
| 1.1 | 2026-11-12 | **Fixed the Daily Quote card's hardcoded colours and, more importantly, the wrong fix for them.** Four views in `activity_main.xml` pointed at static resources: `quoteLabel`, `editQuoteButton` and `quoteAuthor` at `@color/theme_accent` (a fixed `#9C27B0` purple with no night variant) and `quoteText` at `@color/theme_text_primary`. Switched to theme attributes — `?attr/colorPrimary` for the three accents and `?attr/colorOnSurface` for the body — and **deleted the Java overrides that were re-pinning them**: `applyThemeToAllElements()` and `applyAllThemes()` both called `setTextColor(gradientColors[0])` on the author and Edit button, which is why "Edit" stayed cyan on `sunset` even after the XML looked right. `applyThemeToAllElements()` is now intentionally empty. **Then the measured problem:** `colorPrimary` is a *fill* colour, not a text colour. Against the white card it measures `sunset` **1.27:1**, `lime` **1.36:1**, `mint` **1.87:1**, and it fails in 7 of the 10 theme/mode combinations, so the attribute swap alone would have traded a wrong-hue bug for an invisible-text bug. Added `ThemeColors.readableAccent(view)` plus `GradientContrast.blend()`: it keeps the accent's hue and walks it toward black or white in 20 steps, stopping at the first candidate that clears WCAG AA (4.5:1) on the surface it actually sits on; an accent that already passes is returned untouched. `MainActivity` applies it to the label, Edit and author. Measured on device from real pixels: `sunset` → `#80731E` at **4.78:1**, `mint` → `#43805F` at **4.84:1**, body text **9.49:1** on mint. 6 new unit tests pin the blend maths and the four failing accents. Verified this session with the standalone JDK 21: `:app:assembleDebug` BUILD SUCCESSFUL and `:app:testDebugUnitTest` → **183 tests, 0 failures, 0 errors** across 13 suites (`GradientContrastTest` 17); on `emulator-5554` both themes were applied and screenshotted. |
| 1.1 | 2026-11-10 | Restored the theme gradient on the chat screen at user request (reverted the previous turn's solid `?attr/colorSurface`): removed the root background from `activity_chat.xml` and re-enabled `applyGradientBackground()` in `ChatActivity`. To make that safe with a `ConstraintLayout` root, widened `BaseActivity.mainLayout` from `LinearLayout` to `View` — the old type would cast the root and throw on `onConfigurationChanged()`. Bubbles stay opaque solid fills so they remain readable over the gradient. Verified by standalone `javac` (BaseActivity + chat sources); Gradle suite not run this session. |
| 1.1 | 2026-11-12 | **Replaced the AI button's search glyph with a custom sparkle and made its icon colour contrast-derived.** `app:icon` is now `@drawable/ic_ai_sparkle` (a 4-point star; the vector's `#FFFFFFFF` is a placeholder — `app:iconTint` overrides it, so no palette literal reaches the UI). The container was already `app:backgroundTint="?attr/colorPrimary"`, which is correct and stays. **The real bug was the tint.** `app:iconTint="?attr/colorOnPrimary"` resolves to a static white in every theme, but `colorPrimary` is not uniformly dark: on `sunset` it is `#FFE53B` (white measures **1.27:1**), on `mint` dark `#E0F0F0` (**1.17:1**), on `lime` `#00FF5B` (**1.36:1**). The sparkle would have been invisible on exactly the themes a user is most likely to test. Added `ThemeColors.onPrimarySurface(view)`, which runs `GradientContrast.onColorFor()` over the resolved `colorPrimary`, and `MainActivity` now calls `btnAskAi.setIconTint(ColorStateList.valueOf(...))` in the same block as its click listener. Measured after the fix: black on `sunset` **16.52:1**, black on `mint` dark **17.89:1**, black on `lime` **15.46:1**, white on default-dark `#9C27B0` **6.30:1** — every theme now clears AA for a 24dp graphic (needs 3:1). Verified this session with the standalone JDK 21: `:app:assembleDebug` BUILD SUCCESSFUL and `:app:testDebugUnitTest` → **177 tests, 0 failures, 0 errors** across 13 suites; on `emulator-5554` the button renders a black sparkle on a yellow disc under `sunset` (persisted `selected_theme=sunset`, screenshot) and a black sparkle on the blue disc under the default theme. |
| 1.1 | 2026-11-10 | Chat UI polish per visual review: moved `btnAskAi` out of the date card into the Home header as an icon-only circular `TonalButton` (48dp, `colorPrimary`, 24dp corner radius, no text); converted `activity_chat.xml` from `LinearLayout` to `ConstraintLayout` with the RecyclerView pinned `0dp` between `toolbar` and `bottom_input_layout` (fixes the input overlap / cut-off greeting), added `paddingBottom=8dp`; removed the code-applied theme gradient in `ChatActivity` for a solid `?attr/colorSurface`; added bubble row margins and 12dp/8dp padding with `maxWidth=280dp`. Bubble colours remain in `ChatAdapter` via `MaterialColors` (AI `colorSurfaceVariant`, user `colorPrimary` + `colorOnPrimary`). No new strings (208/208 parity). Verified by standalone `javac` + XML/attr checks; Gradle suite not run this session. |
| 1.1 | 2026-11-12 | **Renamed the chat screen from "Second Brain" to "DailyToDo-AI".** `chat_title` is a single string resource consumed only by the toolbar `TextView` in `activity_chat.xml` (`ChatActivity` never calls `setTitle()`), so the change is one key per locale: `values/strings.xml` → `DailyToDo-AI`, `values-zh/strings.xml` → `AI 助手`. Kept the brand name untranslated in English because it doubles as the product name; the Chinese locale uses the descriptive "AI 助手" rather than a transliteration. Also updated the six stale `Second Brain` code comments (`ChatActivity`, `ChatAdapter`, `AIService`, `util/ChatHistoryStore`, `MainActivity`, `AndroidManifest.xml`) and the two PRD mentions, so no source text still refers to the old name. Strings stay at **221 / 221**, diff 0. Verified this session with the standalone JDK 21: `:app:assembleDebug` BUILD SUCCESSFUL and `:app:testDebugUnitTest` → **177 tests, 0 failures, 0 errors** across 13 suites; on `emulator-5554` the chat toolbar renders `DailyToDo-AI` (confirmed by `uiautomator dump` and screenshot). |
| 1.1 | 2026-11-09 | Added AI Q&A chat ("DailyToDo-AI") preview: `AIService.ask()` → `POST /ai-chat` with `question`/`context_data`, shared `postJson()` helper, `ChatActivity` + `ChatAdapter` with right/left bubbles and per-send task context, `btnAskAi` in the Home date card, manifest registration, 10 EN/ZH string keys (208/208). Verified by standalone `javac` + Gson round-trip; Gradle suite not run this session. |
| 1.1 | 2026-11-09 | Hardened `AIService` response parsing: `JsonSyntaxException` is caught, the raw JSON is logged to Logcat (tag `AIService`), and the failure is rethrown as `IOException` instead of crashing on `Expected BEGIN_ARRAY`. Verified by standalone `javac` against `android-34` + Gson 2.10.1; Gradle suite not run this session. |
| 1.1 | 2026-11-09 | Fixed the broken `assembleDebug`: `AIService` referenced Retrofit without the dependency (now `HttpURLConnection` + Gson) and `TaskWithDate` redefined four time setters. Wired the AI auto-schedule button end to end, moved its label to resources (198/198 keys), and extracted the matching rules into tested `AutoScheduleMapper`. Replaced the confusing inline `newTaskInput` + Add row with a single full-width `addTaskButton`, which only opens `AddTaskActivity` and is painted from the active theme gradient with contrast-derived label colour (`util/GradientContrast`). |
| 1.1 | 2026-11-11 | Fixed the "past task" UX. `AddTaskActivity` gained a `MaterialDatePicker` (`btnSelectDate`, defaults to "Today") above the Time Block section; `EXTRA_DATE_ISO` now carries the selected date instead of `Calendar.getInstance()`. Picking Today with a time that has already passed shows a Toast + confirm dialog and, on confirmation, adds one day. `MainActivity` timeline: long-press now starts a drag for **all** blocks (past included — the 0.5 alpha is visual only), the past-block delete branch is gone, and `ItemTouchHelper.SimpleCallback(UP\|DOWN, LEFT\|RIGHT)` handles swipe-to-delete via `confirmDeleteTask()`. 4 new EN/ZH strings (219/219). Verified by standalone `javac` (complete generated R stub) + date round-trip/+1-day/leap-year/`isPast` tests + XML checks; Gradle suite not run this session. |
| 1.1 | 2026-11-11 | Fixed drag snapping back + planner-style grid. **Drag save:** the delta is applied and persisted in `onMove()` (was `clearView()`), so a row cannot animate back to a stale time; `applyDraggedHour()` resolves the authoritative instance via `findTaskById()` because `renderTimelineForSelectedDate()` loads fresh objects, so mutating the adapter's copy never reached `saveTasks()`. `clearView()` only resets gesture state and re-submits the grid once on drop. **Grid UI:** `item_timeline_slot.xml` is now a table row — 80dp TIME cell (translucent `colorSurfaceVariant`, 1dp bottom border), 1dp vertical rule, weighted task column, row bottom border. Labels read `HH:00 - HH:00`. **Header:** `timelineHeaderRow` above the grid with `TIME` + the selected day's name (Monday-first, matching the day strip). 1 new EN/ZH string (220/220). Verified by standalone `javac` + XML checks; Gradle suite not run this session. |
| 1.1 | 2026-11-11 | Chat conversation history + robust action parsing. `ChatMessage` (`role`/`content`, public, own file) is sent as `ChatRequest.history`; `AIService.ask()` gained a 4-arg overload and the 2-arg form sends an empty list. `ChatActivity` keeps a `List<ChatMessage>`, appends the user turn before sending and the assistant turn (plus task-confirmation) after, and persists through the new `util/ChatHistoryStore` (single JSON blob, capped at 50 turns, corrupt blob falls back to empty). Saved turns are replayed on open; the welcome line is chrome and is not stored. Action parsing hardened: accepts a nested object, an `action` string holding JSON, flat top-level fields, or JSON embedded in the `answer` text (fenced/prose) — a genuinely plain reply stays plain text. 0 new strings (220/220). Verified by standalone `javac` + Gson tests for the history payload, persistence round-trip, and all four action shapes; Gradle suite not run this session. |
| 1.1 | 2026-11-11 | Chat grounding + Calendar month view. **Chat:** `buildTaskContext()` now leads with `User name:` (from the `user_name` pref the profile screen writes) and `Today:`, and closes the list with an explicit "these are the only tasks that exist" guard — the model was hallucinating task lists and losing the user's name because neither was ever in the payload, not because history was dropped (verified: `sendMessage()` appends the user turn before `ask()` and never clears). **Calendar:** `CalendarActivity` keeps its month `GridView` and gains a `viewDayTimelineButton`; tapping it launches `MainActivity` with `EXTRA_SHOW_TIMELINE` + `EXTRA_TIMELINE_DATE`, which `applyTimelineIntent()` validates against `IntervalRules.isValidDateIso`, switches to Timeline and re-selects that day. `MainActivity` is now `launchMode="singleTop"` with an `onNewIntent()` so a repeat jump reuses the instance instead of stacking a second Home. 1 new EN/ZH string (221/221). Verified by standalone `javac` + context-payload test; Gradle suite not run this session. |
| 1.1 | 2026-11-11 | Backlog → grid drag-and-drop. Uses the platform `DragAndDrop` framework because `ItemTouchHelper` cannot span two views. **Source:** each backlog chip gets an `OnLongClickListener` that packs the task **UUID** (not the name — names repeat) into `ClipData.newPlainText("task_id", …)` and calls `startDragAndDrop()` with a `DragShadowBuilder`. **Target:** `TimelineAdapter.SlotHolder` attaches an `OnDragListener` to `slotBlocks`; `ACTION_DRAG_STARTED` must return true or no further events arrive, `ACTION_DRAG_ENTERED`/`EXITED` repaint a ~15% accent hover fill (built once in the holder, not per event), and `ACTION_DROP` reads the id and reports `(taskId, slot.hour)` to the host. **Apply:** `MainActivity.scheduleTaskAtHour()` resolves the live instance by UUID, sets start **before** end (`setStartMinute` clears the Anytime sentinel that `setEndMinute` checks), one-hour block clamped at 24:00, then `saveTasks()` + reminder re-arm + deferred `refreshCurrentView()` (posted, because `notifyDataSetChanged()` inside drag dispatch can race a pending layout). 0 new strings (221/221). Verified by standalone `javac` + a state-machine test of the Anytime→scheduled transition at hours 0/9/11/23; Gradle suite not run this session. |
| 1.1 | 2026-11-12 | **Closed the Mint/Sunset gap: all 10 themes are now purchasable and selectable from the Shop.** `ThemeManager` already knew both ids and both had complete `Light`/`Dark` styles in `values/themes.xml`, but neither `ThemeActivity` nor `ShopActivity` listed them, so the only way to reach them was the unlaunched `SettingsActivity`. Added a `mintStatus`/`sunsetStatus` row to both screens (same gradient-card + status-label shape as the other eight), plus a new `drawable/gradient_mint.xml` (`@color/mint_start` → `@color/mint_end`) — `gradient_sunset` already existed. **Two latent bugs fixed on the way:** (a) `getThemeName()` in `ThemeActivity` had no `mint`/`sunset` case, so the confirmation Toast fell through to `default` and said "Default Theme" after applying either one; (b) the theme-card walkers were hardcoded to nine rows (`i < 9` and `new TextView[9]` / `new LinearLayout[9]`), so a tenth and eleventh card would have been silently skipped and its name label would not have followed the theme. The loops now run to `contentLayout.getChildCount()` and the arrays are sized 11. **Deleted `SettingsActivity`** (59-line Java + layout + manifest entry): it contained nothing but a five-theme radio group, its themes are now fully covered by the Shop, and it bypassed the coin economy — it called `ThemeManager.setThemeId()` directly, so any user could switch to a paid theme for free. Verified this session with the standalone JDK 21: `:app:assembleDebug` BUILD SUCCESSFUL and `:app:testDebugUnitTest` → **177 tests, 0 failures, 0 errors** across 13 suites. On `emulator-5554`: both new rows render in Shop (showing `200 COINS`) and Themes; tapping Mint in Themes wrote `selected_theme=mint` and, after relaunch, the whole UI rendered in the mint palette with `APPLIED` on its row; tapping Sunset in Shop correctly reported insufficient coins at 10, proving the purchase gate applies to the new themes too. |
| 1.1 | 2026-11-12 | **Removed the last hardcoded colours (3.5.3 → `DONE`).** Every colour now resolves from the active theme: layouts use `?attr/color*`, Java goes through `ThemeColors`/`MaterialColors`, and semantic status colours come from night-qualified `@color` resources. Three real bugs surfaced while doing it, all caused by a colour chosen for one background being reused on another. **(a) Category text.** `TimeCategory.getColorRes()` is a *fill* (it paints a timeline block, whose label comes from `GradientContrast`), but `MainActivity` also used it as *text* colour, so "Break" sat at **1.87:1** and "Personal" at **2.03:1** on a light card. Added a separate `getTextColorRes()` ramp (`block_*_text`) with a darker light-mode value and a lighter night twin; measured 8.42:1 on the dark card and ≥5.09:1 on white. **(b) Login label.** `actionButton` is painted with the theme gradient but kept the layout's `colorOnSurface`, which on `hotpink` (a gradient ending at pure black) gave **1.40:1**. It now uses `ThemeColors.onGradient(...)` like every other gradient-painted control — 4.13:1 at the pink end, 20.42:1 at the black end. **(c) "NOT APPLIED" status.** A fixed `text_muted` measured **2.68:1** on a white card and 3.20:1 on a dark one; switched to `colorOnSurfaceVariant`, now **6.90:1**. Also rewrote 13 live gradient drawables to reference `@color/*_start` / `@color/*_end` instead of repeating the palette, and deleted 34 unreferenced drawables, 4 dead adapters (`TaskAdapter`, `ShopAdapter`, `SimpleThemeAdapter`, `ThemeAdapter` — none was ever instantiated) and 8 layouts only those adapters inflated. Verified this session with the standalone JDK 21: `:app:assembleDebug` BUILD SUCCESSFUL and `:app:testDebugUnitTest` → **177 tests, 0 failures, 0 errors** across 13 suites; plus emulator screenshots and WCAG pixel measurements on `emulator-5554` in both light and dark. |
| 1.1 | 2026-11-12 | Ran the backlog→grid drag on the emulator end to end (not just compiled). On `emulator-5554` (API 36, 1440×3200, guest mode): created `dragtesttask` as an Anytime task, switched to Timeline, then `adb shell input draganddrop 262 1022 841 2841 1800` (chip centre → centre of the 11:00 `slotBlocks`, coordinates taken from `uiautomator dump`, not eyeballed). Result: a `11:00 - 12:00` block appeared, `backlogTitle` count went 1 → 0, and after `force-stop` + relaunch the block was still at 11:00 — so `saveTasks()` persisted it. Hover highlight (`ACTION_DRAG_ENTERED`) was verified separately by replaying the gesture with `input motionevent DOWN/MOVE/…` and screenshotting while the finger was held over 11:00: the row showed the accent tint. No `FATAL EXCEPTION` in logcat. One environment note: `MainActivity` is not exported, so `am start -n …/.MainActivity` is rejected from the shell (`SecurityException: not exported from uid`) — relaunch via `monkey -p … -c android.intent.category.LAUNCHER 1`. |
| 1.1 | 2026-11-11 | Verified the backlog→grid drag end to end in this session and repaired the stale slot-label tests. `TimelineSlotsTest` still asserted the bare `09:00` label from before the planner-grid change, so 5 of its assertions failed (`expected:<06:00[]> but was:<06:00[ - 07:00]>`); they now expect the `HH:00 - HH:00` range that `TimelineSlots.getSlotLabel()` actually returns. Run this session with the standalone JDK 21: `:app:testDebugUnitTest` → **177 tests, 0 failures, 0 errors** across 13 suites, and `:app:assembleDebug` → BUILD SUCCESSFUL. Matrix rows 3.3.5 / 3.3.6 added. |
| 1.1 | 2026-11-11 | Made the Timeline interactive. **Past tasks:** `TimelineEdits.isPast()` (end-minute comparison, so an in-progress block is not stale), dimmed to 0.5 alpha, long-press opens a delete confirmation that removes **by UUID**. **Drag to reschedule:** `ItemTouchHelper` on the hour grid; `onMove()` only records the hour, `clearView()` applies the delta, duration preserved and clamped to the same day by `TimelineEdits.shiftByHours()`. Rows are never reordered — the 06:00–23:00 frame stays fixed. Empty hours are not draggable but are valid drop targets. 5 EN/ZH strings (215/215). 13 new tests in `TimelineEditsTest` (177 total). Verified by standalone `javac` + `JUnitCore` + `aapt2 compile`; Gradle suite not run this session. |
