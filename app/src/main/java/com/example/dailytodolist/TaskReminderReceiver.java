package com.example.dailytodolist;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.dailytodolist.util.ReminderScheduler;

/**
 * Delivers the "your task starts in 5 minutes" notification.
 *
 * <p>Receives the alarm armed by {@link ReminderScheduler} and shows one
 * notification per task. The notification id is derived from the task UUID, so
 * two tasks never overwrite each other's reminder, and tapping it opens the
 * main screen.</p>
 */
public class TaskReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_REMIND = "com.example.dailytodolist.TASK_REMINDER";
    public static final String EXTRA_TASK_ID = "REMINDER_TASK_ID";
    public static final String EXTRA_TASK_NAME = "REMINDER_TASK_NAME";
    public static final String EXTRA_TASK_COLOR = "REMINDER_TASK_COLOR_RES";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_REMIND.equals(intent.getAction())) {
            return;
        }

        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        String taskName = intent.getStringExtra(EXTRA_TASK_NAME);
        if (taskName == null || taskName.trim().isEmpty()) {
            taskName = context.getString(R.string.reminder_untitled_task);
        }

        ReminderScheduler.ensureChannel(context);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }

        // Tapping the reminder opens the app's main screen.
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                notificationIdFor(taskId),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, ReminderScheduler.getChannelId())
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.reminder_title))
                .setContentText(context.getString(R.string.reminder_body, taskName))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.reminder_body, taskName)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        // Tint the accent with the task's category colour where the platform
        // supports it. Older versions simply ignore the tint.
        int colorRes = intent.getIntExtra(EXTRA_TASK_COLOR, 0);
        if (colorRes != 0) {
            try {
                builder.setColor(context.getColor(colorRes));
            } catch (android.content.res.Resources.NotFoundException ignored) {
                // A colour resource that no longer exists must not crash the
                // receiver; the notification is still useful without a tint.
            }
        }

        Notification notification = builder.build();
        manager.notify(notificationIdFor(taskId), notification);
    }

    /** Stable, per-task notification id. */
    private static int notificationIdFor(String taskId) {
        return taskId == null ? 0 : taskId.hashCode();
    }

    /** Exposed so callers can cancel a posted reminder by task id. */
    public static void cancelNotification(Context context, String taskId) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(notificationIdFor(taskId));
        }
    }

    /** True when this build needs the runtime notification permission. */
    public static boolean requiresRuntimePermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }
}
