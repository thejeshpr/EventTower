package com.prtlabs.eventtower

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prtlabs.eventtower.ui.MainViewModel
import com.prtlabs.eventtower.ui.MainViewModelFactory
import com.prtlabs.eventtower.ui.theme.EventTowerTheme
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EventTowerTheme(darkTheme = true) {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as EventApplication
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(app.database.eventDao())
    )
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var importResult by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var exportResult by remember { mutableStateOf<Int?>(null) }
    
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    var canScheduleExactAlarms by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        )
    }

    // Refresh permission status when returning to the activity
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val json = viewModel.exportToJson()
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(json.toByteArray())
                }
                exportResult = 1
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { it.readText() }
                if (json != null) {
                    importResult = viewModel.importFromJson(json)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                SettingsItem(
                    title = "Precise Notifications",
                    subtitle = if (canScheduleExactAlarms) "Enabled" else "Using inexact timing (may be slightly delayed)",
                    icon = Icons.Default.Alarm,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canScheduleExactAlarms) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    },
                    trailingContent = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Switch(
                                checked = canScheduleExactAlarms,
                                onCheckedChange = { checked ->
                                    if (checked && !canScheduleExactAlarms) {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                SettingsItem(
                    title = "Export Data",
                    subtitle = "Save your events to a JSON file",
                    icon = Icons.Default.FileDownload,
                    onClick = { exportLauncher.launch("events_backup.json") }
                )
            }
            item {
                SettingsItem(
                    title = "Import Data",
                    subtitle = "Restore events from a JSON file",
                    icon = Icons.Default.FileUpload,
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
            }
        }
    }

    if (exportResult != null) {
        AlertDialog(
            onDismissRequest = { exportResult = null },
            title = { Text("Export Successful") },
            text = { Text("Your events have been exported to JSON successfully.") },
            confirmButton = {
                TextButton(onClick = { exportResult = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (importResult != null) {
        val (new, overwritten) = importResult!!
        AlertDialog(
            onDismissRequest = { importResult = null },
            title = { Text(if (new == -1) "Import Failed" else "Import Result") },
            text = {
                if (new == -1) {
                    Text("There was an error importing the file. Please check if the format is correct.")
                } else {
                    Text("Import completed:\n- New events: $new\n- Overwritten: $overwritten")
                }
            },
            confirmButton = {
                TextButton(onClick = { importResult = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = trailingContent,
        modifier = Modifier.clickable(onClick = onClick)
    )
}
