package com.prtlabs.eventtower.data

import java.time.LocalDate

enum class Recurring {
    NO, MONTHLY, YEARLY
}

data class Event(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val date: LocalDate,
    val category: String = "Personal",
    val notes: String? = null,
    val recurring: Recurring = Recurring.NO
)
