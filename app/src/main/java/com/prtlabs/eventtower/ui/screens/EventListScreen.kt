package com.prtlabs.eventtower.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prtlabs.eventtower.data.Event
import com.prtlabs.eventtower.ui.components.EventCard
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    title: String,
    events: List<Event>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    onAddEventClick: () -> Unit,
    onEventClick: (Event) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    isUpcoming: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<Event?>(null) }
    
    var isTodayExpanded by remember { mutableStateOf(true) }
    var isNext10Expanded by remember { mutableStateOf(true) }
    var isUpcomingExpanded by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search events...") },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                singleLine = true
                            )
                        } else {
                            Text(title, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) onSearchQueryChange("")
                        }) {
                            Icon(
                                if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Settings") }, 
                                onClick = { 
                                    showMenu = false
                                    onSettingsClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Help") }, 
                                onClick = { 
                                    showMenu = false
                                    onHelpClick()
                                }
                            )
                        }
                    }
                )
                
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        CategoryChip(
                            label = "All",
                            isSelected = selectedCategory == null,
                            onClick = { onCategorySelect(null) }
                        )
                    }
                    items(categories) { category ->
                        CategoryChip(
                            label = category,
                            isSelected = category == selectedCategory,
                            onClick = { onCategorySelect(category) }
                        )
                    }
                }
            }
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
        val today = LocalDate.now()
        
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty() || selectedCategory != null) 
                        "No events match your filters." else "No events found.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (isUpcoming) {
                    val todayEvents = events.filter { it.date.isEqual(today) }
                    val next10DaysEvents = events.filter { 
                        val diff = ChronoUnit.DAYS.between(today, it.date)
                        diff in 1..10 
                    }
                    val futureEvents = events.filter { 
                        ChronoUnit.DAYS.between(today, it.date) > 10 
                    }

                    if (todayEvents.isNotEmpty()) {
                        item { 
                            SectionHeader(
                                title = "Today", 
                                count = todayEvents.size, 
                                isExpanded = isTodayExpanded, 
                                icon = Icons.Default.Today,
                                accentColor = Color(0xFFFBC02D), 
                                onToggle = { isTodayExpanded = !isTodayExpanded }
                            ) 
                        }
                        if (isTodayExpanded) {
                            items(todayEvents, key = { it.id }) { event ->
                                SwipeToDeleteContainer(
                                    onDelete = { eventToDelete = event }
                                ) {
                                    EventCard(event = event, onClick = { onEventClick(event) }, onDelete = { eventToDelete = event })
                                }
                            }
                        }
                    }
                    if (next10DaysEvents.isNotEmpty()) {
                        item { 
                            SectionHeader(
                                title = "Next 10 Days", 
                                count = next10DaysEvents.size, 
                                isExpanded = isNext10Expanded, 
                                icon = Icons.Default.EventAvailable,
                                accentColor = Color(0xFF81C784), 
                                onToggle = { isNext10Expanded = !isNext10Expanded }
                            ) 
                        }
                        if (isNext10Expanded) {
                            items(next10DaysEvents, key = { it.id }) { event ->
                                SwipeToDeleteContainer(
                                    onDelete = { eventToDelete = event }
                                ) {
                                    EventCard(event = event, onClick = { onEventClick(event) }, onDelete = { eventToDelete = event })
                                }
                            }
                        }
                    }
                    if (futureEvents.isNotEmpty()) {
                        item { 
                            SectionHeader(
                                title = "Upcoming", 
                                count = futureEvents.size, 
                                isExpanded = isUpcomingExpanded, 
                                icon = Icons.Default.CalendarMonth,
                                accentColor = Color.Gray,
                                onToggle = { isUpcomingExpanded = !isUpcomingExpanded }
                            ) 
                        }
                        if (isUpcomingExpanded) {
                            items(futureEvents, key = { it.id }) { event ->
                                SwipeToDeleteContainer(
                                    onDelete = { eventToDelete = event }
                                ) {
                                    EventCard(event = event, onClick = { onEventClick(event) }, onDelete = { eventToDelete = event })
                                }
                            }
                        }
                    }
                } else {
                    items(events, key = { it.id }) { event ->
                        SwipeToDeleteContainer(
                            onDelete = { eventToDelete = event }
                        ) {
                            EventCard(event = event, onClick = { onEventClick(event) }, onDelete = { eventToDelete = event })
                        }
                    }
                }
            }
        }
    }

    if (eventToDelete != null) {
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Delete Event") },
            text = { Text("Are you sure you want to delete '${eventToDelete?.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        eventToDelete?.let { onDeleteEvent(it) }
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                false 
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val color = if (isSwiping) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
            } else Color.Transparent
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isSwiping) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.padding(start = 24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        content = { content() }
    )
}

@Composable
fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SectionHeader(
    title: String, 
    count: Int, 
    isExpanded: Boolean, 
    icon: ImageVector, 
    accentColor: Color,
    onToggle: () -> Unit
) {
    Surface(
        color = Color(0xFF1A1C1E),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = accentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (accentColor == Color.Gray) Color.White else accentColor
            )
            
            Surface(
                color = accentColor.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (accentColor == Color.Gray) Color.White else accentColor
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.Gray
            )
        }
    }
}
