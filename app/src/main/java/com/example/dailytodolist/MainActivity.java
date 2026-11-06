package com.example.dailytodolist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private TextView dateText;
    private EditText newTaskInput;
    private TextView addTaskButton;
    private LinearLayout tasksListLayout;
    private LinearLayout noTasksLayout;

    private TextView quoteText, quoteAuthor;
    private TextView editQuoteButton;

    private LinearLayout navHome, navCalendar, navShop, navThemes, navProfile;
    private LinearLayout mainLayout;

    private ArrayList<String> tasks;
    private ArrayList<Boolean> taskStatus;
    private SharedPreferences sharedPreferences;
    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);

        initViews();
        setCurrentDate();
        loadTasks();
        displayTasks();
        loadQuote();
        giveDailyCoins();
        applyAllThemes();
        setupClickListeners();
        applyDarkMode();

        // Update header title with string resource
        TextView titleText = findViewById(R.id.titleText);
        if (titleText != null) {
            titleText.setText(R.string.app_name);
        }
    }

    private void initViews() {
        dateText = findViewById(R.id.dateText);
        newTaskInput = findViewById(R.id.newTaskInput);
        addTaskButton = findViewById(R.id.addTaskButton);
        tasksListLayout = findViewById(R.id.tasksListLayout);
        noTasksLayout = findViewById(R.id.noTasksLayout);
        mainLayout = findViewById(R.id.mainLayout);

        quoteText = findViewById(R.id.quoteText);
        quoteAuthor = findViewById(R.id.quoteAuthor);
        editQuoteButton = findViewById(R.id.editQuoteButton);

        navHome = findViewById(R.id.navHome);
        navCalendar = findViewById(R.id.navCalendar);
        navShop = findViewById(R.id.navShop);
        navThemes = findViewById(R.id.navThemes);
        navProfile = findViewById(R.id.navProfile);

        // Set hint text from resources
        if (newTaskInput != null) {
            newTaskInput.setHint(R.string.add_new_task_hint);
        }
    }

    private void setCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        dateText.setText(currentDate);
    }

    private void giveDailyCoins() {
        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastRewardDate = sharedPreferences.getString("last_reward_date", "");

        if (!today.equals(lastRewardDate)) {
            int currentCoins = sharedPreferences.getInt("user_coins", 0);
            int newCoins = currentCoins + 10;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("user_coins", newCoins);
            editor.putString("last_reward_date", today);
            editor.apply();
            Toast.makeText(this, getString(R.string.daily_coins_reward_message), Toast.LENGTH_LONG).show();
        }
    }

    private void applyDarkMode() {
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);

        LinearLayout dateCard = findViewById(R.id.dateCard);
        LinearLayout tasksCard = findViewById(R.id.tasksCard);
        LinearLayout quoteCard = findViewById(R.id.quoteCard);
        TextView titleText = findViewById(R.id.titleText);
        LinearLayout bottomNavBar = findViewById(R.id.bottomNavBar);
        View divider = findViewById(R.id.divider);

        TextView navHomeText = findViewById(R.id.navHomeText);
        TextView navCalendarText = findViewById(R.id.navCalendarText);
        TextView navShopText = findViewById(R.id.navShopText);
        TextView navThemesText = findViewById(R.id.navThemesText);
        TextView navProfileText = findViewById(R.id.navProfileText);

        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        int accentColor = gradientColors[0];

        if (isDarkMode) {
            if (dateCard != null) dateCard.setBackgroundResource(R.drawable.dark_card_bg);
            if (tasksCard != null) tasksCard.setBackgroundResource(R.drawable.dark_card_bg);
            if (quoteCard != null) quoteCard.setBackgroundResource(R.drawable.dark_card_bg);
            if (divider != null) divider.setBackgroundColor(android.graphics.Color.parseColor("#404040"));

            if (newTaskInput != null) {
                GradientDrawable grayBg = new GradientDrawable();
                grayBg.setShape(GradientDrawable.RECTANGLE);
                grayBg.setCornerRadius(12 * getResources().getDisplayMetrics().density);
                grayBg.setColor(android.graphics.Color.parseColor("#404040"));
                newTaskInput.setBackground(grayBg);
                newTaskInput.setTextColor(getColor(R.color.white));
                newTaskInput.setHintTextColor(android.graphics.Color.parseColor("#A0A0A0"));
                newTaskInput.setHint(R.string.add_new_task_hint);
            }

            if (titleText != null) titleText.setTextColor(getColor(R.color.black));

            TextView tasksHeaderText = findViewById(R.id.tasksHeaderText);
            if (tasksHeaderText != null) {
                tasksHeaderText.setText(R.string.todays_tasks);
                tasksHeaderText.setTextColor(getColor(R.color.white));
            }

            if (dateText != null) dateText.setTextColor(getColor(R.color.white));
            if (quoteText != null) quoteText.setTextColor(getColor(R.color.white));
            if (quoteAuthor != null) quoteAuthor.setTextColor(accentColor);
            if (editQuoteButton != null) editQuoteButton.setTextColor(accentColor);

            TextView noTasksText = findViewById(R.id.noTasksText);
            if (noTasksText != null) {
                noTasksText.setText(R.string.no_tasks_today);
                noTasksText.setTextColor(android.graphics.Color.parseColor("#A0A0A0"));
            }

            if (bottomNavBar != null) {
                bottomNavBar.setBackgroundColor(getColor(R.color.black));
            }

            if (navHomeText != null) navHomeText.setTextColor(accentColor);
            if (navCalendarText != null) navCalendarText.setTextColor(android.graphics.Color.parseColor("#A0A0A0"));
            if (navShopText != null) navShopText.setTextColor(android.graphics.Color.parseColor("#A0A0A0"));
            if (navThemesText != null) navThemesText.setTextColor(android.graphics.Color.parseColor("#A0A0A0"));
            if (navProfileText != null) navProfileText.setTextColor(android.graphics.Color.parseColor("#A0A0A0"));

        } else {
            if (dateCard != null) dateCard.setBackgroundResource(R.drawable.white_card_bg);
            if (tasksCard != null) tasksCard.setBackgroundResource(R.drawable.white_card_bg);
            if (quoteCard != null) quoteCard.setBackgroundResource(R.drawable.white_card_bg);
            if (divider != null) divider.setBackgroundColor(getColor(R.color.todo_divider));

            if (newTaskInput != null) {
                GradientDrawable whiteBg = new GradientDrawable();
                whiteBg.setShape(GradientDrawable.RECTANGLE);
                whiteBg.setCornerRadius(12 * getResources().getDisplayMetrics().density);
                whiteBg.setColor(getColor(R.color.white));
                whiteBg.setStroke(1, getColor(R.color.todo_divider));
                newTaskInput.setBackground(whiteBg);
                newTaskInput.setTextColor(getColor(R.color.theme_text_primary));
                newTaskInput.setHintTextColor(getColor(R.color.theme_text_secondary));
                newTaskInput.setHint(R.string.add_new_task_hint);
            }

            if (titleText != null) titleText.setTextColor(getColor(R.color.white));

            TextView tasksHeaderText = findViewById(R.id.tasksHeaderText);
            if (tasksHeaderText != null) {
                tasksHeaderText.setText(R.string.todays_tasks);
                tasksHeaderText.setTextColor(getColor(R.color.theme_text_primary));
            }

            if (dateText != null) dateText.setTextColor(getColor(R.color.theme_text_primary));
            if (quoteText != null) quoteText.setTextColor(getColor(R.color.theme_text_primary));

            TextView noTasksText = findViewById(R.id.noTasksText);
            if (noTasksText != null) {
                noTasksText.setText(R.string.no_tasks_today);
                noTasksText.setTextColor(getColor(R.color.theme_text_secondary));
            }

            if (quoteAuthor != null) quoteAuthor.setTextColor(accentColor);
            if (editQuoteButton != null) editQuoteButton.setTextColor(accentColor);

            if (bottomNavBar != null) {
                bottomNavBar.setBackgroundColor(getColor(R.color.white));
            }

            if (navHomeText != null) navHomeText.setTextColor(accentColor);
            if (navCalendarText != null) navCalendarText.setTextColor(getColor(R.color.theme_text_secondary));
            if (navShopText != null) navShopText.setTextColor(getColor(R.color.theme_text_secondary));
            if (navThemesText != null) navThemesText.setTextColor(getColor(R.color.theme_text_secondary));
            if (navProfileText != null) navProfileText.setTextColor(getColor(R.color.theme_text_secondary));
        }

        // Update navigation texts
        updateNavigationTexts();
    }

    private void applyAllThemes() {
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        int accentColor = gradientColors[0];

        if (mainLayout != null) {
            GradientDrawable gradientDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    gradientColors
            );
            mainLayout.setBackground(gradientDrawable);
        }

        if (addTaskButton != null) {
            GradientDrawable buttonGradient = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    gradientColors
            );
            buttonGradient.setCornerRadius(27 * getResources().getDisplayMetrics().density);
            addTaskButton.setBackground(buttonGradient);
            addTaskButton.setTextColor(getColor(R.color.white));
            addTaskButton.setText(R.string.add);
        }

        applyThemeToAllElements();
    }

    private void applyThemeToAllElements() {
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        int accentColor = gradientColors[0];

        if (quoteAuthor != null && !isDarkMode) quoteAuthor.setTextColor(accentColor);
        if (editQuoteButton != null && !isDarkMode) editQuoteButton.setTextColor(accentColor);

        if (isDarkMode) {
            if (quoteAuthor != null) quoteAuthor.setTextColor(accentColor);
            if (editQuoteButton != null) editQuoteButton.setTextColor(accentColor);
        }
    }

    private void loadTasks() {
        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);

        int taskCount = sharedPreferences.getInt("task_count", 0);
        tasks = new ArrayList<>();
        taskStatus = new ArrayList<>();

        for (int i = 0; i < taskCount; i++) {
            String task = sharedPreferences.getString("task_" + i, "");
            boolean status = sharedPreferences.getBoolean("task_status_" + i, false);
            if (!task.isEmpty()) {
                tasks.add(task);
                taskStatus.add(status);
            }
        }
    }

    private void saveTasks() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("task_count", tasks.size());

        for (int i = 0; i < tasks.size(); i++) {
            editor.putString("task_" + i, tasks.get(i));
            editor.putBoolean("task_status_" + i, taskStatus.get(i));
        }

        editor.apply();
    }

    private void displayTasks() {
        tasksListLayout.removeAllViews();

        if (tasks.isEmpty()) {
            noTasksLayout.setVisibility(View.VISIBLE);
            tasksListLayout.setVisibility(View.GONE);
        } else {
            noTasksLayout.setVisibility(View.GONE);
            tasksListLayout.setVisibility(View.VISIBLE);

            for (int i = 0; i < tasks.size(); i++) {
                addTaskToView(tasks.get(i), i);
            }
        }
    }

    private void addTaskToView(String task, int position) {
        View taskView = getLayoutInflater().inflate(R.layout.task_item, tasksListLayout, false);

        androidx.cardview.widget.CardView cardView = (androidx.cardview.widget.CardView) taskView;

        CheckBox taskCheckbox = taskView.findViewById(R.id.taskCheckbox);
        TextView taskText = taskView.findViewById(R.id.taskText);
        TextView deleteTask = taskView.findViewById(R.id.deleteTask);

        taskText.setText(task);
        taskCheckbox.setChecked(taskStatus.get(position));

        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        int accentColor = gradientColors[0];

        if (taskStatus.get(position)) {
            taskCheckbox.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.RED));
            taskText.setPaintFlags(taskText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            taskText.setTextColor(ContextCompat.getColor(this, R.color.base_text_secondary));
            if (isDarkMode) {
                cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#2D2D2D"));
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            }
        } else {
            taskCheckbox.setButtonTintList(android.content.res.ColorStateList.valueOf(accentColor));
            taskText.setPaintFlags(taskText.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));

            if (isDarkMode) {
                taskText.setTextColor(ContextCompat.getColor(this, R.color.white));
                cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#2D2D2D"));
            } else {
                taskText.setTextColor(ContextCompat.getColor(this, R.color.calendar_date_dark));
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            }
        }

        if (deleteTask != null) {
            deleteTask.setTextColor(accentColor);
        }

        taskCheckbox.setOnClickListener(v -> {
            boolean wasCompleted = taskStatus.get(position);
            taskStatus.set(position, taskCheckbox.isChecked());
            saveTasks();

            if (taskCheckbox.isChecked() && !wasCompleted) {
                SharedPreferences prefs = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
                int currentCoins = prefs.getInt("user_coins", 0);
                prefs.edit().putInt("user_coins", currentCoins + 5).apply();
                Toast.makeText(this, getString(R.string.coins_earned_main, 5), Toast.LENGTH_SHORT).show();
            }

            displayTasks();
            String message = taskCheckbox.isChecked() ? getString(R.string.task_completed_message) : getString(R.string.task_uncompleted_message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        deleteTask.setOnClickListener(v -> showDeleteConfirmation(position));

        tasksListLayout.addView(taskView);
    }

    private void showDeleteConfirmation(int position) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_delete, null);

        TextView confirmDelete = sheetView.findViewById(R.id.confirmDelete);
        TextView cancelDelete = sheetView.findViewById(R.id.cancelDelete);
        TextView taskToDelete = sheetView.findViewById(R.id.taskToDelete);

        taskToDelete.setText(tasks.get(position));

        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        int accentColor = gradientColors[0];

        if (confirmDelete != null) {
            confirmDelete.setTextColor(accentColor);
            confirmDelete.setText(R.string.delete);
        }
        if (cancelDelete != null) {
            cancelDelete.setTextColor(accentColor);
            cancelDelete.setText(R.string.cancel);
        }

        confirmDelete.setOnClickListener(v -> {
            tasks.remove(position);
            taskStatus.remove(position);
            saveTasks();
            displayTasks();
            bottomSheetDialog.dismiss();
            Toast.makeText(this, R.string.task_deleted_message, Toast.LENGTH_SHORT).show();
        });

        cancelDelete.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void addNewTask() {
        String task = newTaskInput.getText().toString().trim();

        if (TextUtils.isEmpty(task)) {
            Toast.makeText(this, R.string.enter_task_name, Toast.LENGTH_SHORT).show();
            return;
        }

        tasks.add(task);
        taskStatus.add(false);
        saveTasks();
        displayTasks();

        newTaskInput.setText("");
        Toast.makeText(this, R.string.task_added_message, Toast.LENGTH_SHORT).show();
    }

    private void loadQuote() {
        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);

        String savedQuote = sharedPreferences.getString("quote_text", getString(R.string.default_quote));
        String savedAuthor = sharedPreferences.getString("quote_author", getString(R.string.default_quote_author));

        if (!sharedPreferences.contains("quote_text")) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("quote_text", savedQuote);
            editor.putString("quote_author", savedAuthor);
            editor.apply();
        }

        if (quoteText != null) {
            quoteText.setText(savedQuote);
            quoteText.setVisibility(View.VISIBLE);
        }

        if (quoteAuthor != null) {
            quoteAuthor.setText(savedAuthor);
            quoteAuthor.setVisibility(View.VISIBLE);
        }
    }

    private void saveQuote(String quote, String author) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("quote_text", quote);
        editor.putString("quote_author", author);
        editor.apply();
    }

    private void showEditQuoteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_quote, null);

        EditText quoteEditText = dialogView.findViewById(R.id.quoteEditText);
        EditText authorEditText = dialogView.findViewById(R.id.authorEditText);

        String currentQuote = quoteText.getText().toString();
        String currentAuthor = quoteAuthor.getText().toString();
        quoteEditText.setText(currentQuote);
        authorEditText.setText(currentAuthor);

        builder.setTitle(R.string.edit_quote)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newQuote = quoteEditText.getText().toString().trim();
                    String newAuthor = authorEditText.getText().toString().trim();

                    if (!newQuote.isEmpty()) {
                        saveQuote(newQuote, newAuthor);
                        quoteText.setText(newQuote);
                        quoteAuthor.setText(newAuthor);
                        Toast.makeText(this, R.string.quote_updated, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.quote_cannot_be_empty, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupClickListeners() {
        addTaskButton.setOnClickListener(v -> addNewTask());
        editQuoteButton.setOnClickListener(v -> showEditQuoteDialog());

        navHome.setOnClickListener(v -> {
            Toast.makeText(this, R.string.already_on_home, Toast.LENGTH_SHORT).show();
        });

        navCalendar.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CalendarActivity.class));
        });

        navShop.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ShopActivity.class));
        });

        navThemes.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ThemeActivity.class));
        });

        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        loadTasks();
        displayTasks();
        applyAllThemes();
        applyDarkMode();
        loadQuote();
        updateNavigationTexts();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyAllThemes();
        applyDarkMode();
    }
}