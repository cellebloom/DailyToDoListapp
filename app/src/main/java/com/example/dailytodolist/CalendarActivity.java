package com.example.dailytodolist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class CalendarActivity extends BaseActivity {

    private TextView monthYearText;
    private TextView selectedDateText;
    private GridView calendarGrid;
    private EditText taskNameInput;
    private TextView addTaskButton;
    private LinearLayout tasksListContainer;
    private TextView noTasksText;
    private LinearLayout mainLayout;
    private TextView calendarHeaderText;
    private LinearLayout calendarCard;
    private LinearLayout tasksCard;
    private LinearLayout footerNav;
    private LinearLayout emptyStateLayout;
    private TextView emptyStateEmoji;
    private TextView tasksLabel;

    private Calendar calendar;
    private Calendar selectedDate;
    private SimpleDateFormat monthYearFormat;
    private SimpleDateFormat dateFormat;

    private HashMap<String, ArrayList<CalendarTask>> tasksMap;
    private SharedPreferences sharedPreferences;
    private int currentAccentColor;
    private boolean isDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);

        initViews();
        setupCalendar();
        loadTasks();
        updateCalendar();
        applyGradientBackground();
        applyDarkMode();
        setupListeners();
        setupNavigation();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.mainLayout);
        monthYearText = findViewById(R.id.monthYearText);
        selectedDateText = findViewById(R.id.selectedDateText);
        calendarGrid = findViewById(R.id.calendarGrid);
        taskNameInput = findViewById(R.id.taskNameInput);
        addTaskButton = findViewById(R.id.addTaskButton);
        tasksListContainer = findViewById(R.id.tasksListContainer);
        noTasksText = findViewById(R.id.noTasksText);
        footerNav = findViewById(R.id.footerNav);

        // Find empty state views
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        emptyStateEmoji = findViewById(R.id.emptyStateEmoji);

        // Find header title using ID
        calendarHeaderText = findViewById(R.id.calendarHeaderTitle);
        if (calendarHeaderText != null) {
            calendarHeaderText.setText(R.string.calendar);
        }

        // Find the cards in the scrollview
        View scrollViewContent = ((ViewGroup) ((ViewGroup) mainLayout).getChildAt(1)).getChildAt(0);
        if (scrollViewContent instanceof LinearLayout) {
            LinearLayout contentLayout = (LinearLayout) scrollViewContent;
            if (contentLayout.getChildCount() >= 1) {
                calendarCard = (LinearLayout) contentLayout.getChildAt(0);
            }
            if (contentLayout.getChildCount() >= 2) {
                tasksCard = (LinearLayout) contentLayout.getChildAt(1);
                if (tasksCard != null && tasksCard.getChildCount() > 0) {
                    View firstChild = tasksCard.getChildAt(0);
                    if (firstChild instanceof TextView) {
                        tasksLabel = (TextView) firstChild;
                        tasksLabel.setText(R.string.tasks_for_date_label);
                    }
                }
            }
        }

        calendar = Calendar.getInstance();
        selectedDate = Calendar.getInstance();
        monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        tasksMap = new HashMap<>();
    }

    private void applyDarkMode() {
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);

        if (isDarkMode) {
            // DARK MODE - Cards become dark, text becomes white

            // Calendar header text - WHITE
            if (calendarHeaderText != null) {
                calendarHeaderText.setTextColor(android.graphics.Color.parseColor("#000000"));
            }

            // Calendar Card background - DARK
            if (calendarCard != null) {
                GradientDrawable darkCardBg = new GradientDrawable();
                darkCardBg.setShape(GradientDrawable.RECTANGLE);
                darkCardBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
                darkCardBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
                calendarCard.setBackground(darkCardBg);
            }

            // Tasks Card background - DARK
            if (tasksCard != null) {
                GradientDrawable darkCardBg = new GradientDrawable();
                darkCardBg.setShape(GradientDrawable.RECTANGLE);
                darkCardBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
                darkCardBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
                tasksCard.setBackground(darkCardBg);
            }

            // Month/Year text - WHITE
            if (monthYearText != null) {
                monthYearText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            }

            // Selected date text - Keep accent color
            if (selectedDateText != null) {
                selectedDateText.setTextColor(currentAccentColor);
            }

            // Task input field - DARK background with WHITE text
            if (taskNameInput != null) {
                GradientDrawable darkInputBg = new GradientDrawable();
                darkInputBg.setShape(GradientDrawable.RECTANGLE);
                darkInputBg.setCornerRadius(12 * getResources().getDisplayMetrics().density);
                darkInputBg.setColor(android.graphics.Color.parseColor("#2D2D2D"));
                darkInputBg.setStroke(1, android.graphics.Color.parseColor("#404040"));
                taskNameInput.setBackground(darkInputBg);
                taskNameInput.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                taskNameInput.setHintTextColor(android.graphics.Color.parseColor("#A0A0A0"));
                taskNameInput.setHint(R.string.add_new_task);
            }

            // "Tasks for this date" label - WHITE
            if (tasksLabel != null) {
                tasksLabel.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            }

            // "No tasks" text - LIGHT GRAY
            if (noTasksText != null) {
                noTasksText.setText(R.string.no_tasks);
                noTasksText.setTextColor(android.graphics.Color.parseColor("#A0A0A0"));
            }

            // Empty state emoji - LIGHT GRAY
            if (emptyStateEmoji != null) {
                emptyStateEmoji.setTextColor(android.graphics.Color.parseColor("#CCCCCC"));
            }

            // Find and update day header texts (SUN, MON, etc.)
            if (calendarCard != null && calendarCard.getChildCount() >= 3) {
                LinearLayout dayHeadersLayout = (LinearLayout) calendarCard.getChildAt(1);
                int dayLabelColor = android.graphics.Color.parseColor("#CCCCCC");
                String[] dayLabels = {getString(R.string.sun), getString(R.string.mon), getString(R.string.tue),
                        getString(R.string.wed), getString(R.string.thu), getString(R.string.fri),
                        getString(R.string.sat)};
                for (int i = 0; i < dayHeadersLayout.getChildCount() && i < dayLabels.length; i++) {
                    TextView dayLabel = (TextView) dayHeadersLayout.getChildAt(i);
                    if (dayLabel != null) {
                        dayLabel.setText(dayLabels[i]);
                        if (i == 0) {
                            dayLabel.setTextColor(currentAccentColor);
                        } else {
                            dayLabel.setTextColor(dayLabelColor);
                        }
                    }
                }
            }

            // Divider line
            if (calendarCard != null && calendarCard.getChildCount() >= 6) {
                View divider = calendarCard.getChildAt(5);
                if (divider != null) {
                    divider.setBackgroundColor(android.graphics.Color.parseColor("#404040"));
                }
            }

            // Calendar grid background - DARK
            if (calendarGrid != null) {
                GradientDrawable gridBg = new GradientDrawable();
                gridBg.setShape(GradientDrawable.RECTANGLE);
                gridBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
                gridBg.setColor(android.graphics.Color.parseColor("#2D2D2D"));
                calendarGrid.setBackground(gridBg);
            }

            // Footer navigation - DARK background with LIGHT text
            if (footerNav != null) {
                GradientDrawable darkFooterBg = new GradientDrawable();
                darkFooterBg.setShape(GradientDrawable.RECTANGLE);
                darkFooterBg.setColor(android.graphics.Color.parseColor("#1E1E1E"));
                footerNav.setBackground(darkFooterBg);

                int footerColor = android.graphics.Color.parseColor("#CCCCCC");
                LinearLayout navHome = findViewById(R.id.navHome);
                LinearLayout navCalendar = findViewById(R.id.navCalendar);
                LinearLayout navShop = findViewById(R.id.navShop);
                LinearLayout navThemes = findViewById(R.id.navThemes);
                LinearLayout navProfile = findViewById(R.id.navProfile);

                updateNavItemColors(navHome, footerColor, footerColor);
                updateNavItemColors(navCalendar, currentAccentColor, currentAccentColor);
                updateNavItemColors(navShop, footerColor, footerColor);
                updateNavItemColors(navThemes, footerColor, footerColor);
                updateNavItemColors(navProfile, footerColor, footerColor);
            }

            // Prev/Next month buttons - Keep accent color
            TextView prevMonth = findViewById(R.id.prevMonth);
            TextView nextMonth = findViewById(R.id.nextMonth);
            if (prevMonth != null) prevMonth.setTextColor(currentAccentColor);
            if (nextMonth != null) nextMonth.setTextColor(currentAccentColor);

            // Update existing task card backgrounds to dark mode
            updateAllTaskCardBackgrounds();

        } else {
            // LIGHT MODE - Restore original light theme

            // Calendar header text - WHITE (stays white on gradient)
            if (calendarHeaderText != null) {
                calendarHeaderText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
            }

            // Calendar Card background - WHITE
            if (calendarCard != null) {
                calendarCard.setBackgroundResource(R.drawable.white_card_bg);
            }

            // Tasks Card background - WHITE
            if (tasksCard != null) {
                tasksCard.setBackgroundResource(R.drawable.white_card_bg);
            }

            // Month/Year text - Dark blue
            if (monthYearText != null) {
                monthYearText.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
            }

            // Task input field - Light background
            if (taskNameInput != null) {
                taskNameInput.setBackgroundResource(R.drawable.input_field_bg);
                taskNameInput.setTextColor(getColor(R.color.theme_text_primary));
                taskNameInput.setHintTextColor(getColor(R.color.theme_text_secondary));
                taskNameInput.setHint(R.string.add_new_task);
            }

            // "Tasks for this date" label - Dark blue
            if (tasksLabel != null) {
                tasksLabel.setText(R.string.tasks_for_date_label);
                tasksLabel.setTextColor(android.graphics.Color.parseColor("#2C3E66"));
            }

            // "No tasks" text - Gray
            if (noTasksText != null) {
                noTasksText.setText(R.string.no_tasks);
                noTasksText.setTextColor(android.graphics.Color.parseColor("#7F8C8D"));
            }

            // Empty state emoji - Gray
            if (emptyStateEmoji != null) {
                emptyStateEmoji.setTextColor(android.graphics.Color.parseColor("#95A5A6"));
            }

            // Restore day headers with string resources
            if (calendarCard != null && calendarCard.getChildCount() >= 3) {
                LinearLayout dayHeadersLayout = (LinearLayout) calendarCard.getChildAt(1);
                int[] dayColors = {
                        android.graphics.Color.parseColor("#E94560"), // SUN
                        android.graphics.Color.parseColor("#2C3E66"), // MON
                        android.graphics.Color.parseColor("#2C3E66"), // TUE
                        android.graphics.Color.parseColor("#2C3E66"), // WED
                        android.graphics.Color.parseColor("#2C3E66"), // THU
                        android.graphics.Color.parseColor("#2C3E66"), // FRI
                        android.graphics.Color.parseColor("#2C3E66")  // SAT
                };
                String[] dayLabels = {getString(R.string.sun), getString(R.string.mon), getString(R.string.tue),
                        getString(R.string.wed), getString(R.string.thu), getString(R.string.fri),
                        getString(R.string.sat)};
                for (int i = 0; i < dayHeadersLayout.getChildCount() && i < dayColors.length; i++) {
                    TextView dayLabel = (TextView) dayHeadersLayout.getChildAt(i);
                    if (dayLabel != null) {
                        dayLabel.setText(dayLabels[i]);
                        dayLabel.setTextColor(dayColors[i]);
                    }
                }
            }

            // Restore divider color
            if (calendarCard != null && calendarCard.getChildCount() >= 6) {
                View divider = calendarCard.getChildAt(5);
                if (divider != null) {
                    divider.setBackgroundColor(android.graphics.Color.parseColor("#E2EDF2"));
                }
            }

            // Calendar grid background - Remove dark background
            if (calendarGrid != null) {
                calendarGrid.setBackground(null);
            }

            // Footer navigation - Light background
            if (footerNav != null) {
                footerNav.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"));

                int footerColorLight = android.graphics.Color.parseColor("#555555");
                LinearLayout navHome = findViewById(R.id.navHome);
                LinearLayout navCalendar = findViewById(R.id.navCalendar);
                LinearLayout navShop = findViewById(R.id.navShop);
                LinearLayout navThemes = findViewById(R.id.navThemes);
                LinearLayout navProfile = findViewById(R.id.navProfile);

                updateNavItemColors(navHome, footerColorLight, footerColorLight);
                updateNavItemColors(navCalendar, android.graphics.Color.parseColor("#9C27B0"), android.graphics.Color.parseColor("#9C27B0"));
                updateNavItemColors(navShop, footerColorLight, footerColorLight);
                updateNavItemColors(navThemes, footerColorLight, footerColorLight);
                updateNavItemColors(navProfile, footerColorLight, footerColorLight);
            }

            // Update existing task card backgrounds to light mode
            updateAllTaskCardBackgrounds();
        }
    }

    private void updateAllTaskCardBackgrounds() {
        if (tasksListContainer != null && tasksListContainer.getChildCount() > 0) {
            for (int i = 0; i < tasksListContainer.getChildCount(); i++) {
                View taskView = tasksListContainer.getChildAt(i);
                View innerLayout = taskView;
                if (innerLayout instanceof LinearLayout) {
                    LinearLayout taskCardLayout = (LinearLayout) innerLayout;
                    if (taskCardLayout.getChildAt(0) instanceof LinearLayout) {
                        LinearLayout taskBackgroundLayout = (LinearLayout) taskCardLayout.getChildAt(0);
                        if (isDarkMode) {
                            taskBackgroundLayout.setBackgroundResource(R.drawable.task_card_bg_dark);
                        } else {
                            taskBackgroundLayout.setBackgroundResource(R.drawable.task_card_bg_light);
                        }
                    }
                }
            }
        }
    }

    private void updateNavItemColors(LinearLayout navItem, int iconColor, int textColor) {
        if (navItem != null && navItem.getChildCount() >= 2) {
            TextView icon = (TextView) navItem.getChildAt(0);
            TextView text = (TextView) navItem.getChildAt(1);
            if (icon != null) icon.setTextColor(iconColor);
            if (text != null) text.setTextColor(textColor);
        }
    }

    @Override
    protected void applyGradientBackground() {
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        currentAccentColor = gradientColors[0];

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
    }

    private void loadTasks() {
        String tasksJson = sharedPreferences.getString("calendar_tasks", "");
        if (!tasksJson.isEmpty()) {
            String[] taskEntries = tasksJson.split("\\|\\|");
            for (String entry : taskEntries) {
                String[] parts = entry.split("\\|");
                if (parts.length == 4) {
                    String date = parts[0];
                    String taskName = parts[1];
                    String taskTime = parts[2];
                    boolean isCompleted = Boolean.parseBoolean(parts[3]);

                    if (!tasksMap.containsKey(date)) {
                        tasksMap.put(date, new ArrayList<>());
                    }
                    tasksMap.get(date).add(new CalendarTask(taskName, taskTime, isCompleted));
                }
            }
        }
    }

    private void saveTasks() {
        StringBuilder sb = new StringBuilder();
        for (String date : tasksMap.keySet()) {
            for (CalendarTask task : tasksMap.get(date)) {
                sb.append(date).append("|")
                        .append(task.name).append("|")
                        .append(task.time).append("|")
                        .append(task.isCompleted).append("||");
            }
        }
        sharedPreferences.edit().putString("calendar_tasks", sb.toString()).apply();
    }

    private void setupCalendar() {
        CalendarAdapter adapter = new CalendarAdapter();
        calendarGrid.setAdapter(adapter);
        calendarGrid.setNumColumns(7);
        calendarGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        calendarGrid.setVerticalSpacing(2);
        calendarGrid.setHorizontalSpacing(0);

        calendarGrid.post(() -> {
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            float density = getResources().getDisplayMetrics().density;
            int idealHeight = (int) (320 * density);
            int maxHeight = (int) (screenHeight * 0.55);
            int finalHeight = Math.min(idealHeight, maxHeight);

            ViewGroup.LayoutParams params = calendarGrid.getLayoutParams();
            params.height = finalHeight;
            calendarGrid.setLayoutParams(params);
            calendarGrid.requestLayout();
        });
    }

    private void updateCalendar() {
        monthYearText.setText(monthYearFormat.format(calendar.getTime()));
        ((BaseAdapter) calendarGrid.getAdapter()).notifyDataSetChanged();
        updateSelectedDateDisplay();
        updateTasksForSelectedDate();
    }

    private void updateSelectedDateDisplay() {
        selectedDateText.setText(getString(R.string.selected_prefix) + dateFormat.format(selectedDate.getTime()));
    }

    private void updateTasksForSelectedDate() {
        tasksListContainer.removeAllViews();
        String currentDate = dateFormat.format(selectedDate.getTime());

        if (!tasksMap.containsKey(currentDate) || tasksMap.get(currentDate).isEmpty()) {
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
            if (tasksListContainer != null) {
                tasksListContainer.setVisibility(View.GONE);
            }
            return;
        }

        if (emptyStateLayout != null) {
            emptyStateLayout.setVisibility(View.GONE);
        }
        if (tasksListContainer != null) {
            tasksListContainer.setVisibility(View.VISIBLE);
        }

        for (int i = 0; i < tasksMap.get(currentDate).size(); i++) {
            CalendarTask task = tasksMap.get(currentDate).get(i);
            addTaskToView(task, i);
        }
    }

    private void addTaskToView(CalendarTask task, int position) {
        View taskView = getLayoutInflater().inflate(R.layout.calendar_task_item, tasksListContainer, false);

        CheckBox taskCheckbox = taskView.findViewById(R.id.taskCheckbox);
        TextView taskNameText = taskView.findViewById(R.id.taskNameText);
        TextView taskTimeText = taskView.findViewById(R.id.taskTimeText);
        TextView deleteTask = taskView.findViewById(R.id.deleteTask);

        LinearLayout taskCardLayout = (LinearLayout) ((LinearLayout) taskView).getChildAt(0);

        if (taskCardLayout != null) {
            if (isDarkMode) {
                taskCardLayout.setBackgroundResource(R.drawable.task_card_bg_dark);
            } else {
                taskCardLayout.setBackgroundResource(R.drawable.task_card_bg_light);
            }
        }

        taskNameText.setText(task.name);
        taskTimeText.setText(task.time);
        taskCheckbox.setChecked(task.isCompleted);

        if (task.isCompleted) {
            taskCheckbox.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.RED));
            taskNameText.setPaintFlags(taskNameText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            taskNameText.setTextColor(ContextCompat.getColor(this, R.color.base_text_secondary));
            taskTimeText.setTextColor(ContextCompat.getColor(this, R.color.base_text_secondary));
        } else {
            taskCheckbox.setButtonTintList(android.content.res.ColorStateList.valueOf(currentAccentColor));
            taskNameText.setPaintFlags(taskNameText.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));

            if (isDarkMode) {
                taskNameText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                taskTimeText.setTextColor(android.graphics.Color.parseColor("#CCCCCC"));
                deleteTask.setTextColor(android.graphics.Color.parseColor("#FF6B6B"));
            } else {
                taskNameText.setTextColor(ContextCompat.getColor(this, R.color.calendar_date_dark));
                taskTimeText.setTextColor(ContextCompat.getColor(this, R.color.calendar_date_dark));
                deleteTask.setTextColor(android.graphics.Color.parseColor("#E74C3C"));
            }
        }

        String currentDate = dateFormat.format(selectedDate.getTime());

        taskCheckbox.setOnClickListener(v -> {
            task.isCompleted = taskCheckbox.isChecked();
            saveTasks();
            updateTasksForSelectedDate();

            if (task.isCompleted) {
                SharedPreferences prefs = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
                int currentCoins = prefs.getInt("user_coins", 0);
                prefs.edit().putInt("user_coins", currentCoins + 5).apply();
                Toast.makeText(this, getString(R.string.coins_earned, 5), Toast.LENGTH_SHORT).show();
            }
        });

        deleteTask.setOnClickListener(v -> {
            tasksMap.get(currentDate).remove(position);
            if (tasksMap.get(currentDate).isEmpty()) {
                tasksMap.remove(currentDate);
            }
            saveTasks();
            updateTasksForSelectedDate();
            ((BaseAdapter) calendarGrid.getAdapter()).notifyDataSetChanged();
        });

        tasksListContainer.addView(taskView);
    }

    private void addNewTask() {
        String taskName = taskNameInput.getText().toString().trim();

        if (taskName.isEmpty()) {
            Toast.makeText(this, R.string.enter_task_name, Toast.LENGTH_SHORT).show();
            return;
        }

        String currentDate = dateFormat.format(selectedDate.getTime());

        if (!tasksMap.containsKey(currentDate)) {
            tasksMap.put(currentDate, new ArrayList<>());
        }

        tasksMap.get(currentDate).add(new CalendarTask(taskName, "", false));
        saveTasks();
        updateTasksForSelectedDate();
        ((BaseAdapter) calendarGrid.getAdapter()).notifyDataSetChanged();

        taskNameInput.setText("");
        Toast.makeText(this, R.string.task_added_calendar, Toast.LENGTH_SHORT).show();
    }

    private void setupListeners() {
        addTaskButton.setOnClickListener(v -> addNewTask());
    }

    @Override
    protected void setupNavigation() {
        findViewById(R.id.prevMonth).setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateCalendar();
            applyGradientBackground();
            applyDarkMode();
        });

        findViewById(R.id.nextMonth).setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateCalendar();
            applyGradientBackground();
            applyDarkMode();
        });

        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.navCalendar).setOnClickListener(v -> {
            Toast.makeText(this, R.string.already_on_calendar, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.navShop).setOnClickListener(v -> {
            Intent intent = new Intent(this, ShopActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.navThemes).setOnClickListener(v -> {
            Intent intent = new Intent(this, ThemeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        applyGradientBackground();
        applyDarkMode();
        updateCalendar();

        // Refresh header title
        if (calendarHeaderText != null) {
            calendarHeaderText.setText(R.string.calendar);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyGradientBackground();
        applyDarkMode();
    }

    class CalendarTask {
        String name, time;
        boolean isCompleted;

        CalendarTask(String name, String time, boolean isCompleted) {
            this.name = name;
            this.time = time;
            this.isCompleted = isCompleted;
        }
    }

    class CalendarAdapter extends BaseAdapter {
        private Calendar tempCalendar = (Calendar) calendar.clone();
        private int firstDayOfMonth;
        private int daysInMonth;
        private int currentMonth;
        private int currentYear;

        CalendarAdapter() {
            updateCalendarInfo();
        }

        void updateCalendarInfo() {
            tempCalendar.setTime(calendar.getTime());
            tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
            firstDayOfMonth = tempCalendar.get(Calendar.DAY_OF_WEEK) - 1;
            daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            currentMonth = tempCalendar.get(Calendar.MONTH);
            currentYear = tempCalendar.get(Calendar.YEAR);
        }

        @Override
        public int getCount() {
            updateCalendarInfo();
            return 42;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.calendar_cell, parent, false);
            }

            TextView dayText = convertView.findViewById(R.id.calendarDayText);
            int dayNumber = position - firstDayOfMonth + 1;
            boolean isCurrentMonth = dayNumber > 0 && dayNumber <= daysInMonth;

            if (isCurrentMonth) {
                dayText.setText(String.valueOf(dayNumber));

                if (isDarkMode) {
                    dayText.setTextColor(android.graphics.Color.parseColor("#FFFFFF"));
                } else {
                    dayText.setTextColor(ContextCompat.getColor(CalendarActivity.this, R.color.calendar_date_dark));
                }

                dayText.setTextSize(14);
                dayText.setTypeface(null, android.graphics.Typeface.BOLD);
                dayText.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                dayText.setGravity(android.view.Gravity.CENTER);
                dayText.setPadding(0, 12, 0, 12);

                Calendar checkDate = (Calendar) calendar.clone();
                checkDate.set(Calendar.DAY_OF_MONTH, dayNumber);
                String dateStr = dateFormat.format(checkDate.getTime());

                if (tasksMap.containsKey(dateStr) && !tasksMap.get(dateStr).isEmpty()) {
                    GradientDrawable taskBg = new GradientDrawable();
                    taskBg.setShape(GradientDrawable.RECTANGLE);
                    taskBg.setCornerRadius(25);
                    if (isDarkMode) {
                        taskBg.setColor(currentAccentColor);
                    } else {
                        taskBg.setColor(ContextCompat.getColor(CalendarActivity.this, R.color.task_indicator));
                    }
                    dayText.setBackground(taskBg);
                }

                if (selectedDate.get(Calendar.YEAR) == currentYear &&
                        selectedDate.get(Calendar.MONTH) == currentMonth &&
                        selectedDate.get(Calendar.DAY_OF_MONTH) == dayNumber) {
                    GradientDrawable selectedBg = new GradientDrawable();
                    selectedBg.setShape(GradientDrawable.RECTANGLE);
                    selectedBg.setCornerRadius(25);
                    selectedBg.setColor(currentAccentColor);
                    dayText.setBackground(selectedBg);
                    dayText.setTextColor(android.graphics.Color.WHITE);
                }

            } else {
                dayText.setText("");
                dayText.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            final int finalDayNumber = dayNumber;
            final boolean finalIsCurrentMonth = isCurrentMonth;

            convertView.setOnClickListener(v -> {
                if (finalIsCurrentMonth) {
                    selectedDate.set(currentYear, currentMonth, finalDayNumber);
                    updateSelectedDateDisplay();
                    updateTasksForSelectedDate();
                    notifyDataSetChanged();
                }
            });

            return convertView;
        }

        @Override
        public void notifyDataSetChanged() {
            updateCalendarInfo();
            super.notifyDataSetChanged();
        }
    }
}