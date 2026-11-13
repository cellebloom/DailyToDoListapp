package com.example.dailytodolist;

import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
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
import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.example.dailytodolist.model.DailyReview;
import com.example.dailytodolist.model.IntervalRules;
import com.example.dailytodolist.model.TimeCategory;
import com.example.dailytodolist.model.TimelineEdits;
import com.example.dailytodolist.model.WeekDay;
import com.example.dailytodolist.util.AutoScheduleMapper;
import com.example.dailytodolist.util.ReminderScheduler;
import com.example.dailytodolist.util.ReviewStore;
import com.example.dailytodolist.util.TaskStore;
import com.example.dailytodolist.util.ThemeColors;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {

    /** Set by the Calendar month view: open directly in the Timeline. */
    public static final String EXTRA_SHOW_TIMELINE = "SHOW_TIMELINE";
    /** {@code yyyy-MM-dd} day the Timeline should open on. */
    public static final String EXTRA_TIMELINE_DATE = "TIMELINE_DATE";

    private TextView dateText;
    /** Opens the Add Task page; full-width button since the inline field was removed. */
    private MaterialButton addTaskButton;
    private LinearLayout tasksListLayout;
    private LinearLayout noTasksLayout;

    private TextView quoteText, quoteAuthor;
    private TextView editQuoteButton;
    /** Opens the end-of-day review; painted with the theme gradient. */
    private TextView openReviewButton;

    private LinearLayout navHome, navCalendar, navShop, navThemes, navProfile;
    private LinearLayout mainLayout;

    // View toggle: List (existing cards) vs Timeline (scheduled blocks).
    private MaterialButtonToggleGroup viewToggleGroup;
    private RecyclerView timelineRecyclerView;
    private TimelineAdapter timelineAdapter;
    /** Attached to the hour grid; drives long-press-to-drag on task blocks. */
    private ItemTouchHelper timelineItemTouchHelper;
    /** The callback instance, needed to seed a manual drag before startDrag(). */
    private TimelineDragCallback timelineDragCallback;
    private LinearLayout backlogSection;
    private LinearLayout backlogContainer;
    private LinearLayout prioritiesSection;
    private LinearLayout prioritiesContainer;
    private TextView timelineEmptyHint;
    private RecyclerView daySelectorRecyclerView;
    private DaySelectorAdapter daySelectorAdapter;
    /** Table header above the grid: TIME column + selected day name. */
    private LinearLayout timelineHeaderRow;
    private TextView timelineHeaderDay;
    /** Floating add button shown only in Timeline view. */
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddTask;
    /** Date the timeline is currently showing; defaults to today. */
    private String selectedDateIso;
    private boolean isTimelineView = false;

    /** AI auto-schedule trigger; hidden when the day has no Anytime tasks. */
    private MaterialButton btnAutoSchedule;
    /** Opens the "DailyToDo-AI" Q&A chat from the Home header. */
    private MaterialButton btnAskAi;
    /**
     * Serial worker for the AI request. {@link AIService#getSchedule} waits on
     * network I/O, so it can never run on the main thread.
     */
    private ExecutorService aiExecutor;
    /** True while a request is in flight, so the button cannot double-fire. */
    private boolean aiRequestInFlight = false;

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

        // Create the reminder channel up front so the OS shows a valid channel
        // even if the first notification is posted from a receiver after a
        // reboot, when no activity has run yet.
        ReminderScheduler.ensureChannel(this);

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
                            // Adopt the id AddTaskActivity already used to arm the
                            // reminder. Without this the stored task would carry a
                            // different uuid and the alarm could never be cancelled.
                            String pendingId = data.getStringExtra(AddTaskActivity.EXTRA_TASK_ID);
                            if (pendingId != null) {
                                newTask.restoreId(pendingId);
                            }
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

        // A "View This Day's Timeline" jump from the Calendar month view.
        applyTimelineIntent(getIntent());
    }

    /**
     * Honours a request to open straight into the Timeline on a given day.
     *
     * <p>The date is validated before use: an extra arriving from another
     * screen must never put the grid on an unparseable day.</p>
     */
    private void applyTimelineIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_SHOW_TIMELINE, false)) {
            return;
        }
        String dateIso = intent.getStringExtra(EXTRA_TIMELINE_DATE);
        if (IntervalRules.isValidDateIso(dateIso)) {
            selectedDateIso = dateIso;
        }
        // Consume the extras so a later onNewIntent without them is a no-op.
        intent.removeExtra(EXTRA_SHOW_TIMELINE);
        intent.removeExtra(EXTRA_TIMELINE_DATE);
        switchToTimeline();
    }

    /** Selects the Timeline toggle without firing the listener twice. */
    private void switchToTimeline() {
        if (viewToggleGroup == null) {
            return;
        }
        if (viewToggleGroup.getCheckedButtonId() != R.id.btnTimelineView) {
            viewToggleGroup.check(R.id.btnTimelineView);
        }
        // setTimelineView(true) resets to today unless it is already Timeline;
        // restore the requested day afterwards so the jump lands where asked.
        String wanted = selectedDateIso;
        setTimelineView(true);
        if (IntervalRules.isValidDateIso(wanted)) {
            selectedDateIso = wanted;
            bindDaySelector();
            renderTimelineForSelectedDate();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Home is reused via CLEAR_TOP|SINGLE_TOP, so this is where a second
        // Calendar jump lands rather than onCreate.
        setIntent(intent);
        applyTimelineIntent(intent);
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
        openReviewButton = findViewById(R.id.openReviewButton);

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
        timelineHeaderRow = findViewById(R.id.timelineHeaderRow);
        timelineHeaderDay = findViewById(R.id.timelineHeaderDay);
        fabAddTask = findViewById(R.id.fabAddTask);
        btnAutoSchedule = findViewById(R.id.btnAutoSchedule);
        btnAskAi = findViewById(R.id.btnAskAi);
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
                time.setTextColor(getColor(task.getTimeCategory().getTextColorRes()));
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
    // Timeline interactions: past tasks and drag-to-reschedule
    // ------------------------------------------------------------------

    /**
     * Wires UP/DOWN dragging on the hour grid.
     *
     * <p><b>Why the rows move but the tasks do not.</b> {@link ItemTouchHelper}
     * drags whole hour <em>rows</em>, yet what must move is one task <em>block</em>
     * inside a row: the row is chrome, the block is data. Reordering rows would
     * animate a meaningless shuffle that the next {@code submit} would undo. So the
     * rows are used only as the drag <em>axis</em> (they are what the user reads as
     * hours) and the hour the finger ends on is applied to the task the gesture
     * started from.</p>
     *
     * <p><b>Where the drag starts.</b> Not by automatic long-press: a long-press
     * on a block starts a drag from exactly that block (see
     * {@link #startTimelineDrag}), so every task — past or future — is draggable.
     * Deletion moved to a left/right swipe, which cannot collide with a drag.</p>
     *
     * <p><b>Empty slots.</b> {@code canDropOver} refuses any row without a task, so
     * a blank hour is never a drop target and can never be picked up.</p>
     */
    private void setupTimelineDrag() {
        if (timelineRecyclerView == null) {
            return;
        }

        timelineDragCallback = new TimelineDragCallback(this);
        timelineItemTouchHelper = new ItemTouchHelper(timelineDragCallback);
        timelineItemTouchHelper.attachToRecyclerView(timelineRecyclerView);
    }

    /**
     * Drag behaviour for the hour grid.
     *
     * <p>An inner class rather than an anonymous one so
     * {@link #beginDrag(TaskWithDate, int)} has a visible name and so the
     * per-gesture state ({@code dragging}, {@code fromHour}, {@code pendingHour})
     * lives somewhere a reader can find it.</p>
     *
     * <p>The adapter itself is <em>not</em> reordered: rows are a fixed 06:00–23:00
     * frame, and swapping them would detach each label from the hour it names.
     * {@link #onMove} only records where the finger is, and
     * {@link #clearView} turns that into a delta in hours.</p>
     */
    private final class TimelineDragCallback extends ItemTouchHelper.SimpleCallback {

        private final MainActivity host;

        /** Task the current gesture moves; null when no drag is active. */
        private TaskWithDate dragging;
        /** Hour the last committed move ended on; the base for the next delta. */
        private int fromHour = -1;
        /** Hour the gesture first touched, so a no-op drag can be told apart. */
        private int gestureStartHour = -1;
        /** Hour the finger is currently over. */
        private int pendingHour = -1;

        TimelineDragCallback(MainActivity host) {
            // UP/DOWN drag on every row; LEFT/RIGHT swipe to delete. The two
            // gestures are independent, so a dimmed (past) block still drags.
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            this.host = host;
        }

        /** Seeds the gesture. Must run before {@code ItemTouchHelper.startDrag()}. */
        void beginDrag(TaskWithDate task, int hour) {
            this.dragging = task;
            this.fromHour = hour;
            this.gestureStartHour = hour;
            this.pendingHour = -1;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            // Automatic long-press drag stays off: the host starts the drag from
            // the specific block the user grabbed, so an empty row can never be
            // picked up. Past blocks are draggable too — a missed task is
            // usually rescheduled, not deleted.
            return false;
        }

        @Override
        public boolean canDropOver(@NonNull RecyclerView recyclerView,
                                   @NonNull RecyclerView.ViewHolder current,
                                   @NonNull RecyclerView.ViewHolder target) {
            // Every hour is a legal drop target, including empty ones: moving a
            // task from a busy hour into a blank hour is the whole point of
            // rescheduling. "Empty slots are not draggable" is enforced on the
            // source side instead -- isLongPressDragEnabled is false and the host
            // only calls startDrag() from a block's long-press, so an empty row
            // can never be picked up.
            return timelineAdapter != null;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            if (timelineAdapter == null) {
                return false;
            }
            int hour = timelineAdapter.hourAt(target.getBindingAdapterPosition());
            if (hour < 0) {
                return false;
            }
            // Commit the move here, not in clearView(): clearView() runs after the
            // gesture ends, so a row that had not been persisted would animate back
            // to where it started ("snapping back"). Persisting on every row change
            // keeps storage and the grid in step during the drag itself.
            pendingHour = hour;
            if (dragging != null && fromHour >= 0) {
                host.applyDraggedHour(dragging, hour - fromHour);
                // The following move starts from the committed hour.
                fromHour = hour;
            }
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // Swipe = delete request. Resolve the task by row position through
            // the adapter (a row holds at most one first task), then ask before
            // removing it. Reset the row so a cancelled dialog does not leave a
            // half-translated block on screen.
            int position = viewHolder.getBindingAdapterPosition();
            TaskWithDate task = (timelineAdapter == null || position < 0)
                    ? null : timelineAdapter.taskAt(position);
            if (task != null) {
                host.confirmDeleteTask(task);
            }
            // Restore the row immediately; deletion (if confirmed) re-renders.
            if (timelineAdapter != null && position >= 0) {
                timelineAdapter.notifyItemChanged(position);
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            // The move was already applied and persisted in onMove(). Only the
            // gesture state is reset here; re-applying the delta would shift the
            // task a second time. The grid is re-submitted so each row reads
            // from the committed times instead of the transient drag positions.
            boolean moved = gestureStartHour >= 0 && fromHour != gestureStartHour;
            dragging = null;
            fromHour = -1;
            gestureStartHour = -1;
            pendingHour = -1;
            if (moved) {
                host.renderTimelineForSelectedDate();
                host.bindPriorities();
            }
        }
    }

    /**
     * Starts dragging the row that holds {@code blockView}.
     *
     * <p>The block is a child of the row, so the row's own
     * {@link RecyclerView.ViewHolder} has to be found before
     * {@link ItemTouchHelper#startDrag} can be called.</p>
     */
    private void startTimelineDrag(TaskWithDate task, View blockView) {
        if (timelineRecyclerView == null || timelineItemTouchHelper == null) {
            return;
        }
        RecyclerView.ViewHolder rowHolder = timelineRecyclerView
                .findContainingViewHolder(blockView);
        if (rowHolder == null || rowHolder.getBindingAdapterPosition() == RecyclerView.NO_POSITION) {
            return;
        }
        int hour = timelineAdapter.hourAt(rowHolder.getBindingAdapterPosition());
        if (hour < 0) {
            return;
        }
        timelineDragCallback.beginDrag(task, hour);
        timelineItemTouchHelper.startDrag(rowHolder);
    }

    /**
     * Long-press on a timeline block always begins a drag — past or future.
     *
     * <p>A missed (past) task is usually something the user wants to move to a
     * later hour, so dimming it must never disable the drag. Deletion is a
     * separate left/right swipe gesture handled by {@code onSwiped}.</p>
     */
    private void startDragFromLongPress(TaskWithDate task, View blockView) {
        startTimelineDrag(task, blockView);
    }

    /** Asks before deleting a swiped block; a task is still data. */
    private void confirmDeleteTask(TaskWithDate task) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_past_task_title)
                .setMessage(getString(R.string.delete_past_task_message, task.getTaskName()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteTaskFromTimeline(task))
                .show();
    }

    /**
     * Removes one task by identity, not by position.
     *
     * <p>{@link TaskWithDate#getId()} is the identity (PRD invariant 1). The
     * timeline re-renders from a query, so the task may not sit at the index the
     * user tapped — matching on the list position here would delete the wrong row
     * as soon as two tasks share an hour.</p>
     */
    private void deleteTaskFromTimeline(TaskWithDate task) {
        if (task == null || tasks == null) {
            return;
        }
        String id = task.getId();
        int index = -1;
        for (int i = 0; i < tasks.size(); i++) {
            TaskWithDate candidate = tasks.get(i);
            if (candidate != null && candidate.getId() != null
                    && candidate.getId().equals(id)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        TaskWithDate removed = tasks.remove(index);
        // Cancel first: once the object leaves the list its uuid is unreachable
        // and the alarm would fire for a task the user just deleted.
        ReminderScheduler.cancel(this, removed);
        TaskReminderReceiver.cancelNotification(this, removed.getId());
        saveTasks();
        renderTimelineForSelectedDate();
        bindPriorities();
        Toast.makeText(this, R.string.task_deleted_message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Applies a completed drag: shifts the task by {@code deltaHours}, saves, and
     * re-renders.
     *
     * <p>Duration is preserved and the result is clamped to the same day (see
     * {@link TimelineEdits#shiftByHours}); a block that would run past midnight
     * stops at the latest start that still fits rather than silently changing
     * length.</p>
     */
    private void applyDraggedHour(TaskWithDate task, int deltaHours) {
        int[] shifted = TimelineEdits.shiftByHours(
                task.getStartMinute(), task.getEndMinute(), deltaHours);
        if (shifted == null) {
            return;
        }
        int newStart = shifted[0];
        int newEnd = shifted[1];
        if (newStart == task.getStartMinute() && newEnd == task.getEndMinute()) {
            // Clamped to where it already was: tell the user why nothing moved.
            Toast.makeText(this, R.string.reschedule_no_room, Toast.LENGTH_SHORT).show();
            return;
        }

        // Mutate the instance the store actually writes, not the adapter's copy.
        // renderTimelineForSelectedDate() loads FRESH TaskWithDate objects from
        // storage, so `task` here is a different object from the one in `tasks`.
        // Saving `tasks` without this lookup silently persisted the old time,
        // which is what made the row snap back after a drag.
        TaskWithDate stored = findTaskById(task.getId());
        if (stored == null) {
            return;
        }

        // Start first: it clears the Anytime sentinel, which setEndMinute needs.
        stored.setStartMinute(newStart);
        stored.setEndMinute(newEnd);
        // Persist immediately so the model agrees with what the finger just did.
        saveTasks();
        // The time changed, so the reminder derived from it is stale.
        ReminderScheduler.schedule(this, stored);
    }

    /** The live instance in {@link #tasks} with this UUID, or null. */
    private TaskWithDate findTaskById(String id) {
        if (id == null || tasks == null) {
            return null;
        }
        for (TaskWithDate candidate : tasks) {
            if (candidate != null && id.equals(candidate.getId())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * A backlog chip was dropped on the hour row for {@code hour}.
     *
     * <p>Turns an Anytime task into a one-hour block starting at that hour.
     * Resolved by UUID against the live list: the adapter hands back an id, and
     * the task that gets saved must be the one {@link #saveTasks()} writes.</p>
     */
    private void scheduleTaskAtHour(String taskId, int hour) {
        TaskWithDate task = findTaskById(taskId);
        if (task == null || hour < 0 || hour > 23) {
            return;
        }

        // Start before end: setStartMinute clears the Anytime sentinel, and
        // setEndMinute refuses a real end while start is still NO_TIME.
        task.setStartMinute(hour * 60);
        task.setEndMinute(Math.min((hour + 1) * 60, 24 * 60));

        saveTasks();
        // The task just gained a time, so its reminder must be armed from the
        // new interval; Anytime tasks never had one.
        ReminderScheduler.schedule(this, task);
        Toast.makeText(this,
                getString(R.string.reschedule_success,
                        TimeCategory.formatMinuteOfDay(task.getStartMinute())),
                Toast.LENGTH_SHORT).show();

        // Re-render on the next frame. This runs inside the system's drag-event
        // dispatch, and notifyDataSetChanged() there can race a pending layout
        // in the grid. Saving is already done, so deferring only the redraw is
        // safe: the chip leaves the backlog and the block appears in the grid.
        if (timelineRecyclerView != null) {
            timelineRecyclerView.post(this::refreshCurrentView);
        } else {
            refreshCurrentView();
        }
    }


    private void setupViewToggle() {
        if (timelineRecyclerView != null) {
            timelineAdapter = new TimelineAdapter(this);
            timelineAdapter.setListener(new TimelineAdapter.Listener() {
                @Override
                public void onBlockLongPressed(@NonNull TaskWithDate task,
                                               @NonNull View blockView) {
                    startDragFromLongPress(task, blockView);
                }

                @Override
                public void onTaskDropped(@NonNull String taskId, int hour) {
                    scheduleTaskAtHour(taskId, hour);
                }
            });
            timelineRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            timelineRecyclerView.setAdapter(timelineAdapter);
            setupTimelineDrag();
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
     * Fills the day-header pill with the selected date, e.g. "Thu, Nov 12".
     *
     * <p>Uses the platform's own {@link java.text.SimpleDateFormat} patterns
     * ({@code EEE} / {@code MMM}) rather than the app's {@code weekday_*} string
     * resources: those are upper-case table headers ("THU"), and a date pill
     * wants the locale's natural short form. Formatting is therefore
     * automatically correct in Chinese too ("周四, 11月 12").</p>
     */
    private void bindTimelineHeader(String dateIso) {
        if (timelineHeaderDay == null || dateIso == null || dateIso.length() != 10) {
            return;
        }
        try {
            java.util.Calendar calendar = java.util.Calendar.getInstance(Locale.getDefault());
            calendar.clear();
            calendar.set(Integer.parseInt(dateIso.substring(0, 4)),
                    Integer.parseInt(dateIso.substring(5, 7)) - 1,
                    Integer.parseInt(dateIso.substring(8, 10)));
            java.text.SimpleDateFormat format =
                    new java.text.SimpleDateFormat("EEE, MMM d", Locale.getDefault());
            timelineHeaderDay.setText(format.format(calendar.getTime()));
        } catch (NumberFormatException e) {
            timelineHeaderDay.setText("");
        }
    }

    /** Monday-first weekday labels, matching the day selector strip. */
    private static final int[] WEEKDAY_LABELS = {
            R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed,
            R.string.weekday_thu, R.string.weekday_fri, R.string.weekday_sat,
            R.string.weekday_sun
    };

    /**
     * Loads the grid, backlog and hint for whichever date is selected.
     *
     * <p>Split out from {@link #setTimelineView(boolean)} so tapping a day does
     * not re-run the list/timeline visibility work.</p>
     */
    private void renderTimelineForSelectedDate() {
        String dateIso = selectedDateIso == null ? todayIso() : selectedDateIso;
        bindTimelineHeader(dateIso);
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
            if (timelineHeaderRow != null) {
                timelineHeaderRow.setVisibility(View.VISIBLE);
            }
            if (fabAddTask != null) {
                fabAddTask.setVisibility(View.VISIBLE);
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
            if (timelineHeaderRow != null) {
                timelineHeaderRow.setVisibility(View.GONE);
            }
            if (fabAddTask != null) {
                fabAddTask.setVisibility(View.GONE);
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
            category.setTextColor(getColor(task.getTimeCategory().getTextColorRes()));

            // Long-press starts a cross-view drag onto the hour grid. The UUID
            // travels in the ClipData because names can repeat; the grid looks
            // the task up by id, never by name.
            final TaskWithDate dragged = task;
            chip.setOnLongClickListener(v -> {
                ClipData clip = ClipData.newPlainText("task_id", dragged.getId());
                v.startDragAndDrop(clip, new View.DragShadowBuilder(v), dragged.getId(), 0);
                return true;
            });

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

    // ------------------------------------------------------------------
    // AI auto-schedule (2.0 preview — see docs/PRD.md §5)
    // ------------------------------------------------------------------

    /**
     * Asks the AI scheduler to turn the selected day's Anytime tasks into timed
     * blocks.
     *
     * <p>{@link AIService#getSchedule} is blocking network I/O, so the request
     * runs on a single background worker and the result is applied back on the
     * main thread. The button is disabled for the duration so a double tap
     * cannot start two overlapping runs.</p>
     */
    private void runAutoSchedule() {
        if (aiRequestInFlight) {
            return;
        }

        final String dateIso = selectedDateIso == null ? todayIso() : selectedDateIso;
        final List<TaskWithDate> anytime = loadAnytimeForDate(dateIso);
        if (anytime.isEmpty()) {
            Toast.makeText(this, R.string.ai_schedule_no_anytime, Toast.LENGTH_SHORT).show();
            return;
        }

        // Snapshot the request on the main thread: the server only needs names,
        // and the task objects are mutated later on this same thread.
        List<String> names = new ArrayList<>();
        for (TaskWithDate task : anytime) {
            names.add(task.getTaskName());
        }
        final ScheduleRequest request = new ScheduleRequest(names, recentReviewDigests(dateIso));

        setAiBusy(true);
        Toast.makeText(this, R.string.ai_thinking, Toast.LENGTH_SHORT).show();

        if (aiExecutor == null) {
            aiExecutor = Executors.newSingleThreadExecutor();
        }
        aiExecutor.execute(() -> {
            try {
                final ScheduleResponse response = AIService.getSchedule(request);
                runOnUiThread(() -> onScheduleLoaded(response, dateIso));
            } catch (IOException e) {
                final String detail = e.getMessage() == null ? "" : e.getMessage();
                runOnUiThread(() -> onScheduleFailed(detail));
            } catch (RuntimeException e) {
                // e.g. a malformed base URL; still a user-facing failure, not a crash.
                final String detail = e.getMessage() == null ? "" : e.getMessage();
                runOnUiThread(() -> onScheduleFailed(detail));
            }
        });
    }

    /** Main thread: applies the returned blocks, or reports why nothing happened. */
    private void onScheduleLoaded(ScheduleResponse response, String dateIso) {
        // Checked before touching the button: a result that arrives after the
        // screen is gone must not poke a detached view hierarchy.
        if (isFinishing() || isDestroyed()) {
            return;
        }
        setAiBusy(false);

        if (response == null || !response.isSuccess()) {
            String error = response == null ? "" : response.getError();
            Toast.makeText(this,
                    error.isEmpty()
                            ? getString(R.string.ai_schedule_empty)
                            : getString(R.string.ai_schedule_failed, error),
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Re-resolve from the live list: onResume() reloads tasks from storage,
        // so the objects captured when the request started may no longer be the
        // ones this screen renders.
        int applied = applySchedule(response.getSchedule(), loadAnytimeForDate(dateIso));
        if (applied == 0) {
            Toast.makeText(this, R.string.ai_schedule_empty, Toast.LENGTH_LONG).show();
            return;
        }

        saveTasks();
        // The blocks just changed from "Anytime" to timed, so every reminder
        // derived from them is stale; rebuild them from the saved state.
        ReminderScheduler.rescheduleAll(this, tasks);
        refreshCurrentView();
        Toast.makeText(this, getString(R.string.ai_schedule_success, applied),
                Toast.LENGTH_SHORT).show();
    }

    /** Main thread: surfaces a transport failure and leaves the tasks untouched. */
    private void onScheduleFailed(String detail) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        setAiBusy(false);
        Toast.makeText(this, getString(R.string.ai_schedule_failed, detail), Toast.LENGTH_LONG).show();
    }

    /**
     * Converts the transport type to mapper proposals and applies them.
     *
     * <p>All the rules (name matching, hour sanity, one proposal per task) live
     * in {@link AutoScheduleMapper}, which is plain Java and unit-tested; this
     * method is only the adapter between the two types.</p>
     *
     * @return how many tasks actually received a block
     */
    private int applySchedule(List<ScheduleItem> items, List<TaskWithDate> anytime) {
        List<AutoScheduleMapper.Proposal> proposals = new ArrayList<>();
        if (items != null) {
            for (ScheduleItem item : items) {
                if (item == null) {
                    continue;
                }
                proposals.add(new AutoScheduleMapper.Proposal(
                        item.getTaskName(), item.getStartHour(), item.getEndHour()));
            }
        }
        return AutoScheduleMapper.apply(anytime, proposals);
    }

    /** Enables/disables the trigger while a request is in flight. */
    private void setAiBusy(boolean busy) {
        aiRequestInFlight = busy;
        if (btnAutoSchedule != null) {
            btnAutoSchedule.setEnabled(!busy);
            btnAutoSchedule.setAlpha(busy ? 0.6f : 1f);
        }
    }

    /**
     * Short digests of the reviews written in the week before {@code dateIso}.
     *
     * <p>Skipped days contribute nothing: an empty review is not evidence that
     * the day went badly, so it must not be sent as if it were.</p>
     */
    private List<String> recentReviewDigests(String dateIso) {
        List<String> digests = new ArrayList<>();
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        try {
            java.util.Date anchor = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso);
            if (anchor == null) {
                return digests;
            }
            calendar.setTime(anchor);
        } catch (java.text.ParseException e) {
            return digests;
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        ReviewStore store = ReviewStore.from(this);
        for (int i = 1; i <= 7; i++) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, -1);
            String day = format.format(calendar.getTime());
            if (!IntervalRules.isValidDateIso(day)) {
                continue;
            }
            DailyReview review = store.load(day);
            if (review.isEmpty()) {
                continue;
            }
            StringBuilder line = new StringBuilder(day).append(": ");
            if (!review.getWentWellText().trim().isEmpty()) {
                line.append("went well=").append(review.getWentWellText().trim()).append("; ");
            }
            if (!review.getImproveText().trim().isEmpty()) {
                line.append("improve=").append(review.getImproveText().trim());
            }
            digests.add(line.toString().trim());
        }
        return digests;
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

        // Cards, dividers, labels and the nav bar all read a theme role, so the
        // same code paints every palette correctly; only the card shape differs
        // between light and dark, and that comes from the drawable resources.
        if (isDarkMode) {
            if (dateCard != null) dateCard.setBackgroundResource(R.drawable.dark_card_bg);
            if (tasksCard != null) tasksCard.setBackgroundResource(R.drawable.dark_card_bg);
            if (quoteCard != null) quoteCard.setBackgroundResource(R.drawable.dark_card_bg);
        } else {
            if (dateCard != null) dateCard.setBackgroundResource(R.drawable.white_card_bg);
            if (tasksCard != null) tasksCard.setBackgroundResource(R.drawable.white_card_bg);
            if (quoteCard != null) quoteCard.setBackgroundResource(R.drawable.white_card_bg);
        }

        if (divider != null) divider.setBackgroundColor(ThemeColors.outline(divider));

        // The title sits on the theme gradient, so white is wrong on palettes
        // whose gradient starts light (e.g. sunset).
        if (titleText != null) {
            titleText.setTextColor(ThemeColors.onGradient(gradientColors[0], gradientColors[1]));
        }

        TextView tasksHeaderText = findViewById(R.id.tasksHeaderText);
        if (tasksHeaderText != null) {
            tasksHeaderText.setText(R.string.todays_tasks);
            tasksHeaderText.setTextColor(ThemeColors.onSurface(tasksHeaderText));
        }

        if (dateText != null) dateText.setTextColor(ThemeColors.onSurface(dateText));
        if (quoteText != null) quoteText.setTextColor(ThemeColors.onSurface(quoteText));

        // quoteLabel / editQuoteButton / quoteAuthor are accented with colorPrimary,
        // but colorPrimary is a FILL colour, not a text colour: it is #FFE53B on
        // sunset (1.27:1 on the white card) and #00FF5B on lime (1.36:1). Use the
        // readable variant, which darkens/lightens the accent until it clears AA on
        // the card it actually sits on.
        TextView quoteLabel = findViewById(R.id.quoteLabel);
        int quoteAccent = ThemeColors.readableAccent(quoteLabel != null ? quoteLabel : quoteText);
        if (quoteLabel != null) quoteLabel.setTextColor(quoteAccent);
        if (quoteAuthor != null) quoteAuthor.setTextColor(quoteAccent);
        if (editQuoteButton != null) editQuoteButton.setTextColor(quoteAccent);

        TextView noTasksText = findViewById(R.id.noTasksText);
        if (noTasksText != null) {
            noTasksText.setText(R.string.no_tasks_today);
            noTasksText.setTextColor(ThemeColors.onSurfaceVariant(noTasksText));
        }

        if (bottomNavBar != null) {
            bottomNavBar.setBackgroundColor(ThemeColors.surface(bottomNavBar));
        }

        if (navHomeText != null) navHomeText.setTextColor(accentColor);
        int navInactiveColor = bottomNavBar != null
                ? ThemeColors.onSurfaceVariant(bottomNavBar)
                : accentColor;
        if (navCalendarText != null) navCalendarText.setTextColor(navInactiveColor);
        if (navShopText != null) navShopText.setTextColor(navInactiveColor);
        if (navThemesText != null) navThemesText.setTextColor(navInactiveColor);
        if (navProfileText != null) navProfileText.setTextColor(navInactiveColor);

        updateNavigationTexts();
    }

    private void applyAllThemes() {
        String currentTheme = sharedPreferences.getString("selected_theme", ThemeManager.THEME_DEFAULT);
        // One source of truth for the palette: the same two stops paint the
        // screen background and the Add Task button, so a theme change can never
        // leave the button on the previous theme's colours.
        int[] gradientColors = ThemeManager.getThemeGradientColors(this, currentTheme);

        if (mainLayout != null) {
            GradientDrawable gradientDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    gradientColors
            );
            mainLayout.setBackground(gradientDrawable);
        }

        if (addTaskButton != null) {
            GradientHelper.applyPill(addTaskButton, this, gradientColors);
        }
        if (openReviewButton != null) {
            // Same helper, same stops, same 26dp radius as the Add Task pill, so
            // the two primary actions on this screen always look like siblings.
            GradientHelper.applyPill(openReviewButton, this, gradientColors);
        }
        if (fabAddTask != null) {
            // A FAB cannot use applyPill(): it is circular and its icon is a
            // drawable, not a label. Tint the container with the gradient's
            // first stop and pick the icon colour from that fill, so the FAB
            // stays legible on light accents (sunset #FFE53B needs a dark icon).
            int fabFill = gradientColors[0];
            fabAddTask.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(fabFill));
            fabAddTask.setImageTintList(android.content.res.ColorStateList.valueOf(
                    com.example.dailytodolist.util.GradientContrast.onColorFor(fabFill)));
        }

        applyThemeToAllElements();
    }

    private void applyThemeToAllElements() {
        // The quote card's label, Edit button and author are all tinted with
        // ?attr/colorPrimary and the quote body with ?attr/colorOnSurface, so they
        // already follow the theme. Do not repaint them here: doing so with
        // gradientColors[0] overrode the attribute and pinned them to a fixed
        // accent, which is why "Edit" stayed cyan on sunset.
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
                taskTimeText.setTextColor(getColor(task.getTimeCategory().getTextColorRes()));
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
            taskCheckbox.setButtonTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.destructive)));
            taskText.setPaintFlags(taskText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            taskText.setTextColor(ThemeColors.onSurfaceVariant(taskText));
            cardView.setCardBackgroundColor(ThemeColors.surfaceVariant(cardView));
        } else {
            taskCheckbox.setButtonTintList(android.content.res.ColorStateList.valueOf(accentColor));
            taskText.setPaintFlags(taskText.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));

            taskText.setTextColor(ThemeColors.onSurface(taskText));
            cardView.setCardBackgroundColor(ThemeColors.surface(cardView));
        }

        if (deleteTask != null) {
            deleteTask.setTextColor(accentColor);
        }

        taskCheckbox.setOnClickListener(v -> {
            boolean wasCompleted = task.isChecked();
            task.setChecked(taskCheckbox.isChecked());
            saveTasks();

            // Completing a task cancels its reminder: being told to start
            // something already finished is worse than no reminder at all.
            if (taskCheckbox.isChecked()) {
                ReminderScheduler.cancel(this, task);
                TaskReminderReceiver.cancelNotification(this, task.getId());
            } else {
                // Un-checking re-arms it, so undo restores the full behaviour.
                ReminderScheduler.schedule(this, task);
            }

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
            // Cancel before removing: once the object is gone from the list the
            // uuid is no longer reachable, and the alarm would fire for a task
            // the user has explicitly deleted.
            TaskWithDate toDelete = tasks.get(position);
            ReminderScheduler.cancel(this, toDelete);
            TaskReminderReceiver.cancelNotification(this, toDelete.getId());

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
        // The inline text field is gone: it looked like it saved in place but
        // actually navigated away, so whatever was typed was silently lost.
        addTaskButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            addTaskLauncher.launch(intent);
        });

        // Same destination as the Add Task button; the FAB exists so the
        // timeline can be extended without scrolling back to the bottom.
        if (fabAddTask != null) {
            fabAddTask.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
                addTaskLauncher.launch(intent);
            });
        }

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

        // Field is already resolved in initViews(); re-finding it here would just
        // be a second lookup of the same view.
        if (openReviewButton != null) {
            openReviewButton.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, ReviewActivity.class)));
        }

        if (btnAutoSchedule != null) {
            btnAutoSchedule.setOnClickListener(v -> runAutoSchedule());
        }

        if (btnAskAi != null) {
            // The disc is filled with colorPrimary, so the sparkle must be chosen
            // from that fill rather than the theme's static colorOnPrimary: white
            // on sunset (#FFE53B) is 1.27:1 and on mint dark (#E0F0F0) 1.17:1.
            btnAskAi.setIconTint(ColorStateList.valueOf(ThemeColors.onPrimarySurface(btnAskAi)));
            btnAskAi.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, ChatActivity.class)));
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
    protected void onDestroy() {
        super.onDestroy();
        // The worker thread must not outlive the screen: a queued callback would
        // post to a destroyed Activity.
        if (aiExecutor != null) {
            aiExecutor.shutdownNow();
            aiExecutor = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyAllThemes();
        applyDarkMode();
    }
}