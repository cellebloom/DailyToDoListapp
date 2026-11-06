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

public class ThemeActivity extends AppCompatActivity {

    private TextView defaultThemeStatus, hotpinkStatus, cyanHotpinkStatus, limeStatus, cyanIndigoStatus;
    private TextView yellowRedStatus, creamRoseStatus, yellowCyanStatus, magentaBlueStatus;
    private TextView themeTitle, themesAvailableText;
    private LinearLayout mainLayout;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean isDarkMode = false; // FIX: Initialize with default value

    // Store all card backgrounds and theme names
    private LinearLayout[] cardInnerLayouts = new LinearLayout[9];
    private TextView[] themeNameTexts = new TextView[9];

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
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);

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

        // Find header texts using IDs from updated XML
        themeTitle = findViewById(R.id.themesTitle);
        themesAvailableText = findViewById(R.id.availableThemesSubtitle);

        // Find all theme cards and inner layouts
        ViewGroup scrollView = (ViewGroup) mainLayout.getChildAt(1);
        if (scrollView != null) {
            ViewGroup scrollContent = (ViewGroup) scrollView.getChildAt(0);
            if (scrollContent instanceof LinearLayout) {
                LinearLayout contentLayout = (LinearLayout) scrollContent;
                for (int i = 0; i < contentLayout.getChildCount() && i < 9; i++) {
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
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);

        // Update header colors - Light mode = White, Dark mode = Black
        if (themeTitle != null) {
            themeTitle.setTextColor(isDarkMode ? android.graphics.Color.parseColor("#000000") : android.graphics.Color.parseColor("#FFFFFF"));
        }

        if (themesAvailableText != null) {
            themesAvailableText.setTextColor(isDarkMode ? android.graphics.Color.parseColor("#000000") : android.graphics.Color.parseColor("#E0E0E0"));
        }

        // Update all theme cards
        for (int i = 0; i < cardInnerLayouts.length; i++) {
            if (cardInnerLayouts[i] != null) {
                if (isDarkMode) {
                    // Dark mode - make inner card BLACK
                    GradientDrawable darkCardBg = new GradientDrawable();
                    darkCardBg.setShape(GradientDrawable.RECTANGLE);
                    darkCardBg.setCornerRadius(12 * getResources().getDisplayMetrics().density);
                    darkCardBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
                    cardInnerLayouts[i].setBackground(darkCardBg);
                } else {
                    // Light mode - restore white card
                    cardInnerLayouts[i].setBackgroundResource(R.drawable.white_card_bg);
                }
            }

            // Update theme name colors - WHITE in dark mode
            if (themeNameTexts[i] != null) {
                themeNameTexts[i].setTextColor(isDarkMode ? android.graphics.Color.parseColor("#FFFFFF") : android.graphics.Color.parseColor("#2C3E66"));
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
            if (isDarkMode) {
                // DARK MODE - BLACK background
                GradientDrawable blackFooterBg = new GradientDrawable();
                blackFooterBg.setShape(GradientDrawable.RECTANGLE);
                blackFooterBg.setColor(android.graphics.Color.parseColor("#000000"));
                footerNav.setBackground(blackFooterBg);
            } else {
                // LIGHT MODE - WHITE background
                footerNav.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"));
            }
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

        if (isDarkMode) {
            // Dark mode - WHITE text and icons (except current page)
            if (iconView != null) {
                iconView.setTextColor(isCurrentPage ? android.graphics.Color.parseColor("#9C27B0") : android.graphics.Color.parseColor("#FFFFFF"));
            }
            if (textView != null) {
                textView.setTextColor(isCurrentPage ? android.graphics.Color.parseColor("#9C27B0") : android.graphics.Color.parseColor("#FFFFFF"));
            }
        } else {
            // Light mode - dark gray text and icons (except current page)
            int grayColor = android.graphics.Color.parseColor("#7F8C8D");
            if (iconView != null) {
                iconView.setTextColor(isCurrentPage ? android.graphics.Color.parseColor("#9C27B0") : grayColor);
            }
            if (textView != null) {
                textView.setTextColor(isCurrentPage ? android.graphics.Color.parseColor("#9C27B0") : grayColor);
            }
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
        updateThemeStatus(magentaBlueStatus, ThemeManager.THEME_MAGENTA_BLUE, currentTheme, R.string.magenta_blue);
    }

    private void updateThemeStatus(TextView statusView, String themeId, String currentTheme, int themeNameResId) {
        if (statusView == null) return;

        if (themeId.equals(currentTheme)) {
            statusView.setText(getString(R.string.applied));
            statusView.setTextColor(getColor(R.color.accent_green));
        } else {
            statusView.setText(getString(R.string.not_applied));
            if (isDarkMode) {
                statusView.setTextColor(android.graphics.Color.parseColor("#FF6B6B"));
            } else {
                statusView.setTextColor(getColor(R.color.base_text_secondary));
            }
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
        magentaBlueStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_MAGENTA_BLUE));
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
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
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
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        applyThemeGradients();
        applyDarkMode();
    }
}