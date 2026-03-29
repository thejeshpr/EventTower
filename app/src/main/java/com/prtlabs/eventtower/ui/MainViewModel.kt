package com.prtlabs.eventtower.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.prtlabs.eventtower.data.Event
import com.prtlabs.eventtower.data.EventDao
import com.prtlabs.eventtower.data.Recurring
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class BadgeStatus { NONE, GREEN, YELLOW }

class MainViewModel(private val eventDao: EventDao) : ViewModel() {

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, com.google.gson.JsonSerializer<LocalDate> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE))
        })
        .registerTypeAdapter(LocalDate::class.java, com.google.gson.JsonDeserializer<LocalDate> { json, _, _ ->
            LocalDate.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE)
        })
        .create()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    val allEvents = eventDao.getAllEvents().stateIn(
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
        list.filter { it.recurring == Recurring.NO && it.date.isBefore(LocalDate.now()) }
            .sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingBadgeStatus = upcomingEvents.map { list ->
        val today = LocalDate.now()
        when {
            list.any { it.date.isEqual(today) } -> BadgeStatus.YELLOW
            list.any { ChronoUnit.DAYS.between(today, it.date) in 1..10 } -> BadgeStatus.GREEN
            else -> BadgeStatus.NONE
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BadgeStatus.NONE)

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

    fun exportToJson(): String {
        return gson.toJson(allEvents.value)
    }

    suspend fun importFromJson(json: String): Pair<Int, Int> {
        return try {
            val type = object : TypeToken<List<Event>>() {}.type
            val importedEvents: List<Event> = gson.fromJson(json, type)
            var newCount = 0
            var overwrittenCount = 0
            
            val currentIds = allEvents.value.map { it.id }.toSet()
            
            importedEvents.forEach { event ->
                if (currentIds.contains(event.id)) {
                    overwrittenCount++
                } else {
                    newCount++
                }
                eventDao.insertEvent(event)
            }
            Pair(newCount, overwrittenCount)
        } catch (e: Exception) {
            Pair(-1, -1) // Error case
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
