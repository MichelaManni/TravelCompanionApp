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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
// Import per le schermate
import com.example.travelcompanionapp.ui.TripEntryScreen
import com.example.travelcompanionapp.ui.SchermataIniziale
import com.example.travelcompanionapp.ui.MainMenuScreen
import com.example.travelcompanionapp.ui.MapSelectionScreen
import com.example.travelcompanionapp.ui.TripListScreen
import com.example.travelcompanionapp.ui.TripTrackingScreen // â­ AGGIUNTO: Import per il tracking GPS
import com.example.travelcompanionapp.ui.theme.TravelCompanionAppTheme
import com.example.travelcompanionapp.viewmodel.TripViewModel

/**
 * Definizioni costanti delle rotte di navigazione.
 * Usiamo costanti per evitare errori di battitura.
 */
object Destinations {
    const val SPLASH_ROUTE = "splash_route"
    const val MAIN_MENU_ROUTE = "main_menu_route"
    const val LIST_ROUTE = "list_route"
    const val ENTRY_ROUTE = "entry_route"
    const val TRACKING_ROUTE = "tracking_route"
    const val BACKGROUND_SETTINGS_ROUTE = "background_settings_route"
    const val MAP_SELECTION_ROUTE = "map_selection_route"
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
                    // Otteniamo il repository dall'Application
                    val appRepository = (application as TravelCompanionApplication).repository

                    // Creiamo il ViewModel usando il Factory pattern
                    val viewModel: TripViewModel = viewModel(
                        factory = TripViewModel.Factory(appRepository)
                    )

                    // Avviamo il sistema di navigazione
                    TravelCompanionNavHost(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Sistema di navigazione dell'app.
 * Definisce tutte le schermate e come navigare tra loro.
 */
@Composable
fun TravelCompanionNavHost(
    viewModel: TripViewModel
) {
    // Controller per gestire la navigazione
    val navController = rememberNavController()

    // NavHost definisce tutte le "destinazioni" (schermate) dell'app
    NavHost(
        navController = navController,
        startDestination = Destinations.SPLASH_ROUTE // Schermata iniziale
    ) {

        // === 1. SPLASH SCREEN (Schermata di benvenuto) ===
        composable(Destinations.SPLASH_ROUTE) {
            SchermataIniziale(
                onStartClick = {
                    // Quando clicca "Start", vai al menu principale
                    navController.navigate(Destinations.MAIN_MENU_ROUTE) {
                        // Rimuove lo splash dallo stack (non può tornare indietro)
                        popUpTo(Destinations.SPLASH_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        // === 2. MENU PRINCIPALE ===
        composable(Destinations.MAIN_MENU_ROUTE) {
            MainMenuScreen(
                // Quando clicca "Nuovo Viaggio"
                onAddTripClick = {
                    navController.navigate(Destinations.ENTRY_ROUTE)
                },
                // Quando clicca "I Miei Viaggi"
                onViewListClick = {
                    navController.navigate(Destinations.LIST_ROUTE)
                },
                // Quando clicca "Inizia Viaggio"
                onStartTrackingClick = {
                    navController.navigate(Destinations.TRACKING_ROUTE)
                },
                // Quando clicca "Op. Avanzate"
                onBackgroundSettingsClick = {
                    navController.navigate(Destinations.BACKGROUND_SETTINGS_ROUTE)
                }
            )
        }

        // === 3. INSERIMENTO NUOVO VIAGGIO ===
        composable(Destinations.ENTRY_ROUTE) {
            TripEntryScreen(
                viewModel = viewModel,
                // Torna indietro al menu
                onNavigateBack = { navController.popBackStack() },
                // Apre la mappa per selezionare destinazione
                onNavigateToMapSelection = {
                    navController.navigate(Destinations.MAP_SELECTION_ROUTE)
                }
            )
        }

        // === 4. SELEZIONE DESTINAZIONE SU MAPPA ===
        composable(Destinations.MAP_SELECTION_ROUTE) {
            MapSelectionScreen(
                // Quando seleziona una posizione, salva nel ViewModel
                onDestinationSelected = viewModel::updateDestinationCoordinates,
                // Torna alla schermata precedente (TripEntryScreen)
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // === 5. LISTA VIAGGI ===
        composable(Destinations.LIST_ROUTE) {
            TripListScreen(
                viewModel = viewModel,
                // Torna al menu principale
                onNavigateBack = { navController.popBackStack() },
                // Quando clicca su un viaggio (TODO: implementare dettaglio)
                onTripClick = { trip ->
                    // Per ora non fa nulla, in futuro aprirà una schermata dettaglio
                    // TODO: navigare a schermata dettaglio viaggio
                }
            )
        }

        // === 6. TRACCIAMENTO GPS (â­ AGGIORNATO) ===
        composable(Destinations.TRACKING_ROUTE) {
            TripTrackingScreen(
                viewModel = viewModel,
                // Torna al menu principale
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // === 7. IMPOSTAZIONI BACKGROUND (Placeholder - da implementare) ===
        composable(Destinations.BACKGROUND_SETTINGS_ROUTE) {
            PlaceholderScreen(title = "Operazioni Avanzate")
        }
    }
}

/**
 * Schermata segnaposto per le funzionalità non ancora implementate.
 * Mostra semplicemente un titolo e un messaggio.
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