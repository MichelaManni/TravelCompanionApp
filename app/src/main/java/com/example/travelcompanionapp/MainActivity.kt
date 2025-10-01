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
import com.example.travelcompanionapp.data.AppDatabase
import com.example.travelcompanionapp.repository.TripRepository
import com.example.travelcompanionapp.ui.TripEntryScreen
import com.example.travelcompanionapp.ui.theme.TravelCompanionAppTheme
import com.example.travelcompanionapp.viewmodel.TripViewModel
// Importazioni per la navigazione
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Definizioni costanti delle rotte di navigazione.
 */
object Destinations {
    const val LIST_ROUTE = "tripList"
    const val ENTRY_ROUTE = "tripEntry"
}

// === DICHIARAZIONE DEL REPOSITORY (NON DEL DATABASE) ===
// Il Repository è l'unica dipendenza di cui abbiamo bisogno a livello di Activity/App
// per costruire il ViewModel. Ora usa il Singleton del database.
private val repository by lazy {
    // L'uso di by lazy garantisce che venga creato solo la prima volta che viene richiesto.
    // L'istanza del database viene recuperata usando il contesto dell'applicazione.
    val database = AppDatabase.getDatabase(TravelCompanionApplication.context)
    TripRepository(database.tripDao())
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // **RIMOSSA** tutta la logica di inizializzazione del DB da qui.
        // Viene gestita dal Singleton e da TravelCompanionApplication.

        setContent {
            TravelCompanionAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TravelCompanionApp()
                }
            }
        }
    }
}

/**
 * Composabile principale che gestisce la logica di navigazione e inietta il ViewModel.
 */
@Composable
fun TravelCompanionApp(
    modifier: Modifier = Modifier,
    // Il ViewModel viene creato usando la Factory che dipende dal Repository Singleton.
    viewModel: TripViewModel = viewModel(
        factory = TripViewModel.Factory(repository)
    )
) {
    // ... (Il resto del codice NavHost e composables è INVARIATO)
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.LIST_ROUTE,
        modifier = modifier
    ) {
        composable(Destinations.LIST_ROUTE) {
            PlaceholderListScreen(
                viewModel = viewModel,
                onAddTripClick = {
                    navController.navigate(Destinations.ENTRY_ROUTE)
                }
            )
        }

        composable(Destinations.ENTRY_ROUTE) {
            TripEntryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Componente temporaneo (PlaceholderListScreen) INVARIATO.
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