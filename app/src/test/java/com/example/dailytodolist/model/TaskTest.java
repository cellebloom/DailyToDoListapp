package com.example.dailytodolist.model;

import com.example.dailytodolist.util.UuidUtil;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Covers the unified task model: permanent identity, the expanded fields with
 * their migration defaults, and the home/calendar migration factories.
 */
public class TaskTest {

    // ------------------------------------------------------------------
    // Identity
    // ------------------------------------------------------------------

    @Test
    public void createNew_mintsCanonicalUuid() {
        Task task = Task.createNew("Buy milk", null, Task.ORIGIN_PLANNER);
        assertTrue(UuidUtil.isValid(task.getId()));
    }

    @Test
    public void idSurvivesTitleEdit() {
        Task task = Task.createNew("Old title", null, Task.ORIGIN_PLANNER);
        String id = task.getId();

        task.setTitle("Completely different title");

        assertEquals("editing the title must not change identity", id, task.getId());
    }

    @Test
    public void idSurvivesDateAndPriorityEdits() {
        Task task = Task.createNew("Report", "2026-11-06", Task.ORIGIN_PLANNER);
        String id = task.getId();

        task.setDateIso("2026-12-25");
        task.setPriority(Task.PRIORITY_HIGH);
        task.setEstimatedMinutes(90);
        task.setCompleted(true);

        assertEquals(id, task.getId());
    }

    @Test
    public void idSurvivesRemovingSibling_simulation() {
        // Models the legacy bug: removing by position shifts every later index.
        // With UUIDs the surviving tasks keep the identity they were minted with.
        List<Task> tasks = new ArrayList<>();
        Task a = Task.createNew("A", null, Task.ORIGIN_HOME);
        Task b = Task.createNew("B", null, Task.ORIGIN_HOME);
        Task c = Task.createNew("C", null, Task.ORIGIN_HOME);
        tasks.add(a);
        tasks.add(b);
        tasks.add(c);

        String idB = b.getId();
        String idC = c.getId();

        tasks.remove(0); // remove A by position, legacy style

        assertEquals(2, tasks.size());
        assertEquals(idB, tasks.get(0).getId());
        assertEquals(idC, tasks.get(1).getId());
        assertEquals("B", tasks.get(0).getTitle());
    }

    @Test
    public void equalityIsByIdentityNotContent() {
        Task first = Task.createNew("Same text", null, Task.ORIGIN_PLANNER);
        Task second = Task.createNew("Same text", null, Task.ORIGIN_PLANNER);

        assertEquals(first, first);
        assertFalse("two tasks with equal text are still two tasks", first.equals(second));
        assertFalse(first.hashCode() == second.hashCode() && !first.equals(second));
    }

    // ------------------------------------------------------------------
    // Expanded fields and defaults
    // ------------------------------------------------------------------

    @Test
    public void createNew_usesNeutralDefaults() {
        Task task = Task.createNew("Task", null, Task.ORIGIN_PLANNER);

        assertEquals(Task.PRIORITY_NONE, task.getPriority());
        assertEquals(0, task.getEstimatedMinutes());
        assertNull(task.getDueDateIso());
        assertNull(task.getDateIso());
        assertEquals("", task.getNotes());
        assertFalse(task.isCompleted());
        assertFalse(task.isScheduled());
    }

    @Test
    public void expandedFieldsRoundTrip() {
        Task task = Task.createNew("Write report", "2026-11-06", Task.ORIGIN_PLANNER);

        task.setPriority(Task.PRIORITY_HIGH);
        task.setDueDateIso("2026-11-08");
        task.setEstimatedMinutes(90);
        task.setNotes("Bring the draft");

        assertEquals(Task.PRIORITY_HIGH, task.getPriority());
        assertEquals("2026-11-08", task.getDueDateIso());
        assertEquals(90, task.getEstimatedMinutes());
        assertEquals("Bring the draft", task.getNotes());
    }

    @Test
    public void dueDateIsNotTheScheduledDate() {
        // A scheduled date is not a deadline; migration must not copy one to the other.
        Task task = Task.createMigratedFromCalendar(
                UuidUtil.newId(), "Call Bob", "2026-11-06", false, "10:00 AM", "k1");

        assertEquals("2026-11-06", task.getDateIso());
        assertNull(task.getDueDateIso());
    }

    @Test
    public void priorityRejectsOutOfRange() {
        Task task = Task.createNew("Task", null, Task.ORIGIN_PLANNER);
        try {
            task.setPriority(9);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }

    @Test
    public void estimatedMinutesRejectsNegative() {
        Task task = Task.createNew("Task", null, Task.ORIGIN_PLANNER);
        try {
            task.setEstimatedMinutes(-5);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }

    @Test
    public void blankTitleIsRejected() {
        try {
            Task.createNew("   ", null, Task.ORIGIN_PLANNER);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }

    @Test
    public void invalidDateFormatIsRejected() {
        try {
            Task.createNew("Task", "06/11/2026", Task.ORIGIN_PLANNER);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }

    // ------------------------------------------------------------------
    // Legacy migration: home tasks
    // ------------------------------------------------------------------

    @Test
    public void migratedHomeTask_hasNoDate() {
        Task task = Task.createMigratedFromHome(
                UuidUtil.newId(), "Buy milk", false, "home||Buy milk|0");

        assertNull("legacy home tasks must stay unscheduled", task.getDateIso());
        assertFalse(task.isScheduled());
        assertEquals(Task.ORIGIN_HOME, task.getOrigin());
    }

    @Test
    public void migratedHomeTask_keepsCompletionState() {
        Task done = Task.createMigratedFromHome(UuidUtil.newId(), "Done", true, "k1");
        Task pending = Task.createMigratedFromHome(UuidUtil.newId(), "Pending", false, "k2");

        assertTrue(done.isCompleted());
        assertFalse(pending.isCompleted());
    }

    @Test
    public void migratedHomeTask_hasNoLegacyTimeText() {
        Task task = Task.createMigratedFromHome(UuidUtil.newId(), "Buy milk", false, "k1");
        assertEquals("", task.getLegacyTimeText());
    }

    // ------------------------------------------------------------------
    // Legacy migration: calendar tasks
    // ------------------------------------------------------------------

    @Test
    public void migratedCalendarTask_keepsDateAndLegacyTime() {
        Task task = Task.createMigratedFromCalendar(
                UuidUtil.newId(), "Lecture", "2026-11-06", true, "10:00 AM", "cal-k1");

        assertEquals("2026-11-06", task.getDateIso());
        assertEquals("10:00 AM", task.getLegacyTimeText());
        assertTrue(task.isCompleted());
        assertEquals(Task.ORIGIN_CALENDAR, task.getOrigin());
    }

    @Test
    public void migratedCalendarTask_toleratesEmptyLegacyTime() {
        // The legacy app never actually sets this field, so it is usually "".
        Task task = Task.createMigratedFromCalendar(
                UuidUtil.newId(), "Task", "2026-11-06", false, "", "cal-k1");

        assertEquals("", task.getLegacyTimeText());
    }

    @Test
    public void migrationRejectsPositionalId() {
        // The core guarantee: a legacy coordinate can never become a permanent id.
        try {
            Task.createMigratedFromHome("home:3", "Buy milk", false, "k1");
            fail("positional id must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("UUID"));
        }
    }

    @Test
    public void migrationRejectsNullId() {
        try {
            Task.createMigratedFromHome(null, "Buy milk", false, "k1");
            fail("null id must be rejected");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }

    @Test
    public void migrationRequiresSourceKey() {
        try {
            Task.createMigratedFromHome(UuidUtil.newId(), "Buy milk", false, "  ");
            fail("empty sourceKey must be rejected");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }

    @Test
    public void migratedTask_preservesFrozenIdExactly() {
        String frozen = UuidUtil.newId();

        Task first = Task.createMigratedFromHome(frozen, "Buy milk", false, "k1");
        Task second = Task.createMigratedFromHome(frozen, "Buy milk", false, "k1");

        assertEquals(frozen, first.getId());
        assertEquals("re-running migration must reuse the frozen id",
                first.getId(), second.getId());
    }

    @Test
    public void twoRunsOfMigrationProduceTwoDistinctTasksForTwoLegacyRows() {
        // Duplicate titles are two real rows, not one.
        String key0 = SourceKey.forHome("Buy milk", 0);
        String key1 = SourceKey.forHome("Buy milk", 1);

        Task first = Task.createMigratedFromHome(UuidUtil.newId(), "Buy milk", false, key0);
        Task second = Task.createMigratedFromHome(UuidUtil.newId(), "Buy milk", false, key1);

        assertFalse(first.equals(second));
        assertFalse(first.getSourceKey().equals(second.getSourceKey()));
    }

    @Test
    public void timestampsArePopulated() {
        Task task = Task.createNew("Task", null, Task.ORIGIN_PLANNER);
        assertTrue(task.getCreatedAt() > 0);
        assertNotNull(task.getSourceKey());
    }

    @Test
    public void sourceKeyIsEmptyForNewlyCreatedTasks() {
        assertEquals("", Task.createNew("Task", null, Task.ORIGIN_PLANNER).getSourceKey());
    }
}
