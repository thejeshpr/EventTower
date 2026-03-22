package com.prtlabs.eventtower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.prtlabs.eventtower.ui.MainViewModel
import com.prtlabs.eventtower.ui.screens.AddEventScreen
import com.prtlabs.eventtower.ui.screens.EventListScreen
import com.prtlabs.eventtower.ui.theme.EventTowerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EventTowerTheme {
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
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val upcomingEvents by viewModel.upcomingEvents.collectAsState(initial = emptyList())
    val pastEvents by viewModel.pastEvents.collectAsState(initial = emptyList())

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
                    onAddEventClick = { navController.navigate("add_event") }
                )
            }
            composable(Screen.Past.route) {
                EventListScreen(
                    title = "Past Events",
                    events = pastEvents,
                    onAddEventClick = { navController.navigate("add_event") }
                )
            }
            composable("add_event") {
                AddEventScreen(
                    onEventAdded = { event ->
                        viewModel.addEvent(event)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
