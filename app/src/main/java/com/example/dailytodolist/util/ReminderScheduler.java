package com.example.dailytodolist.util;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.dailytodolist.R;
import com.example.dailytodolist.TaskReminderReceiver;
import com.example.dailytodolist.TaskWithDate;

/**
 * Schedules and cancels "starts in 5 minutes" alarms for time-blocked tasks.
 *
 * <p><b>Identity.</b> An alarm is addressed by the task's permanent UUID, hashed
 * into the {@link PendingIntent} request code. Nothing here uses a list index:
 * cancelling by position would hit whichever task happened to shift into that
 * slot after a delete — the same class of bug the UUID-keyed storage removed.</p>
 *
 * <p><b>Anytime tasks are skipped.</b> {@code startMinute == NO_TIME} is the
 * Anytime sentinel, and in Java {@code -1 / 60 == 0}, so a naive conversion
 * would fire at 00:00 for a task that has no time at all.</p>
 */
public final class ReminderScheduler {

    /** How far before the block starts the user is warned. */
    public static final long LEAD_MILLIS = 5 * 60 * 1000L;

    private static final String CHANNEL_ID = "time_block_reminders";
    private static final CharSequence CHANNEL_NAME = "Time block reminders";

    private ReminderScheduler() {
        // static helpers only
    }

    /** Creates the notification channel. Safe to call repeatedly. */
    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.reminder_channel_description));
        channel.enableVibration(true);
        manager.createNotificationChannel(channel);
    }

    public static String getChannelId() {
        return CHANNEL_ID;
    }

    /**
     * Arms the reminder for one task.
     *
     * <p>The alarm never lands in the past: a task created or edited after its
     * own warning time is silently unscheduled rather than fired instantly,
     * which would produce a notification for a moment that has already passed.</p>
     *
     * @return true when an alarm was actually set
     */
    public static boolean schedule(Context context, TaskWithDate task) {
        if (task == null || task.isAnytime() || task.isChecked()) {
            cancel(context, task);
            return false;
        }

        String dateIso = task.getDateIso();
        if (dateIso == null || dateIso.isEmpty()) {
            return false;
        }

        long startMillis = startEpochMillis(dateIso, task.getStartMinute());
        if (startMillis <= 0L) {
            return false;
        }

        long triggerAt = startMillis - LEAD_MILLIS;
        if (triggerAt <= System.currentTimeMillis()) {
            // Warning time already passed (task added late in the day).
            cancel(context, task);
            return false;
        }

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return false;
        }

        PendingIntent pendingIntent = buildPendingIntent(
                context, task, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && !alarmManager.canScheduleExactAlarms()) {
                // Android 12+ may deny exact alarms. An inexact alarm still
                // delivers the reminder, just possibly a little late, which is
                // better than dropping it silently.
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
            return true;
        } catch (SecurityException e) {
            // Exact-alarm permission withdrawn between the check and the call.
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            return true;
        }
    }

    /** Cancels the reminder for one task. Safe when nothing was scheduled. */
    public static void cancel(Context context, TaskWithDate task) {
        if (context == null || task == null) {
            return;
        }
        String id = task.getId();
        if (id == null || id.isEmpty()) {
            return;
        }
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        // Build a matching PendingIntent (no UPDATE_CURRENT, so the extras are
        // irrelevant) and cancel it when it exists.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCodeFor(id), baseIntent(context),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    /**
     * Cancels every reminder the user should no longer receive.
     *
     * <p>Used after bulk changes (completing, deleting, reloading) so a stale
     * alarm can never outlive the task it refers to.</p>
     */
    public static void cancelAll(Context context, Iterable<TaskWithDate> tasks) {
        if (tasks == null) {
            return;
        }
        for (TaskWithDate task : tasks) {
            cancel(context, task);
        }
    }

    /**
     * Re-arms reminders for a whole list, cancelling anything checked or
     * Anytime. Call after a bulk save so state and alarms cannot drift apart.
     */
    public static void rescheduleAll(Context context, Iterable<TaskWithDate> tasks) {
        if (tasks == null) {
            return;
        }
        for (TaskWithDate task : tasks) {
            if (task == null) {
                continue;
            }
            if (task.isChecked() || task.isAnytime()) {
                cancel(context, task);
            } else {
                schedule(context, task);
            }
        }
    }

    /** Start of the block as epoch millis, or -1 when the date is unusable. */
    private static long startEpochMillis(String dateIso, int startMinute) {
        if (startMinute == TaskWithDate.NO_TIME) {
            return -1L;
        }
        String[] parts = dateIso.split("-");
        if (parts.length != 3) {
            return -1L;
        }
        int year;
        int month;
        int day;
        try {
            year = Integer.parseInt(parts[0]);
            month = Integer.parseInt(parts[1]);
            day = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return -1L;
        }

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month - 1, day, 0, 0, 0);
        calendar.add(java.util.Calendar.MINUTE, startMinute);
        return calendar.getTimeInMillis();
    }

    private static PendingIntent buildPendingIntent(Context context, TaskWithDate task, int flags) {
        Intent intent = baseIntent(context);
        intent.putExtra(TaskReminderReceiver.EXTRA_TASK_ID, task.getId());
        intent.putExtra(TaskReminderReceiver.EXTRA_TASK_NAME, task.getTaskName());
        intent.putExtra(TaskReminderReceiver.EXTRA_TASK_COLOR,
                task.getTimeCategory().getColorRes());
        return PendingIntent.getBroadcast(context, requestCodeFor(task.getId()), intent, flags);
    }

    private static Intent baseIntent(Context context) {
        Intent intent = new Intent(context, TaskReminderReceiver.class);
        intent.setAction(TaskReminderReceiver.ACTION_REMIND);
        return intent;
    }

    /** Stable request code from the task UUID. */
    private static int requestCodeFor(String taskId) {
        return taskId == null ? 0 : taskId.hashCode();
    }
}
