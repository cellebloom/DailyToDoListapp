package com.example.dailytodolist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import com.example.dailytodolist.model.TimeCategory;
import com.example.dailytodolist.model.TimelineEdits;
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
    private MaterialButton btnSelectDate;
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

    /**
     * Selected date as {@code yyyy-MM-dd}. Defaults to today, so behaviour is
     * unchanged until the user picks another day. MaterialDatePicker returns a
     * UTC millis instant, which is converted back with a UTC calendar so the
     * picked calendar day is stable across time zones.
     */
    private String selectedDateIso;

    /** Held while the runtime permission dialog is on screen. */
    private Intent pendingResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        bindViews();
        setupCategorySpinner();
        applyTheme();
        selectedDateIso = todayIso();
        updateDateButton();
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
        btnSelectDate = findViewById(R.id.btnSelectDate);
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

        int[] gradient = GradientHelper.colorsFor(this);
        int accentColor = gradient[0];

        if (formCard != null) {
            formCard.setBackgroundResource(isDarkMode
                    ? R.drawable.dark_card_bg
                    : R.drawable.white_card_bg);
        }

        // Same helper the home screen's Add Task button uses, so the two buttons
        // cannot drift apart again.
        GradientHelper.applyPill(btnStartTime, this, gradient);
        GradientHelper.applyPill(btnEndTime, this, gradient);
        GradientHelper.applyPill(btnSaveTask, this, gradient);

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

    /** Opens MaterialDatePicker, defaulting its selection to the current date. */
    private void pickDate() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setSelection(utcMillisForIso(selectedDateIso))
                .setTheme(datePickerThemeOverlay(ThemeManager.isDarkMode(this)))
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }
            selectedDateIso = isoForUtcMillis(selection);
            updateDateButton();
        });
        picker.show(getSupportFragmentManager(), "task_date_picker");
    }

    /** Shows "Today" for the current date, otherwise the picked ISO date. */
    private void updateDateButton() {
        if (btnSelectDate == null) {
            return;
        }
        btnSelectDate.setText(selectedDateIso.equals(todayIso())
                ? getString(R.string.today) : selectedDateIso);
    }

    /**
     * Date-picker overlay matching the app palette. MaterialDatePicker otherwise
     * follows the system night mode, which can disagree with the app's own
     * light/dark choice (the same problem the time pickers solve).
     */
    private int datePickerThemeOverlay(boolean isDarkMode) {
        return isDarkMode
                ? R.style.ThemeOverlay_DailyToDoList_DatePicker_Dark
                : R.style.ThemeOverlay_DailyToDoList_DatePicker_Light;
    }

    /** Converts {@code yyyy-MM-dd} to the UTC-millis instant the picker expects. */
    private static long utcMillisForIso(String dateIso) {
        Calendar utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(Integer.parseInt(dateIso.substring(0, 4)),
                Integer.parseInt(dateIso.substring(5, 7)) - 1,
                Integer.parseInt(dateIso.substring(8, 10)));
        return utc.getTimeInMillis();
    }

    /** Converts the picker's UTC-millis instant back to a local ISO date. */
    private static String isoForUtcMillis(long millis) {
        Calendar utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(millis);
        return String.format(Locale.US, "%04d-%02d-%02d",
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH) + 1,
                utc.get(Calendar.DAY_OF_MONTH));
    }

    /** Adds one calendar day to an ISO date, used by the past-time fallback. */
    private static String plusOneDay(String dateIso) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Integer.parseInt(dateIso.substring(0, 4)),
                Integer.parseInt(dateIso.substring(5, 7)) - 1,
                Integer.parseInt(dateIso.substring(8, 10)));
        cal.add(Calendar.DAY_OF_MONTH, 1);
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
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
        if (btnSelectDate != null) {
            btnSelectDate.setOnClickListener(v -> pickDate());
        }
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

        // Edge case: the user picked Today but a time that has already gone.
        // Warn, and on confirmation move the date forward one day instead of
        // saving a block that would instantly read as "past" on the timeline.
        String dateIso = selectedDateIso;
        if (timeScheduling && dateIso.equals(todayIso())
                && TimelineEdits.isPast(dateIso, endMinute, System.currentTimeMillis())) {
            Toast.makeText(this, R.string.past_time_warning, Toast.LENGTH_LONG).show();
            final String tomorrow = plusOneDay(dateIso);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.select_date)
                    .setMessage(R.string.past_time_warning)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.save_task,
                            (dialog, which) -> {
                                selectedDateIso = tomorrow;
                                updateDateButton();
                                Toast.makeText(this, R.string.past_time_rescheduled,
                                        Toast.LENGTH_SHORT).show();
                                finishTask(title, category, timeScheduling, tomorrow);
                            })
                    .show();
            return;
        }

        finishTask(title, category, timeScheduling, dateIso);
    }

    /** Builds the result intent, arms the reminder, and returns to the caller. */
    private void finishTask(String title, TimeCategory category,
                            boolean timeScheduling, String dateIso) {
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
        result.putExtra(EXTRA_DATE_ISO, dateIso);

        // Arm the "starts in 5 minutes" reminder before handing the task back.
        // The id is minted here and passed along, so the caller stores the exact
        // task the alarm was set for. Anytime tasks carry NO_TIME and are
        // skipped by the scheduler.
        ReminderScheduler.ensureChannel(this);

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
            showPermissionRationale();
            return;
        }

        finishWithResult(result);
    }

    /**
     * Explains why the app wants notifications <em>before</em> the OS prompt.
     *
     * <p><b>Why a pre-dialog at all.</b> Android 13+ only lets an app raise the
     * POST_NOTIFICATIONS prompt twice; after a second denial it never appears
     * again and the user has to dig through system settings. Spending one of
     * those two scarce prompts on someone who has not been told what the
     * permission buys is a one-way loss, so the rationale is shown first and the
     * system prompt is raised only on an explicit Allow.</p>
     *
     * <p>Every exit path — Allow, Don't Allow, the X, or dismissing by tapping
     * outside — ends in {@link #finishWithResult}, because the task itself is
     * already built and must never be dropped over a courtesy permission.</p>
     */
    private void showPermissionRationale() {
        View content = getLayoutInflater().inflate(R.layout.dialog_permission, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                // The layout supplies its own rounded MaterialCardView, so the
                // dialog window must not paint a second background behind it.
                .setBackgroundInsetStart(0)
                .setBackgroundInsetEnd(0)
                .setView(content);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            android.graphics.Color.TRANSPARENT));
        }

        MaterialButton allow = content.findViewById(R.id.btnPermissionAllow);
        MaterialButton deny = content.findViewById(R.id.btnPermissionDeny);
        View close = content.findViewById(R.id.btnPermissionClose);

        // The Allow button carries the app's live theme gradient, matching every
        // other primary action in the app.
        GradientHelper.applyPill(allow, this, GradientHelper.colorsFor(this));

        allow.setOnClickListener(v -> {
            dialog.dismiss();
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        });

        // Both refusals save the task without ever touching the OS prompt, which
        // preserves the remaining system-dialog allowance for a later attempt.
        deny.setOnClickListener(v -> {
            dialog.dismiss();
            finishPendingWithoutPermission();
        });
        close.setOnClickListener(v -> {
            dialog.dismiss();
            finishPendingWithoutPermission();
        });

        // Back press / outside tap is also a refusal, not a hang.
        dialog.setOnCancelListener(d -> finishPendingWithoutPermission());

        dialog.show();
    }

    /** Completes the save when the user declines the notification prompt. */
    private void finishPendingWithoutPermission() {
        Intent result = pendingResult;
        pendingResult = null;
        if (result != null) {
            finishWithResult(result);
        }
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
