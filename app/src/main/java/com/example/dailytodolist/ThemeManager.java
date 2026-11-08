package com.example.dailytodolist;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

public class ThemeManager {

    public enum Theme {
        DEFAULT, HOTPINK, LIME, MINT, SUNSET
    }

    // Theme identifiers
    public static final String THEME_DEFAULT = "default";
    public static final String THEME_HOTPINK = "hotpink";
    public static final String THEME_LIME = "lime";
    public static final String THEME_MINT = "mint";
    public static final String THEME_SUNSET = "sunset";
    public static final String THEME_CYAN_HOTPINK = "cyan_hotpink";
    public static final String THEME_CYAN_INDIGO = "cyan_indigo";
    public static final String THEME_YELLOW_RED = "yellow_red";
    public static final String THEME_CREAM_ROSE = "cream_rose";
    public static final String THEME_YELLOW_CYAN = "yellow_cyan";
    public static final String THEME_MAGENTA_BLUE = "magenta_blue";

    private static final String PREF_NAME = "TodoAppPrefs";
    private static final String KEY_THEME = "selected_theme";

    /**
     * Night-mode preference: FOLLOW_SYSTEM, MODE_NIGHT_NO or MODE_NIGHT_YES.
     *
     * <p>Replaces the old {@code dark_mode} boolean. A boolean could only express
     * two states and could never defer to the device, which is exactly why the
     * app ignored the system setting.</p>
     */
    public static final String KEY_NIGHT_MODE = "night_mode";
    public static final int NIGHT_MODE_FOLLOW_SYSTEM =
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final int NIGHT_MODE_LIGHT =
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
    public static final int NIGHT_MODE_DARK =
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

    /** Stored tri-state night mode. Defaults to following the device. */
    public static int getNightMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_NIGHT_MODE, NIGHT_MODE_FOLLOW_SYSTEM);
    }

    /** Applies a night mode through AppCompat so every screen picks it up. */
    public static void setNightMode(int mode) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
    }

    /** Persists and applies a night-mode choice. */
    public static void saveNightMode(Context context, int mode) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_NIGHT_MODE, mode)
                .apply();
        setNightMode(mode);
    }

    /** True when the current configuration resolves to the night palette. */
    public static boolean isNightModeActive(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * True when dark mode is currently in effect.
     *
     * <p>Reads the resolved configuration rather than a stored flag, so it honours
     * all three states including "follow system".</p>
     */
    public static boolean isDarkMode(Context context) {
        return isNightModeActive(context);
    }

    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String themeId = prefs.getString(KEY_THEME, THEME_DEFAULT);

        // Which palette (light or dark) now comes from the Android night-mode
        // configuration, which AppCompatDelegate drives from the user's choice or
        // the system setting. Reading the configuration instead of a stored
        // boolean is what makes "follow system" actually work.
        if (isNightModeActive(context)) {
            context.setTheme(getDarkStyleFor(themeId));
        } else {
            context.setTheme(getLightStyleFor(themeId));
        }
    }

    /** Light style for a theme id. */
    private static int getLightStyleFor(String themeId) {
        switch (themeId) {
            case THEME_HOTPINK:
                return R.style.Theme_DailyToDoList_Hotpink_Light;
            case THEME_LIME:
                return R.style.Theme_DailyToDoList_Lime_Light;
            case THEME_MINT:
                return R.style.Theme_DailyToDoList_Mint_Light;
            case THEME_SUNSET:
                return R.style.Theme_DailyToDoList_Sunset_Light;
            default:
                return R.style.Theme_DailyToDoList_Default_Light;
        }
    }

    /** Dark style for a theme id. */
    private static int getDarkStyleFor(String themeId) {
        switch (themeId) {
            case THEME_HOTPINK:
                return R.style.Theme_DailyToDoList_Hotpink_Dark;
            case THEME_LIME:
                return R.style.Theme_DailyToDoList_Lime_Dark;
            case THEME_MINT:
                return R.style.Theme_DailyToDoList_Mint_Dark;
            case THEME_SUNSET:
                return R.style.Theme_DailyToDoList_Sunset_Dark;
            default:
                return R.style.Theme_DailyToDoList_Default_Dark;
        }
    }

    public static void saveTheme(Context context, Theme theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String themeId;

        switch (theme) {
            case HOTPINK:
                themeId = THEME_HOTPINK;
                break;
            case LIME:
                themeId = THEME_LIME;
                break;
            case MINT:
                themeId = THEME_MINT;
                break;
            case SUNSET:
                themeId = THEME_SUNSET;
                break;
            default:
                themeId = THEME_DEFAULT;
                break;
        }

        prefs.edit().putString(KEY_THEME, themeId).apply();
    }

    public static String getCurrentTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_THEME, THEME_DEFAULT);
    }

    public static String getThemeId(Context context) {
        return getCurrentTheme(context);
    }

    public static void setThemeId(Context context, String themeId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, themeId).apply();

        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).recreate();
        }
    }

    public static int[] getThemeGradientColors(Context context, String themeId) {
        switch (themeId) {
            case THEME_HOTPINK:
                return new int[]{
                        ContextCompat.getColor(context, R.color.hotpink_start),
                        ContextCompat.getColor(context, R.color.hotpink_end)
                };
            case THEME_LIME:
                return new int[]{
                        ContextCompat.getColor(context, R.color.lime_start),
                        ContextCompat.getColor(context, R.color.lime_end)
                };
            case THEME_MINT:
                return new int[]{
                        ContextCompat.getColor(context, R.color.mint_start),
                        ContextCompat.getColor(context, R.color.mint_end)
                };
            case THEME_SUNSET:
                return new int[]{
                        ContextCompat.getColor(context, R.color.sunset_start),
                        ContextCompat.getColor(context, R.color.sunset_end)
                };
            case THEME_CYAN_HOTPINK:
                return new int[]{
                        ContextCompat.getColor(context, R.color.cyan_hotpink_start),
                        ContextCompat.getColor(context, R.color.cyan_hotpink_end)
                };
            case THEME_CYAN_INDIGO:
                return new int[]{
                        ContextCompat.getColor(context, R.color.cyan_indigo_start),
                        ContextCompat.getColor(context, R.color.cyan_indigo_end)
                };
            case THEME_YELLOW_RED:
                return new int[]{
                        ContextCompat.getColor(context, R.color.yellow_red_start),
                        ContextCompat.getColor(context, R.color.yellow_red_end)
                };
            case THEME_CREAM_ROSE:
                return new int[]{
                        ContextCompat.getColor(context, R.color.cream_rose_start),
                        ContextCompat.getColor(context, R.color.cream_rose_end)
                };
            case THEME_YELLOW_CYAN:
                return new int[]{
                        ContextCompat.getColor(context, R.color.yellow_cyan_start),
                        ContextCompat.getColor(context, R.color.yellow_cyan_end)
                };
            case THEME_MAGENTA_BLUE:
                return new int[]{
                        ContextCompat.getColor(context, R.color.magenta_blue_start),
                        ContextCompat.getColor(context, R.color.magenta_blue_end)
                };
            default:
                return new int[]{
                        ContextCompat.getColor(context, R.color.default_start),
                        ContextCompat.getColor(context, R.color.default_end)
                };
        }
    }
}