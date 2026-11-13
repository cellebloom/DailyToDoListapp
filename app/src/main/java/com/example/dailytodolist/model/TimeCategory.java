package com.example.dailytodolist.model;

import com.example.dailytodolist.R;

/**
 * The category of a task or time block, and the single place that maps a
 * category to its storage key, its label and its colour.
 *
 * <p>Before this class the mapping lived inline in the activities as
 * {@code if (category.equals("Study")) ...}, which meant the label shown to the
 * user, the value written to storage and the colour all drifted apart. Here the
 * enum constant is the identity; {@link #getStorageKey()} is what is persisted,
 * which stays stable even if a label is translated.</p>
 *
 * <p>Colours are resource ids so dark mode and theming can adjust them later;
 * nothing in the UI should hardcode a hex value.</p>
 */
public enum TimeCategory {

    STUDY("Study", R.string.cat_study, R.color.block_study, R.color.block_study_text),
    CLASSES("Classes", R.string.cat_classes, R.color.block_classes, R.color.block_classes_text),
    ASSIGNMENTS("Assignments", R.string.cat_assignments, R.color.block_assignments,
            R.color.block_assignments_text),
    PERSONAL("Personal", R.string.cat_personal, R.color.block_personal, R.color.block_personal_text),
    BREAK("Break", R.string.cat_break, R.color.block_break, R.color.block_break_text),
    OTHER("Other", R.string.cat_other, R.color.block_other, R.color.block_other_text);

    private final String storageKey;
    private final int labelRes;
    private final int colorRes;
    private final int textColorRes;

    TimeCategory(String storageKey, int labelRes, int colorRes, int textColorRes) {
        this.storageKey = storageKey;
        this.labelRes = labelRes;
        this.colorRes = colorRes;
        this.textColorRes = textColorRes;
    }

    /** Stable value written to storage. Never localised. */
    public String getStorageKey() {
        return storageKey;
    }

    /** String resource id for the human-readable name. */
    public int getLabelRes() {
        return labelRes;
    }

    /**
     * Fill colour resource id for this category: used to paint a timeline block.
     * The label drawn on top is chosen from this fill by GradientContrast.
     */
    public int getColorRes() {
        return colorRes;
    }

    /**
     * Text colour resource id for this category, for drawing the category name
     * directly on a card surface rather than on a filled block.
     *
     * <p>Kept separate from {@link #getColorRes()} on purpose: the fill ramp is
     * tuned to look like a coloured block, and several of those hues are far too
     * light to read as small text (Break is 1.87:1 on white). Each has a
     * night-qualified twin, so one name works in both modes.</p>
     */
    public int getTextColorRes() {
        return textColorRes;
    }

    /**
     * Resolves a stored key back to a category.
     *
     * <p>Unknown or legacy values (for example the old free-text category, or an
     * empty string) fall back to {@link #OTHER} rather than throwing: a stored
     * value the app no longer understands must never crash a screen.</p>
     */
    public static TimeCategory fromStorageKey(String key) {
        if (key != null) {
            String trimmed = key.trim();
            for (TimeCategory category : values()) {
                if (category.storageKey.equalsIgnoreCase(trimmed)) {
                    return category;
                }
            }
        }
        return OTHER;
    }

    /**
     * Formats a minute offset as {@code HH:mm}.
     *
     * <p>Kept here so the add-task screen and the timeline format times the same
     * way. Deliberately not locale-aware: 24-hour numeric times are canonical
     * and compare correctly, unlike a localised "10:00 AM" string.</p>
     */
    public static String formatMinuteOfDay(int minuteOfDay) {
        int safe = Math.max(0, Math.min(24 * 60, minuteOfDay));
        return String.format(java.util.Locale.US, "%02d:%02d", safe / 60, safe % 60);
    }
}
