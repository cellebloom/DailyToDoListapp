package com.example.dailytodolist;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class LanguageManager {
    private static final String PREF_NAME = "TodoAppPrefs";
    private static final String KEY_LANGUAGE = "app_language";

    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_CHINESE = "zh";

    public static void setLocale(Context context, String languageCode) {
        android.util.Log.d("LANGUAGE_TEST", "setLocale - Setting language to: " + languageCode);

        Locale locale;
        if (languageCode.equals(LANGUAGE_CHINESE)) {
            locale = Locale.SIMPLIFIED_CHINESE;
        } else {
            locale = Locale.ENGLISH;
        }

        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            config.setLocales(new LocaleList(locale));
        } else {
            config.locale = locale;
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Save to SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();

        android.util.Log.d("LANGUAGE_TEST", "setLocale - New config locale: " + resources.getConfiguration().locale);
    }

    public static void applyLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String languageCode = prefs.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH);
        setLocale(context, languageCode);
    }

    public static String getCurrentLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH);
    }

    public static boolean isChinese(Context context) {
        return getCurrentLanguage(context).equals(LANGUAGE_CHINESE);
    }
}