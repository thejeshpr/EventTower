package com.prtlabs.eventtower

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prtlabs.eventtower.ui.MainViewModel
import com.prtlabs.eventtower.ui.MainViewModelFactory
import com.prtlabs.eventtower.ui.screens.EventFormScreen
import com.prtlabs.eventtower.ui.screens.EventListScreen
import com.prtlabs.eventtower.ui.theme.EventTowerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EventTowerTheme(darkTheme = true) {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Upcoming : Screen("upcoming", "Upcoming", Icons.Default.Upcoming)
    object Past : Screen("past", "Past", Icons.Default.History)
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as EventApplication
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(app.database.eventDao())
    )
    val scope = rememberCoroutineScope()

    val navController = rememberNavController()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState(initial = emptyList())
    val pastEvents by viewModel.pastEvents.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    var importResult by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var exportResult by remember { mutableStateOf<Int?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val json = viewModel.exportToJson()
                val count = (upcomingEvents.size + pastEvents.size) // This is filtered, but for export we use allEvents.value.size inside VM usually. 
                // Actually allEvents is internal to VM. Let's just say "Data exported".
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(json.toByteArray())
                }
                // To show count accurately, we should get it from allEvents in VM. 
                // Since exportToJson is synchronous and uses current value of stateIn, it's fine.
                exportResult = 1 // Just a trigger to show dialog
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

    val items = listOf(Screen.Upcoming, Screen.Past)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            val currentRoute = currentDestination?.route
            if (currentRoute == Screen.Upcoming.route || currentRoute == Screen.Past.route) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Upcoming.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Upcoming.route) {
                EventListScreen(
                    title = "Upcoming Events",
                    events = upcomingEvents,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { viewModel.onCategorySelect(it) },
                    onAddEventClick = { navController.navigate("add_event") },
                    onEventClick = { event -> navController.navigate("edit_event/${event.id}") },
                    onDeleteEvent = { viewModel.deleteEvent(it) },
                    onExportClick = { exportLauncher.launch("events_backup.json") },
                    onImportClick = { importLauncher.launch(arrayOf("application/json")) },
                    isUpcoming = true
                )
            }
            composable(Screen.Past.route) {
                EventListScreen(
                    title = "Past Events",
                    events = pastEvents,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { viewModel.onCategorySelect(it) },
                    onAddEventClick = { navController.navigate("add_event") },
                    onEventClick = { event -> navController.navigate("edit_event/${event.id}") },
                    onDeleteEvent = { viewModel.deleteEvent(it) },
                    onExportClick = { exportLauncher.launch("events_backup.json") },
                    onImportClick = { importLauncher.launch(arrayOf("application/json")) },
                    isUpcoming = false
                )
            }
            composable("add_event") {
                EventFormScreen(
                    categories = categories,
                    onSave = { event ->
                        viewModel.addEvent(event)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_event/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId")
                val event by viewModel.getEventById(eventId ?: "").collectAsState(initial = null)
                
                event?.let {
                    EventFormScreen(
                        event = it,
                        categories = categories,
                        onSave = { updatedEvent ->
                            viewModel.updateEvent(updatedEvent)
                            navController.popBackStack()
                        },
                        onDelete = { eventToDelete ->
                            viewModel.deleteEvent(eventToDelete)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
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
