package com.example.dailytodolist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import com.example.dailytodolist.model.TimeCategory;
import com.example.dailytodolist.util.ReminderScheduler;

import java.util.Calendar;
import java.util.Locale;

/**
 * Screen for creating one task with a time block.
 *
 * <p><b>Theming.</b> Text and control tints come from theme attributes in the
 * layout; the surface, the time buttons and the primary button are painted here
 * through {@link ThemeManager}. Nothing is hardcoded, so the screen follows all
 * themes plus light and dark mode like the rest of the app.</p>
 *
 * <p><b>Strings.</b> All user-facing text is a resource, so Chinese works
 * automatically.</p>
 *
 * <p><b>Times.</b> Exchanged as minutes since midnight, the model's canonical
 * form (see {@code IntervalRules}), never as display text.</p>
 */
public class AddTaskActivity extends BaseActivity {

    /** Minute-of-day values handed back to the caller. */
    public static final String EXTRA_TITLE = "NEW_TASK_TITLE";
    public static final String EXTRA_DESCRIPTION = "NEW_TASK_DESCRIPTION";
    public static final String EXTRA_START_MINUTE = "NEW_TASK_START_MINUTE";
    public static final String EXTRA_END_MINUTE = "NEW_TASK_END_MINUTE";
    public static final String EXTRA_CATEGORY = "NEW_TASK_CATEGORY";
    public static final String EXTRA_TOP_PRIORITY = "NEW_TASK_TOP_PRIORITY";
    public static final String EXTRA_DATE_ISO = "NEW_TASK_DATE_ISO";
    /**
     * The task's permanent UUID, minted here rather than by the caller.
     *
     * <p>This matters for reminders: the alarm is addressed by this id, so the
     * task that gets stored must carry the <em>same</em> id or a later delete
     * would cancel an alarm nobody has.</p>
     */
    public static final String EXTRA_TASK_ID = "NEW_TASK_ID";

    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final int DEFAULT_START_MINUTE = 9 * 60;
    private static final int DEFAULT_END_MINUTE = 10 * 60;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 501;

    private TextInputEditText editTaskName;
    private TextInputLayout taskNameLayout;
    private TextInputEditText editDescription;
    private TextInputLayout descriptionLayout;
    private TextView btnStartTime;
    private TextView btnEndTime;
    private TextView durationText;
    private TextView btnSaveTask;
    private TextView backButton;
    private Spinner spinnerCategory;
    private CheckBox checkTopPriority;
    private LinearLayout formCard;
    private LinearLayout timeSection;
    private MaterialSwitch switchScheduleTime;
    private View mainLayout;

    private int startMinute = DEFAULT_START_MINUTE;
    private int endMinute = DEFAULT_END_MINUTE;

    /** Held while the runtime permission dialog is on screen. */
    private Intent pendingResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        bindViews();
        setupCategorySpinner();
        applyTheme();
        updateTimeLabels();
        setupListeners();
    }

    private void bindViews() {
        mainLayout = findViewById(R.id.mainLayout);
        formCard = findViewById(R.id.formCard);
        editTaskName = findViewById(R.id.editTaskName);
        taskNameLayout = findViewById(R.id.taskNameLayout);
        editDescription = findViewById(R.id.editDescription);
        descriptionLayout = findViewById(R.id.descriptionLayout);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);
        durationText = findViewById(R.id.durationText);
        btnSaveTask = findViewById(R.id.btnSaveTask);
        backButton = findViewById(R.id.backButton);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        checkTopPriority = findViewById(R.id.checkTopPriority);
        timeSection = findViewById(R.id.timeSection);
        switchScheduleTime = findViewById(R.id.switchScheduleTime);
    }

    // ------------------------------------------------------------------
    // Theming
    // ------------------------------------------------------------------

    /**
     * Applies the parts that a theme attribute cannot express: the card surface
     * and the gradient pills.
     *
     * <p>Text colours are no longer set here. They come from theme attributes in
     * the layout ({@code ?attr/colorOnSurface} and friends), which is what makes
     * light and dark mode both readable without duplicating a colour table in
     * code.</p>
     */
    private void applyTheme() {
        boolean isDarkMode = ThemeManager.isDarkMode(this);

        int[] gradient = ThemeManager.getThemeGradientColors(
                this, ThemeManager.getCurrentTheme(this));
        int accentColor = gradient[0];

        if (formCard != null) {
            formCard.setBackgroundResource(isDarkMode
                    ? R.drawable.dark_card_bg
                    : R.drawable.white_card_bg);
        }

        setPillGradient(btnStartTime, gradient);
        setPillGradient(btnEndTime, gradient);
        setPillGradient(btnSaveTask, gradient);

        if (backButton != null) {
            backButton.setTextColor(accentColor);
        }
        if (checkTopPriority != null) {
            checkTopPriority.setButtonTintList(ColorStateList.valueOf(accentColor));
        }
        if (taskNameLayout != null) {
            taskNameLayout.setBoxStrokeColor(accentColor);
            taskNameLayout.setHintTextColor(ColorStateList.valueOf(accentColor));
        }
    }

    private void setPillGradient(TextView view, int[] gradient) {
        if (view == null) {
            return;
        }
        GradientDrawable shape = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, gradient);
        shape.setCornerRadius(26 * getResources().getDisplayMetrics().density);
        view.setBackground(shape);
        view.setTextColor(getColor(R.color.white));
    }

    // ------------------------------------------------------------------
    // Category
    // ------------------------------------------------------------------

    private void setupCategorySpinner() {
        TimeCategory[] categories = TimeCategory.values();
        String[] labels = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            labels[i] = getString(categories[i].getLabelRes());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    // ------------------------------------------------------------------
    // Time
    // ------------------------------------------------------------------

    private void updateTimeLabels() {
        btnStartTime.setText(TimeCategory.formatMinuteOfDay(startMinute));
        btnEndTime.setText(TimeCategory.formatMinuteOfDay(endMinute));

        int duration = endMinute - startMinute;
        if (duration > 0) {
            durationText.setText(getString(R.string.duration_minutes, duration));
            durationText.setTextColor(ThemeHelper.getCurrentThemeAccentColor(this));
        } else {
            durationText.setText(R.string.duration_invalid);
            durationText.setTextColor(getColor(R.color.todo_delete));
        }
    }

    private void pickStartTime() {
        boolean isDarkMode = ThemeManager.isDarkMode(this);
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(startMinute / 60)
                .setMinute(startMinute % 60)
                .setTitleText(R.string.pick_start_time)
                .setTheme(pickerThemeOverlay(isDarkMode))
                .build();

        picker.addOnPositiveButtonClickListener(dialog -> {
            startMinute = picker.getHour() * 60 + picker.getMinute();
            // Keep the block valid: when the new start passes the end,
            // push the end forward by an hour instead of silently
            // leaving an unsaveable block on screen.
            if (endMinute <= startMinute) {
                endMinute = Math.min(startMinute + 60, MINUTES_PER_DAY);
            }
            updateTimeLabels();
        });

        picker.show(getSupportFragmentManager(), "start_time_picker");
    }

    private void pickEndTime() {
        boolean isDarkMode = ThemeManager.isDarkMode(this);
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(endMinute / 60)
                .setMinute(endMinute % 60)
                .setTitleText(R.string.pick_end_time)
                .setTheme(pickerThemeOverlay(isDarkMode))
                .build();

        picker.addOnPositiveButtonClickListener(dialog -> {
            endMinute = picker.getHour() * 60 + picker.getMinute();
            updateTimeLabels();
        });

        picker.show(getSupportFragmentManager(), "end_time_picker");
    }

    /**
     * Picks the time-picker overlay that matches the app's palette.
     *
     * <p>MaterialTimePicker would otherwise follow the system night mode, which
     * is not the same thing as the app's dark-mode switch: with the app in dark
     * mode on a phone set to light, the picker would come out light while the
     * screen behind it is dark.</p>
     */
    private int pickerThemeOverlay(boolean isDarkMode) {
        return isDarkMode
                ? R.style.ThemeOverlay_DailyToDoList_TimePicker_Dark
                : R.style.ThemeOverlay_DailyToDoList_TimePicker_Light;
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void setupListeners() {
        btnStartTime.setOnClickListener(v -> pickStartTime());
        btnEndTime.setOnClickListener(v -> pickEndTime());
        btnSaveTask.setOnClickListener(v -> saveTask());
        backButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        if (switchScheduleTime != null) {
            switchScheduleTime.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> setTimeSectionVisible(isChecked));
            setTimeSectionVisible(switchScheduleTime.isChecked());
        }
    }

    /**
     * Shows or hides the whole time block.
     *
     * <p>Hidden rather than disabled: a disabled control still occupies space
     * and still looks like something the user is failing to operate.</p>
     */
    private void setTimeSectionVisible(boolean visible) {
        if (timeSection != null) {
            timeSection.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /** True when the user wants a specific time block. */
    private boolean isTimeSchedulingEnabled() {
        return switchScheduleTime == null || switchScheduleTime.isChecked();
    }

    private void saveTask() {
        String title = editTaskName.getText() == null
                ? "" : editTaskName.getText().toString().trim();
        if (title.isEmpty()) {
            taskNameLayout.setError(getString(R.string.enter_task_name));
            editTaskName.requestFocus();
            return;
        }
        taskNameLayout.setError(null);

        boolean timeScheduling = isTimeSchedulingEnabled();
        if (timeScheduling && endMinute <= startMinute) {
            durationText.setText(R.string.duration_invalid);
            durationText.setTextColor(getColor(R.color.todo_delete));
            return;
        }

        int position = spinnerCategory.getSelectedItemPosition();
        TimeCategory[] categories = TimeCategory.values();
        TimeCategory category = (position >= 0 && position < categories.length)
                ? categories[position]
                : TimeCategory.OTHER;

        Intent result = new Intent();
        result.putExtra(EXTRA_TITLE, title);
        result.putExtra(EXTRA_DESCRIPTION, descriptionText());
        // "Anytime" is carried as the NO_TIME sentinel (-1), never as a
        // fabricated 00:00, so the home list and the grid can tell the two apart.
        result.putExtra(EXTRA_START_MINUTE,
                timeScheduling ? startMinute : TaskWithDate.NO_TIME);
        result.putExtra(EXTRA_END_MINUTE,
                timeScheduling ? endMinute : TaskWithDate.NO_TIME);
        result.putExtra(EXTRA_CATEGORY, category.getStorageKey());
        result.putExtra(EXTRA_TOP_PRIORITY, checkTopPriority.isChecked());
        result.putExtra(EXTRA_DATE_ISO, todayIso());

        // Arm the "starts in 5 minutes" reminder before handing the task back.
        // The id is minted here and passed along, so the caller stores the exact
        // task the alarm was set for. Anytime tasks carry NO_TIME and are
        // skipped by the scheduler.
        ReminderScheduler.ensureChannel(this);

        String dateIso = todayIso();
        int scheduledStart = timeScheduling ? startMinute : TaskWithDate.NO_TIME;
        int scheduledEnd = timeScheduling ? endMinute : TaskWithDate.NO_TIME;
        TaskWithDate pending = TaskWithDate.fromMinuteOfDay(
                title, descriptionText(), dateIso, scheduledStart, scheduledEnd,
                checkTopPriority.isChecked(), category.getStorageKey());
        if (pending != null) {
            result.putExtra(EXTRA_TASK_ID, pending.getId());
            if (!pending.isAnytime()) {
                ReminderScheduler.schedule(this, pending);
            }
        }

        // The task is saved with or without the notification permission: an
        // ungranted permission only means the reminder is invisible, and
        // dropping the user's task over it would be a worse trade.
        if (needsNotificationPermission()) {
            pendingResult = result;
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
            return;
        }

        finishWithResult(result);
    }

    /** True when this build needs POST_NOTIFICATIONS and it is not granted. */
    private boolean needsNotificationPermission() {
        return TaskReminderReceiver.requiresRuntimePermission()
                && androidx.core.content.ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @androidx.annotation.NonNull String[] permissions,
                                           @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_NOTIFICATION_PERMISSION) {
            return;
        }
        // Finish on either outcome: the permission is a courtesy, not a
        // precondition for saving the task.
        Intent result = pendingResult;
        pendingResult = null;
        if (result != null) {
            finishWithResult(result);
        }
    }

    private void finishWithResult(Intent result) {
        setResult(RESULT_OK, result);
        finish();
    }

    /** Description text, or empty when the field is blank. */
    private String descriptionText() {
        if (editDescription == null || editDescription.getText() == null) {
            return "";
        }
        return editDescription.getText().toString().trim();
    }

    private String todayIso() {
        Calendar cal = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
    }
}
