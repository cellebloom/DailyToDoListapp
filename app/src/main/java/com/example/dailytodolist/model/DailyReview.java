package com.example.dailytodolist.model;

/**
 * One day's end-of-day review.
 *
 * <p>Stored per date, so opening the screen on the same day pre-fills what was
 * written before and the user can refine it rather than starting over.</p>
 *
 * <p>Energy is a 1–5 star rating for each part of the day. {@link #UNRATED}
 * means "not rated yet" and is kept distinct from 1 ("very low"), because a
 * blank day and a terrible day must not look the same in the data.</p>
 *
 * <p>This class is plain Java and must stay free of Android imports.</p>
 */
public class DailyReview {

    /** Rating bounds and the "no rating yet" sentinel. */
    public static final int UNRATED = 0;
    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    private String dateIso;
    private int morningEnergy;
    private int afternoonEnergy;
    private int eveningEnergy;
    private String wentWellText;
    private String improveText;

    /** Required so the class can be rebuilt from storage. */
    public DailyReview() {
        this.morningEnergy = UNRATED;
        this.afternoonEnergy = UNRATED;
        this.eveningEnergy = UNRATED;
        this.wentWellText = "";
        this.improveText = "";
    }

    /** Creates an empty review for one date. */
    public static DailyReview emptyFor(String dateIso) {
        DailyReview review = new DailyReview();
        review.dateIso = requireDate(dateIso);
        return review;
    }

    public String getDateIso() {
        return dateIso;
    }

    public void setDateIso(String dateIso) {
        this.dateIso = requireDate(dateIso);
    }

    public int getMorningEnergy() {
        return morningEnergy;
    }

    public void setMorningEnergy(int value) {
        this.morningEnergy = sanitizeRating(value);
    }

    public int getAfternoonEnergy() {
        return afternoonEnergy;
    }

    public void setAfternoonEnergy(int value) {
        this.afternoonEnergy = sanitizeRating(value);
    }

    public int getEveningEnergy() {
        return eveningEnergy;
    }

    public void setEveningEnergy(int value) {
        this.eveningEnergy = sanitizeRating(value);
    }

    /** Energy for the part of the day indicated by {@code slot}. */
    public void setEnergy(EnergySlot slot, int value) {
        switch (slot) {
            case MORNING:
                setMorningEnergy(value);
                break;
            case AFTERNOON:
                setAfternoonEnergy(value);
                break;
            default:
                setEveningEnergy(value);
                break;
        }
    }

    public int getEnergy(EnergySlot slot) {
        switch (slot) {
            case MORNING:
                return morningEnergy;
            case AFTERNOON:
                return afternoonEnergy;
            default:
                return eveningEnergy;
        }
    }

    public String getWentWellText() {
        return wentWellText == null ? "" : wentWellText;
    }

    public void setWentWellText(String text) {
        this.wentWellText = text == null ? "" : text;
    }

    public String getImproveText() {
        return improveText == null ? "" : improveText;
    }

    public void setImproveText(String text) {
        this.improveText = text == null ? "" : text;
    }

    /** True when the user has entered nothing at all. */
    public boolean isEmpty() {
        return morningEnergy == UNRATED
                && afternoonEnergy == UNRATED
                && eveningEnergy == UNRATED
                && getWentWellText().trim().isEmpty()
                && getImproveText().trim().isEmpty();
    }

    /** Clamps out-of-range ratings instead of throwing on user input. */
    private static int sanitizeRating(int value) {
        if (value == UNRATED) {
            return UNRATED;
        }
        return Math.max(MIN_RATING, Math.min(MAX_RATING, value));
    }

    private static String requireDate(String dateIso) {
        if (!IntervalRules.isValidDateIso(dateIso)) {
            throw new IllegalArgumentException(
                    "date must be yyyy-MM-dd, was: " + dateIso);
        }
        return dateIso.trim();
    }

    /** The three parts of the day the energy tracker covers. */
    public enum EnergySlot {
        MORNING, AFTERNOON, EVENING
    }
}
