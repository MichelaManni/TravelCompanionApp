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


//activity principale dell’app, punto di ingresso dell’interfaccia compose
//gestisce i permessi runtime, inizializza il viewmodel e imposta la navigazione tra le schermate
class MainActivity : ComponentActivity() {

    //launcher che mostra il dialog di sistema per richiedere più permessi contemporaneamente
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        //callback eseguita quando l’utente risponde alla richiesta dei permessi

        val notificationGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
        val activityGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: true

        println("📱 Permessi: Notifiche=$notificationGranted, Activity=$activityGranted")

        //se il permesso per activity recognition è stato concesso, avvia il monitoraggio automatico
        if (activityGranted) {
            ActivityRecognitionHelper.startActivityRecognition(this)
        }
    }

    //funzione chiamata alla creazione dell’activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //richiede i permessi necessari per notifiche e activity recognition
        requestNecessaryPermissions()

        //imposta il contenuto visivo dell’app usando jetpack compose
        setContent {
            TravelCompanionAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //ottiene il repository condiviso tramite l’applicazione
                    val appRepository = (application as TravelCompanionApplication).repository
                    //crea un’istanza di tripviewmodel usando la factory
                    val viewModel: TripViewModel = viewModel(
                        factory = TripViewModel.Factory(appRepository)
                    )
                    //carica il sistema di navigazione principale dell’app
                    TravelCompanionNavHost(viewModel = viewModel)
                }
            }
        }
    }

    //funzione privata che verifica e richiede i permessi runtime richiesti da android 10+ e 13+
    private fun requestNecessaryPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        //controlla il permesso per le notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        //controlla il permesso per il rilevamento attività
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        //se ci sono permessi mancanti, avvia il launcher per mostrarne la richiesta all’utente
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

//=== oggetto che definisce tutte le rotte di navigazione interne dell’app ===
object Destinations {
    const val SPLASH_ROUTE = "splash_route" //schermata iniziale
    const val MAIN_MENU_ROUTE = "main_menu_route" //menu principale
    const val LIST_ROUTE = "list_route" //lista viaggi salvati
    const val ENTRY_ROUTE = "entry_route" //form di inserimento viaggio
    const val TRACKING_ROUTE = "tracking_route" //schermata di tracking gps
    const val BACKGROUND_SETTINGS_ROUTE = "background_settings_route" //sezione statistiche o impostazioni
    const val MAP_SELECTION_ROUTE = "map_selection_route" //mappa per selezionare destinazione
    const val TRIP_DETAIL_ROUTE = "trip_detail_route/{tripId}" //dettaglio singolo viaggio

    //funzione di utilità per costruire dinamicamente la rotta del dettaglio
    fun tripDetailRoute(tripId: Int) = "trip_detail_route/$tripId"
}

//=== composable che gestisce tutta la navigazione tra le schermate dell’app ===
@Composable
fun TravelCompanionNavHost(viewModel: TripViewModel) {
    val navController = rememberNavController() //gestisce la navigazione tra le schermate
    val uiState by viewModel.uiState.collectAsState() //osserva lo stato dell’elenco viaggi

    NavHost(
        navController = navController,
        startDestination = Destinations.SPLASH_ROUTE //schermata iniziale
    ) {
        //schermata splash con pulsante per iniziare
        composable(Destinations.SPLASH_ROUTE) {
            SchermataIniziale(
                onStartClick = {
                    navController.navigate(Destinations.MAIN_MENU_ROUTE) {
                        popUpTo(Destinations.SPLASH_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        //menu principale con accesso alle sezioni principali
        composable(Destinations.MAIN_MENU_ROUTE) {
            MainMenuScreen(
                onAddTripClick = { navController.navigate(Destinations.ENTRY_ROUTE) },
                onViewListClick = { navController.navigate(Destinations.LIST_ROUTE) },
                onStartTrackingClick = { navController.navigate(Destinations.TRACKING_ROUTE) },
                onBackgroundSettingsClick = { navController.navigate(Destinations.BACKGROUND_SETTINGS_ROUTE) }
            )
        }

        //schermata di inserimento nuovo viaggio
        composable(Destinations.ENTRY_ROUTE) {
            TripEntryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMapSelection = { navController.navigate(Destinations.MAP_SELECTION_ROUTE) }
            )
        }

        //schermata mappa per selezionare una destinazione
        composable(Destinations.MAP_SELECTION_ROUTE) {
            MapSelectionScreen(
                onDestinationSelected = viewModel::updateDestinationCoordinates,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        //lista di tutti i viaggi registrati
        composable(Destinations.LIST_ROUTE) {
            TripListScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onTripClick = { trip -> navController.navigate(Destinations.tripDetailRoute(trip.id)) }
            )
        }

        //dettaglio di un singolo viaggio selezionato dalla lista
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
                //se il viaggio non è trovato, torna alla schermata precedente
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }

        //schermata di tracciamento in tempo reale (gps)
        composable(Destinations.TRACKING_ROUTE) {
            TripTrackingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        //schermata che mostra grafici o impostazioni avanzate
        composable(Destinations.BACKGROUND_SETTINGS_ROUTE) {
            DisplayChartsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

//composable di placeholder per funzionalità non ancora implementate
@Composable
fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Questa funzionalità sarà implementata prossimamente",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}