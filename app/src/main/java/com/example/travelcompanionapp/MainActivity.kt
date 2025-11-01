package com.example.travelcompanionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
// Import per le schermate
import com.example.travelcompanionapp.ui.*
import com.example.travelcompanionapp.ui.theme.TravelCompanionAppTheme
import com.example.travelcompanionapp.viewmodel.TripViewModel

/**
 * Definizioni costanti delle rotte di navigazione.
 */
object Destinations {
    const val SPLASH_ROUTE = "splash_route"
    const val MAIN_MENU_ROUTE = "main_menu_route"
    const val LIST_ROUTE = "list_route"
    const val ENTRY_ROUTE = "entry_route"
    const val TRACKING_ROUTE = "tracking_route"
    const val BACKGROUND_SETTINGS_ROUTE = "background_settings_route"
    const val MAP_SELECTION_ROUTE = "map_selection_route"
    const val TRIP_DETAIL_ROUTE = "trip_detail_route/{tripId}" // ⭐ NUOVO

    // ⭐ NUOVA funzione helper per creare la rotta con parametro
    fun tripDetailRoute(tripId: Int) = "trip_detail_route/$tripId"
}

/**
 * Activity principale dell'app.
 * Gestisce la creazione del ViewModel e la navigazione tra schermate.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}

/**
 * Sistema di navigazione dell'app.
 * Definisce tutte le schermate e come navigare tra loro.
 *
 * ⭐ AGGIORNAMENTO: Aggiunta TripDetailScreen
 */
@Composable
fun TravelCompanionNavHost(
    viewModel: TripViewModel
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Destinations.SPLASH_ROUTE
    ) {

        // === 1. SPLASH SCREEN ===
        composable(Destinations.SPLASH_ROUTE) {
            SchermataIniziale(
                onStartClick = {
                    navController.navigate(Destinations.MAIN_MENU_ROUTE) {
                        popUpTo(Destinations.SPLASH_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        // === 2. MENU PRINCIPALE ===
        composable(Destinations.MAIN_MENU_ROUTE) {
            MainMenuScreen(
                onAddTripClick = {
                    navController.navigate(Destinations.ENTRY_ROUTE)
                },
                onViewListClick = {
                    navController.navigate(Destinations.LIST_ROUTE)
                },
                onStartTrackingClick = {
                    navController.navigate(Destinations.TRACKING_ROUTE)
                },
                onBackgroundSettingsClick = {
                    navController.navigate(Destinations.BACKGROUND_SETTINGS_ROUTE)
                }
            )
        }

        // === 3. INSERIMENTO NUOVO VIAGGIO ===
        composable(Destinations.ENTRY_ROUTE) {
            TripEntryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMapSelection = {
                    navController.navigate(Destinations.MAP_SELECTION_ROUTE)
                }
            )
        }

        // === 4. SELEZIONE DESTINAZIONE SU MAPPA ===
        composable(Destinations.MAP_SELECTION_ROUTE) {
            MapSelectionScreen(
                onDestinationSelected = viewModel::updateDestinationCoordinates,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // === 5. LISTA VIAGGI ===
        composable(Destinations.LIST_ROUTE) {
            TripListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onTripClick = { trip ->
                    // ⭐ NUOVO: Naviga alla schermata dettaglio
                    navController.navigate(Destinations.tripDetailRoute(trip.id))
                }
            )
        }

        // === 6. ⭐ NUOVO: DETTAGLIO VIAGGIO ===
        composable(
            route = Destinations.TRIP_DETAIL_ROUTE,
            arguments = listOf(
                navArgument("tripId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getInt("tripId") ?: return@composable

            // Trova il viaggio nell lista
            val trip = uiState.tripList.find { it.id == tripId }

            if (trip != null) {
                TripDetailScreen(
                    trip = trip,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                // Se il viaggio non esiste, torna indietro
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        // === 7. TRACCIAMENTO GPS ===
        composable(Destinations.TRACKING_ROUTE) {
            TripTrackingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // === 8. IMPOSTAZIONI BACKGROUND (Placeholder) ===
        composable(Destinations.BACKGROUND_SETTINGS_ROUTE) {
            PlaceholderScreen(title = "Operazioni Avanzate")
        }
    }
}

/**
 * Schermata segnaposto per le funzionalità non ancora implementate.
 */
@Composable
fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Questa funzionalità sarà implementata prossimamente",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}