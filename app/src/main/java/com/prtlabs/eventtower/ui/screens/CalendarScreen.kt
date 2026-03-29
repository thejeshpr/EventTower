package com.prtlabs.eventtower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prtlabs.eventtower.data.Event
import com.prtlabs.eventtower.ui.components.EventCard
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    events: List<Event>,
    onAddEventClick: () -> Unit,
    onEventClick: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val eventsByDate = remember(events) {
        events.groupBy { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.Bold) },
                actions = {
                    MonthYearPicker(
                        currentMonth = currentMonth,
                        onMonthYearSelected = { currentMonth = it }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEventClick,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            CalendarHeader(
                currentMonth = currentMonth,
                onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
            )

            CalendarGrid(
                currentMonth = currentMonth,
                selectedDate = selectedDate,
                eventsByDate = eventsByDate,
                onDateSelected = { selectedDate = it }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = Color.Gray.copy(alpha = 0.3f)
            )

            Text(
                text = "Events on ${selectedDate.dayOfMonth} ${selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val selectedDateEvents = eventsByDate[selectedDate] ?: emptyList()
            if (selectedDateEvents.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "No events on this day",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(selectedDateEvents) { event ->
                        EventCard(
                            event = event,
                            onClick = { onEventClick(event) },
                            onDelete = { onDeleteEvent(event) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Previous Month", modifier = Modifier.size(20.dp))
        }
        
        Text(
            text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Next Month", modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    eventsByDate: Map<LocalDate, List<Event>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = (firstOfMonth.dayOfWeek.value % 7) // 0 = Sunday, 1 = Monday...

    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val days = mutableListOf<LocalDate?>()
        for (i in 0 until firstDayOfWeek) {
            days.add(null)
        }
        for (i in 1..daysInMonth) {
            days.add(currentMonth.atDay(i))
        }

        // Fill remaining slots for a clean grid
        while (days.size % 7 != 0) {
            days.add(null)
        }

        val rows = days.chunked(7)
        rows.forEach { rowDays ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowDays.forEach { date ->
                    if (date == null) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val isSelected = date == selectedDate
                        val hasEvents = eventsByDate.containsKey(date)
                        val isToday = date == LocalDate.now()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                                        isToday -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasEvents) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthYearPicker(
    currentMonth: YearMonth,
    onMonthYearSelected: (YearMonth) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Month")
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Jump to Today
            DropdownMenuItem(
                text = { Text("Jump to Today") },
                onClick = {
                    onMonthYearSelected(YearMonth.now())
                    expanded = false
                }
            )
            
            HorizontalDivider()
            
            // Month selection could be another level, but let's do a simple Year scroll for now
            // or just provide common jumps.
            val years = (LocalDate.now().year - 2..LocalDate.now().year + 5)
            years.forEach { year ->
                var yearExpanded by remember { mutableStateOf(false) }
                DropdownMenuItem(
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(year.toString())
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, Modifier.size(12.dp))
                        }
                    },
                    onClick = { yearExpanded = true }
                )
                
                DropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                    Month.entries.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month.getDisplayName(TextStyle.FULL, Locale.getDefault())) },
                            onClick = {
                                onMonthYearSelected(YearMonth.of(year, month))
                                yearExpanded = false
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
