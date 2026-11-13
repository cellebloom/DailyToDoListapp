package com.example.dailytodolist.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Buckets scheduled tasks into the hour slots of a timeline.
 *
 * <p>Kept as pure Java (no Android imports) so the placement logic can be unit
 * tested without an emulator. The adapter only renders what this returns.</p>
 *
 * <p><b>Placement rule.</b> A task is shown in the slot that contains the hour
 * of its start time: a task starting 09:00 appears next to the "09:00" label,
 * and one starting 09:45 appears there too (it would be misleading to hide it
 * until 10:00). Tasks outside the visible range are clamped to the nearest
 * slot rather than dropped, so nothing silently disappears: a 05:30 task lands
 * in the first slot and a 23:30 task lands in the last.</p>
 */
public final class TimelineSlots {

    /** First hour rendered by default. */
    public static final int DEFAULT_FIRST_HOUR = 6;
    /** Last hour rendered by default. */
    public static final int DEFAULT_LAST_HOUR = 23;

    private final int firstHour;
    private final int lastHour;
    private final Map<Integer, List<TimeBlockRow>> byHour = new HashMap<>();

    /**
     * A minimal view of a scheduled task, so this class does not depend on the
     * Android-facing task class.
     */
    public static final class TimeBlockRow {
        private final String title;
        private final int startMinute;
        private final int endMinute;

        public TimeBlockRow(String title, int startMinute, int endMinute) {
            this.title = title;
            this.startMinute = startMinute;
            this.endMinute = endMinute;
        }

        public String getTitle() {
            return title;
        }

        public int getStartMinute() {
            return startMinute;
        }

        public int getEndMinute() {
            return endMinute;
        }
    }

    public TimelineSlots() {
        this(DEFAULT_FIRST_HOUR, DEFAULT_LAST_HOUR);
    }

    public TimelineSlots(int firstHour, int lastHour) {
        if (lastHour < firstHour) {
            throw new IllegalArgumentException(
                    "lastHour must be >= firstHour, was: " + firstHour + ".." + lastHour);
        }
        this.firstHour = firstHour;
        this.lastHour = lastHour;
    }

    public int getFirstHour() {
        return firstHour;
    }

    public int getLastHour() {
        return lastHour;
    }

    /** Number of hour rows to render, inclusive of both ends. */
    public int getSlotCount() {
        return lastHour - firstHour + 1;
    }

    /** Hour number for slot {@code index}. */
    public int hourAt(int index) {
        return firstHour + index;
    }

    /**
     * Places a task into the slot matching its start hour.
     *
     * @return the slot index the task was placed in
     */
    public int add(TimeBlockRow row) {
        int hour = row.getStartMinute() / 60;
        int index = hour - firstHour;
        // Clamp rather than drop: a task outside the visible window should still
        // be reachable, otherwise it vanishes from the only timeline there is.
        if (index < 0) {
            index = 0;
        } else if (index >= getSlotCount()) {
            index = getSlotCount() - 1;
        }

        List<TimeBlockRow> bucket = byHour.get(index);
        if (bucket == null) {
            bucket = new ArrayList<>();
            byHour.put(index, bucket);
        }
        bucket.add(row);
        return index;
    }

    /** Tasks placed in the slot at {@code index}, never null. */
    public List<TimeBlockRow> getRowsAt(int index) {
        List<TimeBlockRow> bucket = byHour.get(index);
        return bucket == null ? new ArrayList<>() : bucket;
    }

    /** Label for a slot, e.g. {@code 09:00 - 10:00}.
     *
     * <p>The range form matches a paper planner's TIME column, where each row
     * names the hour it covers rather than a single instant.</p>
     */
    public String getSlotLabel(int index) {
        int start = hourAt(index) * 60;
        return TimeCategory.formatMinuteOfDay(start)
                + " - " + TimeCategory.formatMinuteOfDay(Math.min(start + 60, 24 * 60));
    }
}
