package com.prtlabs.eventtower.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prtlabs.eventtower.data.Event
import com.prtlabs.eventtower.data.Recurring
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(onEventAdded: (Event) -> Unit, onBack: () -> Unit) {
    var title by remember { mutableStateFlowOf("") }
    var date by remember { mutableStateFlowOf(LocalDate.now()) }
    var category by remember { mutableStateFlowOf("Personal") }
    var notes by remember { mutableStateFlowOf("") }
    var recurring by remember { mutableStateFlowOf(Recurring.NO) }

    var showDatePicker by remember { mutableStateFlowOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Event") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                onValueChange = { },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Text("Select")
                    }
                }
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Recurring", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Recurring.values().forEach { option ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = (recurring == option),
                            onClick = { recurring = option }
                        )
                        Text(text = option.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onEventAdded(Event(title = title, date = date, category = category, notes = notes, recurring = recurring))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank()
            ) {
                Text("Create Event")
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun <T> mutableStateFlowOf(value: T) = mutableStateOf(value)
