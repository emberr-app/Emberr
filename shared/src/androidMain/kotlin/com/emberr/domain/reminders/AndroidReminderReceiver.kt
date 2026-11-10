package com.emberr.domain.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.emberr.R
import com.emberr.domain.people.BirthdayReminderScheduler
import com.emberr.domain.reminders.ReminderRescheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Listens for alarms triggered by the system and pushes the actual notification to the user.
 * Runs independently in the background, even if the app is completely closed.
 */
class AndroidReminderReceiver : BroadcastReceiver(), KoinComponent {

    private val reminderRescheduler: ReminderRescheduler by inject()
    private val birthdayReminderScheduler: BirthdayReminderScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val noteTitle = intent.getStringExtra("note_title") ?: context.getString(R.string.notification_reminder)
        val blockText = intent.getStringExtra("block_text") ?: context.getString(R.string.notification_you_have_a_task_to_check)
        val notificationId = intent.getStringExtra("block_id")?.hashCode() ?: 1

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "emberr_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.notification_channel_task_reminders),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(noteTitle)
            .setContentText(blockText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                reminderRescheduler.rescheduleUpcomingReminders()
                birthdayReminderScheduler.rescheduleBirthdayReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }
}