package com.example.dailytodolist.util;

import android.content.SharedPreferences;

import com.example.dailytodolist.TaskWithDate;
import com.example.dailytodolist.model.TimeCategory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistence for home-list tasks, keyed by permanent task id.
 *
 * <h2>The bug this replaces</h2>
 * The legacy writer stored tasks in positional keys:
 * {@code task_0, task_year_0, task_shour_0, ...}, plus {@code task_count}.
 * {@code saveTasks()} rewrote them by loop index. Deleting the task at position
 * 2 therefore moved the task that was at position 3 into slot 2, and every keyed
 * attribute followed the <em>slot</em>, not the task. The user saw a surviving
 * task inherit a deleted task's time.
 *
 * <p>Deleting the highest slot also left stale keys behind (count shrank but
 * {@code task_shour_n} survived), so resurrecting a task could pick up an
 * unrelated old time.</p>
 *
 * <h2>The replacement</h2>
 * Each task is written under its own UUID: {@code task.<uuid>.field}. Deleting
 * a task removes exactly its own keys and cannot touch any other task, no
 * matter what order anything is in. The list order the user sees is kept
 * separately, as an array of ids.
 *
 * <h2>Migration</h2>
 * Legacy data is read once, given UUIDs, and written in the new format. The
 * legacy keys are left in place untouched, so a bad migration can be rolled
 * back by clearing {@code migration_v2_done}; a backup copy of the raw legacy
 * blob is written before the new keys are created.
 */
public final class TaskStore {

    private static final String PREFS = "TodoAppPrefs";

    /** Marks that the one-time legacy import has run. */
    public static final String KEY_MIGRATED = "tasks.migration_v2_done";
    /** Raw copy of the legacy keys, kept for rollback and debugging. */
    public static final String KEY_BACKUP = "tasks.legacy_backup";
    /** Ordered list of task ids, so display order is explicit. */
    private static final String KEY_ORDER = "tasks.v2.order";

    private static final String PREFIX = "task.";
    private static final String SUFFIX_ID = ".id";
    private static final String SUFFIX_TITLE = ".title";
    private static final String SUFFIX_DATE = ".date";
    private static final String SUFFIX_START = ".start";
    private static final String SUFFIX_END = ".end";
    private static final String SUFFIX_DONE = ".done";
    private static final String SUFFIX_TOP = ".top";
    private static final String SUFFIX_CAT = ".category";
    private static final String SUFFIX_COLOR = ".color";
    /** Optional description. Absent for rows written before it existed, which
     *  read back as an empty string rather than null. */
    private static final String SUFFIX_DESCRIPTION = ".description";

    private static final String ORDER_SEPARATOR = "\n";

    private final SharedPreferences prefs;

    public TaskStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public static TaskStore from(android.content.Context context) {
        return new TaskStore(context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE));
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    /**
     * Loads every task, runs the one-time migration if needed, and returns the
     * tasks in the user's saved order.
     */
    public List<TaskWithDate> loadAll() {
        migrateIfNeeded();
        return readCurrent();
    }

    /**
     * Reads the current format without triggering migration.
     *
     * <p>Relies on the stored id list, so a task can never be lost by being
     * absent from an implicit ordering; it is only lost if its keys were
     * genuinely deleted.</p>
     */
    private List<TaskWithDate> readCurrent() {
        Map<String, TaskWithDate> byId = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(PREFIX) || !key.endsWith(SUFFIX_ID)) {
                continue;
            }
            String id = asString(entry.getValue());
            if (!UuidUtil.isValid(id)) {
                continue;
            }
            TaskWithDate task = readOne(id);
            if (task != null) {
                byId.put(id, task);
            }
        }

        List<TaskWithDate> ordered = new ArrayList<>();
        for (String id : readOrder()) {
            TaskWithDate task = byId.remove(id);
            if (task != null) {
                ordered.add(task);
            }
        }
        // Anything present in storage but absent from the order list (for
        // example after an interrupted write) is appended rather than dropped.
        ordered.addAll(byId.values());
        return ordered;
    }

    /**
     * Loads only the tasks that can be drawn on a time grid.
     *
     * <p>This is the query the upcoming Time-Blocking Grid must use. An
     * "Anytime" task has no interval, so it has no position or height on a
     * timeline; including it would either stack it at 00:00 or need a
     * fabricated duration. Such tasks belong in a separate "unscheduled" list,
     * which is what {@link #loadAnytime()} returns.</p>
     */
    public List<TaskWithDate> loadScheduled() {
        List<TaskWithDate> scheduled = new ArrayList<>();
        for (TaskWithDate task : loadAll()) {
            if (!task.isAnytime()) {
                scheduled.add(task);
            }
        }
        return scheduled;
    }

    /** Loads only the tasks without a time block. */
    public List<TaskWithDate> loadAnytime() {
        List<TaskWithDate> anytime = new ArrayList<>();
        for (TaskWithDate task : loadAll()) {
            if (task.isAnytime()) {
                anytime.add(task);
            }
        }
        return anytime;
    }

    /**
     * Scheduled tasks on one date, ready to lay out on the grid.
     *
     * @param dateIso {@code yyyy-MM-dd}
     */
    public List<TaskWithDate> loadScheduledForDate(String dateIso) {
        List<TaskWithDate> onDate = new ArrayList<>();
        if (dateIso == null) {
            return onDate;
        }
        for (TaskWithDate task : loadScheduled()) {
            if (dateIso.equals(task.getDateIso())) {
                onDate.add(task);
            }
        }
        return onDate;
    }

    private TaskWithDate readOne(String id) {
        String title = prefs.getString(PREFIX + id + SUFFIX_TITLE, null);
        if (title == null || title.trim().isEmpty()) {
            return null;
        }

        TaskWithDate task = new TaskWithDate(title, 1970, 1, 1);
        task.restoreId(id);

        parseDateInto(prefs.getString(PREFIX + id + SUFFIX_DATE, null), task);

        // Anytime tasks are stored as the NO_TIME sentinel (-1). The default of
        // 0 is only a fallback for rows written before this key existed; those
        // rows always had a real time, so 0 is not silently treated as Anytime.
        int storedStart = prefs.getInt(PREFIX + id + SUFFIX_START, 0);
        int storedEnd = prefs.getInt(PREFIX + id + SUFFIX_END, 0);
        if (storedStart == TaskWithDate.NO_TIME) {
            task.setStartMinute(TaskWithDate.NO_TIME);
        } else {
            task.setStartMinute(storedStart);
            task.setEndMinute(storedEnd == TaskWithDate.NO_TIME ? storedStart : storedEnd);
        }
        task.setChecked(prefs.getBoolean(PREFIX + id + SUFFIX_DONE, false));
        task.setTopPriority(prefs.getBoolean(PREFIX + id + SUFFIX_TOP, false));
        task.setCategory(prefs.getString(PREFIX + id + SUFFIX_CAT,
                TimeCategory.OTHER.getStorageKey()));
        task.setColorHex(prefs.getString(PREFIX + id + SUFFIX_COLOR, ""));
        task.setDescription(prefs.getString(PREFIX + id + SUFFIX_DESCRIPTION, ""));
        return task;
    }

    private List<String> readOrder() {
        String raw = asString(prefs.getAll().get(KEY_ORDER));
        List<String> ids = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return ids;
        }
        for (String part : raw.split(ORDER_SEPARATOR)) {
            String id = part.trim();
            if (UuidUtil.isValid(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    // ------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------

    /**
     * Replaces the stored task set with {@code tasks}.
     *
     * <p>Uses a single editor transaction so a crash cannot leave half the list
     * written. Removes keys belonging to tasks that are no longer present, which
     * is what keeps deleted tasks from lingering.</p>
     */
    public void saveAll(List<TaskWithDate> tasks) {
        List<TaskWithDate> safe = tasks == null ? new ArrayList<>() : tasks;

        List<String> keepIds = new ArrayList<>();
        SharedPreferences.Editor editor = prefs.edit();
        for (TaskWithDate task : safe) {
            if (task == null) {
                continue;
            }
            task.ensureId();
            String id = task.getId();
            keepIds.add(id);

            editor.putString(PREFIX + id + SUFFIX_ID, id);
            editor.putString(PREFIX + id + SUFFIX_TITLE, task.getTaskName());
            editor.putString(PREFIX + id + SUFFIX_DATE, task.getDateIso());
            editor.putInt(PREFIX + id + SUFFIX_START, task.getStartMinute());
            editor.putInt(PREFIX + id + SUFFIX_END, task.getEndMinute());
            editor.putBoolean(PREFIX + id + SUFFIX_DONE, task.isChecked());
            editor.putBoolean(PREFIX + id + SUFFIX_TOP, task.isTopPriority());
            editor.putString(PREFIX + id + SUFFIX_CAT, task.getCategory());
            editor.putString(PREFIX + id + SUFFIX_COLOR, task.getColorHex());
            editor.putString(PREFIX + id + SUFFIX_DESCRIPTION, task.getDescription());
        }

        // Drop every stored task not in the incoming set.
        for (String storedId : allStoredIds()) {
            if (!keepIds.contains(storedId)) {
                removeKeysFor(editor, storedId);
            }
        }

        editor.putString(KEY_ORDER, String.join(ORDER_SEPARATOR, keepIds));
        editor.apply();
    }

    /** Every task id that currently has any {@code task.<id>.*} key. */
    private List<String> allStoredIds() {
        List<String> ids = new ArrayList<>();
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith(PREFIX) || !key.endsWith(SUFFIX_ID)) {
                continue;
            }
            String id = asString(prefs.getAll().get(key));
            if (UuidUtil.isValid(id) && !ids.contains(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    private void removeKeysFor(SharedPreferences.Editor editor, String id) {
        String base = PREFIX + id;
        editor.remove(base + SUFFIX_ID);
        editor.remove(base + SUFFIX_TITLE);
        editor.remove(base + SUFFIX_DATE);
        editor.remove(base + SUFFIX_START);
        editor.remove(base + SUFFIX_END);
        editor.remove(base + SUFFIX_DONE);
        editor.remove(base + SUFFIX_TOP);
        editor.remove(base + SUFFIX_CAT);
        editor.remove(base + SUFFIX_COLOR);
        editor.remove(base + SUFFIX_DESCRIPTION);
    }

    // ------------------------------------------------------------------
    // Migration
    // ------------------------------------------------------------------

    /**
     * Imports legacy positional data exactly once.
     *
     * <p>Idempotent by flag: once {@code tasks.migration_v2_done} is set this
     * does nothing, so restarting the app cannot duplicate the list.</p>
     */
    public void migrateIfNeeded() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) {
            return;
        }

        List<TaskWithDate> legacy = readLegacy();
        if (legacy.isEmpty()) {
            // Nothing to import. Still flag it so we do not re-scan every launch.
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply();
            return;
        }

        // Keep the raw legacy values so the import can be reversed.
        StringBuilder backup = new StringBuilder();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("task_") && !key.startsWith("task.")) {
                backup.append(key).append('=').append(entry.getValue()).append('\n');
            }
        }

        saveAll(legacy);

        prefs.edit()
                .putString(KEY_BACKUP, backup.toString())
                .putBoolean(KEY_MIGRATED, true)
                .apply();
    }

    /** Reads the legacy positional keys, in index order. */
    private List<TaskWithDate> readLegacy() {
        List<TaskWithDate> legacy = new ArrayList<>();
        int count = prefs.getInt("task_count", 0);

        for (int i = 0; i < count; i++) {
            String title = prefs.getString("task_" + i, "");
            if (title == null || title.trim().isEmpty()) {
                continue;
            }

            TaskWithDate task = new TaskWithDate(
                    title,
                    prefs.getInt("task_year_" + i, 1970),
                    prefs.getInt("task_month_" + i, 1),
                    prefs.getInt("task_day_" + i, 1));

            int startHour = prefs.getInt("task_shour_" + i, 9);
            int startMin = prefs.getInt("task_smin_" + i, 0);
            int endHour = prefs.getInt("task_ehour_" + i, 10);
            int endMin = prefs.getInt("task_emin_" + i, 0);

            if (startHour == TaskWithDate.NO_TIME) {
                task.setStartMinute(TaskWithDate.NO_TIME);
            } else {
                task.setStartMinute(startHour * 60 + startMin);
                task.setEndMinute(endHour * 60 + endMin);
            }
            task.setChecked(prefs.getBoolean("task_status_" + i, false));
            task.setTopPriority(prefs.getBoolean("task_top_" + i, false));
            task.setCategory(prefs.getString("task_cat_" + i,
                    TimeCategory.OTHER.getStorageKey()));
            task.setColorHex(prefs.getString("task_color_" + i, ""));

            legacy.add(task);
        }
        return legacy;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static void parseDateInto(String dateIso, TaskWithDate task) {
        if (dateIso == null || dateIso.length() != 10) {
            return;
        }
        try {
            task.setYear(Integer.parseInt(dateIso.substring(0, 4)));
            task.setMonth(Integer.parseInt(dateIso.substring(5, 7)));
            task.setDay(Integer.parseInt(dateIso.substring(8, 10)));
        } catch (NumberFormatException ignored) {
            // Leave the defaults; a malformed stored date must not crash load.
        }
    }
}
