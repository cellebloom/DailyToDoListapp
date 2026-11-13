package com.example.dailytodolist;

import android.content.SharedPreferences;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dailytodolist.util.ThemeColors;

public class ThemeActivity extends AppCompatActivity {

    private TextView defaultThemeStatus, hotpinkStatus, cyanHotpinkStatus, limeStatus, cyanIndigoStatus;
    private TextView yellowRedStatus, creamRoseStatus, yellowCyanStatus, magentaBlueStatus;
    private TextView mintStatus, sunsetStatus;
    private TextView themeTitle, themesAvailableText;
    private LinearLayout mainLayout;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean isDarkMode = false; // FIX: Initialize with default value

    // Store all card backgrounds and theme names
    private LinearLayout[] cardInnerLayouts = new LinearLayout[11];
    private TextView[] themeNameTexts = new TextView[11];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply language FIRST - before anything else
        LanguageManager.applyLanguage(this);

        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme);

        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // FIX: Initialize dark mode from SharedPreferences like MainActivity
        isDarkMode = ThemeManager.isDarkMode(this);

        initViews();
        updateThemeStatuses();
        applyThemeGradients();
        applyDarkMode(); // This will apply colors based on isDarkMode
        setupClickListeners();
        setupNavigation();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.mainLayout);
        defaultThemeStatus = findViewById(R.id.defaultThemeStatus);
        hotpinkStatus = findViewById(R.id.hotpinkStatus);
        cyanHotpinkStatus = findViewById(R.id.cyanHotpinkStatus);
        limeStatus = findViewById(R.id.limeStatus);
        cyanIndigoStatus = findViewById(R.id.cyanIndigoStatus);
        yellowRedStatus = findViewById(R.id.yellowRedStatus);
        creamRoseStatus = findViewById(R.id.creamRoseStatus);
        yellowCyanStatus = findViewById(R.id.yellowCyanStatus);
        magentaBlueStatus = findViewById(R.id.magentaBlueStatus);
        mintStatus = findViewById(R.id.mintStatus);
        sunsetStatus = findViewById(R.id.sunsetStatus);

        // Find header texts using IDs from updated XML
        themeTitle = findViewById(R.id.themesTitle);
        themesAvailableText = findViewById(R.id.availableThemesSubtitle);

        // Find all theme cards and inner layouts
        ViewGroup scrollView = (ViewGroup) mainLayout.getChildAt(1);
        if (scrollView != null) {
            ViewGroup scrollContent = (ViewGroup) scrollView.getChildAt(0);
            if (scrollContent instanceof LinearLayout) {
                LinearLayout contentLayout = (LinearLayout) scrollContent;
                for (int i = 0; i < contentLayout.getChildCount(); i++) {
                    View themeCard = contentLayout.getChildAt(i);
                    if (themeCard instanceof LinearLayout) {
                        LinearLayout outerCard = (LinearLayout) themeCard;
                        if (outerCard.getChildCount() > 0 && outerCard.getChildAt(0) instanceof LinearLayout) {
                            cardInnerLayouts[i] = (LinearLayout) outerCard.getChildAt(0);

                            LinearLayout innerCard = cardInnerLayouts[i];
                            if (innerCard.getChildCount() >= 2) {
                                View nameContainer = innerCard.getChildAt(1);
                                if (nameContainer instanceof LinearLayout) {
                                    LinearLayout nameLayout = (LinearLayout) nameContainer;
                                    if (nameLayout.getChildCount() > 0 && nameLayout.getChildAt(0) instanceof TextView) {
                                        themeNameTexts[i] = (TextView) nameLayout.getChildAt(0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Update header texts with string resources
        if (themeTitle != null) {
            themeTitle.setText(R.string.themes);
        }
        if (themesAvailableText != null) {
            themesAvailableText.setText(R.string.available_themes);
        }
    }

    private void applyDarkMode() {
        // FIX: Always get fresh dark mode value from SharedPreferences
        isDarkMode = ThemeManager.isDarkMode(this);

        // Header text sits on the theme gradient, so it is derived from that
        // gradient rather than from a fixed dark/light pair.
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        int onGradient = ThemeColors.onGradient(gradientColors[0], gradientColors[1]);

        if (themeTitle != null) {
            themeTitle.setTextColor(onGradient);
        }

        if (themesAvailableText != null) {
            themesAvailableText.setTextColor(onGradient);
        }

        // Update all theme cards
        for (int i = 0; i < cardInnerLayouts.length; i++) {
            if (cardInnerLayouts[i] != null) {
                GradientDrawable cardBg = new GradientDrawable();
                cardBg.setShape(GradientDrawable.RECTANGLE);
                cardBg.setCornerRadius(12 * getResources().getDisplayMetrics().density);
                cardBg.setColor(ThemeColors.surface(cardInnerLayouts[i]));
                cardInnerLayouts[i].setBackground(cardBg);
            }

            if (themeNameTexts[i] != null) {
                themeNameTexts[i].setTextColor(ThemeColors.onSurface(themeNameTexts[i]));
            }
        }

        // Update status text colors
        updateThemeStatuses();

        // Update bottom navigation
        updateBottomNavigation();
    }

    private void updateBottomNavigation() {
        // Get the bottom navigation bar (it's the last child of mainLayout)
        LinearLayout footerNav = null;
        if (mainLayout.getChildCount() > 2) {
            footerNav = (LinearLayout) mainLayout.getChildAt(2);
        }

        if (footerNav != null) {
            GradientDrawable footerBg = new GradientDrawable();
            footerBg.setShape(GradientDrawable.RECTANGLE);
            footerBg.setColor(ThemeColors.surface(footerNav));
            footerNav.setBackground(footerBg);
        }

        // Find all navigation items and set their colors
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navCalendar = findViewById(R.id.navCalendar);
        LinearLayout navShop = findViewById(R.id.navShop);
        LinearLayout navThemes = findViewById(R.id.navThemes);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        // Set text and icon colors for each navigation item using string resources
        updateSingleNavItem(navHome, getString(R.string.nav_home), "🏠");
        updateSingleNavItem(navCalendar, getString(R.string.nav_calendar), "📅");
        updateSingleNavItem(navShop, getString(R.string.nav_shop), "🛒");
        updateSingleNavItem(navThemes, getString(R.string.nav_themes), "🎨", true); // Themes is current page
        updateSingleNavItem(navProfile, getString(R.string.profile), "👤");
    }

    private void updateSingleNavItem(LinearLayout navItem, String label, String icon) {
        updateSingleNavItem(navItem, label, icon, false);
    }

    private void updateSingleNavItem(LinearLayout navItem, String label, String icon, boolean isCurrentPage) {
        if (navItem == null) return;

        // Get the icon TextView (first child)
        TextView iconView = null;
        if (navItem.getChildCount() > 0 && navItem.getChildAt(0) instanceof TextView) {
            iconView = (TextView) navItem.getChildAt(0);
            if (iconView != null && iconView.getText().toString().isEmpty()) {
                iconView.setText(icon);
            }
        }

        // Get the text TextView (second child)
        TextView textView = null;
        if (navItem.getChildCount() > 1 && navItem.getChildAt(1) instanceof TextView) {
            textView = (TextView) navItem.getChildAt(1);
            if (textView != null) {
                textView.setText(label);
            }
        }

        int navColor = isCurrentPage
                ? ThemeColors.primary(navItem)
                : ThemeColors.onSurfaceVariant(navItem);
        if (iconView != null) {
            iconView.setTextColor(navColor);
        }
        if (textView != null) {
            textView.setTextColor(navColor);
        }
    }

    private void applyThemeGradients() {
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);

        if (mainLayout != null) {
            GradientDrawable gradientDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    gradientColors
            );
            mainLayout.setBackground(gradientDrawable);
        }

        updateThemeStatuses();
    }

    private void updateThemeStatuses() {
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);

        // Update all theme statuses using string resources
        updateThemeStatus(defaultThemeStatus, ThemeManager.THEME_DEFAULT, currentTheme, R.string.default_theme);
        updateThemeStatus(hotpinkStatus, ThemeManager.THEME_HOTPINK, currentTheme, R.string.hot_pink);
        updateThemeStatus(cyanHotpinkStatus, ThemeManager.THEME_CYAN_HOTPINK, currentTheme, R.string.cyan_hot_pink);
        updateThemeStatus(limeStatus, ThemeManager.THEME_LIME, currentTheme, R.string.lime_blue);
        updateThemeStatus(cyanIndigoStatus, ThemeManager.THEME_CYAN_INDIGO, currentTheme, R.string.cyan_indigo);
        updateThemeStatus(yellowRedStatus, ThemeManager.THEME_YELLOW_RED, currentTheme, R.string.yellow_red);
        updateThemeStatus(creamRoseStatus, ThemeManager.THEME_CREAM_ROSE, currentTheme, R.string.cream_rose);
        updateThemeStatus(yellowCyanStatus, ThemeManager.THEME_YELLOW_CYAN, currentTheme, R.string.yellow_cyan);
        updateThemeStatus(mintStatus, ThemeManager.THEME_MINT, currentTheme, R.string.mint_green);
        updateThemeStatus(magentaBlueStatus, ThemeManager.THEME_MAGENTA_BLUE, currentTheme, R.string.magenta_blue);
        updateThemeStatus(sunsetStatus, ThemeManager.THEME_SUNSET, currentTheme, R.string.sunset_orange);
    }

    private void updateThemeStatus(TextView statusView, String themeId, String currentTheme, int themeNameResId) {
        if (statusView == null) return;

        if (themeId.equals(currentTheme)) {
            statusView.setText(getString(R.string.applied));
            statusView.setTextColor(getColor(R.color.accent_green));
        } else {
            statusView.setText(getString(R.string.not_applied));
            // colorOnSurfaceVariant is the secondary-text role: it is guaranteed
            // to keep AA contrast against colorSurface in every palette, which a
            // fixed grey does not (text_muted measured 2.68:1 on white).
            statusView.setTextColor(ThemeColors.onSurfaceVariant(statusView));
        }
        statusView.setBackgroundColor(0);
    }

    private void applyTheme(String themeId) {
        editor.putString("selected_theme", themeId);
        editor.apply();

        // Get theme name from resources
        String themeName = getThemeName(themeId);
        Toast.makeText(this, themeName + " " + getString(R.string.applied), Toast.LENGTH_SHORT).show();
        recreate();
    }

    private String getThemeName(String themeId) {
        switch (themeId) {
            case ThemeManager.THEME_DEFAULT:
                return getString(R.string.default_theme);
            case ThemeManager.THEME_HOTPINK:
                return getString(R.string.hot_pink);
            case ThemeManager.THEME_CYAN_HOTPINK:
                return getString(R.string.cyan_hot_pink);
            case ThemeManager.THEME_LIME:
                return getString(R.string.lime_blue);
            case ThemeManager.THEME_CYAN_INDIGO:
                return getString(R.string.cyan_indigo);
            case ThemeManager.THEME_YELLOW_RED:
                return getString(R.string.yellow_red);
            case ThemeManager.THEME_CREAM_ROSE:
                return getString(R.string.cream_rose);
            case ThemeManager.THEME_YELLOW_CYAN:
                return getString(R.string.yellow_cyan);
            case ThemeManager.THEME_MAGENTA_BLUE:
                return getString(R.string.magenta_blue);
            case ThemeManager.THEME_MINT:
                return getString(R.string.mint_green);
            case ThemeManager.THEME_SUNSET:
                return getString(R.string.sunset_orange);
            default:
                return getString(R.string.default_theme);
        }
    }

    private void setupClickListeners() {
        defaultThemeStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_DEFAULT));
        hotpinkStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_HOTPINK));
        cyanHotpinkStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_CYAN_HOTPINK));
        limeStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_LIME));
        cyanIndigoStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_CYAN_INDIGO));
        yellowRedStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_YELLOW_RED));
        creamRoseStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_CREAM_ROSE));
        yellowCyanStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_YELLOW_CYAN));
        mintStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_MINT));
        magentaBlueStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_MAGENTA_BLUE));
        sunsetStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_SUNSET));
    }

    private void setupNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.navCalendar).setOnClickListener(v -> {
            Intent intent = new Intent(this, CalendarActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.navShop).setOnClickListener(v -> {
            Intent intent = new Intent(this, ShopActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.navThemes).setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.youre_on_themes), Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FIX: Refresh everything when returning to the activity
        isDarkMode = ThemeManager.isDarkMode(this);
        applyThemeGradients();
        applyDarkMode();
        updateThemeStatuses();

        // Refresh header texts
        if (themeTitle != null) {
            themeTitle.setText(R.string.themes);
        }
        if (themesAvailableText != null) {
            themesAvailableText.setText(R.string.available_themes);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // FIX: Get fresh dark mode value on configuration change
        isDarkMode = ThemeManager.isDarkMode(this);
        applyThemeGradients();
        applyDarkMode();
    }
}