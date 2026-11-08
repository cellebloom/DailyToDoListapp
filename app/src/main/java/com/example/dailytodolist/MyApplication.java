package com.example.dailytodolist;

import android.app.Application;
import android.content.SharedPreferences;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Apply the saved night mode before any Activity inflates, so the very
        // first screen already uses the right palette instead of flashing light
        // and then switching.
        ThemeManager.setNightMode(ThemeManager.getNightMode(this));

        // Apply saved language on app start
        SharedPreferences prefs = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        String languageCode = prefs.getString("app_language", "en");
        LanguageManager.setLocale(this, languageCode);
    }
}