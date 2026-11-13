package com.example.dailytodolist;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dailytodolist.model.DailyReview;
import com.example.dailytodolist.util.ReviewStore;
import com.example.dailytodolist.util.ThemeColors;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * End-of-day review: energy ratings plus two reflection prompts.
 *
 * <p>The review is addressed by date, so reopening the screen on the same day
 * loads what was already written and the user edits it rather than starting
 * over.</p>
 */
public class ReviewActivity extends BaseActivity {

    private static final int STAR_COUNT = DailyReview.MAX_RATING;

    private TextView reviewDate;
    private TextView backButton;
    private TextView saveReviewButton;
    private LinearLayout energyCard;
    private LinearLayout reviewCard;
    private TextInputEditText wentWellInput;
    private TextInputEditText improveInput;

    private ReviewStore store;
    private String dateIso;
    private DailyReview review;

    /** Star views per part of the day, rebuilt when a rating changes. */
    private final Map<DailyReview.EnergySlot, LinearLayout> starContainers =
            new EnumMap<>(DailyReview.EnergySlot.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        dateIso = todayIso();
        store = ReviewStore.from(this);
        review = store.load(dateIso);

        bindViews();
        buildEnergyRows();
        prefill();
        applyTheme();
        setupListeners();
    }

    private void bindViews() {
        reviewDate = findViewById(R.id.reviewDate);
        backButton = findViewById(R.id.backButton);
        saveReviewButton = findViewById(R.id.saveReviewButton);
        energyCard = findViewById(R.id.energyCard);
        reviewCard = findViewById(R.id.reviewCard);
        wentWellInput = findViewById(R.id.wentWellInput);
        improveInput = findViewById(R.id.improveInput);

        reviewDate.setText(dateIso);
    }

    // ------------------------------------------------------------------
    // Energy tracker
    // ------------------------------------------------------------------

    /** Builds the three labelled rows of five stars each. */
    private void buildEnergyRows() {
        addEnergyRow(DailyReview.EnergySlot.MORNING, R.id.energyMorning,
                R.string.energy_morning);
        addEnergyRow(DailyReview.EnergySlot.AFTERNOON, R.id.energyAfternoon,
                R.string.energy_afternoon);
        addEnergyRow(DailyReview.EnergySlot.EVENING, R.id.energyEvening,
                R.string.energy_evening);
    }

    private void addEnergyRow(DailyReview.EnergySlot slot, int targetId, int labelRes) {
        View target = findViewById(targetId);
        if (!(target instanceof LinearLayout)) {
            return;
        }

        // The three placeholders in the XML are swapped for the real row, so the
        // layout keeps its declared order without duplicating markup.
        LinearLayout container = (LinearLayout) target;
        container.removeAllViews();

        View row = LayoutInflater.from(this).inflate(R.layout.item_energy_row, container, false);
        TextView label = row.findViewById(R.id.energyLabel);
        LinearLayout stars = row.findViewById(R.id.energyStars);

        label.setText(labelRes);
        starContainers.put(slot, stars);

        for (int star = 1; star <= STAR_COUNT; star++) {
            final int value = star;
            TextView starView = new TextView(this);
            starView.setText("\u2605");
            starView.setTextSize(22);
            starView.setPadding(dp(4), dp(4), dp(4), dp(4));
            starView.setClickable(true);
            starView.setFocusable(true);
            starView.setOnClickListener(v -> {
                // Tapping the current rating again clears it, so an accidental
                // tap can be undone without leaving the screen.
                int current = review.getEnergy(slot);
                review.setEnergy(slot, current == value ? DailyReview.UNRATED : value);
                refreshStars(slot);
            });
            stars.addView(starView);
        }

        container.addView(row);
        refreshStars(slot);
    }

    /** Repaints one row's stars to match the stored rating. */
    private void refreshStars(DailyReview.EnergySlot slot) {
        LinearLayout stars = starContainers.get(slot);
        if (stars == null) {
            return;
        }

        int rating = review.getEnergy(slot);
        int filled = ThemeHelper.getCurrentThemeAccentColor(this);
        // Unfilled stars are supporting text on the themed surface; a static
        // palette constant would be invisible on the darker palettes.
        int empty = ThemeColors.onSurfaceVariant(stars);

        for (int i = 0; i < stars.getChildCount(); i++) {
            TextView star = (TextView) stars.getChildAt(i);
            // Children are ordered 1..5, so compare against position + 1.
            star.setTextColor(i < rating ? filled : empty);
        }
    }

    // ------------------------------------------------------------------
    // Pre-fill
    // ------------------------------------------------------------------

    private void prefill() {
        wentWellInput.setText(review.getWentWellText());
        improveInput.setText(review.getImproveText());
    }

    // ------------------------------------------------------------------
    // Theming
    // ------------------------------------------------------------------

    private void applyTheme() {
        boolean isDarkMode = ThemeManager.isDarkMode(this);
        int cardBackground = isDarkMode
                ? R.drawable.dark_card_bg
                : R.drawable.white_card_bg;

        if (energyCard != null) {
            energyCard.setBackgroundResource(cardBackground);
        }
        if (reviewCard != null) {
            reviewCard.setBackgroundResource(cardBackground);
        }

        int[] gradient = ThemeManager.getThemeGradientColors(
                this, ThemeManager.getCurrentTheme(this));
        if (saveReviewButton != null) {
            GradientDrawable shape = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT, gradient);
            shape.setCornerRadius(26 * getResources().getDisplayMetrics().density);
            saveReviewButton.setBackground(shape);
            // The label sits on the theme gradient, so it is picked from the
            // gradient: a fixed white disappears on the light end of sunset
            // (#FFE53B).
            saveReviewButton.setTextColor(ThemeColors.onGradient(gradient[0], gradient[1]));
        }
        if (backButton != null) {
            backButton.setTextColor(ThemeHelper.getCurrentThemeAccentColor(this));
        }
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        saveReviewButton.setOnClickListener(v -> saveReview());
    }

    private void saveReview() {
        review.setWentWellText(textOf(wentWellInput));
        review.setImproveText(textOf(improveInput));

        store.save(review);
        Toast.makeText(this, R.string.review_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private static String textOf(TextInputEditText input) {
        if (input == null || input.getText() == null) {
            return "";
        }
        return input.getText().toString().trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private String todayIso() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
