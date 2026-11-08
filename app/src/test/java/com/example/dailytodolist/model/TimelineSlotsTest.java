package com.example.dailytodolist.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Covers placement of tasks into hour slots on the time-blocking grid.
 *
 * <p>The important requirement: a task starting at 09:00 must appear in the
 * slot labelled "09:00".</p>
 */
public class TimelineSlotsTest {

    @Test
    public void defaultRangeCoversSixToTwentyThree() {
        TimelineSlots slots = new TimelineSlots();

        assertEquals(6, slots.getFirstHour());
        assertEquals(23, slots.getLastHour());
        assertEquals(18, slots.getSlotCount());
    }

    @Test
    public void taskStartingAtNineLandsInNineSlot() {
        TimelineSlots slots = new TimelineSlots();

        int index = slots.add(new TimelineSlots.TimeBlockRow("Team sync", 9 * 60, 10 * 60));

        assertEquals(3, index); // 06,07,08 then 09
        assertEquals("09:00", slots.getSlotLabel(index));
        assertEquals(1, slots.getRowsAt(index).size());
        assertEquals("Team sync", slots.getRowsAt(index).get(0).getTitle());
    }

    @Test
    public void taskInsideTheHourStillShowsInThatHour() {
        // 09:45 belongs next to the 09:00 label, not hidden until 10:00.
        TimelineSlots slots = new TimelineSlots();

        int index = slots.add(new TimelineSlots.TimeBlockRow("Late start", 9 * 60 + 45, 10 * 60));

        assertEquals("09:00", slots.getSlotLabel(index));
    }

    @Test
    public void emptySlotsHaveNoRows() {
        TimelineSlots slots = new TimelineSlots();
        slots.add(new TimelineSlots.TimeBlockRow("Team sync", 9 * 60, 10 * 60));

        for (int i = 0; i < slots.getSlotCount(); i++) {
            if (i == 3) {
                continue;
            }
            assertTrue("slot " + i + " should be empty", slots.getRowsAt(i).isEmpty());
        }
    }

    @Test
    public void multipleTasksInSameHourShareASlot() {
        TimelineSlots slots = new TimelineSlots();

        slots.add(new TimelineSlots.TimeBlockRow("A", 9 * 60, 9 * 60 + 30));
        slots.add(new TimelineSlots.TimeBlockRow("B", 9 * 60 + 30, 10 * 60));

        assertEquals(2, slots.getRowsAt(3).size());
    }

    @Test
    public void taskBeforeRangeClampsToFirstSlot() {
        TimelineSlots slots = new TimelineSlots();

        int index = slots.add(new TimelineSlots.TimeBlockRow("Early", 5 * 60 + 30, 6 * 60));

        assertEquals(0, index);
        assertEquals("06:00", slots.getSlotLabel(index));
    }

    @Test
    public void taskAfterRangeClampsToLastSlot() {
        TimelineSlots slots = new TimelineSlots();

        int index = slots.add(new TimelineSlots.TimeBlockRow("Late", 23 * 60 + 30, 24 * 60));

        assertEquals(slots.getSlotCount() - 1, index);
        assertEquals("23:00", slots.getSlotLabel(index));
    }

    @Test
    public void midnightTaskClampsToFirstSlot() {
        TimelineSlots slots = new TimelineSlots();

        int index = slots.add(new TimelineSlots.TimeBlockRow("Midnight", 0, 60));

        assertEquals(0, index);
    }

    @Test
    public void customRangeIsRespected() {
        TimelineSlots slots = new TimelineSlots(8, 10);

        assertEquals(3, slots.getSlotCount());
        assertEquals("08:00", slots.getSlotLabel(0));
        assertEquals("10:00", slots.getSlotLabel(2));

        int index = slots.add(new TimelineSlots.TimeBlockRow("Study", 8 * 60, 9 * 60));
        assertEquals(0, index);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invertedRangeIsRejected() {
        new TimelineSlots(10, 8);
    }
}
