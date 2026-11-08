package com.example.dailytodolist.util;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Helpers for permanent, stable identifiers.
 *
 * <p><b>Why this class exists.</b> The legacy app addresses tasks by their
 * position in an {@code ArrayList} (for example {@code tasks.remove(position)}).
 * A position is not an identity: delete the task in slot 2 and every task after
 * it silently changes slot. Anything that stores a position as a permanent
 * reference will therefore point at the wrong task sooner or later.</p>
 *
 * <p>Every permanent id in the planner is a canonical UUID string minted by
 * {@link #newId()}. A UUID is a value: it is created once, stored, and never
 * derived from anything else, so it survives renames, date changes, reordering
 * and the deletion of its neighbours.</p>
 *
 * <p>This class is plain Java and must stay free of Android imports.</p>
 */
public final class UuidUtil {

    /**
     * Canonical 8-4-4-4-12 textual UUID form.
     *
     * <p>Deliberately a regex rather than {@code UUID.fromString(...)}:
     * the lenient parser accepts short forms such as {@code "1-2-3-4-5"} on
     * some platforms and rejects them on others, which would make validation
     * behave differently on the JVM and on a device.</p>
     */
    private static final Pattern CANONICAL_UUID = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}"
                    + "-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private UuidUtil() {
        // Static helpers only.
    }

    /**
     * Mints a brand new random UUID.
     *
     * <p>Call this exactly once per task or time block, at creation time, and
     * store the result. Never call it again for an existing item.</p>
     */
    public static String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns true when {@code candidate} is a canonical UUID string.
     *
     * <p>Positional or composite legacy coordinates such as {@code "home:3"}
     * or {@code "cal:2026-11-06:0"} return false, which is the point: they must
     * never be accepted as a permanent id.</p>
     */
    public static boolean isValid(String candidate) {
        if (candidate == null) {
            return false;
        }
        return CANONICAL_UUID.matcher(candidate.trim()).matches();
    }

    /**
     * Trims and lowercases an id so comparisons are consistent.
     *
     * @return {@code null} for null or blank input
     */
    public static String normalize(String candidate) {
        if (candidate == null) {
            return null;
        }
        String trimmed = candidate.trim().toLowerCase(Locale.US);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
