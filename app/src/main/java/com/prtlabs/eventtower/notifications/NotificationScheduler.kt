package com.prtlabs.eventtower.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"

    fun scheduleDailyBriefing(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyBriefingReceiver::class.java).apply {
            action = "com.prtlabs.eventtower.DAILY_BRIEFING"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If it's already past 8 AM today, schedule for 8 AM tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val timeString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(calendar.time)
        Log.d(TAG, "Attempting to schedule briefing for: $timeString")

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            try {
                // Using setAlarmClock is the gold standard for reliability as it is exempt from Doze/App Standby
                val info = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
                Log.d(TAG, "Scheduled successfully using setAlarmClock")
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException while scheduling exact alarm, falling back to inexact", e)
                scheduleInexactAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
            }
        } else {
            Log.d(TAG, "Cannot schedule exact alarms, using inexact fallback")
            scheduleInexactAlarm(alarmManager, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun scheduleInexactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
        Log.d(TAG, "Scheduled using inexact fallback method")
    }
}
