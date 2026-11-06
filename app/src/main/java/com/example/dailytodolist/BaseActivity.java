package com.example.dailytodolist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class BaseActivity extends AppCompatActivity {

    protected LinearLayout mainLayout;
    protected SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }

    private Context updateBaseContextLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        String languageCode = prefs.getString("app_language", "en");

        Locale locale;
        if (languageCode.equals("zh")) {
            locale = new Locale("zh", "CN");
        } else {
            locale = new Locale("en", "US");
        }

        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply language FIRST - before anything else
        applyLanguage();

        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);

        preferenceListener = (sharedPreferences, key) -> {
            if ("selected_theme".equals(key)) {
                recreate();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
    }

    private void applyLanguage() {
        SharedPreferences prefs = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        String languageCode = prefs.getString("app_language", "en");

        Locale locale;
        if (languageCode.equals("zh")) {
            locale = new Locale("zh", "CN");
        } else {
            locale = new Locale("en", "US");
        }

        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    protected void applyGradientBackground() {
        if (mainLayout == null) {
            mainLayout = findViewById(R.id.mainLayout);
        }
        if (mainLayout != null) {
            String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
            int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
            GradientDrawable gradientDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    gradientColors
            );
            mainLayout.setBackground(gradientDrawable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (preferenceListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        }
    }

    protected void setupNavigation() {
        if (findViewById(R.id.navHome) != null) {
            findViewById(R.id.navHome).setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        if (findViewById(R.id.navCalendar) != null) {
            findViewById(R.id.navCalendar).setOnClickListener(v -> {
                Intent intent = new Intent(this, CalendarActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        if (findViewById(R.id.navShop) != null) {
            findViewById(R.id.navShop).setOnClickListener(v -> {
                Intent intent = new Intent(this, ShopActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        if (findViewById(R.id.navThemes) != null) {
            findViewById(R.id.navThemes).setOnClickListener(v -> {
                Intent intent = new Intent(this, ThemeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }

        if (findViewById(R.id.navProfile) != null) {
            findViewById(R.id.navProfile).setOnClickListener(v -> {
                if (this instanceof ProfileActivity) {
                    Toast.makeText(this, R.string.you_are_on_profile, Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(this, ProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            });
        }
    }

    protected void updateNavigationTexts() {
        TextView homeText = findViewById(R.id.navHomeText);
        if (homeText != null) homeText.setText(R.string.nav_home);

        TextView calendarText = findViewById(R.id.navCalendarText);
        if (calendarText != null) calendarText.setText(R.string.nav_calendar);

        TextView shopText = findViewById(R.id.navShopText);
        if (shopText != null) shopText.setText(R.string.nav_shop);

        TextView themesText = findViewById(R.id.navThemesText);
        if (themesText != null) themesText.setText(R.string.nav_themes);

        TextView profileText = findViewById(R.id.navProfileText);
        if (profileText != null) profileText.setText(R.string.profile);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyGradientBackground();
        updateNavigationTexts();
    }
}