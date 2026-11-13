package com.example.dailytodolist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.dailytodolist.util.ThemeColors;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ShopActivity extends BaseActivity {

    private TextView defaultThemeStatus, hotpinkStatus, cyanHotpinkStatus, limeStatus, cyanIndigoStatus;
    private TextView yellowRedStatus, creamRoseStatus, yellowCyanStatus, magentaBlueStatus;
    private TextView mintStatus, sunsetStatus;
    private TextView coinCountText;
    private TextView shopTitle;
    private TextView themesAvailableText;
    private LinearLayout mainLayout;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean isDarkMode = false;

    // Store all card backgrounds
    private LinearLayout[] cardInnerLayouts = new LinearLayout[11];
    private TextView[] themeNameTexts = new TextView[11];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // FIX: Initialize dark mode from SharedPreferences like MainActivity does
        isDarkMode = ThemeManager.isDarkMode(this);

        initViews();
        updateCoinDisplay();
        giveDailyCoins();
        updateThemeStatuses();
        applyGradientBackground();
        applyDarkMode(); // This will apply colors based on isDarkMode
        setupClickListeners();
        setupNavigation();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.mainLayout);

        // Find header texts using IDs
        shopTitle = findViewById(R.id.shopTitle);
        themesAvailableText = findViewById(R.id.shopSubtitle);

        // Find coin counter
        coinCountText = findViewById(R.id.coinCountText);

        // Find all status TextViews
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

        // Update header texts with string resources
        if (shopTitle != null) {
            shopTitle.setText(R.string.shop);
        }
        if (themesAvailableText != null) {
            themesAvailableText.setText(R.string.themes_available_in_shop);
        }

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
    }

    private void applyDarkMode() {
        // FIX: Always get fresh dark mode value from SharedPreferences
        isDarkMode = ThemeManager.isDarkMode(this);

        // Header text sits on the theme gradient, so it is derived from that
        // gradient rather than from a fixed dark/light pair.
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        int onGradient = ThemeColors.onGradient(gradientColors[0], gradientColors[1]);

        if (shopTitle != null) {
            shopTitle.setTextColor(onGradient);
        }

        if (themesAvailableText != null) {
            themesAvailableText.setTextColor(onGradient);
        }

        if (coinCountText != null) {
            coinCountText.setTextColor(onGradient);
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

        updateThemeStatuses();
        updateBottomNavigation();
    }

    private void updateBottomNavigation() {
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

        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navCalendar = findViewById(R.id.navCalendar);
        LinearLayout navShop = findViewById(R.id.navShop);
        LinearLayout navThemes = findViewById(R.id.navThemes);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        updateSingleNavItem(navHome, getString(R.string.nav_home), "🏠");
        updateSingleNavItem(navCalendar, getString(R.string.nav_calendar), "📅");
        updateSingleNavItem(navThemes, getString(R.string.nav_themes), "🎨");
        updateSingleNavItem(navProfile, getString(R.string.profile), "👤");
        updateSingleNavItem(navShop, getString(R.string.nav_shop), "🛒", true);
    }

    private void updateSingleNavItem(LinearLayout navItem, String label, String icon) {
        updateSingleNavItem(navItem, label, icon, false);
    }

    private void updateSingleNavItem(LinearLayout navItem, String label, String icon, boolean isShop) {
        if (navItem == null) return;

        TextView iconView = null;
        if (navItem.getChildCount() > 0 && navItem.getChildAt(0) instanceof TextView) {
            iconView = (TextView) navItem.getChildAt(0);
            if (iconView != null && iconView.getText().toString().isEmpty()) {
                iconView.setText(icon);
            }
        }

        TextView textView = null;
        if (navItem.getChildCount() > 1 && navItem.getChildAt(1) instanceof TextView) {
            textView = (TextView) navItem.getChildAt(1);
            if (textView != null) {
                textView.setText(label);
            }
        }

        int navColor = isShop
                ? ThemeColors.primary(navItem)
                : ThemeColors.onSurfaceVariant(navItem);
        if (iconView != null) {
            iconView.setTextColor(navColor);
        }
        if (textView != null) {
            textView.setTextColor(navColor);
        }
    }

    @Override
    protected void applyGradientBackground() {
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);

        if (mainLayout != null) {
            GradientDrawable gradientDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    gradientColors
            );
            mainLayout.setBackground(gradientDrawable);
        }
    }

    private void updateCoinDisplay() {
        int currentCoins = sharedPreferences.getInt("user_coins", 0);
        if (coinCountText != null) {
            coinCountText.setText(String.valueOf(currentCoins));
        }
    }

    private void giveDailyCoins() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastRewardDate = sharedPreferences.getString("last_reward_date", "");

        if (!today.equals(lastRewardDate)) {
            int currentCoins = sharedPreferences.getInt("user_coins", 0);
            int newCoins = currentCoins + 10;
            editor.putInt("user_coins", newCoins);
            editor.putString("last_reward_date", today);
            editor.apply();

            updateCoinDisplay();
            Toast.makeText(this, "🎉 " + getString(R.string.daily_coins_reward) + " 🎉", Toast.LENGTH_LONG).show();
        }
    }

    private void updateThemeStatuses() {
        // Default theme always owned
        if (defaultThemeStatus != null) {
            defaultThemeStatus.setText(getString(R.string.owned));
            defaultThemeStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
            defaultThemeStatus.setBackgroundColor(0);
        }

        String coinString = getString(R.string.coins_text);
        updateThemeStatus(hotpinkStatus, ThemeManager.THEME_HOTPINK, "200 " + coinString);
        updateThemeStatus(cyanHotpinkStatus, "cyan_hotpink", "200 " + coinString);
        updateThemeStatus(limeStatus, ThemeManager.THEME_LIME, "200 " + coinString);
        updateThemeStatus(cyanIndigoStatus, "cyan_indigo", "200 " + coinString);
        updateThemeStatus(yellowRedStatus, "yellow_red", "200 " + coinString);
        updateThemeStatus(creamRoseStatus, "cream_rose", "200 " + coinString);
        updateThemeStatus(yellowCyanStatus, "yellow_cyan", "200 " + coinString);
        updateThemeStatus(mintStatus, ThemeManager.THEME_MINT, "200 " + coinString);
        updateThemeStatus(magentaBlueStatus, "magenta_blue", "200 " + coinString);
        updateThemeStatus(sunsetStatus, ThemeManager.THEME_SUNSET, "200 " + coinString);
    }

    private void updateThemeStatus(TextView statusView, String themeId, String priceText) {
        if (statusView == null) return;

        if (isThemeOwned(themeId)) {
            statusView.setText(getString(R.string.owned));
            statusView.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
        } else {
            statusView.setText(priceText);
            statusView.setTextColor(ContextCompat.getColor(this, R.color.price_gold));
        }
        statusView.setBackgroundColor(0);
    }

    private boolean isThemeOwned(String themeId) {
        if (themeId.equals(ThemeManager.THEME_DEFAULT)) {
            return true;
        }
        return sharedPreferences.getBoolean("theme_owned_" + themeId, false);
    }

    private void purchaseTheme(String themeId, String themeName, int price) {
        int currentCoins = sharedPreferences.getInt("user_coins", 0);

        if (currentCoins >= price) {
            showPurchaseConfirmation(themeId, themeName, price, currentCoins);
        } else {
            Toast.makeText(this, getString(R.string.insufficient_coins_message), Toast.LENGTH_LONG).show();
        }
    }

    private void showPurchaseConfirmation(String themeId, String themeName, int price, int currentCoins) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.purchase_title) + " " + themeName)
                .setMessage(String.format(getString(R.string.purchase_confirmation_message), themeName, price, currentCoins))
                .setPositiveButton(getString(R.string.buy), (dialog, which) -> {
                    editor.putBoolean("theme_owned_" + themeId, true);
                    editor.putInt("user_coins", currentCoins - price);
                    editor.apply();
                    updateThemeStatuses();
                    updateCoinDisplay();
                    applyDarkMode();
                    Toast.makeText(this, themeName + " " + getString(R.string.theme_purchased), Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void applyTheme(String themeId, String themeName) {
        editor.putString("selected_theme", themeId);
        editor.apply();
        updateThemeStatuses();
        Toast.makeText(this, themeName + " " + getString(R.string.applied_text), Toast.LENGTH_SHORT).show();
        recreate();
    }

    private void setupClickListeners() {
        defaultThemeStatus.setOnClickListener(v -> applyTheme(ThemeManager.THEME_DEFAULT, getString(R.string.default_theme)));

        hotpinkStatus.setOnClickListener(v -> {
            if (isThemeOwned(ThemeManager.THEME_HOTPINK)) {
                applyTheme(ThemeManager.THEME_HOTPINK, getString(R.string.hot_pink));
            } else {
                purchaseTheme(ThemeManager.THEME_HOTPINK, getString(R.string.hot_pink), 200);
            }
        });

        cyanHotpinkStatus.setOnClickListener(v -> {
            if (isThemeOwned("cyan_hotpink")) {
                applyTheme("cyan_hotpink", getString(R.string.cyan_hot_pink));
            } else {
                purchaseTheme("cyan_hotpink", getString(R.string.cyan_hot_pink), 200);
            }
        });

        limeStatus.setOnClickListener(v -> {
            if (isThemeOwned(ThemeManager.THEME_LIME)) {
                applyTheme(ThemeManager.THEME_LIME, getString(R.string.lime_blue));
            } else {
                purchaseTheme(ThemeManager.THEME_LIME, getString(R.string.lime_blue), 200);
            }
        });

        cyanIndigoStatus.setOnClickListener(v -> {
            if (isThemeOwned("cyan_indigo")) {
                applyTheme("cyan_indigo", getString(R.string.cyan_indigo));
            } else {
                purchaseTheme("cyan_indigo", getString(R.string.cyan_indigo), 200);
            }
        });

        yellowRedStatus.setOnClickListener(v -> {
            if (isThemeOwned("yellow_red")) {
                applyTheme("yellow_red", getString(R.string.yellow_red));
            } else {
                purchaseTheme("yellow_red", getString(R.string.yellow_red), 200);
            }
        });

        creamRoseStatus.setOnClickListener(v -> {
            if (isThemeOwned("cream_rose")) {
                applyTheme("cream_rose", getString(R.string.cream_rose));
            } else {
                purchaseTheme("cream_rose", getString(R.string.cream_rose), 200);
            }
        });

        yellowCyanStatus.setOnClickListener(v -> {
            if (isThemeOwned("yellow_cyan")) {
                applyTheme("yellow_cyan", getString(R.string.yellow_cyan));
            } else {
                purchaseTheme("yellow_cyan", getString(R.string.yellow_cyan), 200);
            }
        });

        mintStatus.setOnClickListener(v -> {
            if (isThemeOwned(ThemeManager.THEME_MINT)) {
                applyTheme(ThemeManager.THEME_MINT, getString(R.string.mint_green));
            } else {
                purchaseTheme(ThemeManager.THEME_MINT, getString(R.string.mint_green), 200);
            }
        });

        magentaBlueStatus.setOnClickListener(v -> {
            if (isThemeOwned("magenta_blue")) {
                applyTheme("magenta_blue", getString(R.string.magenta_blue));
            } else {
                purchaseTheme("magenta_blue", getString(R.string.magenta_blue), 200);
            }
        });

        sunsetStatus.setOnClickListener(v -> {
            if (isThemeOwned(ThemeManager.THEME_SUNSET)) {
                applyTheme(ThemeManager.THEME_SUNSET, getString(R.string.sunset_orange));
            } else {
                purchaseTheme(ThemeManager.THEME_SUNSET, getString(R.string.sunset_orange), 200);
            }
        });
    }

    @Override
    protected void setupNavigation() {
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        findViewById(R.id.navCalendar).setOnClickListener(v -> {
            startActivity(new Intent(this, CalendarActivity.class));
            finish();
        });
        findViewById(R.id.navShop).setOnClickListener(v -> {});
        findViewById(R.id.navThemes).setOnClickListener(v -> {
            startActivity(new Intent(this, ThemeActivity.class));
            finish();
        });
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // FIX: This is the most important part - refresh everything when returning to the activity
        // Just like MainActivity does in its onResume()
        isDarkMode = ThemeManager.isDarkMode(this);
        updateCoinDisplay();
        updateThemeStatuses();
        applyGradientBackground();
        applyDarkMode(); // This will apply the correct dark/light mode colors

        if (shopTitle != null) {
            shopTitle.setText(R.string.shop);
        }
        if (themesAvailableText != null) {
            themesAvailableText.setText(R.string.themes_available_in_shop);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // FIX: Handle configuration changes like MainActivity does
        applyGradientBackground();
        applyDarkMode();
    }
}