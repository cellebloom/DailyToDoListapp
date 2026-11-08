package com.example.dailytodolist;

import com.example.dailytodolist.model.TimeCategory;
import com.example.dailytodolist.util.UuidUtil;

/**
 * One task on the home list, with its scheduled time block.
 *
 * <p><b>Identity.</b> Every task carries a permanent {@link #getId()}, a UUID
 * minted exactly once at creation by {@link UuidUtil#newId()}. Storage is keyed
 * by that id instead of by list position.</p>
 *
 * <p>This replaces the previous behaviour where the whole task was addressed by
 * its index in an {@code ArrayList}: deleting the task in slot 2 shifted every
 * later task down one slot, so their stored time suffixed keys
 * ({@code task_shour_2}, ...) silently became another task's time. With ids,
 * deleting a neighbour changes nothing about any surviving task.</p>
 *
 * <p>Times are stored as minutes since midnight, the same canonical form the
 * model layer uses ({@code IntervalRules}), never as display text. The old
 * hour/minute getters are kept as derived accessors so existing call sites and
 * the calendar screen keep working.</p>
 */
public class TaskWithDate implements java.io.Serializable {

    private static final long serialVersionUID = 2L;

    private String id;
    private String taskName;
    private int year;
    private int month;
    private int day;
    private boolean isChecked;

    private int startMinute;
    private int endMinute;

    private boolean hasTime = true;

    private boolean isTopPriority;
    private String category;
    private String colorHex;

    /**
     * Optional free-text note. Empty string, never null, so callers can compare
     * and render without null checks. Absent in storage written before this
     * field existed, which reads back as empty.
     */
    private String description = "";

    // --- CONSTRUCTOR 1 (Basic) ---
    public TaskWithDate(String taskName, int year, int month, int day) {
        this.id = UuidUtil.newId();
        this.taskName = taskName;
        this.year = year;
        this.month = month;
        this.day = day;
        this.isChecked = false;
        this.isTopPriority = false;
        this.category = TimeCategory.OTHER.getStorageKey();
        this.colorHex = "";
        this.startMinute = 0;
        this.endMinute = 0;
        this.hasTime = true;
    }

    // --- CONSTRUCTOR 2 (Full) ---
    public TaskWithDate(String taskName, int year, int month, int day,
                        int startTimeHour, int startTimeMinute,
                        int endTimeHour, int endTimeMinute,
                        boolean isTopPriority, String category, String colorHex) {
        this.id = UuidUtil.newId();
        this.taskName = taskName;
        this.year = year;
        this.month = month;
        this.day = day;
        // A -1 hour means "Anytime": keep the sentinel rather than computing
        // -60, so the task never pretends to start at 23:00 the previous day.
        if (startTimeHour == NO_TIME) {
            this.startMinute = NO_TIME;
            this.endMinute = NO_TIME;
            this.hasTime = false;
        } else {
            this.startMinute = startTimeHour * 60 + startTimeMinute;
            this.endMinute = endTimeHour == NO_TIME
                    ? this.startMinute
                    : endTimeHour * 60 + endTimeMinute;
            this.hasTime = true;
        }
        this.isTopPriority = isTopPriority;
        this.category = category == null ? TimeCategory.OTHER.getStorageKey() : category;
        this.colorHex = colorHex == null ? "" : colorHex;
        this.isChecked = false;
    }

    /**
     * Builds a task from a date and minute-of-day values, which is what the
     * add-task screen returns.
     *
     * @param dateIso {@code yyyy-MM-dd}
     */
    public static TaskWithDate fromMinuteOfDay(String taskName,
                                               String dateIso,
                                               int startMinute,
                                               int endMinute,
                                               boolean isTopPriority,
                                               String category) {
        return fromMinuteOfDay(taskName, "", dateIso, startMinute, endMinute,
                isTopPriority, category);
    }

    /** Overload that also carries the optional description. */
    public static TaskWithDate fromMinuteOfDay(String taskName,
                                               String description,
                                               String dateIso,
                                               int startMinute,
                                               int endMinute,
                                               boolean isTopPriority,
                                               String category) {
        int year = 1970;
        int month = 1;
        int day = 1;
        if (dateIso != null && dateIso.length() == 10) {
            try {
                year = Integer.parseInt(dateIso.substring(0, 4));
                month = Integer.parseInt(dateIso.substring(5, 7));
                day = Integer.parseInt(dateIso.substring(8, 10));
            } catch (NumberFormatException ignored) {
                // Fall through to the epoch defaults rather than crashing a save.
            }
        }

        TaskWithDate task = new TaskWithDate(taskName, year, month, day);
        task.setDescription(description);
        if (startMinute == NO_TIME) {
            task.startMinute = NO_TIME;
            task.endMinute = NO_TIME;
            task.hasTime = false;
        } else {
            task.setStartMinute(startMinute);
            task.setEndMinute(endMinute == NO_TIME ? startMinute : endMinute);
        }
        task.setTopPriority(isTopPriority);
        task.setCategory(category);
        return task;
    }

    /**
     * Restores the anytime / scheduled flag when rehydrating from storage.
     *
     * <p>Storage only keeps the minute values, so the flag is reconstructed from
     * them; this setter exists for the case where storage and the flag disagree
     * and the stored value should win.</p>
     */
    public void restoreHasTime(boolean hasTime) {
        if (hasTime && startMinute == NO_TIME) {
            return;
        }
        this.hasTime = hasTime;
        if (!hasTime) {
            this.startMinute = NO_TIME;
            this.endMinute = NO_TIME;
        }
    }

    // --- IDENTITY ------------------------------------------------------

    /** The permanent identity of this task. Never changes. */
    public String getId() {
        return id;
    }

    /**
     * Assigns an id only when the task does not already have a valid one.
     *
     * <p>This is the migration safety net: rows loaded from the legacy
     * positional storage had no id, so they receive one on first load. An
     * existing valid id is never overwritten.</p>
     *
     * @return true when a new id was minted
     */
    public boolean ensureId() {
        if (UuidUtil.isValid(id)) {
            return false;
        }
        this.id = UuidUtil.newId();
        return true;
    }

    /**
     * Restores a specific id when rehydrating from storage.
     *
     * <p>Public because {@code TaskStore} lives in another package. It still
     * refuses anything that is not a canonical UUID, so a corrupted stored value
     * can never become a permanent identity.</p>
     */
    public void restoreId(String storedId) {
        if (UuidUtil.isValid(storedId)) {
            this.id = storedId;
        } else {
            ensureId();
        }
    }

    // --- GETTERS AND SETTERS ---

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }

    // --- Canonical time representation ---

    /**
     * Sentinel meaning "this task has no time block".
     *
     * <p>Callers pass {@code startMinute == NO_TIME} (and normally
     * {@code endMinute == NO_TIME}) for an Anytime task.</p>
     */
    public static final int NO_TIME = -1;

    public int getStartMinute() { return startMinute; }

    /**
     * Sets the start. Passing {@link #NO_TIME} turns this into an Anytime task,
     * which is the switch shown on the add-task screen.
     */
    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
        if (startMinute == NO_TIME) {
            this.hasTime = false;
            if (endMinute >= 0) {
                this.endMinute = NO_TIME;
            }
        } else {
            this.hasTime = true;
        }
    }

    public int getEndMinute() { return endMinute; }

    public void setEndMinute(int endMinute) {
        this.endMinute = endMinute;
        if (endMinute != NO_TIME && startMinute == NO_TIME) {
            // An end without a start is not a valid block; treat it as Anytime.
            this.startMinute = NO_TIME;
        }
    }

    /**
     * True when this task has no scheduled time block.
     *
     * <p>Derived from the stored state rather than from a divider on
     * {@link #getStartMinute()}, because {@code -1 / 60} is {@code 0} in Java
     * integer division: code that branches on {@code getStartTimeHour() == -1}
     * would silently treat an Anytime task as 00:00.</p>
     */
    public boolean isAnytime() {
        return !hasTime || startMinute == NO_TIME;
    }

    /**
     * Start hour, or {@link #NO_TIME} for an Anytime task.
     *
     * <p>Returns the sentinel unchanged so callers can test
     * {@code == NO_TIME} instead of guessing from a truncated value.</p>
     */
    public int getStartTimeHour() {
        return isAnytime() ? NO_TIME : startMinute / 60;
    }

    public int getStartTimeMinute() {
        return isAnytime() ? NO_TIME : startMinute % 60;
    }

    /** End hour, or {@link #NO_TIME} for an Anytime task. */
    public int getEndTimeHour() {
        return isAnytime() ? NO_TIME : endMinute / 60;
    }

    public int getEndTimeMinute() {
        return isAnytime() ? NO_TIME : endMinute % 60;
    }

    public void setStartTimeHour(int hour) {
        setStartMinute(hour == NO_TIME ? NO_TIME : hour * 60 + Math.max(0, getStartTimeMinute()));
    }

    public void setStartTimeMinute(int minute) {
        setStartMinute(isAnytime() ? NO_TIME : getStartTimeHour() * 60 + minute);
    }

    public void setEndTimeHour(int hour) {
        this.endMinute = hour == NO_TIME ? NO_TIME : hour * 60 + Math.max(0, getEndTimeMinute());
    }

    public void setEndTimeMinute(int minute) {
        this.endMinute = isAnytime() ? NO_TIME : getEndTimeHour() * 60 + minute;
    }

    /** Duration in minutes, or 0 for an Anytime task. */
    public int getDurationMinutes() {
        return isAnytime() ? 0 : endMinute - startMinute;
    }

    /** {@code HH:mm - HH:mm}, or empty text for an Anytime task. */
    public String getTimeRangeLabel() {
        if (isAnytime()) {
            return "";
        }
        return TimeCategory.formatMinuteOfDay(startMinute)
                + " - " + TimeCategory.formatMinuteOfDay(endMinute);
    }

    /** {@code yyyy-MM-dd}. */
    public String getDateIso() {
        return String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month, day);
    }

    // --- Priority / category ---

    public boolean isTopPriority() { return isTopPriority; }
    public void setTopPriority(boolean topPriority) { isTopPriority = topPriority; }

    public String getCategory() { return category; }

    public void setCategory(String category) {
        this.category = category == null ? TimeCategory.OTHER.getStorageKey() : category;
    }

    /** Typed category; unknown stored values resolve to {@link TimeCategory#OTHER}. */
    public TimeCategory getTimeCategory() { return TimeCategory.fromStorageKey(category); }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) {
        this.colorHex = colorHex == null ? "" : colorHex;
    }

    // --- Description ---------------------------------------------------

    /** Optional description. Never null; empty when not set. */
    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    /** True when this task carries a non-blank description. */
    public boolean hasDescription() {
        return !getDescription().trim().isEmpty();
    }
}
