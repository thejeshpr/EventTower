package com.prtlabs.eventtower.data

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromRecurring(value: String?): Recurring? {
        return value?.let { Recurring.valueOf(it) }
    }

    @TypeConverter
    fun recurringToString(recurring: Recurring?): String? {
        return recurring?.name
    }
}
