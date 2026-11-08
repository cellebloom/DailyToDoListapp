package com.example.dailytodolist.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.dailytodolist.model.DailyReview;
import com.example.dailytodolist.model.IntervalRules;

/**
 * Persistence for end-of-day reviews, one per date.
 *
 * <p>Kept separate from {@code TaskStore}: reviews are not tasks, have no id and
 * no ordering, and are addressed purely by date. Folding them into the task
 * store would make that store's "everything here is a list of tasks" assumption
 * false for no benefit.</p>
 *
 * <p>Storage is one pref key per date: {@code review.<yyyy-MM-dd>.*}. Saving
 * twice overwrites the same date rather than appending, so reopening the screen
 * edits today's review instead of creating a second one.</p>
 */
public final class ReviewStore {

    private static final String PREFS = "TodoAppPrefs";
    private static final String PREFIX = "review.";

    private static final String SUFFIX_MORNING = ".morning";
    private static final String SUFFIX_AFTERNOON = ".afternoon";
    private static final String SUFFIX_EVENING = ".evening";
    private static final String SUFFIX_WENT_WELL = ".wentwell";
    private static final String SUFFIX_IMPROVE = ".improve";
    private static final String SUFFIX_SAVED = ".savedAt";

    private final SharedPreferences prefs;

    public ReviewStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public static ReviewStore from(Context context) {
        return new ReviewStore(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }

    /**
     * Loads the review for a date, or an empty one when none was saved yet.
     *
     * <p>Never returns null: the screen always has something to edit, and an
     * unvisited day legitimately has no review.</p>
     */
    public DailyReview load(String dateIso) {
        DailyReview review = DailyReview.emptyFor(dateIso);
        if (!IntervalRules.isValidDateIso(dateIso)) {
            return review;
        }

        String base = PREFIX + dateIso.trim();
        review.setMorningEnergy(prefs.getInt(base + SUFFIX_MORNING, DailyReview.UNRATED));
        review.setAfternoonEnergy(prefs.getInt(base + SUFFIX_AFTERNOON, DailyReview.UNRATED));
        review.setEveningEnergy(prefs.getInt(base + SUFFIX_EVENING, DailyReview.UNRATED));
        review.setWentWellText(prefs.getString(base + SUFFIX_WENT_WELL, ""));
        review.setImproveText(prefs.getString(base + SUFFIX_IMPROVE, ""));
        return review;
    }

    /** True when a review for this date has been saved before. */
    public boolean hasReview(String dateIso) {
        if (!IntervalRules.isValidDateIso(dateIso)) {
            return false;
        }
        return prefs.contains(PREFIX + dateIso.trim() + SUFFIX_SAVED);
    }

    /** Writes the review for its own date, replacing any previous one. */
    public void save(DailyReview review) {
        if (review == null) {
            return;
        }
        String dateIso = review.getDateIso();
        if (!IntervalRules.isValidDateIso(dateIso)) {
            return;
        }

        String base = PREFIX + dateIso.trim();
        prefs.edit()
                .putInt(base + SUFFIX_MORNING, review.getMorningEnergy())
                .putInt(base + SUFFIX_AFTERNOON, review.getAfternoonEnergy())
                .putInt(base + SUFFIX_EVENING, review.getEveningEnergy())
                .putString(base + SUFFIX_WENT_WELL, review.getWentWellText())
                .putString(base + SUFFIX_IMPROVE, review.getImproveText())
                .putLong(base + SUFFIX_SAVED, System.currentTimeMillis())
                .apply();
    }
}
