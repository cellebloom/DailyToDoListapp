package com.example.dailytodolist.util;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Guarantees the identity contract: ids are minted, canonical, and never
 * derived from a position in a list.
 */
public class UuidUtilTest {

    @Test
    public void newId_producesCanonicalUuid() {
        String id = UuidUtil.newId();
        assertTrue("expected canonical form, was: " + id, UuidUtil.isValid(id));
        assertEquals(36, id.length());
        assertEquals(4, countChar(id, '-'));
    }

    @Test
    public void newId_isUniqueAcrossManyCalls() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            assertTrue("duplicate uuid generated", seen.add(UuidUtil.newId()));
        }
        assertEquals(2000, seen.size());
    }

    @Test
    public void isValid_acceptsUppercaseAndWhitespace() {
        assertTrue(UuidUtil.isValid("  A1B2C3D4-E5F6-7890-ABCD-EF0123456789  "));
    }

    @Test
    public void isValid_rejectsLegacyPositionalCoordinates() {
        // These are exactly the identifiers a migration must never persist.
        assertFalse(UuidUtil.isValid("home:3"));
        assertFalse(UuidUtil.isValid("cal:2026-11-06:0"));
        assertFalse(UuidUtil.isValid("home:0"));
        assertFalse(UuidUtil.isValid("3"));
    }

    @Test
    public void isValid_rejectsMalformedInput() {
        assertFalse(UuidUtil.isValid(null));
        assertFalse(UuidUtil.isValid(""));
        assertFalse(UuidUtil.isValid("   "));
        // Lenient parser on some platforms accepts this; we must not.
        assertFalse(UuidUtil.isValid("1-2-3-4-5"));
        // Missing a character.
        assertFalse(UuidUtil.isValid("a1b2c3d4-e5f6-7890-abcd-ef012345678"));
        // Wrong separator positions.
        assertFalse(UuidUtil.isValid("a1b2c3d4e5f67890abcdef0123456789"));
    }

    @Test
    public void normalize_foldsCaseAndTrims() {
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef0123456789",
                UuidUtil.normalize("  A1B2C3D4-E5F6-7890-ABCD-EF0123456789 "));
        assertNull(UuidUtil.normalize(null));
        assertNull(UuidUtil.normalize("   "));
    }

    @Test
    public void normalize_outputIsStillValid() {
        assertTrue(UuidUtil.isValid(UuidUtil.normalize("  A1B2C3D4-E5F6-7890-ABCD-EF0123456789 ")));
    }

    @Test
    public void idsAreIndependentAcrossCalls() {
        String first = UuidUtil.newId();
        String second = UuidUtil.newId();
        assertNotEquals(first, second);
    }

    private static int countChar(String text, char target) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }
}
