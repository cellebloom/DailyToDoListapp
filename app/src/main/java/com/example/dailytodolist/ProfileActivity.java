package com.example.dailytodolist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.widget.ArrayAdapter;
import android.graphics.Color;
import android.content.Context;

public class ProfileActivity extends BaseActivity {

    private TextView userName, userType, memberSince, tasksCompleted, totalCoins;
    private TextView avatarInitial;

    private TextView profileTitle;
    private LinearLayout editProfileName, switchAccount, manageAccount, themePreferences, logout, language;
    private LinearLayout mainLayout;
    private CardView[] cards = new CardView[4];
    private com.google.android.material.button.MaterialButtonToggleGroup nightModeGroup;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean isDarkMode = false;
    private boolean isSwitchingAccount = false; // Flag to prevent navigation during account switch


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // BaseActivity now handles language automatically
        // Just call super.onCreate() first
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Debug - Check what language is actually loaded
        Configuration config = getResources().getConfiguration();
        android.util.Log.d("LANGUAGE_TEST", "=== LANGUAGE DEBUG ===");
        android.util.Log.d("LANGUAGE_TEST", "Locale: " + config.locale);
        android.util.Log.d("LANGUAGE_TEST", "Profile string: " + getString(R.string.profile));
        android.util.Log.d("LANGUAGE_TEST", "Nav home string: " + getString(R.string.nav_home));
        android.util.Log.d("LANGUAGE_TEST", "Expected Chinese? " + (getString(R.string.profile).equals("个人资料") ? "YES ✓" : "NO ✗"));

        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        editor = sharedPreferences.edit();

        String savedLanguage = sharedPreferences.getString("app_language", "not set");
        android.util.Log.d("LANGUAGE_TEST", "Saved language in prefs: " + savedLanguage);

        String profileString = getString(R.string.profile);
        android.util.Log.d("LANGUAGE_TEST", "Profile string after onCreate: " + profileString);

        initViews();
        loadUserData();
        updateStats();
        setupDarkModeSwitch();
        applyTheme();
        applyDarkMode();
        setupClickListeners();
        setupNavigation();
    }

    /**
     * Shows the first character of the user's name in the avatar.
     *
     * <p>Falls back to a neutral person icon when the name is blank. Uses a
     * code-point-aware substring rather than {@code charAt(0)} so a name
     * starting with an emoji (which is a surrogate pair) is not cut in half and
     * rendered as a broken glyph.</p>
     */
    private void updateAvatarInitial(String name) {
        if (avatarInitial == null) {
            return;
        }

        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            avatarInitial.setText("👤");
            return;
        }

        int firstCodePoint = trimmed.codePointAt(0);
        avatarInitial.setText(new String(Character.toChars(firstCodePoint))
                .toUpperCase(Locale.getDefault()));
    }

    private void initViews() {
        mainLayout = findViewById(R.id.mainLayout);
        userName = findViewById(R.id.userName);
        userType = findViewById(R.id.userType);
        memberSince = findViewById(R.id.memberSince);
        tasksCompleted = findViewById(R.id.tasksCompleted);
        totalCoins = findViewById(R.id.totalCoins);
        avatarInitial = findViewById(R.id.avatarInitial);
        nightModeGroup = findViewById(R.id.nightModeGroup);

        editProfileName = findViewById(R.id.editProfileName);
        switchAccount = findViewById(R.id.switchAccount);
        manageAccount = findViewById(R.id.manageAccount);
        themePreferences = findViewById(R.id.themePreferences);
        logout = findViewById(R.id.logout);
        language = findViewById(R.id.language);

        // Find profile title directly using ID (from updated XML)
        profileTitle = findViewById(R.id.profileTitle);

        // Find section titles (optional - for future use)
        TextView accountSettingsTitle = findViewById(R.id.accountSettingsTitle);
        TextView appearanceTitle = findViewById(R.id.appearanceTitle);
        TextView darkLightText = findViewById(R.id.darkLightText);

        // Find all cards
        ViewGroup scrollView = (ViewGroup) mainLayout.getChildAt(1);
        if (scrollView != null) {
            ViewGroup scrollContent = (ViewGroup) scrollView.getChildAt(0);
            if (scrollContent instanceof LinearLayout) {
                LinearLayout contentLayout = (LinearLayout) scrollContent;
                int cardIndex = 0;
                for (int i = 0; i < contentLayout.getChildCount() && cardIndex < 4; i++) {
                    View child = contentLayout.getChildAt(i);
                    if (child instanceof CardView) {
                        cards[cardIndex] = (CardView) child;
                        cardIndex++;
                    }
                }
            }
        }
    }

    private void applyDarkMode() {
        // FIX: Always get fresh dark mode value from SharedPreferences
        isDarkMode = ThemeManager.isDarkMode(this);

        // Update profile title - WHITE in both modes (visible on gradient backgrounds)
        if (profileTitle != null) {
            profileTitle.setTextColor(Color.WHITE);
            profileTitle.setText(R.string.profile);
            profileTitle.invalidate();
        }

        // Update user name and type
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        int accentColor = gradientColors[0];

        if (userName != null) {
            userName.setTextColor(isDarkMode ? android.graphics.Color.parseColor("#FFFFFF") : accentColor);
        }
        if (userType != null) {
            userType.setTextColor(isDarkMode ? android.graphics.Color.parseColor("#CCCCCC") : android.graphics.Color.parseColor("#7F8C8D"));
        }

        // Update stats values
        if (memberSince != null) {
            memberSince.setTextColor(isDarkMode ? android.graphics.Color.parseColor("#FFFFFF") : accentColor);
        }
        if (tasksCompleted != null) {
            tasksCompleted.setTextColor(isDarkMode ? android.graphics.Color.parseColor("#FFFFFF") : accentColor);
        }
        if (totalCoins != null) {
            totalCoins.setTextColor(isDarkMode ? android.graphics.Color.parseColor("#FFFFFF") : accentColor);
        }

        // Find and update the avatar circle background
        ViewGroup scrollView = (ViewGroup) mainLayout.getChildAt(1);
        if (scrollView != null) {
            ViewGroup scrollContent = (ViewGroup) scrollView.getChildAt(0);
            if (scrollContent instanceof LinearLayout) {
                LinearLayout contentLayout = (LinearLayout) scrollContent;
                if (contentLayout.getChildCount() > 0) {
                    androidx.cardview.widget.CardView profileCard = (androidx.cardview.widget.CardView) contentLayout.getChildAt(0);
                    if (profileCard != null && profileCard.getChildAt(0) instanceof LinearLayout) {
                        LinearLayout cardInner = (LinearLayout) profileCard.getChildAt(0);
                        if (cardInner.getChildCount() > 0 && cardInner.getChildAt(0) instanceof LinearLayout) {
                            LinearLayout avatarCircle = (LinearLayout) cardInner.getChildAt(0);
                            if (avatarCircle != null) {
                                if (isDarkMode) {
                                    GradientDrawable grayCircleBg = new GradientDrawable();
                                    grayCircleBg.setShape(GradientDrawable.RECTANGLE);
                                    grayCircleBg.setCornerRadius(40 * getResources().getDisplayMetrics().density);
                                    grayCircleBg.setColor(android.graphics.Color.parseColor("#3D3D3D"));
                                    avatarCircle.setBackground(grayCircleBg);
                                } else {
                                    avatarCircle.setBackgroundResource(R.drawable.avatar_circle_bg);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Update card backgrounds
        for (CardView card : cards) {
            if (card != null) {
                if (isDarkMode) {
                    if (card.getChildAt(0) instanceof LinearLayout) {
                        LinearLayout innerLayout = (LinearLayout) card.getChildAt(0);
                        GradientDrawable darkCardBg = new GradientDrawable();
                        darkCardBg.setShape(GradientDrawable.RECTANGLE);
                        darkCardBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
                        darkCardBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
                        innerLayout.setBackground(darkCardBg);
                    }
                    card.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
                } else {
                    if (card.getChildAt(0) instanceof LinearLayout) {
                        LinearLayout innerLayout = (LinearLayout) card.getChildAt(0);
                        innerLayout.setBackgroundResource(R.drawable.white_card_bg);
                    }
                    card.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"));
                }
            }
        }

        // Update all text colors in cards
        updateAllTextColors();

        // Update settings menu items
        updateSettingsMenuItemWithForce(editProfileName, R.string.edit_profile_name, "›");
        updateSettingsMenuItemWithForce(switchAccount, R.string.switch_account, "›");
        updateSettingsMenuItemWithForce(manageAccount, R.string.manage_account, "›");
        updateSettingsMenuItemWithForce(themePreferences, R.string.theme_preferences, "›");
        updateSettingsMenuItemWithForce(logout, R.string.logout, "›");

        if (language != null) {
            updateSettingsMenuItemWithForce(language, R.string.language, "›");
        }

        // Update section titles
        TextView accountSettingsTitle = findViewById(R.id.accountSettingsTitle);
        if (accountSettingsTitle != null) {
            accountSettingsTitle.setTextColor(isDarkMode ? android.graphics.Color.parseColor("#FFFFFF") : android.graphics.Color.parseColor("#2C3E66"));
        }

        TextView appearanceTitle = findViewById(R.id.appearanceTitle);
        if (appearanceTitle != null) {
            appearanceTitle.setTextColor(isDarkMode ? android.graphics.Color.parseColor("#FFFFFF") : android.graphics.Color.parseColor("#2C3E66"));
        }

        TextView darkLightText = findViewById(R.id.darkLightText);
        if (darkLightText != null) {
            darkLightText.setTextColor(isDarkMode ? android.graphics.Color.parseColor("#CCCCCC") : android.graphics.Color.parseColor("#49607C"));
        }

        // Night mode is a tri-state toggle group whose colours come from theme
        // attributes, so nothing needs recolouring here.

        updateBottomNavigation();
    }

    private void updateTextViewColorsInLayout(LinearLayout layout) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                String text = textView.getText().toString();
                if (!text.equals("›")) {
                    if (isDarkMode) {
                        if (textView.getTextSize() > 16 && textView.getText().toString().matches(".*[A-Z].*")) {
                            textView.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                        } else {
                            textView.setTextColor(android.graphics.Color.parseColor("#CCCCCC"));
                        }
                    } else {
                        if (textView.getTextSize() > 16 && textView.getText().toString().matches(".*[A-Z].*")) {
                            textView.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
                        } else {
                            textView.setTextColor(android.graphics.Color.parseColor("#49607C"));
                        }
                    }
                }
            } else if (child instanceof LinearLayout) {
                updateTextViewColorsInLayout((LinearLayout) child);
            }
        }
    }

    private void updateSettingsMenuItemWithForce(LinearLayout menuItem, int stringResId, String arrow) {
        if (menuItem == null) return;

        TextView labelView = null;
        TextView arrowView = null;

        // Find the TextViews in the menu item
        if (menuItem.getChildCount() > 0 && menuItem.getChildAt(0) instanceof TextView) {
            labelView = (TextView) menuItem.getChildAt(0);
        }
        if (menuItem.getChildCount() > 1 && menuItem.getChildAt(1) instanceof TextView) {
            arrowView = (TextView) menuItem.getChildAt(1);
        }

        // FORCE set the text - don't check if it's empty
        if (labelView != null) {
            labelView.setText(stringResId);  // Set using resource ID directly
        }

        if (arrowView != null && arrowView.getText().toString().isEmpty()) {
            arrowView.setText(arrow);
        }

        // Apply colors
        if (isDarkMode) {
            if (labelView != null) {
                if (stringResId == R.string.logout) {
                    labelView.setTextColor(android.graphics.Color.parseColor("#FF6B6B"));
                } else {
                    labelView.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                }
            }
            if (arrowView != null) {
                if (stringResId == R.string.logout) {
                    arrowView.setTextColor(android.graphics.Color.parseColor("#FF6B6B"));
                } else {
                    arrowView.setTextColor(android.graphics.Color.parseColor("#9C27B0"));
                }
            }
        } else {
            if (labelView != null) {
                if (stringResId == R.string.logout) {
                    labelView.setTextColor(android.graphics.Color.parseColor("#E94560"));
                } else {
                    labelView.setTextColor(android.graphics.Color.parseColor("#49607C"));
                }
            }
            if (arrowView != null) {
                if (stringResId == R.string.logout) {
                    arrowView.setTextColor(android.graphics.Color.parseColor("#E94560"));
                } else {
                    arrowView.setTextColor(android.graphics.Color.parseColor("#9C27B0"));
                }
            }
        }
    }

    private void updateBottomNavigation() {
        LinearLayout footerNav = null;
        if (mainLayout.getChildCount() > 2) {
            footerNav = (LinearLayout) mainLayout.getChildAt(2);
        }

        if (footerNav != null) {
            if (isDarkMode) {
                GradientDrawable blackFooterBg = new GradientDrawable();
                blackFooterBg.setShape(GradientDrawable.RECTANGLE);
                blackFooterBg.setColor(android.graphics.Color.parseColor("#000000"));
                footerNav.setBackground(blackFooterBg);
            } else {
                footerNav.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"));
            }
        }

        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navCalendar = findViewById(R.id.navCalendar);
        LinearLayout navShop = findViewById(R.id.navShop);
        LinearLayout navThemes = findViewById(R.id.navThemes);
        LinearLayout navProfile = findViewById(R.id.navProfile);

        updateSingleNavItem(navHome, getString(R.string.nav_home), "🏠");
        updateSingleNavItem(navCalendar, getString(R.string.nav_calendar), "📅");
        updateSingleNavItem(navShop, getString(R.string.nav_shop), "🛒");
        updateSingleNavItem(navThemes, getString(R.string.nav_themes), "🎨");
        updateSingleNavItem(navProfile, getString(R.string.profile), "👤", true);
    }

    private void updateSingleNavItem(LinearLayout navItem, String label, String icon) {
        updateSingleNavItem(navItem, label, icon, false);
    }

    private void updateSingleNavItem(LinearLayout navItem, String label, String icon, boolean isCurrentPage) {
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
            if (textView != null && textView.getText().toString().isEmpty()) {
                textView.setText(label);
            }
        }

        if (isDarkMode) {
            if (iconView != null) {
                iconView.setTextColor(isCurrentPage ? android.graphics.Color.parseColor("#9C27B0") : android.graphics.Color.parseColor("#FFFFFF"));
            }
            if (textView != null) {
                textView.setTextColor(isCurrentPage ? android.graphics.Color.parseColor("#9C27B0") : android.graphics.Color.parseColor("#FFFFFF"));
            }
        } else {
            int grayColor = android.graphics.Color.parseColor("#7F8C8D");
            if (iconView != null) {
                iconView.setTextColor(isCurrentPage ? android.graphics.Color.parseColor("#9C27B0") : grayColor);
            }
            if (textView != null) {
                textView.setTextColor(isCurrentPage ? android.graphics.Color.parseColor("#9C27B0") : grayColor);
            }
        }
    }

    private void loadUserData() {
        if (isSwitchingAccount) return;

        String name = sharedPreferences.getString("user_name", getString(R.string.guest));
        String accountType = sharedPreferences.getString("account_type", "Local");
        String joinedDate = sharedPreferences.getString("member_since", getCurrentDate());

        userName.setText(name);
        updateAvatarInitial(name);

        if (accountType.equals("Guest")) {
            userType.setText(R.string.guest_account);
        } else {
            userType.setText(R.string.local_account);
        }

        memberSince.setText(joinedDate);

        if (!sharedPreferences.contains("member_since")) {
            editor.putString("member_since", getCurrentDate());
            editor.apply();
        }

        // FIX: Get fresh dark mode value
        isDarkMode = ThemeManager.isDarkMode(this);
    }
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void updateStats() {
        android.util.Log.d("ProfileActivity", "updateStats called");
        int taskCount = sharedPreferences.getInt("task_count", 0);
        int completedCount = 0;
        for (int i = 0; i < taskCount; i++) {
            boolean status = sharedPreferences.getBoolean("task_status_" + i, false);
            if (status) {
                completedCount++;
            }
        }
        tasksCompleted.setText(String.valueOf(completedCount));

        int coins = sharedPreferences.getInt("user_coins", 0);
        totalCoins.setText(String.valueOf(coins));
    }

    /**
     * Wires the System / Light / Dark choice.
     *
     * <p>Delegates to AppCompatDelegate instead of storing a boolean, so "follow
     * system" behaves correctly and every Activity is recreated when it changes.</p>
     */
    private void setupDarkModeSwitch() {
        if (nightModeGroup == null) {
            return;
        }

        int current = ThemeManager.getNightMode(this);
        int checkedId = R.id.nightModeSystem;
        if (current == ThemeManager.NIGHT_MODE_LIGHT) {
            checkedId = R.id.nightModeLight;
        } else if (current == ThemeManager.NIGHT_MODE_DARK) {
            checkedId = R.id.nightModeDark;
        }
        nightModeGroup.check(checkedId);

        nightModeGroup.addOnButtonCheckedListener((group, id, isChecked) -> {
            if (!isChecked) {
                return;
            }
            int mode;
            if (id == R.id.nightModeLight) {
                mode = ThemeManager.NIGHT_MODE_LIGHT;
            } else if (id == R.id.nightModeDark) {
                mode = ThemeManager.NIGHT_MODE_DARK;
            } else {
                mode = ThemeManager.NIGHT_MODE_FOLLOW_SYSTEM;
            }

            ThemeManager.saveNightMode(this, mode);
            Toast.makeText(this, R.string.night_mode_changed, Toast.LENGTH_SHORT).show();
        });
    }


    private void applyTheme() {
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

    private void showEditNameDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_name, null);

        EditText editNameInput = dialogView.findViewById(R.id.editNameInput);
        TextView cancelButton = dialogView.findViewById(R.id.cancelButton);
        TextView saveButton = dialogView.findViewById(R.id.saveButton);
        LinearLayout dialogLayout = (LinearLayout) dialogView;
        TextView dialogTitle = (TextView) dialogLayout.getChildAt(0);

        editNameInput.setText(userName.getText().toString());

        // Set text from string resources
        dialogTitle.setText(R.string.edit_name);
        cancelButton.setText(R.string.cancel);
        saveButton.setText(R.string.save);
        editNameInput.setHint(R.string.enter_new_name);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
            int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
            int accentColor = gradientColors[0];

            if (isDarkMode) {
                GradientDrawable darkDialogBg = new GradientDrawable();
                darkDialogBg.setShape(GradientDrawable.RECTANGLE);
                darkDialogBg.setCornerRadius(25);
                darkDialogBg.setColor(android.graphics.Color.parseColor("#2D2D2D"));
                dialogView.setBackground(darkDialogBg);

                dialogTitle.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));

                editNameInput.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                editNameInput.setHintTextColor(android.graphics.Color.parseColor("#A0A0A0"));
                GradientDrawable darkInputBg = new GradientDrawable();
                darkInputBg.setShape(GradientDrawable.RECTANGLE);
                darkInputBg.setCornerRadius(12);
                darkInputBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
                editNameInput.setBackground(darkInputBg);
            } else {
                GradientDrawable lightDialogBg = new GradientDrawable();
                lightDialogBg.setShape(GradientDrawable.RECTANGLE);
                lightDialogBg.setCornerRadius(25);
                lightDialogBg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                dialogView.setBackground(lightDialogBg);

                dialogTitle.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
                editNameInput.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
                editNameInput.setHintTextColor(android.graphics.Color.parseColor("#95A5A6"));
                editNameInput.setBackgroundResource(R.drawable.input_field_bg);
            }

            if (saveButton != null) {
                GradientDrawable buttonGradient = new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        gradientColors
                );
                buttonGradient.setCornerRadius(22);
                saveButton.setBackground(buttonGradient);
                saveButton.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            }
            if (cancelButton != null) cancelButton.setTextColor(accentColor);
        }

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        saveButton.setOnClickListener(v -> {
            String newName = editNameInput.getText().toString().trim();
            if (!newName.isEmpty()) {
                editor.putString("user_name", newName);
                editor.apply();
                userName.setText(newName);
                // Updated toast with string resource
                Toast.makeText(this, String.format(getString(R.string.name_updated), newName), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                // Updated toast with string resource
                Toast.makeText(this, R.string.name_cannot_be_empty, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showLanguageDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_language, null);

        LinearLayout englishOption = dialogView.findViewById(R.id.englishOption);
        LinearLayout chineseOption = dialogView.findViewById(R.id.chineseOption);
        TextView cancelButton = dialogView.findViewById(R.id.cancelButton);
        TextView titleText = dialogView.findViewById(R.id.titleText);

        // Find the text views inside the options
        TextView englishText = dialogView.findViewById(R.id.englishText);
        TextView chineseText = dialogView.findViewById(R.id.chineseText);
        TextView englishArrow = dialogView.findViewById(R.id.englishArrow);
        TextView chineseArrow = dialogView.findViewById(R.id.chineseArrow);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Apply gradient to cancel button
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        int accentColor = gradientColors[0];

        GradientDrawable buttonGradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                gradientColors
        );
        buttonGradient.setCornerRadius(22 * getResources().getDisplayMetrics().density);
        cancelButton.setBackground(buttonGradient);

        // Apply dark mode styling
        if (isDarkMode) {
            // Set dialog background to dark
            if (dialog.getWindow() != null) {
                GradientDrawable darkDialogBg = new GradientDrawable();
                darkDialogBg.setShape(GradientDrawable.RECTANGLE);
                darkDialogBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
                darkDialogBg.setColor(android.graphics.Color.parseColor("#1E1E1E")); // Dark background
                dialog.getWindow().setBackgroundDrawable(darkDialogBg);
            }

            // Title text - WHITE
            if (titleText != null) {
                titleText.setTextColor(android.graphics.Color.WHITE);
            }

            // English and Chinese text - WHITE
            if (englishText != null) {
                englishText.setTextColor(android.graphics.Color.WHITE);
            }
            if (chineseText != null) {
                chineseText.setTextColor(android.graphics.Color.WHITE);
            }

            // Arrow icons - ACCENT color (purple)
            if (englishArrow != null) {
                englishArrow.setTextColor(accentColor);
            }
            if (chineseArrow != null) {
                chineseArrow.setTextColor(accentColor);
            }

            // Cancel button text - WHITE (button background already has gradient)
            cancelButton.setTextColor(android.graphics.Color.WHITE);

            // Update option backgrounds for dark mode
            GradientDrawable darkOptionBg = new GradientDrawable();
            darkOptionBg.setShape(GradientDrawable.RECTANGLE);
            darkOptionBg.setCornerRadius(12 * getResources().getDisplayMetrics().density);
            darkOptionBg.setColor(android.graphics.Color.parseColor("#2D2D2D"));
            englishOption.setBackground(darkOptionBg);
            chineseOption.setBackground(darkOptionBg);

        } else {
            // Light mode - restore default colors
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
            }

            if (titleText != null) {
                titleText.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
            }

            if (englishText != null) {
                englishText.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
            }
            if (chineseText != null) {
                chineseText.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
            }

            // Restore option backgrounds for light mode
            englishOption.setBackgroundResource(R.drawable.language_option_bg);
            chineseOption.setBackgroundResource(R.drawable.language_option_bg);
        }

        englishOption.setOnClickListener(v -> {
            changeLanguage(LanguageManager.LANGUAGE_ENGLISH);
            dialog.dismiss();
        });

        chineseOption.setOnClickListener(v -> {
            changeLanguage(LanguageManager.LANGUAGE_CHINESE);
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
    }

    private void applyDialogTheme(View dialogView, AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            boolean isDarkMode = ThemeManager.isDarkMode(this);

            if (isDarkMode) {
                GradientDrawable darkDialogBg = new GradientDrawable();
                darkDialogBg.setShape(GradientDrawable.RECTANGLE);
                darkDialogBg.setCornerRadius(25);
                darkDialogBg.setColor(android.graphics.Color.parseColor("#2D2D2D"));
                dialog.getWindow().setBackgroundDrawable(darkDialogBg);

                TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
                if (dialogTitle != null) dialogTitle.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));

                // Set text colors for options
                TextView englishText = dialogView.findViewById(R.id.englishOption).findViewById(android.R.id.text1);
                TextView chineseText = dialogView.findViewById(R.id.chineseOption).findViewById(android.R.id.text1);
            }
        }
    }

    private void changeLanguage(String languageCode) {
        SharedPreferences prefs = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        prefs.edit().putString("app_language", languageCode).apply();
        LanguageManager.setLocale(ProfileActivity.this, languageCode);

        if (languageCode.equals("en")) {
            Toast.makeText(this, "Language changed to English", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "语言已切换为中文", Toast.LENGTH_SHORT).show();
        }

        recreate();
    }




    private void updateUILanguage() {
        // Update profile title
        if (profileTitle != null) {
            profileTitle.setText(R.string.profile);
        }

        // Update navigation texts (calls BaseActivity method)
        updateNavigationTexts();
    }

    private void showSwitchAccountDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_switch_account, null);

        TextView titleText = dialogView.findViewById(R.id.titleText);
        LinearLayout guestAccountLayout = dialogView.findViewById(R.id.guestAccount);
        LinearLayout localAccountLayout = dialogView.findViewById(R.id.localAccount);
        LinearLayout googleAccountLayout = dialogView.findViewById(R.id.googleAccount);
        TextView cancelButton = dialogView.findViewById(R.id.cancelButton);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
            int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
            int accentColor = gradientColors[0];

            // Apply gradient to cancel button
            if (cancelButton != null) {
                GradientDrawable buttonGradient = new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        gradientColors
                );
                buttonGradient.setCornerRadius(22);
                cancelButton.setBackground(buttonGradient);
                cancelButton.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            }

            if (isDarkMode) {
                // Dark mode background
                GradientDrawable darkDialogBg = new GradientDrawable();
                darkDialogBg.setShape(GradientDrawable.RECTANGLE);
                darkDialogBg.setCornerRadius(25);
                darkDialogBg.setColor(android.graphics.Color.parseColor("#2D2D2D"));
                dialog.getWindow().setBackgroundDrawable(darkDialogBg);

                // Title text color - WHITE
                if (titleText != null) {
                    titleText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                }

                // Option backgrounds - dark gray
                GradientDrawable darkOptionBg = new GradientDrawable();
                darkOptionBg.setShape(GradientDrawable.RECTANGLE);
                darkOptionBg.setCornerRadius(12);
                darkOptionBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
                guestAccountLayout.setBackground(darkOptionBg);
                localAccountLayout.setBackground(darkOptionBg);
                googleAccountLayout.setBackground(darkOptionBg);

                // Get text views and set colors (now at index 0 since no icon)
                TextView guestText = (TextView) guestAccountLayout.getChildAt(0);
                TextView localText = (TextView) localAccountLayout.getChildAt(0);
                TextView googleText = (TextView) googleAccountLayout.getChildAt(0);

                TextView guestArrow = (TextView) guestAccountLayout.getChildAt(1);
                TextView localArrow = (TextView) localAccountLayout.getChildAt(1);
                TextView googleArrow = (TextView) googleAccountLayout.getChildAt(1);

                if (guestText != null) guestText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                if (localText != null) localText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                if (googleText != null) googleText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));

                if (guestArrow != null) guestArrow.setTextColor(accentColor);
                if (localArrow != null) localArrow.setTextColor(accentColor);
                if (googleArrow != null) googleArrow.setTextColor(accentColor);

            } else {
                // Light mode background
                GradientDrawable lightDialogBg = new GradientDrawable();
                lightDialogBg.setShape(GradientDrawable.RECTANGLE);
                lightDialogBg.setCornerRadius(25);
                lightDialogBg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                dialog.getWindow().setBackgroundDrawable(lightDialogBg);

                // Option backgrounds - light gray
                GradientDrawable lightOptionBg = new GradientDrawable();
                lightOptionBg.setShape(GradientDrawable.RECTANGLE);
                lightOptionBg.setCornerRadius(12);
                lightOptionBg.setColor(android.graphics.Color.parseColor("#F5F5F5"));
                guestAccountLayout.setBackground(lightOptionBg);
                localAccountLayout.setBackground(lightOptionBg);
                googleAccountLayout.setBackground(lightOptionBg);

                // Get text views and set colors (now at index 0 since no icon)
                TextView guestText = (TextView) guestAccountLayout.getChildAt(0);
                TextView localText = (TextView) localAccountLayout.getChildAt(0);
                TextView googleText = (TextView) googleAccountLayout.getChildAt(0);

                TextView guestArrow = (TextView) guestAccountLayout.getChildAt(1);
                TextView localArrow = (TextView) localAccountLayout.getChildAt(1);
                TextView googleArrow = (TextView) googleAccountLayout.getChildAt(1);

                if (guestText != null) guestText.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
                if (localText != null) localText.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
                if (googleText != null) googleText.setTextColor(android.graphics.Color.parseColor("#2C3E66"));

                if (guestArrow != null) guestArrow.setTextColor(accentColor);
                if (localArrow != null) localArrow.setTextColor(accentColor);
                if (googleArrow != null) googleArrow.setTextColor(accentColor);
            }
        }

        // Set click listeners
        guestAccountLayout.setOnClickListener(v -> {
            editor.putString("account_type", "Guest");
            editor.putString("user_name", getString(R.string.guest));
            editor.putString("user_email", "");
            editor.apply();
            userName.setText(getString(R.string.guest));
            userType.setText(R.string.guest_account);
            Toast.makeText(this, R.string.switched_to_guest, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        localAccountLayout.setOnClickListener(v -> {
            editor.putString("account_type", "Local");
            editor.putString("user_name", getString(R.string.guest));
            editor.putString("user_email", "user@example.com");
            editor.apply();
            userName.setText(getString(R.string.guest));
            userType.setText(R.string.local_account);
            Toast.makeText(this, R.string.switched_to_local, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        googleAccountLayout.setOnClickListener(v -> {
            Toast.makeText(this, R.string.google_coming_soon, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
    }

    private void showManageAccountDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_manage_account, null);

        TextView titleText = dialogView.findViewById(R.id.titleText);
        LinearLayout changePasswordLayout = dialogView.findViewById(R.id.changePassword);
        LinearLayout updateEmailLayout = dialogView.findViewById(R.id.updateEmail);
        LinearLayout deleteAccountLayout = dialogView.findViewById(R.id.deleteAccount);
        TextView cancelButton = dialogView.findViewById(R.id.cancelButton);

        // Get the text views from inside the LinearLayouts
        TextView changePasswordText = null;
        TextView updateEmailText = null;
        TextView deleteAccountText = null;
        TextView changePasswordArrow = null;
        TextView updateEmailArrow = null;
        TextView deleteAccountArrow = null;

        // Get Change Password text and arrow (index 0=icon, 1=text, 2=arrow)
        if (changePasswordLayout != null && changePasswordLayout.getChildCount() > 1) {
            changePasswordText = (TextView) changePasswordLayout.getChildAt(1);
        }
        if (changePasswordLayout != null && changePasswordLayout.getChildCount() > 2) {
            changePasswordArrow = (TextView) changePasswordLayout.getChildAt(2);
        }

        // Get Update Email text and arrow
        if (updateEmailLayout != null && updateEmailLayout.getChildCount() > 1) {
            updateEmailText = (TextView) updateEmailLayout.getChildAt(1);
        }
        if (updateEmailLayout != null && updateEmailLayout.getChildCount() > 2) {
            updateEmailArrow = (TextView) updateEmailLayout.getChildAt(2);
        }

        // Get Delete Account text and arrow
        if (deleteAccountLayout != null && deleteAccountLayout.getChildCount() > 1) {
            deleteAccountText = (TextView) deleteAccountLayout.getChildAt(1);
        }
        if (deleteAccountLayout != null && deleteAccountLayout.getChildCount() > 2) {
            deleteAccountArrow = (TextView) deleteAccountLayout.getChildAt(2);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
            int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
            int accentColor = gradientColors[0];

            // Apply gradient to cancel button
            if (cancelButton != null) {
                GradientDrawable buttonGradient = new GradientDrawable(
                        GradientDrawable.Orientation.LEFT_RIGHT,
                        gradientColors
                );
                buttonGradient.setCornerRadius(22);
                cancelButton.setBackground(buttonGradient);
                cancelButton.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            }

            if (isDarkMode) {
                // Dark mode background
                GradientDrawable darkDialogBg = new GradientDrawable();
                darkDialogBg.setShape(GradientDrawable.RECTANGLE);
                darkDialogBg.setCornerRadius(25);
                darkDialogBg.setColor(android.graphics.Color.parseColor("#2D2D2D"));
                dialog.getWindow().setBackgroundDrawable(darkDialogBg);

                // Title text color - WHITE
                if (titleText != null) {
                    titleText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                }

                // Option backgrounds - dark gray
                GradientDrawable darkOptionBg = new GradientDrawable();
                darkOptionBg.setShape(GradientDrawable.RECTANGLE);
                darkOptionBg.setCornerRadius(12);
                darkOptionBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
                changePasswordLayout.setBackground(darkOptionBg);
                updateEmailLayout.setBackground(darkOptionBg);
                deleteAccountLayout.setBackground(darkOptionBg);

                // Change Password text - WHITE
                if (changePasswordText != null) {
                    changePasswordText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                }
                if (changePasswordArrow != null) {
                    changePasswordArrow.setTextColor(accentColor);
                }

                // Update Email text - WHITE
                if (updateEmailText != null) {
                    updateEmailText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                }
                if (updateEmailArrow != null) {
                    updateEmailArrow.setTextColor(accentColor);
                }

                // Delete Account text - RED
                if (deleteAccountText != null) {
                    deleteAccountText.setTextColor(android.graphics.Color.parseColor("#FF6B6B"));
                }
                if (deleteAccountArrow != null) {
                    deleteAccountArrow.setTextColor(android.graphics.Color.parseColor("#FF6B6B"));
                }

            } else {
                // Light mode background
                GradientDrawable lightDialogBg = new GradientDrawable();
                lightDialogBg.setShape(GradientDrawable.RECTANGLE);
                lightDialogBg.setCornerRadius(25);
                lightDialogBg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                dialog.getWindow().setBackgroundDrawable(lightDialogBg);

                // Option backgrounds - light gray
                GradientDrawable lightOptionBg = new GradientDrawable();
                lightOptionBg.setShape(GradientDrawable.RECTANGLE);
                lightOptionBg.setCornerRadius(12);
                lightOptionBg.setColor(android.graphics.Color.parseColor("#F5F5F5"));
                changePasswordLayout.setBackground(lightOptionBg);
                updateEmailLayout.setBackground(lightOptionBg);
                deleteAccountLayout.setBackground(lightOptionBg);

                // Change Password text
                if (changePasswordText != null) {
                    changePasswordText.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
                }
                if (changePasswordArrow != null) {
                    changePasswordArrow.setTextColor(accentColor);
                }

                // Update Email text
                if (updateEmailText != null) {
                    updateEmailText.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
                }
                if (updateEmailArrow != null) {
                    updateEmailArrow.setTextColor(accentColor);
                }

                // Delete Account text - RED
                if (deleteAccountText != null) {
                    deleteAccountText.setTextColor(android.graphics.Color.parseColor("#E94560"));
                }
                if (deleteAccountArrow != null) {
                    deleteAccountArrow.setTextColor(android.graphics.Color.parseColor("#E94560"));
                }
            }
        }

        // Set click listeners
        changePasswordLayout.setOnClickListener(v -> {
            dialog.dismiss();
            showChangePasswordDialog();
        });

        updateEmailLayout.setOnClickListener(v -> {
            dialog.dismiss();
            showUpdateEmailDialog();
        });

        deleteAccountLayout.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteAccountConfirmation();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
    }

    // Helper method to make all TextViews white
    private void makeAllTextViewsWhite(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            // Don't change cancel button color (keep it purple)
            if (!textView.getText().toString().equals("Cancel")) {
                textView.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            } else {
                textView.setTextColor(android.graphics.Color.parseColor("#9C27B0"));
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                makeAllTextViewsWhite(viewGroup.getChildAt(i));
            }
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            if (isDarkMode) {
                GradientDrawable darkDialogBg = new GradientDrawable();
                darkDialogBg.setShape(GradientDrawable.RECTANGLE);
                darkDialogBg.setCornerRadius(25);
                darkDialogBg.setColor(android.graphics.Color.parseColor("#2D2D2D"));
                dialog.getWindow().setBackgroundDrawable(darkDialogBg);
            } else {
                GradientDrawable lightDialogBg = new GradientDrawable();
                lightDialogBg.setShape(GradientDrawable.RECTANGLE);
                lightDialogBg.setCornerRadius(25);
                lightDialogBg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                dialog.getWindow().setBackgroundDrawable(lightDialogBg);
            }
        }
    }

    private void showUpdateEmailDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_email, null);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            if (isDarkMode) {
                GradientDrawable darkDialogBg = new GradientDrawable();
                darkDialogBg.setShape(GradientDrawable.RECTANGLE);
                darkDialogBg.setCornerRadius(25);
                darkDialogBg.setColor(android.graphics.Color.parseColor("#2D2D2D"));
                dialog.getWindow().setBackgroundDrawable(darkDialogBg);
            } else {
                GradientDrawable lightDialogBg = new GradientDrawable();
                lightDialogBg.setShape(GradientDrawable.RECTANGLE);
                lightDialogBg.setCornerRadius(25);
                lightDialogBg.setColor(android.graphics.Color.parseColor("#FFFFFF"));
                dialog.getWindow().setBackgroundDrawable(lightDialogBg);
            }
        }
    }

    private void showDeleteAccountConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_account_title)
                .setMessage(R.string.delete_account_message)
                .setPositiveButton(R.string.delete, (dialog2, which2) -> {
                    sharedPreferences.edit().clear().apply();
                    editor.putString("member_since", getCurrentDate());
                    editor.putString("selected_theme", ThemeManager.THEME_DEFAULT);
                    editor.putString("user_name", getString(R.string.guest));
                    editor.apply();
                    loadUserData();
                    updateStats();
                    Toast.makeText(this, R.string.account_reset_to_default, Toast.LENGTH_SHORT).show();
                    recreate();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupClickListeners() {
        // Edit Profile Name
        editProfileName.setOnClickListener(v -> showEditNameDialog());

        // Switch Account
        if (switchAccount != null) {
            switchAccount.setOnClickListener(v -> {
                android.util.Log.d("ProfileActivity", "Switch Account clicked - showing dialog");
                showSwitchAccountDialog();
            });
        } else {
            android.util.Log.e("ProfileActivity", "switchAccount is NULL!");
        }

        // Manage Account
        if (manageAccount != null) {
            manageAccount.setOnClickListener(v -> {
                android.util.Log.d("ProfileActivity", "Manage Account clicked");
                showManageAccountDialog();
            });
        }

        // Theme Preferences
        if (themePreferences != null) {
            themePreferences.setOnClickListener(v -> {
                android.util.Log.d("ProfileActivity", "Theme Preferences clicked");
                Intent intent = new Intent(this, ThemeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        // Language Selection
        if (language != null) {
            language.setOnClickListener(v -> {
                android.util.Log.d("ProfileActivity", "Language clicked - showing dialog");
                showLanguageDialog();
            });
        }

        // Logout - Updated to use custom dialog
        if (logout != null) {
            logout.setOnClickListener(v -> showLogoutDialog());
        }
    }


    @Override
    protected void setupNavigation() {
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
            Intent intent = new Intent(this, ThemeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            Toast.makeText(this, R.string.you_are_on_profile, Toast.LENGTH_SHORT).show();
        });
    }
    private void updateAllTextColors() {
        // Find all TextViews in the main layout and update their colors
        updateTextViewColorsInLayout(mainLayout);

        // Also update the stats cards specifically
        ViewGroup scrollView = (ViewGroup) mainLayout.getChildAt(1);
        if (scrollView != null) {
            ViewGroup scrollContent = (ViewGroup) scrollView.getChildAt(0);
            if (scrollContent instanceof LinearLayout) {
                LinearLayout contentLayout = (LinearLayout) scrollContent;
                for (int i = 0; i < contentLayout.getChildCount(); i++) {
                    View child = contentLayout.getChildAt(i);
                    if (child instanceof CardView) {
                        CardView card = (CardView) child;
                        if (card.getChildAt(0) instanceof LinearLayout) {
                            LinearLayout cardInner = (LinearLayout) card.getChildAt(0);
                            updateTextViewColorsInLayout(cardInner);
                        }
                    }
                }
            }
        }
    }
    private void showLogoutDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_logout, null);

        TextView titleText = dialogView.findViewById(R.id.titleText);
        TextView messageText = dialogView.findViewById(R.id.messageText);
        TextView cancelButton = dialogView.findViewById(R.id.cancelButton);
        TextView logoutButton = dialogView.findViewById(R.id.logoutButton);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Apply gradient to logout button
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);

        GradientDrawable buttonGradient = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                gradientColors
        );
        buttonGradient.setCornerRadius(22 * getResources().getDisplayMetrics().density);
        logoutButton.setBackground(buttonGradient);

        // Apply dark mode styling
        if (isDarkMode) {
            if (dialog.getWindow() != null) {
                GradientDrawable darkDialogBg = new GradientDrawable();
                darkDialogBg.setShape(GradientDrawable.RECTANGLE);
                darkDialogBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
                darkDialogBg.setColor(android.graphics.Color.parseColor("#2D2D2D"));
                dialog.getWindow().setBackgroundDrawable(darkDialogBg);
            }

            titleText.setTextColor(android.graphics.Color.WHITE);
            messageText.setTextColor(android.graphics.Color.parseColor("#CCCCCC"));

            GradientDrawable cancelBg = new GradientDrawable();
            cancelBg.setShape(GradientDrawable.RECTANGLE);
            cancelBg.setCornerRadius(22 * getResources().getDisplayMetrics().density);
            cancelBg.setStroke(2, android.graphics.Color.parseColor("#9C27B0"));
            cancelBg.setColor(android.graphics.Color.TRANSPARENT);
            cancelButton.setBackground(cancelBg);
            cancelButton.setTextColor(android.graphics.Color.parseColor("#9C27B0"));

        } else {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.white);
            }

            titleText.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
            messageText.setTextColor(android.graphics.Color.parseColor("#49607C"));

            GradientDrawable cancelBg = new GradientDrawable();
            cancelBg.setShape(GradientDrawable.RECTANGLE);
            cancelBg.setCornerRadius(22 * getResources().getDisplayMetrics().density);
            cancelBg.setStroke(2, android.graphics.Color.parseColor("#9C27B0"));
            cancelBg.setColor(android.graphics.Color.TRANSPARENT);
            cancelButton.setBackground(cancelBg);
            cancelButton.setTextColor(android.graphics.Color.parseColor("#9C27B0"));
        }

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        logoutButton.setOnClickListener(v -> {
            SharedPreferences.Editor clearEditor = sharedPreferences.edit();
            clearEditor.putBoolean("remember_me", false);
            clearEditor.apply();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
            Toast.makeText(this, R.string.logout_success, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Don't reload if we're in the middle of switching accounts
        if (!isSwitchingAccount) {
            updateStats();
            loadUserData();
            applyTheme();
            applyDarkMode();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyTheme();
        applyDarkMode();
    }

}

