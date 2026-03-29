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

            val upcomingEvents = allEvents.map { event ->
                if (event.recurring != Recurring.NO && event.date.isBefore(today)) {
                    var nextDate = event.date
                    while (nextDate.isBefore(today)) {
                        nextDate = when (event.recurring) {
                            Recurring.MONTHLY -> nextDate.plusMonths(1)
                            Recurring.YEARLY -> nextDate.plusYears(1)
                            else -> nextDate
                        }
                    }
                    event.copy(date = nextDate)
                } else {
                    event
                }
            }

            val todayCount = upcomingEvents.count { it.date.isEqual(today) }
            val weekendStart = today.plusDays(if (today.dayOfWeek.value >= 5) 0 else (5 - today.dayOfWeek.value).toLong())
            val weekendEnd = weekendStart.plusDays(2)
            
            val comingUpCount = upcomingEvents.count { 
                (it.date.isAfter(today) && it.date.isBefore(today.plusDays(4))) ||
                (it.date.isAfter(weekendStart.minusDays(1)) && it.date.isBefore(weekendEnd.plusDays(1)))
            }

            Log.d("DailyBriefing", "Found $todayCount today and $comingUpCount coming up.")

            if (todayCount > 0 || comingUpCount > 0) {
                showNotification(context, todayCount, comingUpCount)
            } else {
                Log.d("DailyBriefing", "No events for today or soon. Skipping notification.")
            }
        }
    }

    private fun showNotification(context: Context, todayCount: Int, comingUpCount: Int) {
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

        val message = StringBuilder("Good morning! ")
        if (todayCount > 0) {
            message.append("You have $todayCount event${if (todayCount > 1) "s" else ""} today. ")
        }
        if (comingUpCount > 0) {
            message.append("And $comingUpCount coming up soon.")
        }

        // Use a standard Android icon to ensure visibility during testing
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Event Tower Briefing")
            .setContentText(message.toString())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())
        Log.d("DailyBriefing", "Notification posted successfully!")
    }
}
