package com.example.dailytodolist.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure date/time and overlap rules for time blocking.
 *
 * <p><b>Half-open intervals.</b> A block occupies {@code [start, end)}: the start
 * minute is inside the block and the end minute is the first minute after it.
 * Two blocks therefore do <em>not</em> conflict when one ends exactly where the
 * next begins. Without this rule every back-to-back schedule (09:00-10:00 then
 * 10:00-11:00) would report a false conflict.</p>
 *
 * <p><b>Times are stored as minutes since midnight</b>, never as display
 * strings. Integers compare the same way in every locale, and a display string
 * such as {@code "10:00 AM"} changes meaning when the app language changes.</p>
 *
 * <p><b>Severity.</b> Only structural problems are hard errors. Overlaps,
 * short blocks and overloaded days are warnings, because a planner that refuses
 * to let you put a break inside a long study block is a planner people stop
 * using.</p>
 *
 * <p>This class is plain Java and must stay free of Android imports.</p>
 */
public final class IntervalRules {

    /** Minutes in one day. Blocks never cross midnight in this version. */
    public static final int MINUTES_PER_DAY = 1440;

    /** Blocks shorter than this are almost always a mistake, so they warn. */
    public static final int SHORT_BLOCK_MINUTES = 5;

    // Hard errors: the block must not be saved.
    public static final String E_BLOCK_MISSING = "block is missing";
    public static final String E_EMPTY_TITLE = "title is empty";
    public static final String E_TASK_ID_INVALID = "taskId is not a canonical UUID";
    public static final String E_START_OUT_OF_RANGE = "startMinute must be within 0..1439";
    public static final String E_END_OUT_OF_RANGE = "endMinute must be within 1..1440";
    public static final String E_END_NOT_AFTER_START = "endMinute must be greater than startMinute";
    public static final String E_DATE_INVALID = "date must be a real yyyy-MM-dd date";
    public static final String E_KIND_UNKNOWN = "kind is not a recognised block kind";

    // Warnings: save, but tell the user.
    public static final String W_OVERLAP = "overlaps another block on the same day";
    public static final String W_SHORT_BLOCK = "block is shorter than 5 minutes";
    public static final String W_DUPLICATE_TASK = "another block on this day links the same task";
    public static final String W_OVERBOOKED = "more than 24 hours booked on this day";

    private IntervalRules() {
        // Static helpers only.
    }

    // ------------------------------------------------------------------
    // Overlap
    // ------------------------------------------------------------------

    /**
     * Half-open overlap test.
     *
     * <pre>
     * 09:00-10:00 vs 10:00-11:00 -> false (back to back)
     * 09:00-10:00 vs 09:30-10:30 -> true  (partial)
     * 09:00-12:00 vs 10:00-11:00 -> true  (containment)
     * 09:00-10:00 vs 09:00-10:00 -> true  (identical)
     * </pre>
     */
    public static boolean overlaps(int aStart, int aEnd, int bStart, int bEnd) {
        return aStart < bEnd && bStart < aEnd;
    }

    /**
     * Returns the blocks in {@code others} that conflict with {@code candidate} on
     * the same date. A block never conflicts with itself.
     */
    public static List<TimeBlock> findOverlaps(TimeBlock candidate, List<TimeBlock> others) {
        List<TimeBlock> conflicts = new ArrayList<>();
        if (candidate == null || others == null) {
            return conflicts;
        }

        for (TimeBlock other : others) {
            if (other == null || other == candidate) {
                continue;
            }
            if (candidate.getId() != null && candidate.getId().equals(other.getId())) {
                continue;
            }
            if (candidate.getDateIso() == null
                    || !candidate.getDateIso().equals(other.getDateIso())) {
                continue;
            }
            if (overlaps(candidate.getStartMinute(), candidate.getEndMinute(),
                    other.getStartMinute(), other.getEndMinute())) {
                conflicts.add(other);
            }
        }
        return conflicts;
    }

    /**
     * Validates one block against the rest of the day's schedule.
     *
     * <p>Combines the structural check with the schedule-level warnings. Use this
     * before saving from the planner UI; use {@link TimeBlock#validate()} when no
     * schedule is available.</p>
     */
    public static ValidationResult validateAgainstSchedule(TimeBlock candidate,
                                                           List<TimeBlock> sameDayBlocks) {
        if (candidate == null) {
            ValidationResult missing = new ValidationResult();
            missing.addError(E_BLOCK_MISSING);
            return missing;
        }

        ValidationResult result = candidate.validate();

        List<TimeBlock> others = new ArrayList<>();
        if (sameDayBlocks != null) {
            others.addAll(sameDayBlocks);
        }

        if (!findOverlaps(candidate, others).isEmpty()) {
            result.addWarning(W_OVERLAP);
        }

        if (candidate.getDurationMinutes() < SHORT_BLOCK_MINUTES) {
            result.addWarning(W_SHORT_BLOCK);
        }

        String taskId = candidate.getTaskId();
        if (taskId != null) {
            for (TimeBlock other : others) {
                if (other == null || other.getId() != null
                        && other.getId().equals(candidate.getId())) {
                    continue;
                }
                if (taskId.equals(other.getTaskId())) {
                    result.addWarning(W_DUPLICATE_TASK);
                    break;
                }
            }
        }

        int booked = 0;
        for (TimeBlock other : others) {
            if (other == null || other.getId() != null
                    && other.getId().equals(candidate.getId())) {
                continue;
            }
            if (candidate.getDateIso() != null
                    && candidate.getDateIso().equals(other.getDateIso())) {
                booked += other.getDurationMinutes();
            }
        }
        if (booked + candidate.getDurationMinutes() > MINUTES_PER_DAY) {
            result.addWarning(W_OVERBOOKED);
        }

        return result;
    }

    /**
     * Detects blocks whose {@code taskId} no longer resolves to a known task.
     *
     * <p>Used when loading the planner so a stale reference is shown as
     * "linked task was deleted" instead of failing silently. This method only
     * reports; it never mutates anything.</p>
     */
    public static List<TimeBlock> findDanglingBlocks(List<TimeBlock> blocks,
                                                     List<Task> knownTasks) {
        List<TimeBlock> dangling = new ArrayList<>();
        if (blocks == null) {
            return dangling;
        }

        List<Task> safeTasks = knownTasks == null
                ? Collections.<Task>emptyList() : knownTasks;

        for (TimeBlock block : blocks) {
            if (block == null || block.getTaskId() == null) {
                continue;
            }
            boolean found = false;
            for (Task task : safeTasks) {
                if (task != null && block.getTaskId().equals(task.getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                dangling.add(block);
            }
        }
        return dangling;
    }

    // ------------------------------------------------------------------
    // Dates
    // ------------------------------------------------------------------

    /**
     * Validates a {@code yyyy-MM-dd} string, including real month lengths.
     *
     * <p>Deliberately manual rather than {@code java.time}: {@code LocalDate}
     * requires API 26 and this project's minSdk is 24.</p>
     */
    public static boolean isValidDateIso(String dateIso) {
        if (dateIso == null || dateIso.length() != 10) {
            return false;
        }
        if (dateIso.charAt(4) != '-' || dateIso.charAt(7) != '-') {
            return false;
        }

        int year;
        int month;
        int day;
        try {
            year = Integer.parseInt(dateIso.substring(0, 4));
            month = Integer.parseInt(dateIso.substring(5, 7));
            day = Integer.parseInt(dateIso.substring(8, 10));
        } catch (NumberFormatException e) {
            return false;
        }

        if (month < 1 || month > 12) {
            return false;
        }
        return day >= 1 && day <= daysInMonth(year, month);
    }

    private static int daysInMonth(int year, int month) {
        switch (month) {
            case 2:
                return isLeapYear(year) ? 29 : 28;
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            default:
                return 31;
        }
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }

    // ------------------------------------------------------------------
    // Result type
    // ------------------------------------------------------------------

    /**
     * Outcome of validating one block: hard errors and soft warnings.
     *
     * <p>The block may be saved only when {@link #isValid()} is true. Warnings
     * never block saving.</p>
     */
    public static final class ValidationResult {

        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addError(String message) {
            if (message != null && !errors.contains(message)) {
                errors.add(message);
            }
        }

        public void addWarning(String message) {
            if (message != null && !warnings.contains(message)) {
                warnings.add(message);
            }
        }

        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        /** True when there are no hard errors. */
        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        @Override
        public String toString() {
            return "ValidationResult{valid=" + isValid()
                    + ", errors=" + errors + ", warnings=" + warnings + "}";
        }
    }
}
