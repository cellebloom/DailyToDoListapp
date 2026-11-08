package com.example.dailytodolist.model;

import com.example.dailytodolist.util.UuidUtil;

/**
 * One task in the unified planner model.
 *
 * <p><b>Identity contract.</b> {@link #getId()} is a UUID minted exactly once by
 * {@link UuidUtil#newId()}. It is never derived from a list index, a title, or a
 * date, and it is never recomputed when the task is edited, moved to another
 * day, reordered, or when other tasks are deleted. There is deliberately no
 * {@code setId} method on this class.</p>
 *
 * <p>Tasks carried over from the legacy storage are built with
 * {@link #createMigratedFromHome} or {@link #createMigratedFromCalendar}. Those
 * take an already frozen UUID so that the same legacy row always maps to the
 * same id, no matter how many times the migration runs.</p>
 *
 * <p>Defaults are chosen so migration never invents data: priority
 * {@link #PRIORITY_NONE}, {@code estimatedMinutes} 0, {@code dueDateIso} null,
 * {@code notes} empty.</p>
 *
 * <p>This class is plain Java and must stay free of Android imports.</p>
 */
public class Task {

    /** No priority set. The default for every migrated task. */
    public static final int PRIORITY_NONE = 0;
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_MEDIUM = 2;
    public static final int PRIORITY_HIGH = 3;

    /** Task came from the legacy home screen list (no date of its own). */
    public static final String ORIGIN_HOME = "home";
    /** Task came from the legacy calendar screen (always has a date). */
    public static final String ORIGIN_CALENDAR = "calendar";
    /** Task created fresh inside the new planner. */
    public static final String ORIGIN_PLANNER = "planner";

    private String id;
    private String title;
    private String dateIso;
    private int priority;
    private String dueDateIso;
    private int estimatedMinutes;
    private String notes;
    private boolean completed;
    private String origin;
    private String legacyTimeText;
    private String sourceKey;
    private long createdAt;
    private long updatedAt;

    /** Required so the class can be reconstructed from stored JSON. */
    public Task() {
        this.notes = "";
        this.legacyTimeText = "";
        this.sourceKey = "";
    }

    // ------------------------------------------------------------------
    // Factories
    // ------------------------------------------------------------------

    /**
     * Creates a brand new task and mints its permanent id.
     *
     * @param title    must not be blank
     * @param dateIso  {@code "yyyy-MM-dd"}, or {@code null} for an unscheduled task
     * @param origin   one of the {@code ORIGIN_*} constants
     */
    public static Task createNew(String title, String dateIso, String origin) {
        long now = System.currentTimeMillis();

        Task task = new Task();
        task.id = UuidUtil.newId();
        task.title = requireTitle(title);
        task.dateIso = requireDateOrNull(dateIso);
        task.priority = PRIORITY_NONE;
        task.dueDateIso = null;
        task.estimatedMinutes = 0;
        task.notes = "";
        task.completed = false;
        task.origin = (origin == null || origin.trim().isEmpty()) ? ORIGIN_PLANNER : origin;
        task.legacyTimeText = "";
        task.sourceKey = "";
        task.createdAt = now;
        task.updatedAt = now;
        return task;
    }

    /**
     * Rebuilds a task from legacy storage while preserving an already frozen id.
     *
     * <p>This is the only migration entry point. It refuses anything that is not
     * a canonical UUID, which structurally prevents coordinates such as
     * {@code "home:3"} from ever becoming a permanent reference.</p>
     *
     * @param frozenId       UUID frozen by an earlier migration run; must be valid
     * @param sourceKey      idempotency fingerprint, see {@link SourceKey}
     * @param legacyTimeText display-only time text carried over from the old
     *                       calendar field; never used for scheduling
     */
    public static Task createMigrated(String frozenId,
                                      String title,
                                      String dateIso,
                                      boolean completed,
                                      String origin,
                                      String legacyTimeText,
                                      String sourceKey) {
        if (!UuidUtil.isValid(frozenId)) {
            throw new IllegalArgumentException(
                    "frozen id must be a canonical UUID, was: " + frozenId);
        }
        if (sourceKey == null || sourceKey.trim().isEmpty()) {
            throw new IllegalArgumentException("sourceKey must not be empty");
        }

        long now = System.currentTimeMillis();

        Task task = new Task();
        task.id = frozenId.trim();
        task.title = requireTitle(title);
        task.dateIso = requireDateOrNull(dateIso);
        task.priority = PRIORITY_NONE;
        task.dueDateIso = null;
        task.estimatedMinutes = 0;
        task.notes = "";
        task.completed = completed;
        task.origin = (origin == null || origin.trim().isEmpty()) ? ORIGIN_PLANNER : origin;
        task.legacyTimeText = legacyTimeText == null ? "" : legacyTimeText;
        task.sourceKey = sourceKey;
        task.createdAt = now;
        task.updatedAt = now;
        return task;
    }

    /**
     * Migrates one row of the legacy home list.
     *
     * <p>Home tasks have no date, so {@code dateIso} stays {@code null}
     * (unscheduled). Stamping today's date would invent history that the user
     * never entered.</p>
     */
    public static Task createMigratedFromHome(String frozenId,
                                              String title,
                                              boolean completed,
                                              String sourceKey) {
        return createMigrated(frozenId, title, null, completed,
                ORIGIN_HOME, "", sourceKey);
    }

    /**
     * Migrates one row of the legacy calendar store, keeping both its date and
     * its original time text.
     */
    public static Task createMigratedFromCalendar(String frozenId,
                                                  String title,
                                                  String dateIso,
                                                  boolean completed,
                                                  String legacyTimeText,
                                                  String sourceKey) {
        return createMigrated(frozenId, title, dateIso, completed,
                ORIGIN_CALENDAR, legacyTimeText, sourceKey);
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    /** The permanent identity. Never changes for the lifetime of the task. */
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = requireTitle(title);
        markUpdated();
    }

    /** {@code "yyyy-MM-dd"}, or {@code null} when the task is unscheduled. */
    public String getDateIso() {
        return dateIso;
    }

    public void setDateIso(String dateIso) {
        this.dateIso = requireDateOrNull(dateIso);
        markUpdated();
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        if (priority < PRIORITY_NONE || priority > PRIORITY_HIGH) {
            throw new IllegalArgumentException("priority out of range: " + priority);
        }
        this.priority = priority;
        markUpdated();
    }

    public String getDueDateIso() {
        return dueDateIso;
    }

    public void setDueDateIso(String dueDateIso) {
        this.dueDateIso = requireDateOrNull(dueDateIso);
        markUpdated();
    }

    /** Planned duration in minutes. 0 means "not estimated", not "zero minutes". */
    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        if (estimatedMinutes < 0) {
            throw new IllegalArgumentException(
                    "estimatedMinutes must be >= 0, was: " + estimatedMinutes);
        }
        this.estimatedMinutes = estimatedMinutes;
        markUpdated();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes;
        markUpdated();
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        markUpdated();
    }

    public String getOrigin() {
        return origin;
    }

    /** Display-only time text inherited from the legacy calendar field. */
    public String getLegacyTimeText() {
        return legacyTimeText;
    }

    /** Migration fingerprint. Empty for tasks created inside the planner. */
    public String getSourceKey() {
        return sourceKey;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    /** True when the task has a date of its own. */
    public boolean isScheduled() {
        return dateIso != null;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private void markUpdated() {
        this.updatedAt = System.currentTimeMillis();
    }

    private static String requireTitle(String title) {
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("title must not be empty");
        }
        return trimmed;
    }

    private static String requireDateOrNull(String dateIso) {
        if (dateIso == null) {
            return null;
        }
        String trimmed = dateIso.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!IntervalRules.isValidDateIso(trimmed)) {
            throw new IllegalArgumentException(
                    "date must be yyyy-MM-dd, was: " + dateIso);
        }
        return trimmed;
    }

    /** Identity-based equality: two tasks are the same task only if ids match. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Task)) {
            return false;
        }
        Task that = (Task) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "Task{id=" + id + ", title=" + title + ", date=" + dateIso
                + ", completed=" + completed + "}";
    }
}
