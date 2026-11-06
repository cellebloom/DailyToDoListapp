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

    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String themeId = prefs.getString(KEY_THEME, THEME_DEFAULT);

        // Apply theme based on the themeId
        switch (themeId) {
            case THEME_HOTPINK:
                context.setTheme(R.style.Theme_DailyToDoList_Hotpink_Light);
                break;
            case THEME_LIME:
                context.setTheme(R.style.Theme_DailyToDoList_Lime_Light);
                break;
            case THEME_MINT:
                context.setTheme(R.style.Theme_DailyToDoList_Mint_Light);
                break;
            case THEME_SUNSET:
                context.setTheme(R.style.Theme_DailyToDoList_Sunset_Light);
                break;
            case THEME_CYAN_HOTPINK:
            case THEME_CYAN_INDIGO:
            case THEME_YELLOW_RED:
            case THEME_CREAM_ROSE:
            case THEME_YELLOW_CYAN:
            case THEME_MAGENTA_BLUE:
                context.setTheme(R.style.Theme_DailyToDoList_Default_Light);
                break;
            default:
                context.setTheme(R.style.Theme_DailyToDoList_Default_Light);
                break;
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