package com.prtlabs.eventtower.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class Recurring {
    NO, MONTHLY, YEARLY
}

@Entity(tableName = "events")
data class Event(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val date: LocalDate,
    val category: String = "Personal",
    val notes: String? = null,
    val recurring: Recurring = Recurring.NO
)
