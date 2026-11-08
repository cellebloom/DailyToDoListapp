package com.example.dailytodolist.model;

import com.example.dailytodolist.util.UuidUtil;

import java.util.Locale;

/**
 * One block of scheduled time on the planner.
 *
 * <p>A block either <b>links</b> to a task or stands on its own:</p>
 *
 * <ul>
 *   <li><b>Linked</b> — {@code taskId} is a real {@link Task#getId()}. Example:
 *       "Write report" scheduled 09:00-10:30.</li>
 *   <li><b>Independent</b> — {@code taskId} is {@code null}. A lecture, lunch,
 *       a commute, a break. These have no underlying task at all and are fully
 *       first-class: the planner must stay usable on a device with zero tasks.</li>
 * </ul>
 *
 * <p><b>Why the block carries its own title.</b> Even a linked block keeps a
 * copy of the text. When the linked task is deleted the block is not deleted —
 * see {@link #detachTask()} — it simply becomes independent and still reads
 * correctly on the timeline.</p>
 *
 * <p>Times are minutes since midnight as a half-open interval
 * {@code [startMinute, endMinute)}. See {@link IntervalRules}.</p>
 *
 * <p>This class is plain Java and must stay free of Android imports.</p>
 */
public class TimeBlock {

    /** A block that represents scheduled work on a linked task. */
    public static final String KIND_TASK = "task";
    /** A block that represents a class or lecture. */
    public static final String KIND_CLASS = "class";
    /** A block that represents a break. */
    public static final String KIND_BREAK = "break";
    /** A block that represents a meal. */
    public static final String KIND_MEAL = "meal";
    /** A block that represents personal time or an independent activity. */
    public static final String KIND_PERSONAL = "personal";

    private String id;
    private String taskId;
    private String title;
    private String dateIso;
    private int startMinute;
    private int endMinute;
    private String kind;
    private String colorKey;
    private boolean completed;
    private long createdAt;
    private long updatedAt;

    /** Required so the class can be reconstructed from stored JSON. */
    public TimeBlock() {
        this.kind = KIND_PERSONAL;
        this.colorKey = "";
    }

    // ------------------------------------------------------------------
    // Factories
    // ------------------------------------------------------------------

    /**
     * Creates a block linked to an existing task and mints its permanent id.
     *
     * @param taskId must be the canonical UUID of an existing task
     */
    public static TimeBlock createLinked(String taskId,
                                         String title,
                                         String dateIso,
                                         int startMinute,
                                         int endMinute,
                                         String kind) {
        requireTaskId(taskId);

        TimeBlock block = createIndependent(title, dateIso, startMinute, endMinute, kind);
        block.taskId = taskId.trim();
        return block;
    }

    /**
     * Creates an independent block with no linked task.
     *
     * <p>This is the entry point for classes, breaks, meals and personal
     * activities. {@code taskId} stays {@code null} and {@link #isLinked()} is
     * false; nothing about the block depends on a task existing.</p>
     */
    public static TimeBlock createIndependent(String title,
                                              String dateIso,
                                              int startMinute,
                                              int endMinute,
                                              String kind) {
        long now = System.currentTimeMillis();

        TimeBlock block = new TimeBlock();
        block.id = UuidUtil.newId();
        block.taskId = null;
        block.title = requireTitle(title);
        block.dateIso = requireDate(dateIso);
        block.startMinute = startMinute;
        block.endMinute = endMinute;
        block.kind = normalizeKind(kind);
        block.colorKey = "";
        block.completed = false;
        block.createdAt = now;
        block.updatedAt = now;
        return block;
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    /** The permanent identity of this block. Never changes. */
    public String getId() {
        return id;
    }

    /** The linked task id, or {@code null} when this block is independent. */
    public String getTaskId() {
        return taskId;
    }

    /** True when this block references a task. */
    public boolean isLinked() {
        return taskId != null;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = requireTitle(title);
        markUpdated();
    }

    public String getDateIso() {
        return dateIso;
    }

    public void setDateIso(String dateIso) {
        this.dateIso = requireDate(dateIso);
        markUpdated();
    }

    public int getStartMinute() {
        return startMinute;
    }

    public int getEndMinute() {
        return endMinute;
    }

    /**
     * Moves and/or resizes the block.
     *
     * @throws IllegalArgumentException when the interval is not structurally valid
     */
    public void setInterval(int startMinute, int endMinute) {
        if (startMinute < 0 || startMinute > IntervalRules.MINUTES_PER_DAY - 1) {
            throw new IllegalArgumentException(
                    IntervalRules.E_START_OUT_OF_RANGE + ", was: " + startMinute);
        }
        if (endMinute < 1 || endMinute > IntervalRules.MINUTES_PER_DAY) {
            throw new IllegalArgumentException(
                    IntervalRules.E_END_OUT_OF_RANGE + ", was: " + endMinute);
        }
        if (endMinute <= startMinute) {
            throw new IllegalArgumentException(
                    IntervalRules.E_END_NOT_AFTER_START
                            + ", start=" + startMinute + ", end=" + endMinute);
        }
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        markUpdated();
    }

    /** Length of the block in minutes. Always positive for a valid block. */
    public int getDurationMinutes() {
        return endMinute - startMinute;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = normalizeKind(kind);
        markUpdated();
    }

    /** Optional colour token. Kept independent of the app theme system. */
    public String getColorKey() {
        return colorKey;
    }

    public void setColorKey(String colorKey) {
        this.colorKey = colorKey == null ? "" : colorKey;
        markUpdated();
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        markUpdated();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Links this block to a task, or unlinks it when {@code taskId} is null.
     *
     * @throws IllegalArgumentException when a non-null id is not a canonical UUID
     */
    public void setTaskId(String taskId) {
        if (taskId == null) {
            this.taskId = null;
        } else {
            requireTaskId(taskId);
            this.taskId = taskId.trim();
        }
        markUpdated();
    }

    /**
     * Turns a linked block into an independent one, keeping everything else.
     *
     * <p>This is the mechanism behind the deletion rule: deleting a task never
     * deletes its blocks. The title, date, interval, kind and completion state
     * all survive, so the block keeps reading correctly on the timeline.</p>
     *
     * @return the previous task id, or {@code null} when the block was already
     *         independent
     */
    public String detachTask() {
        String previous = this.taskId;
        this.taskId = null;
        if (previous != null) {
            markUpdated();
        }
        return previous;
    }

    /**
     * Structural validation with no knowledge of the rest of the day.
     *
     * <p>Overlap and overbooking need a schedule, so they live in
     * {@link IntervalRules#validateAgainstSchedule}. This method only checks
     * what a single block can know on its own.</p>
     */
    public IntervalRules.ValidationResult validate() {
        IntervalRules.ValidationResult result = new IntervalRules.ValidationResult();

        if (title == null || title.trim().isEmpty()) {
            result.addError(IntervalRules.E_EMPTY_TITLE);
        }
        if (!IntervalRules.isValidDateIso(dateIso)) {
            result.addError(IntervalRules.E_DATE_INVALID);
        }
        if (startMinute < 0 || startMinute > IntervalRules.MINUTES_PER_DAY - 1) {
            result.addError(IntervalRules.E_START_OUT_OF_RANGE);
        }
        if (endMinute < 1 || endMinute > IntervalRules.MINUTES_PER_DAY) {
            result.addError(IntervalRules.E_END_OUT_OF_RANGE);
        }
        if (endMinute <= startMinute) {
            result.addError(IntervalRules.E_END_NOT_AFTER_START);
        }
        if (taskId != null && !UuidUtil.isValid(taskId)) {
            result.addError(IntervalRules.E_TASK_ID_INVALID);
        }
        if (!isKnownKind(kind)) {
            result.addError(IntervalRules.E_KIND_UNKNOWN);
        }
        if (result.isValid() && getDurationMinutes() < IntervalRules.SHORT_BLOCK_MINUTES) {
            result.addWarning(IntervalRules.W_SHORT_BLOCK);
        }

        return result;
    }

    /** Convenience: {@code true} when the block could be saved. */
    public boolean isValid() {
        return validate().isValid();
    }

    // ------------------------------------------------------------------
    // Display helpers
    // ------------------------------------------------------------------

    /** Formats a minute offset as {@code HH:mm} in 24-hour form. */
    public static String formatMinuteOfDay(int minuteOfDay) {
        int safe = Math.max(0, Math.min(IntervalRules.MINUTES_PER_DAY, minuteOfDay));
        int hours = safe / 60;
        int minutes = safe % 60;
        return String.format(Locale.US, "%02d:%02d", hours, minutes);
    }

    /** Human readable start and end, e.g. {@code 09:00 - 10:30}. */
    public String getTimeRangeLabel() {
        return formatMinuteOfDay(startMinute) + " - " + formatMinuteOfDay(endMinute);
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
            throw new IllegalArgumentException(IntervalRules.E_EMPTY_TITLE);
        }
        return trimmed;
    }

    private static String requireDate(String dateIso) {
        if (!IntervalRules.isValidDateIso(dateIso)) {
            throw new IllegalArgumentException(
                    IntervalRules.E_DATE_INVALID + ", was: " + dateIso);
        }
        return dateIso.trim();
    }

    private static void requireTaskId(String taskId) {
        if (!UuidUtil.isValid(taskId)) {
            throw new IllegalArgumentException(
                    IntervalRules.E_TASK_ID_INVALID + ", was: " + taskId);
        }
    }

    private static String normalizeKind(String kind) {
        if (kind == null) {
            return KIND_PERSONAL;
        }
        String trimmed = kind.trim().toLowerCase(Locale.US);
        return trimmed.isEmpty() ? KIND_PERSONAL : trimmed;
    }

    private static boolean isKnownKind(String kind) {
        return KIND_TASK.equals(kind)
                || KIND_CLASS.equals(kind)
                || KIND_BREAK.equals(kind)
                || KIND_MEAL.equals(kind)
                || KIND_PERSONAL.equals(kind);
    }

    /** Identity-based equality: same block only when ids match. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TimeBlock)) {
            return false;
        }
        TimeBlock that = (TimeBlock) other;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "TimeBlock{id=" + id + ", taskId=" + taskId + ", title=" + title
                + ", " + getTimeRangeLabel() + " on " + dateIso + "}";
    }
}
