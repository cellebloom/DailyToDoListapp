package com.example.dailytodolist;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import java.util.Calendar;

public class DailyRewardDialog extends Dialog {

    private UserData userData;
    private SharedPreferences prefs;
    private OnRewardClaimedListener listener;

    public interface OnRewardClaimedListener {
        void onRewardClaimed(int coinsEarned, int newStreak);
    }

    public DailyRewardDialog(Context context, UserData userData, SharedPreferences prefs, OnRewardClaimedListener listener) {
        super(context);
        this.userData = userData;
        this.prefs = prefs;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_daily_reward);

        TextView titleText = findViewById(R.id.titleText);
        TextView coinsText = findViewById(R.id.coinsText);
        TextView streakText = findViewById(R.id.streakText);
        TextView claimButton = findViewById(R.id.claimButton);

        long lastLogin = userData.getLastLoginDate();
        long currentTime = System.currentTimeMillis();
        Calendar lastCal = Calendar.getInstance();
        lastCal.setTimeInMillis(lastLogin);
        Calendar currentCal = Calendar.getInstance();
        currentCal.setTimeInMillis(currentTime);

        boolean isSameDay = lastCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                lastCal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR);

        int streak = userData.getLoginStreak();
        int coinsEarned = 10;

        if (!isSameDay && lastLogin != 0) {
            if (lastCal.get(Calendar.DAY_OF_YEAR) + 1 == currentCal.get(Calendar.DAY_OF_YEAR)) {
                streak++;
            } else {
                streak = 1;
            }
            coinsEarned = 10 + (streak * 2);
            if (coinsEarned > 50) coinsEarned = 50;
        } else if (lastLogin == 0) {
            streak = 1;
            coinsEarned = 20;
        } else {
            claimButton.setEnabled(false);
            claimButton.setText("Already Claimed Today");
            coinsText.setText("0");
            streakText.setText(String.valueOf(streak));
            dismiss();
            return;
        }

        int finalStreak = streak;
        int finalCoins = coinsEarned;

        titleText.setText("Daily Reward!");
        coinsText.setText("+" + finalCoins);
        streakText.setText("🔥 " + finalStreak + " Day Streak");

        claimButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userData.addCoins(finalCoins);
                userData.setLoginStreak(finalStreak);
                userData.setLastLoginDate(currentTime);
                if (listener != null) {
                    listener.onRewardClaimed(finalCoins, finalStreak);
                }
                dismiss();
            }
        });
    }
}