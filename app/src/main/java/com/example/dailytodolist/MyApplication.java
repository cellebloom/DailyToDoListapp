package com.example.dailytodolist;

import android.app.Application;
import android.content.SharedPreferences;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Apply saved language on app start
        SharedPreferences prefs = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        String languageCode = prefs.getString("app_language", "en");
        LanguageManager.setLocale(this, languageCode);
    }
}