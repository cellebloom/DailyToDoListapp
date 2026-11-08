package com.example.dailytodolist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.example.dailytodolist.model.WeekDay;
import com.example.dailytodolist.util.TaskStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    private TextView dateText;
    private TextView addTaskButton;
    private LinearLayout tasksListLayout;
    private LinearLayout noTasksLayout;

    private TextView quoteText, quoteAuthor;
    private TextView editQuoteButton;

    private LinearLayout navHome, navCalendar, navShop, navThemes, navProfile;
    private LinearLayout mainLayout;

    // View toggle: List (existing cards) vs Timeline (scheduled blocks).
    private MaterialButtonToggleGroup viewToggleGroup;
    private RecyclerView timelineRecyclerView;
    private TimelineAdapter timelineAdapter;
    private LinearLayout backlogSection;
    private LinearLayout backlogContainer;
    private LinearLayout prioritiesSection;
    private LinearLayout prioritiesContainer;
    private TextView timelineEmptyHint;
    private RecyclerView daySelectorRecyclerView;
    private DaySelectorAdapter daySelectorAdapter;
    /** Date the timeline is currently showing; defaults to today. */
    private String selectedDateIso;
    private boolean isTimelineView = false;

    // CHANGED: Using TaskWithDate instead of String
    private ArrayList<TaskWithDate> tasks;
    private SharedPreferences sharedPreferences;
    private boolean isDarkMode = false;

    // NEW: Launcher to receive the task from AddTaskActivity
    private ActivityResultLauncher<Intent> addTaskLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
        isDarkMode = ThemeManager.isDarkMode(this);

        // NEW: Setup the launcher
        addTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String title = data.getStringExtra(AddTaskActivity.EXTRA_TITLE);
                        String category = data.getStringExtra(AddTaskActivity.EXTRA_CATEGORY);
                        String dateIso = data.getStringExtra(AddTaskActivity.EXTRA_DATE_ISO);
                        int startMinute = data.getIntExtra(
                                AddTaskActivity.EXTRA_START_MINUTE, TaskWithDate.NO_TIME);
                        int endMinute = data.getIntExtra(
                                AddTaskActivity.EXTRA_END_MINUTE, TaskWithDate.NO_TIME);
                        boolean isTop = data.getBooleanExtra(
                                AddTaskActivity.EXTRA_TOP_PRIORITY, false);

                        if (title != null && !title.trim().isEmpty() && dateIso != null) {
                            String description = data.getStringExtra(
                                    AddTaskActivity.EXTRA_DESCRIPTION);
                            TaskWithDate newTask = TaskWithDate.fromMinuteOfDay(
                                    title, description, dateIso, startMinute, endMinute,
                                    isTop, category);
                            tasks.add(newTask);
                            saveTasks();
                            displayTasks();
                            Toast.makeText(this, R.string.task_added_message, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        initViews();
        setCurrentDate();
        loadTasks();
        displayTasks();
        bindPriorities();
        loadQuote();
        giveDailyCoins();
        applyAllThemes();
        setupClickListeners();
        setupViewToggle();
        applyDarkMode();

        TextView titleText = findViewById(R.id.titleText);
        if (titleText != null) {
            titleText.setText(R.string.app_name);
        }
    }

    private void initViews() {
        dateText = findViewById(R.id.dateText);
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

        viewToggleGroup = findViewById(R.id.viewToggleGroup);
        timelineRecyclerView = findViewById(R.id.timelineRecyclerView);
        backlogSection = findViewById(R.id.backlogSection);
        backlogContainer = findViewById(R.id.backlogContainer);
        prioritiesSection = findViewById(R.id.prioritiesSection);
        prioritiesContainer = findViewById(R.id.prioritiesContainer);
        timelineEmptyHint = findViewById(R.id.timelineEmptyHint);
        daySelectorRecyclerView = findViewById(R.id.daySelectorRecyclerView);
        selectedDateIso = todayIso();
    }

    // ------------------------------------------------------------------
    // Top 3 Priorities
    // ------------------------------------------------------------------

    /**
     * Renders up to three tasks flagged as top priority for today.
     *
     * <p>Hidden entirely when none qualify: an empty "Top 3 Priorities" heading
     * would read as a broken section rather than an absence of priorities.</p>
     */
    private void bindPriorities() {
        if (prioritiesSection == null || prioritiesContainer == null) {
            return;
        }

        prioritiesContainer.removeAllViews();
        List<TaskWithDate> priorities = loadTopPriorities();
        if (priorities.isEmpty()) {
            prioritiesSection.setVisibility(View.GONE);
            return;
        }

        prioritiesSection.setVisibility(View.VISIBLE);

        int accentColor = ThemeHelper.getCurrentThemeAccentColor(this);

        for (TaskWithDate task : priorities) {
            View chip = getLayoutInflater().inflate(
                    R.layout.item_priority_task, prioritiesContainer, false);

            TextView star = chip.findViewById(R.id.priorityStar);
            TextView title = chip.findViewById(R.id.priorityTitle);
            TextView time = chip.findViewById(R.id.priorityTime);

            title.setText(task.getTaskName());
            star.setTextColor(accentColor);

            // Anytime tasks show their category instead of a clock time, so the
            // chip never shows a fabricated interval.
            if (task.isAnytime()) {
                time.setText(task.getTimeCategory().getLabelRes());
            } else {
                time.setText(task.getTimeRangeLabel());
                time.setTextColor(getColor(task.getTimeCategory().getColorRes()));
            }

            if (chip instanceof com.google.android.material.card.MaterialCardView) {
                ((com.google.android.material.card.MaterialCardView) chip)
                        .setStrokeColor(accentColor);
            }

            prioritiesContainer.addView(chip);
        }
    }

    /**
     * Today's top-priority tasks, capped at three.
     *
     * <p>Capped because the section is called "Top 3": showing a fourth would
     * quietly change what the section means.</p>
     */
    private List<TaskWithDate> loadTopPriorities() {
        List<TaskWithDate> priorities = new ArrayList<>();
        if (tasks == null) {
            return priorities;
        }
        String today = todayIso();
        for (TaskWithDate task : tasks) {
            if (priorities.size() >= MAX_TOP_PRIORITIES) {
                break;
            }
            if (task != null && task.isTopPriority() && today.equals(task.getDateIso())) {
                priorities.add(task);
            }
        }
        return priorities;
    }

    /** The section is called "Top 3", so never render more than three. */
    private static final int MAX_TOP_PRIORITIES = 3;

    // ------------------------------------------------------------------
    // List / Timeline toggle
    // ------------------------------------------------------------------

    private void setupViewToggle() {
        if (timelineRecyclerView != null) {
            timelineAdapter = new TimelineAdapter(this);
            timelineRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            timelineRecyclerView.setAdapter(timelineAdapter);
        }

        setupDaySelector();

        if (viewToggleGroup == null) {
            return;
        }

        viewToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            // The listener fires twice per change (once for the button losing
            // selection), so only react to the newly checked button.
            if (!isChecked) {
                return;
            }
            setTimelineView(checkedId == R.id.btnTimelineView);
        });
    }

    /**
     * Builds the horizontal week strip above the grid.
     *
     * <p>It is always laid out Monday-first and rebuilt around the selected
     * date, so tapping a day at the edge of the week keeps the strip aligned
     * with what is actually on screen rather than silently scrolling away.</p>
     */
    private void setupDaySelector() {
        if (daySelectorRecyclerView == null) {
            return;
        }

        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        daySelectorAdapter = new DaySelectorAdapter(this, gradientColors[0]);

        daySelectorRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        daySelectorRecyclerView.setAdapter(daySelectorAdapter);

        daySelectorAdapter.setOnDaySelectedListener(dateIso -> {
            selectedDateIso = dateIso;
            daySelectorAdapter.setSelectedIso(dateIso);
            renderTimelineForSelectedDate();
        });

        bindDaySelector();
    }

    /** Rebuilds the week strip around {@link #selectedDateIso}. */
    private void bindDaySelector() {
        if (daySelectorAdapter == null) {
            return;
        }
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        daySelectorAdapter.setAccentColor(gradientColors[0]);
        daySelectorAdapter.submit(
                WeekDay.weekOf(selectedDateIso, todayIso()), selectedDateIso);
    }

    /**
     * Loads the grid, backlog and hint for whichever date is selected.
     *
     * <p>Split out from {@link #setTimelineView(boolean)} so tapping a day does
     * not re-run the list/timeline visibility work.</p>
     */
    private void renderTimelineForSelectedDate() {
        String dateIso = selectedDateIso == null ? todayIso() : selectedDateIso;
        List<TaskWithDate> scheduled = TaskStore.from(this).loadScheduledForDate(dateIso);
        List<TaskWithDate> anytime = loadAnytimeForDate(dateIso);

        if (timelineAdapter != null) {
            timelineAdapter.submit(scheduled);
        }
        bindBacklog(anytime);

        // The grid is a fixed 06:00–23:00 frame, so on an empty day it is 18
        // rows of nothing, which reads as a failed load. One line of muted text
        // explains it; the grid stays visible so the structure is still there.
        // The wording follows the selected day: "today" is only true of today.
        if (timelineEmptyHint != null) {
            if (scheduled.isEmpty()) {
                boolean isToday = dateIso.equals(todayIso());
                timelineEmptyHint.setText(isToday
                        ? R.string.no_tasks_scheduled_hint
                        : R.string.no_tasks_scheduled_for_day);
                timelineEmptyHint.setVisibility(View.VISIBLE);
            } else {
                timelineEmptyHint.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Switches between the card list and the timeline.
     *
     * <p>The timeline shows two things: the hour grid of scheduled tasks, and a
     * backlog strip of Anytime tasks. Anytime tasks have no interval, so they
     * cannot be positioned on an axis and are listed above it instead.</p>
     */
    private void setTimelineView(boolean showTimeline) {
        // Only reset to today when the user actually enters the Timeline view.
        // refreshCurrentView() also calls this while already in Timeline (after
        // adding or deleting a task); resetting there would silently throw away
        // the day the user had picked.
        boolean wasTimeline = isTimelineView;
        isTimelineView = showTimeline;

        if (showTimeline) {
            if (tasksListLayout != null) {
                tasksListLayout.setVisibility(View.GONE);
            }
            if (noTasksLayout != null) {
                noTasksLayout.setVisibility(View.GONE);
            }
            if (timelineRecyclerView != null) {
                timelineRecyclerView.setVisibility(View.VISIBLE);
            }
            if (daySelectorRecyclerView != null) {
                daySelectorRecyclerView.setVisibility(View.VISIBLE);
            }

            // Entering Timeline always starts on today, so the strip and the
            // grid never disagree about which day is on screen. While already
            // in Timeline, the picked day is preserved instead.
            if (!wasTimeline) {
                selectedDateIso = todayIso();
            }
            bindDaySelector();
            renderTimelineForSelectedDate();
        } else {
            if (timelineRecyclerView != null) {
                timelineRecyclerView.setVisibility(View.GONE);
            }
            if (daySelectorRecyclerView != null) {
                daySelectorRecyclerView.setVisibility(View.GONE);
            }
            if (timelineEmptyHint != null) {
                timelineEmptyHint.setVisibility(View.GONE);
            }
            if (backlogSection != null) {
                backlogSection.setVisibility(View.GONE);
            }
            TextView empty = findViewById(R.id.noTasksText);
            if (empty != null) {
                empty.setText(R.string.no_tasks_today);
            }
            displayTasks();
        }
    }

    /**
     * Fills the backlog strip with the day's Anytime tasks.
     *
     * <p>Hidden entirely when there are none, so an empty "Anytime" heading does
     * not sit above the grid looking like a loading state.</p>
     */
    private void bindBacklog(List<TaskWithDate> anytime) {
        if (backlogSection == null || backlogContainer == null) {
            return;
        }

        backlogContainer.removeAllViews();
        if (anytime == null || anytime.isEmpty()) {
            backlogSection.setVisibility(View.GONE);
            return;
        }

        backlogSection.setVisibility(View.VISIBLE);
        for (TaskWithDate task : anytime) {
            View chip = getLayoutInflater().inflate(R.layout.item_backlog_task, backlogContainer, false);

            TextView title = chip.findViewById(R.id.backlogTitle);
            TextView category = chip.findViewById(R.id.backlogCategory);

            title.setText(task.getTaskName());
            category.setText(task.getTimeCategory().getLabelRes());
            category.setTextColor(getColor(task.getTimeCategory().getColorRes()));

            backlogContainer.addView(chip);
        }
    }

    /** Anytime tasks for one date, from the fully loaded task list. */
    private List<TaskWithDate> loadAnytimeForDate(String dateIso) {
        List<TaskWithDate> anytime = new ArrayList<>();
        if (tasks == null) {
            return anytime;
        }
        for (TaskWithDate task : tasks) {
            if (task != null && task.isAnytime() && dateIso.equals(task.getDateIso())) {
                anytime.add(task);
            }
        }
        return anytime;
    }

    /** Today as {@code yyyy-MM-dd}. */
    private String todayIso() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    /**
     * Re-renders whichever view is currently selected.
     *
     * <p>The priorities strip is shared by both views, so it is refreshed here
     * rather than inside either branch.</p>
     */
    private void refreshCurrentView() {
        bindPriorities();
        if (isTimelineView) {
            setTimelineView(true);
        } else {
            displayTasks();
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
        isDarkMode = ThemeManager.isDarkMode(this);

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

    // CHANGED: Loads through TaskStore, which is keyed by permanent task id.
    private void loadTasks() {
        TaskStore store = TaskStore.from(this);
        tasks = new ArrayList<>(store.loadAll());
    }

    // CHANGED: Saves through TaskStore. Deleting one task can no longer shift
    // another task's stored time, because nothing is addressed by position.
    private void saveTasks() {
        TaskStore.from(this).saveAll(tasks);
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

    // CHANGED: Now accepts TaskWithDate and shows the time text
    private void addTaskToView(TaskWithDate task, int position) {
        View taskView = getLayoutInflater().inflate(R.layout.task_item, tasksListLayout, false);

        androidx.cardview.widget.CardView cardView = (androidx.cardview.widget.CardView) taskView;

        CheckBox taskCheckbox = taskView.findViewById(R.id.taskCheckbox);
        TextView taskText = taskView.findViewById(R.id.taskText);
        TextView deleteTask = taskView.findViewById(R.id.deleteTask);
        TextView taskTimeText = taskView.findViewById(R.id.taskTimeText); // NEW
        TextView taskDescriptionText = taskView.findViewById(R.id.taskDescriptionText);

        taskText.setText(task.getTaskName());
        taskCheckbox.setChecked(task.isChecked());

        // NEW: Show the time block and apply the category colour.
        // An "Anytime" task has no interval at all, so the row is hidden
        // entirely rather than left blank or filled with a placeholder time.
        if (taskTimeText != null) {
            if (task.isAnytime()) {
                taskTimeText.setVisibility(View.GONE);
            } else {
                taskTimeText.setVisibility(View.VISIBLE);
                taskTimeText.setText(task.getTimeRangeLabel());
                taskTimeText.setTextColor(getColor(task.getTimeCategory().getColorRes()));
            }
        }

        // Description subtitle: only when the task actually has one.
        if (taskDescriptionText != null) {
            if (task.hasDescription()) {
                taskDescriptionText.setVisibility(View.VISIBLE);
                taskDescriptionText.setText(task.getDescription());
            } else {
                taskDescriptionText.setVisibility(View.GONE);
            }
        }

        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);
        int accentColor = gradientColors[0];

        if (task.isChecked()) {
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
            boolean wasCompleted = task.isChecked();
            task.setChecked(taskCheckbox.isChecked());
            saveTasks();

            if (taskCheckbox.isChecked() && !wasCompleted) {
                SharedPreferences prefs = getSharedPreferences("TodoAppPrefs", MODE_PRIVATE);
                int currentCoins = prefs.getInt("user_coins", 0);
                prefs.edit().putInt("user_coins", currentCoins + 5).apply();
                Toast.makeText(this, getString(R.string.coins_earned_main, 5), Toast.LENGTH_SHORT).show();
            }

            refreshCurrentView();
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

        taskToDelete.setText(tasks.get(position).getTaskName());

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
            saveTasks();
            refreshCurrentView();
            bottomSheetDialog.dismiss();
            Toast.makeText(this, R.string.task_deleted_message, Toast.LENGTH_SHORT).show();
        });

        cancelDelete.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    // REMOVED: addNewTask() is gone because the new page handles it.

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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
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
        // CHANGED: This now opens the new page instead of typing in the bottom bar
        addTaskButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            addTaskLauncher.launch(intent);
        });

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

        View openReview = findViewById(R.id.openReviewButton);
        if (openReview != null) {
            openReview.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, ReviewActivity.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDarkMode = ThemeManager.isDarkMode(this);
        loadTasks();
        refreshCurrentView();
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