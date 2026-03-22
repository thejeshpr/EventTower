package com.prtlabs.eventtower.ui

import androidx.lifecycle.ViewModel
import com.prtlabs.eventtower.data.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class MainViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events = _events.asStateFlow()

    val upcomingEvents = _events.map { list ->
        list.filter { it.date.isAfter(LocalDate.now()) || it.date.isEqual(LocalDate.now()) }
            .sortedBy { it.date }
    }

    val pastEvents = _events.map { list ->
        list.filter { it.date.isBefore(LocalDate.now()) }
            .sortedByDescending { it.date }
    }

    fun addEvent(event: Event) {
        _events.value = _events.value + event
    }
}
