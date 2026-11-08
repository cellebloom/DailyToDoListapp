package com.example.dailytodolist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.dailytodolist.util.ReminderScheduler;
import com.example.dailytodolist.util.TaskStore;

/**
 * Re-arms time-block reminders after a reboot or an app update.
 *
 * <p>AlarmManager alarms are cleared when the device restarts, so without this
 * every reminder scheduled before the reboot would be silently lost. Reading
 * the tasks back from {@link TaskStore} is the source of truth; nothing is
 * cached in the alarm itself.</p>
 */
public class BootReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }

        // Receivers get a short window; a full store read is still fast here.
        ReminderScheduler.ensureChannel(context);
        ReminderScheduler.rescheduleAll(context, TaskStore.from(context).loadAll());
    }
}
