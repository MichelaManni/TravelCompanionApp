package com.example.travelcompanionapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.travelcompanionapp.ui.*
import com.example.travelcompanionapp.ui.theme.TravelCompanionAppTheme
import com.example.travelcompanionapp.utils.ActivityRecognitionHelper
import com.example.travelcompanionapp.viewmodel.TripViewModel

/**
 * Activity principale dell'app.
 *
 * ⭐ AGGIORNATA per richiedere permessi per background jobs:
 * - POST_NOTIFICATIONS (Android 13+)
 * - ACTIVITY_RECOGNITION (Android 10+)
 */
class MainActivity : ComponentActivity() {

    // ⭐ Launcher per richiedere permessi multipli
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Callback quando l'utente risponde alla richiesta permessi

        val notificationGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
        val activityGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: true

        println("📱 Permessi: Notifiche=$notificationGranted, Activity=$activityGranted")

        // Se il permesso Activity Recognition è stato concesso, avvia il monitoraggio
        if (activityGranted) {
            ActivityRecognitionHelper.startActivityRecognition(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⭐ Richiedi permessi necessari all'avvio
        requestNecessaryPermissions()

        setContent {
            TravelCompanionAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val appRepository = (application as TravelCompanionApplication).repository
                    val viewModel: TripViewModel = viewModel(
                        factory = TripViewModel.Factory(appRepository)
                    )
                    TravelCompanionNavHost(viewModel = viewModel)
                }
            }
        }
    }

    /**
     * Richiede i permessi necessari per i background jobs.
     *
     * Permessi richiesti:
     * - POST_NOTIFICATIONS (Android 13+): per inviare notifiche
     * - ACTIVITY_RECOGNITION (Android 10+): per rilevare movimento
     */
    private fun requestNecessaryPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Permesso notifiche (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Permesso Activity Recognition (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        // Se ci sono permessi da richiedere, mostra il dialog di sistema
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

// === Resto del codice invariato (Destinations, NavHost, ecc.) ===
object Destinations {
    const val SPLASH_ROUTE = "splash_route"
    const val MAIN_MENU_ROUTE = "main_menu_route"
    const val LIST_ROUTE = "list_route"
    const val ENTRY_ROUTE = "entry_route"
    const val TRACKING_ROUTE = "tracking_route"
    const val BACKGROUND_SETTINGS_ROUTE = "background_settings_route"
    const val MAP_SELECTION_ROUTE = "map_selection_route"
    const val TRIP_DETAIL_ROUTE = "trip_detail_route/{tripId}"

    fun tripDetailRoute(tripId: Int) = "trip_detail_route/$tripId"
}

@Composable
fun TravelCompanionNavHost(viewModel: TripViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Destinations.SPLASH_ROUTE
    ) {
        composable(Destinations.SPLASH_ROUTE) {
            SchermataIniziale(
                onStartClick = {
                    navController.navigate(Destinations.MAIN_MENU_ROUTE) {
                        popUpTo(Destinations.SPLASH_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.MAIN_MENU_ROUTE) {
            MainMenuScreen(
                onAddTripClick = { navController.navigate(Destinations.ENTRY_ROUTE) },
                onViewListClick = { navController.navigate(Destinations.LIST_ROUTE) },
                onStartTrackingClick = { navController.navigate(Destinations.TRACKING_ROUTE) },
                onBackgroundSettingsClick = { navController.navigate(Destinations.BACKGROUND_SETTINGS_ROUTE) }
            )
        }

        composable(Destinations.ENTRY_ROUTE) {
            TripEntryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMapSelection = { navController.navigate(Destinations.MAP_SELECTION_ROUTE) }
            )
        }

        composable(Destinations.MAP_SELECTION_ROUTE) {
            MapSelectionScreen(
                onDestinationSelected = viewModel::updateDestinationCoordinates,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.LIST_ROUTE) {
            TripListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onTripClick = { trip -> navController.navigate(Destinations.tripDetailRoute(trip.id)) }
            )
        }

        composable(
            route = Destinations.TRIP_DETAIL_ROUTE,
            arguments = listOf(navArgument("tripId") { type = NavType.IntType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getInt("tripId") ?: return@composable
            val trip = uiState.tripList.find { it.id == tripId }
            if (trip != null) {
                TripDetailScreen(
                    trip = trip,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        composable(Destinations.TRACKING_ROUTE) {
            TripTrackingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ⭐ Display Charts Screen
        composable(Destinations.BACKGROUND_SETTINGS_ROUTE) {
            DisplayChartsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Questa funzionalità sarà implementata prossimamente", style = MaterialTheme.typography.bodyMedium)
    }
}