package com.example.dailytodolist.model;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers the week maths behind the horizontal day selector.
 *
 * <p>The requirement: the strip always shows Monday to Sunday for the week
 * containing the selected day, and flags today so it can be highlighted.</p>
 */
public class WeekDayTest {

    @Test
    public void weekStartsOnMondayAndHasSevenDays() {
        // 2026-11-07 is a Saturday.
        List<WeekDay> week = WeekDay.weekOf("2026-11-07", "2026-11-07");

        assertEquals(7, week.size());
        assertEquals("2026-11-02", week.get(0).getDateIso()); // Monday
        assertEquals("2026-11-08", week.get(6).getDateIso()); // Sunday
    }

    @Test
    public void weekDaysAreContiguousAndIndexedMondayFirst() {
        List<WeekDay> week = WeekDay.weekOf("2026-11-07", "2026-11-07");

        for (int i = 0; i < week.size(); i++) {
            assertEquals(i, week.get(i).getDayOfWeekIndex());
        }
        assertEquals("02", week.get(0).getDayOfMonthLabel());
        assertEquals("08", week.get(6).getDayOfMonthLabel());
    }

    @Test
    public void anchorOnMondayKeepsThatMondayFirst() {
        // 2026-11-02 is a Monday.
        List<WeekDay> week = WeekDay.weekOf("2026-11-02", "2026-11-02");

        assertEquals("2026-11-02", week.get(0).getDateIso());
    }

    @Test
    public void anchorOnSundayPutsItLast() {
        // 2026-11-08 is a Sunday.
        List<WeekDay> week = WeekDay.weekOf("2026-11-08", "2026-11-08");

        assertEquals("2026-11-02", week.get(0).getDateIso());
        assertEquals("2026-11-08", week.get(6).getDateIso());
    }

    @Test
    public void onlyTheMatchingDateIsFlaggedAsToday() {
        List<WeekDay> week = WeekDay.weekOf("2026-11-07", "2026-11-05");

        int todayCount = 0;
        for (WeekDay day : week) {
            if (day.isToday()) {
                todayCount++;
                assertEquals("2026-11-05", day.getDateIso());
            }
        }
        assertEquals(1, todayCount);
    }

    @Test
    public void noDayIsTodayWhenTheWeekDoesNotContainIt() {
        List<WeekDay> week = WeekDay.weekOf("2026-11-07", "2026-12-25");

        for (WeekDay day : week) {
            assertFalse(day.isToday());
        }
    }

    @Test
    public void weekCrossingAMonthBoundaryKeepsRealDates() {
        // 2026-12-01 is a Tuesday, so its week starts in November.
        List<WeekDay> week = WeekDay.weekOf("2026-12-01", "2026-12-01");

        assertEquals("2026-11-30", week.get(0).getDateIso());
        assertEquals("2026-12-01", week.get(1).getDateIso());
        assertEquals("2026-12-06", week.get(6).getDateIso());
    }

    @Test
    public void leapDayIsHandled() {
        // 2028 is a leap year; 2028-02-29 is a Tuesday.
        List<WeekDay> week = WeekDay.weekOf("2028-02-29", "2028-02-29");

        assertEquals("2028-02-28", week.get(0).getDateIso());
        assertEquals("2028-02-29", week.get(1).getDateIso());
        assertTrue(week.get(1).isToday());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unparseableAnchorThrows() {
        WeekDay.weekOf("not-a-date", "2026-11-07");
    }
}
