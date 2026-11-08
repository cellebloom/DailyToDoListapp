package com.example.dailytodolist;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers "Anytime" tasks: tasks that have no time block at all.
 *
 * <p>The sentinel is {@link TaskWithDate#NO_TIME} ({@code -1}). The main hazard
 * is Java integer division: {@code -1 / 60 == 0}, so any code that derives the
 * hour first and compares afterwards would silently report 00:00 instead of
 * "no time". These tests pin that behaviour down.</p>
 */
public class TaskWithDateAnytimeTest {

    private static final String DATE = "2026-11-06";

    @Test
    public void anytimeTask_hasNoTime() {
        TaskWithDate task = TaskWithDate.fromMinuteOfDay(
                "Read a book", DATE, TaskWithDate.NO_TIME, TaskWithDate.NO_TIME,
                false, "Personal");

        assertTrue(task.isAnytime());
        assertEquals(-1, task.getStartTimeHour());
        assertEquals(0, task.getDurationMinutes());
    }

    @Test
    public void anytimeTask_doesNotPretendToStartAtMidnight() {
        TaskWithDate task = TaskWithDate.fromMinuteOfDay(
                "Anytime", DATE, TaskWithDate.NO_TIME, TaskWithDate.NO_TIME,
                false, "Other");

        // The whole point: -1 must not become 0 through integer division.
        assertEquals(-1, task.getStartTimeHour());
        assertEquals(-1, task.getEndTimeHour());
    }

    @Test
    public void anytimeTask_hasEmptyTimeLabel() {
        TaskWithDate task = TaskWithDate.fromMinuteOfDay(
                "Anytime", DATE, TaskWithDate.NO_TIME, TaskWithDate.NO_TIME,
                false, "Other");

        assertEquals("", task.getTimeRangeLabel());
    }

    @Test
    public void legacyConstructorWithMinusOneHour_isAnytime() {
        TaskWithDate task = new TaskWithDate(
                "Someday", 2026, 11, 6,
                -1, 0, -1, 0, false, "Other", "");

        assertTrue(task.isAnytime());
        assertEquals(-1, task.getStartTimeHour());
        assertEquals(0, task.getDurationMinutes());
    }

    @Test
    public void scheduledTask_isNotAnytime() {
        TaskWithDate task = TaskWithDate.fromMinuteOfDay(
                "Study", DATE, 9 * 60, 10 * 60, false, "Study");

        assertFalse(task.isAnytime());
        assertEquals(9, task.getStartTimeHour());
        assertEquals(0, task.getStartTimeMinute());
        assertEquals(60, task.getDurationMinutes());
        assertEquals("09:00 - 10:00", task.getTimeRangeLabel());
    }

    @Test
    public void midnightTask_isScheduledNotAnytime() {
        // 00:00 is a real time and must not be confused with the sentinel.
        TaskWithDate task = TaskWithDate.fromMinuteOfDay(
                "Night shift", DATE, 0, 60, false, "Other");

        assertFalse(task.isAnytime());
        assertEquals(0, task.getStartTimeHour());
        assertEquals(0, task.getStartMinute());
    }

    @Test
    public void settingSentinelStart_makesTaskAnytime() {
        TaskWithDate task = TaskWithDate.fromMinuteOfDay(
                "Study", DATE, 9 * 60, 10 * 60, false, "Study");

        task.setStartMinute(TaskWithDate.NO_TIME);

        assertTrue(task.isAnytime());
        assertEquals(-1, task.getStartTimeHour());
    }

    @Test
    public void endWithoutStart_isNormalisedToAnytime() {
        TaskWithDate task = TaskWithDate.fromMinuteOfDay(
                "Weird", DATE, TaskWithDate.NO_TIME, 600, false, "Other");

        assertTrue(task.isAnytime());
    }

    @Test
    public void restoreHasTime_recoversScheduledState() {
        TaskWithDate task = TaskWithDate.fromMinuteOfDay(
                "Study", DATE, 9 * 60, 10 * 60, false, "Study");

        task.restoreHasTime(false);

        assertTrue(task.isAnytime());
    }

    @Test
    public void anytimeTaskSurvivesSettersWithoutGainingTime() {
        TaskWithDate task = TaskWithDate.fromMinuteOfDay(
                "Anytime", DATE, TaskWithDate.NO_TIME, TaskWithDate.NO_TIME,
                false, "Other");

        task.setEndMinute(TaskWithDate.NO_TIME);

        assertTrue(task.isAnytime());
        assertEquals(-1, task.getStartTimeHour());
    }

    @Test
    public void dateStillWorksForAnytimeTask() {
        TaskWithDate task = TaskWithDate.fromMinuteOfDay(
                "Anytime", DATE, TaskWithDate.NO_TIME, TaskWithDate.NO_TIME,
                false, "Other");

        assertEquals(DATE, task.getDateIso());
        assertTrue(task.isAnytime());
    }
}
