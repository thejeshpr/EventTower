package com.prtlabs.eventtower.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.prtlabs.eventtower.EventApplication
import com.prtlabs.eventtower.MainActivity
import com.prtlabs.eventtower.data.Recurring
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class DailyBriefingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("DailyBriefing", "Receiver triggered! Action: $action")
        
        // Always reschedule for the next day when triggered by the alarm
        if (action == "com.prtlabs.eventtower.DAILY_BRIEFING" || action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationScheduler.scheduleDailyBriefing(context)
        }

        if (action == Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as EventApplication
        val eventDao = app.database.eventDao()

        CoroutineScope(Dispatchers.IO).launch {
            val allEvents = eventDao.getAllEvents().first()
            val today = LocalDate.now()
            
            Log.d("DailyBriefing", "Checking events for $today. Total events in DB: ${allEvents.size}")

            val todayEvents = allEvents.mapNotNull { event ->
                val adjustedDate = if (event.recurring != Recurring.NO && event.date.isBefore(today)) {
                    var nextDate = event.date
                    while (nextDate.isBefore(today)) {
                        nextDate = when (event.recurring) {
                            Recurring.MONTHLY -> nextDate.plusMonths(1)
                            Recurring.YEARLY -> nextDate.plusYears(1)
                            else -> nextDate
                        }
                    }
                    nextDate
                } else {
                    event.date
                }
                
                if (adjustedDate.isEqual(today)) {
                    event
                } else {
                    null
                }
            }

            Log.d("DailyBriefing", "Found ${todayEvents.size} events today.")

            if (todayEvents.isNotEmpty()) {
                showNotification(context, todayEvents.size, todayEvents.map { it.title })
            } else {
                Log.d("DailyBriefing", "No events for today. Skipping notification.")
            }
        }
    }

    private fun showNotification(context: Context, count: Int, titles: List<String>) {
        val channelId = "daily_briefing_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Briefing"
            val descriptionText = "Morning updates for your events"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val titleText = "$count Event${if (count > 1) "s" else ""} Today"
        
        val bodyText = if (count == 1) {
            titles.first()
        } else {
            titles.joinToString("\n") { "• $it" }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titleText)
            .setContentText(if (count == 1) titles.first() else "Tap to see $count events")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())
        Log.d("DailyBriefing", "Notification posted successfully!")
    }
}
