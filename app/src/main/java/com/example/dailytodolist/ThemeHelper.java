package com.example.dailytodolist;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

public class ThemeHelper {

    // Get current theme accent color
    public static int getCurrentThemeAccentColor(Context context) {
        String currentTheme = ThemeManager.getCurrentTheme(context);
        int[] gradientColors = ThemeManager.getThemeGradientColors(context, currentTheme);
        return gradientColors[0]; // Return the start color as accent
    }

    // Get current theme primary color
    public static int getCurrentThemePrimaryColor(Context context) {
        String currentTheme = ThemeManager.getCurrentTheme(context);
        switch (currentTheme) {
            case ThemeManager.THEME_HOTPINK:
                return ContextCompat.getColor(context, R.color.hotpink_primary);
            case ThemeManager.THEME_LIME:
                return ContextCompat.getColor(context, R.color.lime_primary);
            case ThemeManager.THEME_MINT:
                return ContextCompat.getColor(context, R.color.mint_primary);
            case ThemeManager.THEME_SUNSET:
                return ContextCompat.getColor(context, R.color.sunset_primary);
            case ThemeManager.THEME_CYAN_HOTPINK:
                return ContextCompat.getColor(context, R.color.cyan_hotpink_start);
            case ThemeManager.THEME_CYAN_INDIGO:
                return ContextCompat.getColor(context, R.color.cyan_indigo_start);
            case ThemeManager.THEME_YELLOW_RED:
                return ContextCompat.getColor(context, R.color.yellow_red_start);
            case ThemeManager.THEME_CREAM_ROSE:
                return ContextCompat.getColor(context, R.color.cream_rose_start);
            case ThemeManager.THEME_YELLOW_CYAN:
                return ContextCompat.getColor(context, R.color.yellow_cyan_start);
            case ThemeManager.THEME_MAGENTA_BLUE:
                return ContextCompat.getColor(context, R.color.magenta_blue_start);
            default:
                return ContextCompat.getColor(context, R.color.default_primary);
        }
    }

    // Get current theme secondary color
    public static int getCurrentThemeSecondaryColor(Context context) {
        String currentTheme = ThemeManager.getCurrentTheme(context);
        switch (currentTheme) {
            case ThemeManager.THEME_HOTPINK:
                return ContextCompat.getColor(context, R.color.hotpink_secondary);
            case ThemeManager.THEME_LIME:
                return ContextCompat.getColor(context, R.color.lime_secondary);
            case ThemeManager.THEME_MINT:
                return ContextCompat.getColor(context, R.color.mint_secondary);
            case ThemeManager.THEME_SUNSET:
                return ContextCompat.getColor(context, R.color.sunset_secondary);
            default:
                return ContextCompat.getColor(context, R.color.default_secondary);
        }
    }

    // Apply theme to a TextView
    public static void styleTextView(TextView textView, Context context) {
        if (textView != null) {
            int accentColor = getCurrentThemeAccentColor(context);
            textView.setTextColor(accentColor);
        }
    }
}