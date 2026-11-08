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
| 3.5.3 | No hardcoded hex in UI | `OPEN` | **169** occurrences across 7 files — see 7.6 |

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
- new task: `AddTaskActivity` always stamps `EXTRA_DATE_ISO = todayIso()`.

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
| All colours via semantic attributes / resources | `OPEN` | 169 hardcoded hex |
| No hardcoded colour in UI components | `OPEN` | same |
| Purchased themes work in light and dark | `PARTIAL` | 5 themes × 2 modes exist in `values/themes.xml` + `values-night/themes.xml`, but runtime hex styling can override the theme |

**Hot spots** (`#RRGGBB` literals): `CalendarActivity`, `LoginActivity`, `MainActivity`,
`ProfileActivity`, `ShopActivity`, `ThemeActivity`, `TimelineAdapter`. Examples:
`#2D2D2D` for dark card surfaces in `MainActivity`, `#1F2B48` as on-colour in
`TimelineAdapter`.

**Policy for new code.** Move a colour to `colors.xml` (plus `values-night/colors.xml`
when it must differ) and read it with `ContextCompat.getColor` or a theme attribute.
Contrast decisions (white vs dark text on a category fill) belong in one helper — a
luminance helper already exists in `TimelineAdapter` (`isDark`) and should be promoted
rather than copied.

### 4.7 Localization — `PARTIAL`

| Sub-requirement | Status | Notes |
|---|---|---|
| English + Simplified Chinese for all user-facing text | `PARTIAL` | 180 / 180 string keys match exactly (was 181/181 before `no_scheduled_tasks` was removed with the Timeline empty state) |
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
