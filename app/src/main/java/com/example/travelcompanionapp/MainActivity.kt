package com.example.travelcompanionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcompanionapp.ui.TripEntryScreen
import com.example.travelcompanionapp.ui.SchermataIniziale
// ⭐ NUOVI IMPORT
import com.example.travelcompanionapp.ui.MainMenuScreen
import com.example.travelcompanionapp.ui.MapSelectionScreen // Importa la nuova schermata
// ⭐ FINE NUOVI IMPORT
import com.example.travelcompanionapp.ui.theme.TravelCompanionAppTheme
import com.example.travelcompanionapp.viewmodel.TripViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
// ⭐ IMPORT NECESSARIO per il cast dell'Application
import com.example.travelcompanionapp.TravelCompanionApplication


/**
 * Definizioni costanti delle rotte di navigazione (per evitare errori di battitura).
 */
object Destinations {
    const val SPLASH_ROUTE = "splash_route"
    const val MAIN_MENU_ROUTE = "main_menu_route"
    const val LIST_ROUTE = "list_route"
    const val ENTRY_ROUTE = "entry_route"
    const val TRACKING_ROUTE = "tracking_route"
    const val BACKGROUND_SETTINGS_ROUTE = "background_settings_route"
    // ⭐ NUOVA ROTTA
    const val MAP_SELECTION_ROUTE = "map_selection_route"
    // ⭐ FINE NUOVA ROTTA
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TravelCompanionAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ⭐ CORREZIONE: Uso della proprietà 'repository' anziché 'container'
                    val appRepository = (application as TravelCompanionApplication).repository

                    val viewModel: TripViewModel = viewModel(
                        // ⭐ Passiamo direttamente il repository al factory
                        factory = TripViewModel.Factory(appRepository)
                    )
                    TravelCompanionNavHost(viewModel = viewModel)
                }
            }
        }
    }
}


@Composable
fun TravelCompanionNavHost(
    viewModel: TripViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Destinations.SPLASH_ROUTE) {

        // 1. Splash Screen
        composable(Destinations.SPLASH_ROUTE) {
            SchermataIniziale(
                onStartClick = {
                    navController.navigate(Destinations.MAIN_MENU_ROUTE) {
                        popUpTo(Destinations.SPLASH_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        // 2. Menu Principale
        composable(Destinations.MAIN_MENU_ROUTE) {
            MainMenuScreen(
                onAddTripClick = { navController.navigate(Destinations.ENTRY_ROUTE) },
                onViewListClick = { navController.navigate(Destinations.LIST_ROUTE) },
                onStartTrackingClick = { navController.navigate(Destinations.TRACKING_ROUTE) },
                onBackgroundSettingsClick = { navController.navigate(Destinations.BACKGROUND_SETTINGS_ROUTE) }
            )
        }

        // 3. Inserimento Viaggio (Form)
        composable(Destinations.ENTRY_ROUTE) {
            TripEntryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                // ⭐ Implementa il callback per la navigazione alla mappa
                onNavigateToMapSelection = {
                    navController.navigate(Destinations.MAP_SELECTION_ROUTE)
                }
            )
        }

        // ⭐ 4. Selezione Mappa (OSMDROID)
        composable(Destinations.MAP_SELECTION_ROUTE) {
            MapSelectionScreen(
                // Il callback chiama il metodo nel ViewModel per salvare i dati
                onDestinationSelected = viewModel::updateDestinationCoordinates,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. Lista Viaggi (Placeholder)
        composable(Destinations.LIST_ROUTE) {
            PlaceholderListScreen(
                viewModel = viewModel,
                onAddTripClick = {
                    navController.navigate(Destinations.ENTRY_ROUTE)
                }
            )
        }

        // 6. Tracciamento (Placeholder)
        composable(Destinations.TRACKING_ROUTE) {
            PlaceholderScreen(title = "Tracciamento GPS")
        }

        // 7. Impostazioni Background (Placeholder)
        composable(Destinations.BACKGROUND_SETTINGS_ROUTE) {
            PlaceholderScreen(title = "Impostazioni Background")
        }
    }
}


/**
 * Componente generico per le rotte ancora da implementare.
 */
@Composable
fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Da implementare secondo le specifiche del progetto.", style = MaterialTheme.typography.bodyMedium)
    }
}


/**
 * Componente temporaneo (PlaceholderListScreen) per mostrare lo stato del database.
 */
@Composable
fun PlaceholderListScreen(viewModel: TripViewModel, onAddTripClick: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Stato del DB: ${if (uiState.isLoading) "Caricamento in corso..." else "OK"}")
        Text("Viaggi nel database: ${uiState.tripList.size}")
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onAddTripClick) {
            Text("Aggiungi Nuovo Viaggio (EntryScreen)")
        }
    }
}