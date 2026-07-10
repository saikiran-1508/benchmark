package com.example.benchmark

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.benchmark.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AlarmManager alarms do not survive a reboot. This receiver re-registers
 * every pending (future, not completed) task reminder after the device restarts.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(context).taskDao()
                dao.getAllTasksOnce()
                    .filter { !it.isCompleted }
                    .forEach { task ->
                        // schedule() itself skips tasks whose time already passed
                        ReminderScheduler.schedule(context, task.id, task.day, task.startTime, task.name, task.soundUri)
                    }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
