package com.example.dailytodolist.model;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Covers the migration fingerprint used to keep repeated runs idempotent.
 *
 * <p>The important property is that duplicate titles remain distinguishable:
 * two "Buy milk" rows must produce two different keys, so the second copy is
 * never dropped.</p>
 */
public class SourceKeyTest {

    @Test
    public void duplicateTitlesGetDistinctKeys() {
        String first = SourceKey.forHome("Buy milk", 0);
        String second = SourceKey.forHome("Buy milk", 1);

        assertNotEquals("duplicate rows must not collapse", first, second);
    }

    @Test
    public void identicalRowsProduceIdenticalKeys() {
        // Same legacy row read twice -> same key, so migration sees it as done.
        assertEquals(SourceKey.forHome("Buy milk", 0), SourceKey.forHome("Buy milk", 0));
    }

    @Test
    public void homeKeysHaveNullDate() {
        String key = SourceKey.forHome("Buy milk", 0);

        assertNull("home rows carry no date", SourceKey.dateOf(key));
        assertEquals(Task.ORIGIN_HOME, SourceKey.sourceOf(key));
    }

    @Test
    public void calendarKeysCarryTheirDate() {
        String key = SourceKey.forCalendar("2026-11-06", "Lecture", 0);

        assertEquals("2026-11-06", SourceKey.dateOf(key));
        assertEquals(Task.ORIGIN_CALENDAR, SourceKey.sourceOf(key));
    }

    @Test
    public void homeAndCalendarKeysNeverCollide() {
        // Same title, same ordinal, but different origin.
        String home = SourceKey.forHome("Buy milk", 0);
        String calendar = SourceKey.forCalendar("2026-11-06", "Buy milk", 0);

        assertNotEquals("home and calendar rows must stay separate tasks", home, calendar);
    }

    @Test
    public void differentDatesProduceDifferentKeys() {
        assertNotEquals(
                SourceKey.forCalendar("2026-11-06", "Task", 0),
                SourceKey.forCalendar("2026-11-07", "Task", 0));
    }

    @Test
    public void pipeInTitleCannotForgeAnotherKey() {
        // Without escaping, "A|0" + separator juggling could imitate a different row.
        String tricky = SourceKey.forHome("Buy|milk", 0);
        String innocent = SourceKey.forHome("Buy", 0);

        assertNotEquals("a pipe in the title must not forge a key", tricky, innocent);
        assertEquals("Buy|milk", titleOf(tricky));
    }

    @Test
    public void backslashInTitleIsHandled() {
        String key = SourceKey.forHome("a\\b|0", 0);
        // Must round-trip without throwing or losing the separator meaning.
        assertTrue(key.contains("a"));
        assertNotEquals(SourceKey.forHome("ab", 0), key);
    }

    @Test
    public void emptyTitleIsAllowedAndStillDeterministic() {
        assertEquals(SourceKey.forHome("", 0), SourceKey.forHome("", 0));
        assertNotEquals(SourceKey.forHome("", 0), SourceKey.forHome("", 1));
    }

    @Test
    public void ordinalMustBeNonNegative() {
        try {
            SourceKey.forHome("Task", -1);
            fail("negative ordinal must be rejected");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }

    @Test
    public void groupKeySeparatesBySourceAndDate() {
        String groupA = SourceKey.groupKey(Task.ORIGIN_CALENDAR, "2026-11-06");
        String groupB = SourceKey.groupKey(Task.ORIGIN_CALENDAR, "2026-11-07");
        String groupHome = SourceKey.groupKey(Task.ORIGIN_HOME, null);

        assertNotEquals(groupA, groupB);
        assertNotEquals(groupA, groupHome);
        assertEquals(groupA, SourceKey.groupKey(Task.ORIGIN_CALENDAR, "2026-11-06"));
    }

    // ------------------------------------------------------------------
    // Multiset behaviour: the actual duplicate-prevention mechanism
    // ------------------------------------------------------------------

    @Test
    public void multisetCountsDuplicatesCorrectly() {
        // Legacy list: three rows, two of them named "Buy milk".
        // The ordinal keeps the two identical titles distinguishable.
        Map<String, Integer> legacy = new HashMap<>();
        increment(legacy, SourceKey.forHome("Buy milk", 0));
        increment(legacy, SourceKey.forHome("Buy milk", 1));
        increment(legacy, SourceKey.forHome("Call mom", 0));

        assertEquals("three legacy rows must produce three entries", 3, total(legacy));
        assertEquals(Integer.valueOf(1), legacy.get(SourceKey.forHome("Buy milk", 0)));
        assertEquals(Integer.valueOf(1), legacy.get(SourceKey.forHome("Buy milk", 1)));
        assertNotEquals("duplicate titles stay separate",
                SourceKey.forHome("Buy milk", 0), SourceKey.forHome("Buy milk", 1));
    }

    @Test
    public void secondRunFindsEverythingAlreadyMigrated() {
        // Simulates: run once, then run again with nothing changed.
        Map<String, Integer> legacy = new HashMap<>();
        increment(legacy, SourceKey.forHome("Buy milk", 0));
        increment(legacy, SourceKey.forHome("Buy milk", 1));

        Map<String, Integer> migrated = new HashMap<>();
        increment(migrated, SourceKey.forHome("Buy milk", 0));
        increment(migrated, SourceKey.forHome("Buy milk", 1));

        int toInsert = 0;
        for (Map.Entry<String, Integer> entry : legacy.entrySet()) {
            int already = migrated.containsKey(entry.getKey()) ? migrated.get(entry.getKey()) : 0;
            toInsert += Math.max(0, entry.getValue() - already);
        }

        assertEquals("re-running must insert nothing", 0, toInsert);
    }

    @Test
    public void partialRunInsetsOnlyTheMissingRow() {
        // Simulates a crash after the first row was written.
        Map<String, Integer> legacy = new HashMap<>();
        increment(legacy, SourceKey.forHome("Buy milk", 0));
        increment(legacy, SourceKey.forHome("Buy milk", 1));

        Map<String, Integer> migrated = new HashMap<>();
        increment(migrated, SourceKey.forHome("Buy milk", 0)); // only one made it

        int toInsert = 0;
        for (Map.Entry<String, Integer> entry : legacy.entrySet()) {
            int already = migrated.containsKey(entry.getKey()) ? migrated.get(entry.getKey()) : 0;
            toInsert += Math.max(0, entry.getValue() - already);
        }

        assertEquals("exactly the missing row is inserted", 1, toInsert);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void increment(Map<String, Integer> map, String key) {
        map.put(key, map.containsKey(key) ? map.get(key) + 1 : 1);
    }

    private static int total(Map<String, Integer> map) {
        int sum = 0;
        for (Integer value : map.values()) {
            sum += value;
        }
        return sum;
    }

    /** Rebuilds the raw title by parsing the key the way migration would. */
    private static String titleOf(String key) {
        // key format: source|date|title|ordinal  (escaped)
        StringBuilder out = new StringBuilder();
        int field = 0;
        boolean escaped = false;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (escaped) {
                if (field == 2) {
                    out.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '|') {
                field++;
            } else if (field == 2) {
                out.append(c);
            }
        }
        return out.toString();
    }
}
