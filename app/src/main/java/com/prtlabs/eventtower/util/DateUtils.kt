package com.prtlabs.eventtower.util

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

object DateUtils {
    fun formatDuration(targetDate: LocalDate): String {
        val today = LocalDate.now()
        if (targetDate.isEqual(today)) return "Today!"

        val period = Period.between(today, targetDate)
        val isPast = targetDate.isBefore(today)
        val absolutePeriod = if (isPast) Period.between(targetDate, today) else period

        val years = absolutePeriod.years
        val months = absolutePeriod.months
        val days = absolutePeriod.days

        val parts = mutableListOf<String>()
        if (years > 0) parts.add("${years}Y")
        if (months > 0) parts.add("${months}M")
        if (days > 0 || parts.isEmpty()) parts.add("${days}D")

        return parts.joinToString(" ")
    }
}
