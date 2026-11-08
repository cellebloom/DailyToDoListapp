package com.example.dailytodolist.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Covers the end-of-day review model: energy ratings and reflection text.
 */
public class DailyReviewTest {

    private static final String DATE = "2026-11-07";

    @Test
    public void newReviewStartsEmptyAndUnrated() {
        DailyReview review = DailyReview.emptyFor(DATE);

        assertTrue(review.isEmpty());
        assertEquals(DailyReview.UNRATED, review.getMorningEnergy());
        assertEquals(DailyReview.UNRATED, review.getAfternoonEnergy());
        assertEquals(DailyReview.UNRATED, review.getEveningEnergy());
        assertEquals("", review.getWentWellText());
        assertEquals("", review.getImproveText());
    }

    @Test
    public void unratedIsDistinctFromLowestRating() {
        DailyReview unrated = DailyReview.emptyFor(DATE);
        DailyReview terrible = DailyReview.emptyFor(DATE);
        terrible.setMorningEnergy(1);

        assertEquals(0, unrated.getMorningEnergy());
        assertEquals(1, terrible.getMorningEnergy());
        // A blank morning must not look like an awful one.
        assertFalse(unrated.getMorningEnergy() == terrible.getMorningEnergy());
    }

    @Test
    public void ratingsAreStoredPerSlot() {
        DailyReview review = DailyReview.emptyFor(DATE);
        review.setEnergy(DailyReview.EnergySlot.MORNING, 2);
        review.setEnergy(DailyReview.EnergySlot.AFTERNOON, 4);
        review.setEnergy(DailyReview.EnergySlot.EVENING, 5);

        assertEquals(2, review.getEnergy(DailyReview.EnergySlot.MORNING));
        assertEquals(4, review.getEnergy(DailyReview.EnergySlot.AFTERNOON));
        assertEquals(5, review.getEnergy(DailyReview.EnergySlot.EVENING));
    }

    @Test
    public void ratingsAreClampedToValidRange() {
        DailyReview review = DailyReview.emptyFor(DATE);

        review.setMorningEnergy(99);
        assertEquals(DailyReview.MAX_RATING, review.getMorningEnergy());

        // A smaller-than-possible rating clamps up to the lowest real rating;
        // only the explicit UNRATED sentinel means "no rating".
        review.setAfternoonEnergy(-5);
        assertEquals(DailyReview.MIN_RATING, review.getAfternoonEnergy());

        review.setEveningEnergy(DailyReview.UNRATED);
        assertEquals(DailyReview.UNRATED, review.getEveningEnergy());
    }

    @Test
    public void reviewWithContentIsNotEmpty() {
        DailyReview review = DailyReview.emptyFor(DATE);
        review.setWentWellText("Shipped the timeline");

        assertFalse(review.isEmpty());
    }

    @Test
    public void whitespaceOnlyTextStillCountsAsEmpty() {
        DailyReview review = DailyReview.emptyFor(DATE);
        review.setWentWellText("   ");

        assertTrue(review.isEmpty());
    }

    @Test
    public void textIsNeverNull() {
        DailyReview review = DailyReview.emptyFor(DATE);
        review.setWentWellText(null);
        review.setImproveText(null);

        assertEquals("", review.getWentWellText());
        assertEquals("", review.getImproveText());
    }

    @Test
    public void invalidDateIsRejected() {
        try {
            DailyReview.emptyFor("2026-13-01");
            fail("an impossible date must be rejected");
        } catch (IllegalArgumentException expected) {
            // correct
        }
    }
}
