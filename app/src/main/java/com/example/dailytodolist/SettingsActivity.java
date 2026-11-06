package com.example.dailytodolist;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup themeRadioGroup;
    private RadioButton defaultRadio, hotpinkRadio, limeRadio, mintRadio, sunsetRadio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        themeRadioGroup = findViewById(R.id.themeRadioGroup);
        defaultRadio = findViewById(R.id.defaultRadio);
        hotpinkRadio = findViewById(R.id.hotpinkRadio);
        limeRadio = findViewById(R.id.limeRadio);
        mintRadio = findViewById(R.id.mintRadio);
        sunsetRadio = findViewById(R.id.sunsetRadio);

        String currentTheme = ThemeManager.getThemeId(this);
        switch (currentTheme) {
            case ThemeManager.THEME_HOTPINK: hotpinkRadio.setChecked(true); break;
            case ThemeManager.THEME_LIME:    limeRadio.setChecked(true); break;
            case ThemeManager.THEME_MINT:    mintRadio.setChecked(true); break;
            case ThemeManager.THEME_SUNSET:  sunsetRadio.setChecked(true); break;
            default:                         defaultRadio.setChecked(true); break;
        }

        findViewById(R.id.applyButton).setOnClickListener(v -> {
            int selectedId = themeRadioGroup.getCheckedRadioButtonId();
            String theme = ThemeManager.THEME_DEFAULT;

            if (selectedId == R.id.hotpinkRadio) theme = ThemeManager.THEME_HOTPINK;
            else if (selectedId == R.id.limeRadio) theme = ThemeManager.THEME_LIME;
            else if (selectedId == R.id.mintRadio) theme = ThemeManager.THEME_MINT;
            else if (selectedId == R.id.sunsetRadio) theme = ThemeManager.THEME_SUNSET;

            ThemeManager.setThemeId(this, theme);

            Toast.makeText(this, "Theme changed! Restarting...", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }
}
