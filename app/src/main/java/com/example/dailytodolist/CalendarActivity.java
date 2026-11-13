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

import com.example.dailytodolist.util.ThemeColors;
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
    /** Endpoints of the active theme gradient, used for contrast-safe labels. */
    private int currentGradientStart;
    private int currentGradientEnd;
    private boolean isDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        isDarkMode = ThemeManager.isDarkMode(this);

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
        isDarkMode = ThemeManager.isDarkMode(this);

        // Every colour below is a theme role, so this single path is correct on
        // all ten palettes; the old dark/light branches are gone.
        if (calendarHeaderText != null) {
            calendarHeaderText.setTextColor(
                    ThemeColors.onGradient(currentGradientStart, currentGradientEnd));
        }

        if (calendarCard != null) {
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
            cardBg.setColor(ThemeColors.surface(calendarCard));
            calendarCard.setBackground(cardBg);
        }

        if (tasksCard != null) {
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setShape(GradientDrawable.RECTANGLE);
            cardBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
            cardBg.setColor(ThemeColors.surface(tasksCard));
            tasksCard.setBackground(cardBg);
        }

        if (monthYearText != null) {
            monthYearText.setTextColor(ThemeColors.onSurface(monthYearText));
        }

        if (selectedDateText != null) {
            // This is a label on a card, not a tinted control, so it reads the
            // secondary-text role. Using the raw gradient accent here put pink
            // #FF1493 on the dark card at 3.95:1, below AA for body text.
            selectedDateText.setTextColor(ThemeColors.onSurfaceVariant(selectedDateText));
        }

        if (taskNameInput != null) {
            GradientDrawable inputBg = new GradientDrawable();
            inputBg.setShape(GradientDrawable.RECTANGLE);
            inputBg.setCornerRadius(12 * getResources().getDisplayMetrics().density);
            inputBg.setColor(ThemeColors.surfaceVariant(taskNameInput));
            inputBg.setStroke(1, ThemeColors.outline(taskNameInput));
            taskNameInput.setBackground(inputBg);
            taskNameInput.setTextColor(ThemeColors.onSurface(taskNameInput));
            taskNameInput.setHintTextColor(ThemeColors.onSurfaceVariant(taskNameInput));
            taskNameInput.setHint(R.string.add_new_task);
        }

        if (tasksLabel != null) {
            tasksLabel.setTextColor(ThemeColors.onSurface(tasksLabel));
        }

        if (noTasksText != null) {
            noTasksText.setText(R.string.no_tasks);
            noTasksText.setTextColor(ThemeColors.onSurfaceVariant(noTasksText));
        }

        if (emptyStateEmoji != null) {
            emptyStateEmoji.setTextColor(ThemeColors.onSurfaceVariant(emptyStateEmoji));
        }

        // Day headers: Sunday is the accent, the rest are supporting text.
        if (calendarCard != null && calendarCard.getChildCount() >= 3) {
            LinearLayout dayHeadersLayout = (LinearLayout) calendarCard.getChildAt(1);
            String[] dayLabels = {getString(R.string.sun), getString(R.string.mon), getString(R.string.tue),
                    getString(R.string.wed), getString(R.string.thu), getString(R.string.fri),
                    getString(R.string.sat)};
            for (int i = 0; i < dayHeadersLayout.getChildCount() && i < dayLabels.length; i++) {
                TextView dayLabel = (TextView) dayHeadersLayout.getChildAt(i);
                if (dayLabel != null) {
                    dayLabel.setText(dayLabels[i]);
                    dayLabel.setTextColor(i == 0
                            ? currentAccentColor
                            : ThemeColors.onSurfaceVariant(dayLabel));
                }
            }
        }

        if (calendarCard != null && calendarCard.getChildCount() >= 6) {
            View divider = calendarCard.getChildAt(5);
            if (divider != null) {
                divider.setBackgroundColor(ThemeColors.outline(divider));
            }
        }

        if (calendarGrid != null) {
            GradientDrawable gridBg = new GradientDrawable();
            gridBg.setShape(GradientDrawable.RECTANGLE);
            gridBg.setCornerRadius(16 * getResources().getDisplayMetrics().density);
            gridBg.setColor(ThemeColors.surfaceVariant(calendarGrid));
            calendarGrid.setBackground(gridBg);
        }

        if (footerNav != null) {
            GradientDrawable footerBg = new GradientDrawable();
            footerBg.setShape(GradientDrawable.RECTANGLE);
            footerBg.setColor(ThemeColors.surface(footerNav));
            footerNav.setBackground(footerBg);

            int footerColor = ThemeColors.onSurfaceVariant(footerNav);
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

        TextView prevMonth = findViewById(R.id.prevMonth);
        TextView nextMonth = findViewById(R.id.nextMonth);
        if (prevMonth != null) prevMonth.setTextColor(currentAccentColor);
        if (nextMonth != null) nextMonth.setTextColor(currentAccentColor);

        updateAllTaskCardBackgrounds();
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
                        // Single themed drawable; the style picks the shade.
                        taskBackgroundLayout.setBackgroundResource(R.drawable.task_card_bg);
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
        currentGradientStart = gradientColors[0];
        currentGradientEnd = gradientColors[1];

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
            addTaskButton.setTextColor(ThemeColors.onGradient(gradientColors[0], gradientColors[1]));
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
            // One drawable: its solid and stroke now resolve against the active
            // theme, so the light/dark split is handled by the style itself.
            taskCardLayout.setBackgroundResource(R.drawable.task_card_bg);
        }

        taskNameText.setText(task.name);
        taskTimeText.setText(task.time);
        taskCheckbox.setChecked(task.isCompleted);

        if (task.isCompleted) {
            taskCheckbox.setButtonTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.destructive)));
            taskNameText.setPaintFlags(taskNameText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            taskNameText.setTextColor(ThemeColors.onSurfaceVariant(taskNameText));
            taskTimeText.setTextColor(ThemeColors.onSurfaceVariant(taskTimeText));
        } else {
            taskCheckbox.setButtonTintList(android.content.res.ColorStateList.valueOf(currentAccentColor));
            taskNameText.setPaintFlags(taskNameText.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));

            taskNameText.setTextColor(ThemeColors.onSurface(taskNameText));
            taskTimeText.setTextColor(ThemeColors.onSurfaceVariant(taskTimeText));
            deleteTask.setTextColor(ContextCompat.getColor(this, R.color.destructive));
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

        // The calendar is a month overview; the hour grid lives on Home. This
        // opens Home already switched to Timeline, on the day picked here.
        View timelineButton = findViewById(R.id.viewDayTimelineButton);
        if (timelineButton != null) {
            timelineButton.setOnClickListener(v -> openTimelineForSelectedDate());
        }
    }

    /** Launches Home in Timeline view for {@link #selectedDate}. */
    private void openTimelineForSelectedDate() {
        if (selectedDate == null) {
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        // CLEAR_TOP + SINGLE_TOP reuses the existing Home instance instead of
        // stacking a second one, so its day strip keeps its scroll state.
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.EXTRA_SHOW_TIMELINE, true);
        intent.putExtra(MainActivity.EXTRA_TIMELINE_DATE, dateFormat.format(selectedDate.getTime()));
        startActivity(intent);
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
        isDarkMode = ThemeManager.isDarkMode(this);
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

                dayText.setTextColor(ThemeColors.onSurface(dayText));

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
                    taskBg.setColor(currentAccentColor);
                    dayText.setBackground(taskBg);
                    dayText.setTextColor(ThemeColors.onPrimary(dayText));
                }

                if (selectedDate.get(Calendar.YEAR) == currentYear &&
                        selectedDate.get(Calendar.MONTH) == currentMonth &&
                        selectedDate.get(Calendar.DAY_OF_MONTH) == dayNumber) {
                    GradientDrawable selectedBg = new GradientDrawable();
                    selectedBg.setShape(GradientDrawable.RECTANGLE);
                    selectedBg.setCornerRadius(25);
                    selectedBg.setColor(currentAccentColor);
                    dayText.setBackground(selectedBg);
                    dayText.setTextColor(ThemeColors.onPrimary(dayText));
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