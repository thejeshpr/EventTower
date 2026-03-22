package com.prtlabs.eventtower.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prtlabs.eventtower.data.Event
import com.prtlabs.eventtower.data.EventDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(private val eventDao: EventDao) : ViewModel() {

    val events = eventDao.getAllEvents().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val upcomingEvents = events.map { list ->
        list.filter { it.date.isAfter(LocalDate.now()) || it.date.isEqual(LocalDate.now()) }
            .sortedBy { it.date }
    }

    val pastEvents = events.map { list ->
        list.filter { it.date.isBefore(LocalDate.now()) }
            .sortedByDescending { it.date }
    }

    fun addEvent(event: Event) {
        viewModelScope.launch {
            eventDao.insertEvent(event)
        }
    }
}

class MainViewModelFactory(private val eventDao: EventDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(eventDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
