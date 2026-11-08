package com.example.dailytodolist.model;

/**
 * Idempotency fingerprint for one migrated legacy row.
 *
 * <p><b>Why this exists.</b> Running the migration twice must not create two
 * copies of the same task. A migration state flag alone is not enough: the app
 * can be killed between the flag being written and the data being written, an
 * old backup can be restored, or the migration can be triggered from a second
 * screen. So each migrated row also carries a deterministic fingerprint that is
 * recomputed the same way on every run.</p>
 *
 * <p><b>The duplicate-title problem.</b> A real list can contain "Buy milk"
 * twice. Comparing sets of titles would silently drop the second copy. The
 * fingerprint therefore ends with an ordinal within the group, and migration
 * compares <em>counts</em> (a multiset), not membership.</p>
 *
 * <p>Format: {@code source|date|title|ordinal} where {@code |} and {@code \}
 * inside the date and title are backslash-escaped so a title containing a pipe
 * cannot forge a different key.</p>
 *
 * <p>This class is plain Java and must stay free of Android imports.</p>
 */
public final class SourceKey {

    private static final char SEPARATOR = '|';
    private static final char ESCAPE = '\\';

    private SourceKey() {
        // Static helpers only.
    }

    /**
     * Builds a fingerprint.
     *
     * @param source  {@link Task#ORIGIN_HOME} or {@link Task#ORIGIN_CALENDAR}
     * @param dateIso the legacy date, or {@code null} for undated home rows
     * @param title   the legacy title
     * @param ordinal position among rows that share the same source, date and
     *                title; starts at 0
     */
    public static String build(String source, String dateIso, String title, int ordinal) {
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must be >= 0, was: " + ordinal);
        }

        String safeSource = escape(source == null ? "" : source);
        String safeDate = escape(dateIso == null ? "" : dateIso);
        String safeTitle = escape(title == null ? "" : title);

        return safeSource + SEPARATOR + safeDate + SEPARATOR + safeTitle
                + SEPARATOR + ordinal;
    }

    /** Fingerprint for one row of the legacy home list. Home rows have no date. */
    public static String forHome(String title, int ordinal) {
        return build(Task.ORIGIN_HOME, null, title, ordinal);
    }

    /** Fingerprint for one row of the legacy calendar store for a given date. */
    public static String forCalendar(String dateIso, String title, int ordinal) {
        return build(Task.ORIGIN_CALENDAR, dateIso, title, ordinal);
    }

    /**
     * Grouping key shared by every row of the same source and date.
     * Migration uses this to count how many rows of each group were already
     * migrated, so duplicate titles are handled correctly.
     */
    public static String groupKey(String source, String dateIso) {
        return escape(source == null ? "" : source)
                + SEPARATOR + escape(dateIso == null ? "" : dateIso);
    }

    /** Extracts the source part of a fingerprint built by this class. */
    public static String sourceOf(String key) {
        return unescape(part(key, 0));
    }

    /** Extracts the date part of a fingerprint built by this class. */
    public static String dateOf(String key) {
        String date = unescape(part(key, 1));
        return date.isEmpty() ? null : date;
    }

    // ------------------------------------------------------------------
    // Escaping
    // ------------------------------------------------------------------

    private static String escape(String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 8);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == ESCAPE || c == SEPARATOR) {
                out.append(ESCAPE);
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String unescape(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) {
                out.append(c);
                escaped = false;
            } else if (c == ESCAPE) {
                escaped = true;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Returns the n-th unescaped-boundary field.
     *
     * <p>Walks the string honouring escapes so a pipe that belongs to the title
     * is not mistaken for a field separator.</p>
     */
    private static String part(String key, int index) {
        if (key == null) {
            return "";
        }

        StringBuilder current = new StringBuilder();
        int found = 0;
        boolean escaped = false;

        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (escaped) {
                current.append(ESCAPE).append(c);
                escaped = false;
            } else if (c == ESCAPE) {
                escaped = true;
            } else if (c == SEPARATOR) {
                if (found == index) {
                    return current.toString();
                }
                found++;
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (escaped) {
            current.append(ESCAPE);
        }
        return found == index ? current.toString() : "";
    }
}
