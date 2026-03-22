package com.prtlabs.eventtower.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prtlabs.eventtower.data.Event
import com.prtlabs.eventtower.ui.components.EventCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    title: String,
    events: List<Event>,
    onAddEventClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(title) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEventClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No events found.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(events) { event ->
                    EventCard(event = event)
                }
            }
        }
    }
}
