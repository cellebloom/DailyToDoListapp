package com.example.dailytodolist;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup themeRadioGroup;
    private RadioButton lightRadio, pinkRadio, darkRadio;
    private Button applyButton, backButton;

    public static final String PREFS_NAME = "ThemePrefs";
    public static final String THEME_KEY = "theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String theme = prefs.getString(THEME_KEY, "light");

        if (theme.equals("pink")) {
            setTheme(R.style.Theme_DailyToDoList_Pink);
        } else if (theme.equals("dark")) {
            setTheme(R.style.Theme_DailyToDoList_Dark);
        } else {
            setTheme(R.style.Theme_DailyToDoList_Light);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize views
        themeRadioGroup = findViewById(R.id.themeRadioGroup);
        lightRadio = findViewById(R.id.lightRadio);
        pinkRadio = findViewById(R.id.pinkRadio);
        darkRadio = findViewById(R.id.darkRadio);
        applyButton = findViewById(R.id.applyButton);
        backButton = findViewById(R.id.backButton);

        // Set current theme selection
        String currentTheme = prefs.getString(THEME_KEY, "light");

        if (currentTheme.equals("pink")) {
            pinkRadio.setChecked(true);
        } else if (currentTheme.equals("dark")) {
            darkRadio.setChecked(true);
        } else {
            lightRadio.setChecked(true);
        }

        // Apply button
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedId = themeRadioGroup.getCheckedRadioButtonId();
                String theme = "light";

                if (selectedId == R.id.pinkRadio) {
                    theme = "pink";
                } else if (selectedId == R.id.darkRadio) {
                    theme = "dark";
                }

                // Save theme preference
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putString(THEME_KEY, theme);
                editor.apply();

                Toast.makeText(SettingsActivity.this, "Theme changed! Restarting...", Toast.LENGTH_SHORT).show();

                // Restart app to apply theme to all activities
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        // Back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}