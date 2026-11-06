package com.example.dailytodolist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileActivity extends AppCompatActivity {

    private TextView userNameText, userEmailText;
    private Button backButton;
    private LinearLayout notificationsItem, autoDeleteItem;
    private SwitchCompat notificationsSwitch;

    private static final String PREF_NAME = "ProfilePrefs";
    private static final String USER_NAME_KEY = "user_name";
    private static final String USER_EMAIL_KEY = "user_email";
    private static final String NOTIFICATIONS_KEY = "notifications";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize views
        userNameText = findViewById(R.id.userNameText);
        userEmailText = findViewById(R.id.userEmailText);
        backButton = findViewById(R.id.backButton);
        notificationsItem = findViewById(R.id.notificationsItem);
        autoDeleteItem = findViewById(R.id.autoDeleteItem);
        notificationsSwitch = findViewById(R.id.notificationsSwitch);

        // Load saved settings
        loadSettings();

        // Back button
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Notifications switch
        notificationsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(NOTIFICATIONS_KEY, isChecked);
                editor.apply();

                Toast.makeText(ProfileActivity.this,
                        "Notifications " + (isChecked ? "enabled" : "disabled"),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Auto-delete item click
        autoDeleteItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileActivity.this,
                        "Auto-delete feature coming soon!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String userName = sharedPreferences.getString(USER_NAME_KEY, "Hachimi");
        String userEmail = sharedPreferences.getString(USER_EMAIL_KEY, "brucewayne@gmail.com");

        userNameText.setText(userName);
        userEmailText.setText(userEmail);

        boolean notifications = sharedPreferences.getBoolean(NOTIFICATIONS_KEY, true);
        notificationsSwitch.setChecked(notifications);
    }
}