package com.example.dailytodolist.model;

import com.example.dailytodolist.util.UuidUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Covers time blocks: independent blocks with no task, linked blocks, and the
 * guarantee that detaching (task deletion) never destroys the block.
 */
public class TimeBlockTest {

    private static final String DATE = "2026-11-06";

    // ------------------------------------------------------------------
    // Independent blocks (requirement: must exist without a Task)
    // ------------------------------------------------------------------

    @Test
    public void independentBlock_hasNoTaskId() {
        TimeBlock lunch = TimeBlock.createIndependent(
                "Lunch", DATE, 12 * 60, 13 * 60, TimeBlock.KIND_MEAL);

        assertNull(lunch.getTaskId());
        assertFalse(lunch.isLinked());
        assertEquals("Lunch", lunch.getTitle());
        assertEquals(60, lunch.getDurationMinutes());
        assertTrue(UuidUtil.isValid(lunch.getId()));
    }

    @Test
    public void independentBlock_isValidWithoutAnyTaskExisting() {
        // Nothing here references a Task; validation must pass on its own.
        TimeBlock lecture = TimeBlock.createIndependent(
                "Algorithms", DATE, 14 * 60, 15 * 60 + 30, TimeBlock.KIND_CLASS);

        assertTrue(lecture.isValid());
        assertTrue(lecture.validate().getErrors().isEmpty());
    }

    @Test
    public void everyIndependentKindIsSupported() {
        String[] kinds = {
                TimeBlock.KIND_CLASS,
                TimeBlock.KIND_BREAK,
                TimeBlock.KIND_MEAL,
                TimeBlock.KIND_PERSONAL
        };

        for (String kind : kinds) {
            TimeBlock block = TimeBlock.createIndependent("X", DATE, 540, 600, kind);
            assertEquals(kind, block.getKind());
            assertTrue("kind " + kind + " should validate", block.isValid());
        }
    }

    @Test
    public void unknownKindIsRejectedByValidation() {
        TimeBlock block = TimeBlock.createIndependent("X", DATE, 540, 600, TimeBlock.KIND_MEAL);
        block.setKind("nonsense");

        assertFalse(block.isValid());
        assertTrue(block.validate().getErrors()
                .contains(IntervalRules.E_KIND_UNKNOWN));
    }

    @Test
    public void nullKindFallsBackToPersonal() {
        TimeBlock block = TimeBlock.createIndependent("X", DATE, 540, 600, null);
        assertEquals(TimeBlock.KIND_PERSONAL, block.getKind());
    }

    // ------------------------------------------------------------------
    // Linked blocks
    // ------------------------------------------------------------------

    @Test
    public void linkedBlock_storesTaskId() {
        Task task = Task.createNew("Write report", DATE, Task.ORIGIN_PLANNER);
        TimeBlock block = TimeBlock.createLinked(
                task.getId(), task.getTitle(), DATE, 540, 630, TimeBlock.KIND_TASK);

        assertTrue(block.isLinked());
        assertEquals(task.getId(), block.getTaskId());
        assertEquals(90, block.getDurationMinutes());
        assertTrue(block.isValid());
    }

    @Test
    public void linkedBlock_rejectsNonUuidTaskId() {
        try {
            TimeBlock.createLinked("home:3", "Title", DATE, 540, 600, TimeBlock.KIND_TASK);
            fail("positional task id must be rejected");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }

    @Test
    public void linkedBlock_keepsItsOwnTitleEvenIfTaskTitleChanges() {
        Task task = Task.createNew("Draft", DATE, Task.ORIGIN_PLANNER);
        TimeBlock block = TimeBlock.createLinked(
                task.getId(), "Draft", DATE, 540, 600, TimeBlock.KIND_TASK);

        task.setTitle("Final");

        assertEquals("block keeps its own copy of the text", "Draft", block.getTitle());
    }

    // ------------------------------------------------------------------
    // Deletion safety (requirement: deleting a task must not delete blocks)
    // ------------------------------------------------------------------

    @Test
    public void detach_clearsTaskIdButPreservesEverythingElse() {
        Task task = Task.createNew("Write report", DATE, Task.ORIGIN_PLANNER);
        TimeBlock block = TimeBlock.createLinked(
                task.getId(), "Write report", DATE, 540, 630, TimeBlock.KIND_TASK);
        block.setCompleted(true);

        String blockId = block.getId();
        String previousTask = block.detachTask();

        assertEquals(task.getId(), previousTask);
        assertNull("block must survive as independent", block.getTaskId());
        assertFalse(block.isLinked());
        // Everything else untouched.
        assertEquals(blockId, block.getId());
        assertEquals("Write report", block.getTitle());
        assertEquals(DATE, block.getDateIso());
        assertEquals(540, block.getStartMinute());
        assertEquals(630, block.getEndMinute());
        assertEquals(TimeBlock.KIND_TASK, block.getKind());
        assertTrue(block.isCompleted());
    }

    @Test
    public void detach_onIndependentBlockIsHarmless() {
        TimeBlock block = TimeBlock.createIndependent("Lunch", DATE, 720, 780, TimeBlock.KIND_MEAL);
        String id = block.getId();

        assertNull(block.detachTask());
        assertEquals(id, block.getId());
        assertNull(block.getTaskId());
    }

    @Test
    public void detachedBlockIsStillValid() {
        Task task = Task.createNew("Task", DATE, Task.ORIGIN_PLANNER);
        TimeBlock block = TimeBlock.createLinked(
                task.getId(), "Task", DATE, 540, 600, TimeBlock.KIND_TASK);

        block.detachTask();

        assertTrue("a detached block must remain saveable", block.isValid());
    }

    @Test
    public void blockIdIsIndependentOfTaskId() {
        Task task = Task.createNew("Task", DATE, Task.ORIGIN_PLANNER);
        TimeBlock block = TimeBlock.createLinked(
                task.getId(), "Task", DATE, 540, 600, TimeBlock.KIND_TASK);

        assertNotEquals("block identity is its own", task.getId(), block.getId());
        assertTrue(UuidUtil.isValid(block.getId()));
    }

    @Test
    public void blockIdSurvivesRescheduling() {
        TimeBlock block = TimeBlock.createIndependent("Study", DATE, 540, 600, TimeBlock.KIND_TASK);
        String id = block.getId();

        block.setInterval(900, 1020);
        block.setDateIso("2026-11-07");
        block.setTitle("Study harder");

        assertEquals(id, block.getId());
    }

    // ------------------------------------------------------------------
    // Interval and validation
    // ------------------------------------------------------------------

    @Test
    public void setInterval_rejectsBackwardsRange() {
        TimeBlock block = TimeBlock.createIndependent("X", DATE, 600, 700, TimeBlock.KIND_MEAL);
        try {
            block.setInterval(700, 700);
            fail("end must be after start");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }

    @Test
    public void setInterval_rejectsOutOfRange() {
        TimeBlock block = TimeBlock.createIndependent("X", DATE, 600, 700, TimeBlock.KIND_MEAL);

        try {
            block.setInterval(-1, 700);
            fail("negative start must be rejected");
        } catch (IllegalArgumentException expected) {
            // correct
        }
        try {
            block.setInterval(600, IntervalRules.MINUTES_PER_DAY + 1);
            fail("end beyond midnight must be rejected");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }

    @Test
    public void fullDayBlockIsAllowed() {
        TimeBlock block = TimeBlock.createIndependent(
                "Deep work", DATE, 0, IntervalRules.MINUTES_PER_DAY, TimeBlock.KIND_TASK);

        assertTrue(block.isValid());
        assertEquals(1440, block.getDurationMinutes());
    }

    @Test
    public void shortBlockWarnsButRemainsValid() {
        TimeBlock block = TimeBlock.createIndependent("Blink", DATE, 540, 542, TimeBlock.KIND_BREAK);

        assertTrue("short blocks must still be saveable", block.isValid());
        assertTrue(block.validate().getWarnings()
                .contains(IntervalRules.W_SHORT_BLOCK));
    }

    @Test
    public void invalidDateIsRejected() {
        try {
            TimeBlock.createIndependent("X", "2026-02-30", 540, 600, TimeBlock.KIND_MEAL);
            fail("February 30 must be rejected");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }

    @Test
    public void leapDayIsAccepted() {
        TimeBlock block = TimeBlock.createIndependent(
                "X", "2028-02-29", 540, 600, TimeBlock.KIND_MEAL);
        assertTrue(block.isValid());
    }

    @Test
    public void emptyTitleIsRejected() {
        try {
            TimeBlock.createIndependent("  ", DATE, 540, 600, TimeBlock.KIND_MEAL);
            fail("blank title must be rejected");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }

    // ------------------------------------------------------------------
    // Display helpers
    // ------------------------------------------------------------------

    @Test
    public void formatsTimeRangeIn24HourForm() {
        TimeBlock block = TimeBlock.createIndependent("X", DATE, 540, 630, TimeBlock.KIND_TASK);
        assertEquals("09:00 - 10:30", block.getTimeRangeLabel());
    }

    @Test
    public void formatsMidnightAndEndOfDay() {
        assertEquals("00:00", TimeBlock.formatMinuteOfDay(0));
        assertEquals("24:00", TimeBlock.formatMinuteOfDay(1440));
        assertEquals("13:05", TimeBlock.formatMinuteOfDay(785));
    }
}
