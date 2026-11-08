package com.example.dailytodolist.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Covers overlap detection, validation severity, and pathological date input.
 */
public class IntervalRulesTest {

    private static final String DATE = "2026-11-06";
    private static final String OTHER_DATE = "2026-11-07";

    // ------------------------------------------------------------------
    // Half-open overlap
    // ------------------------------------------------------------------

    @Test
    public void backToBackIsNotAnOverlap() {
        // 09:00-10:00 then 10:00-11:00
        assertFalse(IntervalRules.overlaps(540, 600, 600, 660));
        assertFalse(IntervalRules.overlaps(600, 660, 540, 600));
    }

    @Test
    public void partialOverlapIsDetected() {
        // 09:00-10:00 vs 09:30-10:30
        assertTrue(IntervalRules.overlaps(540, 600, 570, 630));
        assertTrue(IntervalRules.overlaps(570, 630, 540, 600));
    }

    @Test
    public void containmentIsDetected() {
        // 09:00-12:00 contains 10:00-11:00
        assertTrue(IntervalRules.overlaps(540, 720, 600, 660));
        assertTrue(IntervalRules.overlaps(600, 660, 540, 720));
    }

    @Test
    public void identicalIntervalsOverlap() {
        assertTrue(IntervalRules.overlaps(540, 600, 540, 600));
    }

    @Test
    public void disjointIntervalsDoNotOverlap() {
        assertFalse(IntervalRules.overlaps(0, 60, 600, 660));
    }

    @Test
    public void oneMinuteTouchIsNotOverlap() {
        assertFalse(IntervalRules.overlaps(540, 600, 601, 660));
    }

    // ------------------------------------------------------------------
    // findOverlaps: date scoping and self exclusion
    // ------------------------------------------------------------------

    @Test
    public void findOverlaps_ignoresOtherDates() {
        TimeBlock candidate = TimeBlock.createIndependent("A", DATE, 540, 600, TimeBlock.KIND_TASK);
        TimeBlock otherDay = TimeBlock.createIndependent(
                "B", OTHER_DATE, 540, 600, TimeBlock.KIND_TASK);

        List<TimeBlock> conflicts = IntervalRules.findOverlaps(candidate, single(otherDay));
        assertTrue(conflicts.isEmpty());
    }

    @Test
    public void findOverlaps_findsSameDayConflict() {
        TimeBlock candidate = TimeBlock.createIndependent("A", DATE, 540, 600, TimeBlock.KIND_TASK);
        TimeBlock conflict = TimeBlock.createIndependent("B", DATE, 570, 630, TimeBlock.KIND_TASK);

        List<TimeBlock> conflicts = IntervalRules.findOverlaps(candidate, single(conflict));
        assertEquals(1, conflicts.size());
        assertEquals("B", conflicts.get(0).getTitle());
    }

    @Test
    public void findOverlaps_excludesItself() {
        TimeBlock candidate = TimeBlock.createIndependent("A", DATE, 540, 600, TimeBlock.KIND_TASK);
        List<TimeBlock> conflicts = IntervalRules.findOverlaps(candidate, single(candidate));
        assertTrue("a block cannot conflict with itself", conflicts.isEmpty());
    }

    @Test
    public void findOverlaps_handlesNullInput() {
        assertTrue(IntervalRules.findOverlaps(null, null).isEmpty());
        TimeBlock block = TimeBlock.createIndependent("A", DATE, 540, 600, TimeBlock.KIND_TASK);
        assertTrue(IntervalRules.findOverlaps(block, null).isEmpty());
    }

    // ------------------------------------------------------------------
    // validateAgainstSchedule: warnings must not block saving
    // ------------------------------------------------------------------

    @Test
    public void overlapWarnsButStaysValid() {
        TimeBlock existing = TimeBlock.createIndependent("Existing", DATE, 540, 600, TimeBlock.KIND_TASK);
        TimeBlock candidate = TimeBlock.createIndependent("New", DATE, 570, 630, TimeBlock.KIND_TASK);

        IntervalRules.ValidationResult result =
                IntervalRules.validateAgainstSchedule(candidate, single(existing));

        assertTrue("overlaps are a warning, not an error", result.isValid());
        assertTrue(result.getWarnings().contains(IntervalRules.W_OVERLAP));
    }

    @Test
    public void backToBackScheduleProducesNoWarnings() {
        TimeBlock existing = TimeBlock.createIndependent("First", DATE, 540, 600, TimeBlock.KIND_TASK);
        TimeBlock candidate = TimeBlock.createIndependent("Second", DATE, 600, 660, TimeBlock.KIND_TASK);

        IntervalRules.ValidationResult result =
                IntervalRules.validateAgainstSchedule(candidate, single(existing));

        assertTrue(result.isValid());
        assertFalse("back-to-back must not warn", result.hasWarnings());
    }

    @Test
    public void duplicateTaskLinkWarns() {
        Task task = Task.createNew("Report", DATE, Task.ORIGIN_PLANNER);

        TimeBlock first = TimeBlock.createLinked(
                task.getId(), "Report", DATE, 540, 600, TimeBlock.KIND_TASK);
        TimeBlock second = TimeBlock.createLinked(
                task.getId(), "Report", DATE, 900, 960, TimeBlock.KIND_TASK);

        IntervalRules.ValidationResult result =
                IntervalRules.validateAgainstSchedule(second, single(first));

        assertTrue(result.isValid());
        assertTrue(result.getWarnings().contains(IntervalRules.W_DUPLICATE_TASK));
    }

    @Test
    public void independentBlocksNeverTriggerDuplicateTaskWarning() {
        TimeBlock first = TimeBlock.createIndependent("Lunch", DATE, 720, 780, TimeBlock.KIND_MEAL);
        TimeBlock second = TimeBlock.createIndependent("Lunch", DATE, 720, 780, TimeBlock.KIND_MEAL);

        IntervalRules.ValidationResult result =
                IntervalRules.validateAgainstSchedule(second, single(first));

        assertFalse(result.getWarnings().contains(IntervalRules.W_DUPLICATE_TASK));
    }

    @Test
    public void overbookedDayWarns() {
        List<TimeBlock> existing = new ArrayList<>();
        // 13 hours already booked.
        for (int i = 0; i < 13; i++) {
            existing.add(TimeBlock.createIndependent(
                    "Block " + i, DATE, i * 60, (i + 1) * 60, TimeBlock.KIND_TASK));
        }
        // Candidate adds another 12 hours -> 25h total.
        TimeBlock candidate = TimeBlock.createIndependent(
                "Overnight", DATE, 12 * 60, IntervalRules.MINUTES_PER_DAY, TimeBlock.KIND_TASK);

        IntervalRules.ValidationResult result =
                IntervalRules.validateAgainstSchedule(candidate, existing);

        assertTrue(result.isValid());
        assertTrue(result.getWarnings().contains(IntervalRules.W_OVERBOOKED));
    }

    @Test
    public void structuralErrorsAreReportedAsErrors() {
        TimeBlock block = TimeBlock.createIndependent("X", DATE, 540, 600, TimeBlock.KIND_TASK);
        block.setInterval(960, 1020);
        // Force an invalid interval by mutating through the setter's validated path
        // is not possible, so simulate a corrupt record loaded from storage.
        TimeBlock corrupt = new TimeBlock();
        corrupt.setKind(TimeBlock.KIND_TASK);

        IntervalRules.ValidationResult result =
                IntervalRules.validateAgainstSchedule(corrupt, single(block));

        assertFalse("a structurally broken block must not be valid", result.isValid());
        assertTrue(result.getErrors().contains(IntervalRules.E_EMPTY_TITLE));
    }

    @Test
    public void nullCandidateIsNotValid() {
        IntervalRules.ValidationResult result =
                IntervalRules.validateAgainstSchedule(null, new ArrayList<TimeBlock>());
        assertFalse(result.isValid());
    }

    // ------------------------------------------------------------------
    // Dangling blocks (deleted task) — read-only reporting
    // ------------------------------------------------------------------

    @Test
    public void danglingBlockIsReportedSeparately() {
        Task alive = Task.createNew("Alive", DATE, Task.ORIGIN_PLANNER);
        Task deleted = Task.createNew("Deleted", DATE, Task.ORIGIN_PLANNER);

        TimeBlock goodBlock = TimeBlock.createLinked(
                alive.getId(), "Alive", DATE, 540, 600, TimeBlock.KIND_TASK);
        TimeBlock orphanBlock = TimeBlock.createLinked(
                deleted.getId(), "Deleted", DATE, 900, 960, TimeBlock.KIND_TASK);

        List<TimeBlock> all = new ArrayList<>();
        all.add(goodBlock);
        all.add(orphanBlock);

        List<Task> known = new ArrayList<>();
        known.add(alive); // deleted task is gone

        List<TimeBlock> dangling = IntervalRules.findDanglingBlocks(all, known);

        assertEquals(1, dangling.size());
        assertEquals(orphanBlock.getId(), dangling.get(0).getId());
        assertEquals("the orphan keeps its title and times", "Deleted",
                dangling.get(0).getTitle());
    }

    @Test
    public void independentBlocksAreNeverDangling() {
        TimeBlock lunch = TimeBlock.createIndependent("Lunch", DATE, 720, 780, TimeBlock.KIND_MEAL);
        List<TimeBlock> blocks = single(lunch);

        assertTrue(IntervalRules.findDanglingBlocks(blocks, new ArrayList<Task>()).isEmpty());
    }

    @Test
    public void findDanglingHandlesNulls() {
        assertTrue(IntervalRules.findDanglingBlocks(null, null).isEmpty());
        assertTrue(IntervalRules.findDanglingBlocks(new ArrayList<TimeBlock>(), null).isEmpty());
    }

    // ------------------------------------------------------------------
    // Date validation
    // ------------------------------------------------------------------

    @Test
    public void acceptsValidDates() {
        assertTrue(IntervalRules.isValidDateIso("2026-11-06"));
        assertTrue(IntervalRules.isValidDateIso("2024-02-29"));
        assertTrue(IntervalRules.isValidDateIso("2000-02-29"));
        assertTrue(IntervalRules.isValidDateIso("2026-12-31"));
    }

    @Test
    public void rejectsImpossibleDates() {
        assertFalse(IntervalRules.isValidDateIso("2026-02-30"));
        assertFalse(IntervalRules.isValidDateIso("2026-13-01"));
        assertFalse(IntervalRules.isValidDateIso("2026-00-10"));
        assertFalse(IntervalRules.isValidDateIso("2026-04-31"));
        assertFalse(IntervalRules.isValidDateIso("1900-02-29"));
    }

    @Test
    public void rejectsMalformedDates() {
        assertFalse(IntervalRules.isValidDateIso(null));
        assertFalse(IntervalRules.isValidDateIso(""));
        assertFalse(IntervalRules.isValidDateIso("2026-1-6"));
        assertFalse(IntervalRules.isValidDateIso("06/11/2026"));
        assertFalse(IntervalRules.isValidDateIso("2026-11-06T00:00"));
        assertFalse(IntervalRules.isValidDateIso("abcd-ef-gh"));
    }

    @Test
    public void dateStringsCompareChronologically() {
        // Storing ISO strings means plain string comparison is date order.
        assertTrue("2026-11-06".compareTo("2026-11-07") < 0);
        assertNotEquals("2026-11-06", "2026-11-07");
    }

    @Test
    public void constantsAreSane() {
        assertEquals(1440, IntervalRules.MINUTES_PER_DAY);
        assertEquals(5, IntervalRules.SHORT_BLOCK_MINUTES);
        assertNull(null);
    }

    private static List<TimeBlock> single(TimeBlock block) {
        List<TimeBlock> list = new ArrayList<>();
        list.add(block);
        return list;
    }
}
