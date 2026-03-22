package com.prtlabs.eventtower.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prtlabs.eventtower.data.Event
import com.prtlabs.eventtower.data.EventDao
import com.prtlabs.eventtower.data.Recurring
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(private val eventDao: EventDao) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val allEvents = eventDao.getAllEvents().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories = allEvents.map { list ->
        list.map { it.category }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filteredEvents = combine(allEvents, _searchQuery, _selectedCategory) { events, query, category ->
        events.filter { event ->
            val matchesQuery = event.title.contains(query, ignoreCase = true) ||
                    (event.notes?.contains(query, ignoreCase = true) == true)
            val matchesCategory = category == null || event.category == category
            matchesQuery && matchesCategory
        }
    }

    val upcomingEvents = filteredEvents.map { list ->
        list.map { event ->
            if (event.recurring != Recurring.NO && event.date.isBefore(LocalDate.now())) {
                event.copy(date = calculateNextOccurrence(event.date, event.recurring))
            } else {
                event
            }
        }.filter { it.date.isAfter(LocalDate.now()) || it.date.isEqual(LocalDate.now()) }
            .sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pastEvents = filteredEvents.map { list ->
        // Exclude recurring events from the Past tab as they "roll over" to the Upcoming tab
        list.filter { it.recurring == Recurring.NO && it.date.isBefore(LocalDate.now()) }
            .sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun calculateNextOccurrence(startDate: LocalDate, recurring: Recurring): LocalDate {
        var nextDate = startDate
        val today = LocalDate.now()
        while (nextDate.isBefore(today)) {
            nextDate = when (recurring) {
                Recurring.MONTHLY -> nextDate.plusMonths(1)
                Recurring.YEARLY -> nextDate.plusYears(1)
                else -> nextDate
            }
        }
        return nextDate
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelect(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun addEvent(event: Event) {
        viewModelScope.launch {
            eventDao.insertEvent(event)
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            eventDao.insertEvent(event)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            eventDao.deleteEvent(event)
        }
    }

    fun getEventById(id: String): Flow<Event?> {
        return allEvents.map { list -> list.find { it.id == id } }
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
