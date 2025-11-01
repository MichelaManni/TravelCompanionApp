package com.example.travelcompanionapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.*
import com.example.travelcompanionapp.R
import com.example.travelcompanionapp.data.Trip
import com.example.travelcompanionapp.viewmodel.TripViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Schermata per il tracciamento GPS di un viaggio con Google Maps.
 *
 * ⭐ MIGLIORAMENTI:
 * - Distanza minima richiesta per salvare (0.1 km)
 * - Dialog di conferma se la distanza è troppo bassa
 * - Messaggio chiaro all'utente
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripTrackingScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Stato locale per il tracciamento
    var selectedTrip by remember { mutableStateOf<Trip?>(null) }
    var isTracking by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var totalDistance by remember { mutableStateOf(0.0) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // ⭐ NUOVO: Dialog per confermare salvataggio con distanza bassa
    var showLowDistanceDialog by remember { mutableStateOf(false) }

    // Camera position per Google Maps
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(42.5, 12.5), 6f)
    }

    // Client per il servizio di localizzazione
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Controlla i permessi all'avvio
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Timer per il tempo trascorso
    LaunchedEffect(isTracking) {
        if (isTracking) {
            while (isTracking) {
                delay(1000)
                elapsedTime += 1
            }
        }
    }

    // Aggiorna la camera quando cambia la posizione durante il tracking
    LaunchedEffect(currentLocation, isTracking) {
        if (isTracking && currentLocation != null) {
            val newPosition = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(newPosition, 15f)
        }
    }

    // Launcher per richiedere i permessi
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Callback per gli aggiornamenti di posizione
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location

                    if (isTracking) {
                        val newPoint = LatLng(location.latitude, location.longitude)

                        // Calcola la distanza dal punto precedente
                        if (routePoints.isNotEmpty()) {
                            val lastPoint = routePoints.last()
                            val results = FloatArray(1)
                            Location.distanceBetween(
                                lastPoint.latitude, lastPoint.longitude,
                                newPoint.latitude, newPoint.longitude,
                                results
                            )
                            totalDistance += results[0] / 1000.0 // Converti in km
                        }

                        // Aggiungi il nuovo punto al percorso
                        routePoints = routePoints + newPoint
                    }
                }
            }
        }
    }

    // Funzione per avviare il tracciamento
    fun startTracking() {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        if (selectedTrip == null) return

        isTracking = true
        routePoints = emptyList()
        totalDistance = 0.0
        elapsedTime = 0L

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000 // Aggiornamento ogni 5 secondi
        ).setMinUpdateIntervalMillis(2000).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } catch (e: SecurityException) {
            // Gestisci l'eccezione
        }
    }

    fun saveTrip() {
        selectedTrip?.let { trip ->
            val updatedTrip = trip.copy(
                totalDistanceKm = totalDistance,
                status = "Completato",
                isCompleted = true
            )
            viewModel.updateTrip(updatedTrip)

            // Reset della schermata
            selectedTrip = null
            routePoints = emptyList()
            totalDistance = 0.0
            elapsedTime = 0L
        }
    }

    // ⭐ NUOVA FUNZIONE: Verifica distanza prima di salvare
    fun stopTracking() {
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // ⭐ CONTROLLO: Distanza minima 0.1 km (100 metri)
        if (totalDistance < 0.1) {
            // Mostra dialog di conferma
            showLowDistanceDialog = true
        } else {
            // Salva direttamente se la distanza è sufficiente
            saveTrip()
        }
    }


    // Lottie Animation per il titolo
    val headerComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.inizia_viaggio_menu)
    )
    val headerProgress by animateLottieCompositionAsState(
        composition = headerComposition,
        iterations = LottieConstants.IterateForever
    )

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LottieAnimation(
                            composition = headerComposition,
                            progress = headerProgress,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Inizia Viaggio",
                            color = TravelGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna Indietro",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Selettore viaggio
            if (!isTracking && selectedTrip == null) {
                TripSelector(
                    trips = uiState.tripList.filter { !it.isCompleted },
                    onTripSelected = { selectedTrip = it }
                )
            }

            // Pannello informazioni viaggio
            selectedTrip?.let { trip ->
                TripInfoPanel(
                    trip = trip,
                    isTracking = isTracking,
                    distance = totalDistance,
                    elapsedTime = elapsedTime
                )
            }

            // Google Map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = hasLocationPermission
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        myLocationButtonEnabled = hasLocationPermission
                    )
                ) {
                    // Marker posizione corrente
                    currentLocation?.let { location ->
                        Marker(
                            state = rememberMarkerState(
                                position = LatLng(location.latitude, location.longitude)
                            ),
                            title = "Posizione Attuale"
                        )
                    }

                    // Disegna il percorso
                    if (routePoints.isNotEmpty()) {
                        Polyline(
                            points = routePoints,
                            color = TravelGreen,
                            width = 8f
                        )
                    }
                }

                // Messaggio permessi
                if (!hasLocationPermission) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📍", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Permesso GPS Richiesto",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Per tracciare il viaggio serve accedere alla posizione",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Pulsanti di controllo
            if (selectedTrip != null) {
                TrackingControls(
                    isTracking = isTracking,
                    hasPermission = hasLocationPermission,
                    onStartClick = { startTracking() },
                    onStopClick = { stopTracking() }
                )
            }
        }

        // ⭐ NUOVO: Dialog di conferma per distanza bassa
        if (showLowDistanceDialog) {
            AlertDialog(
                onDismissRequest = {
                    showLowDistanceDialog = false
                    // Reset senza salvare
                    selectedTrip = null
                    routePoints = emptyList()
                    totalDistance = 0.0
                    elapsedTime = 0L
                },
                icon = {
                    Text(text = "⚠️", fontSize = 48.sp)
                },
                title = {
                    Text(
                        text = "Distanza Troppo Bassa",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Hai percorso solo ${String.format("%.0f", totalDistance * 1000)} metri.",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vuoi salvare comunque questo viaggio come completato?",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLowDistanceDialog = false
                            saveTrip() // Salva comunque
                        }
                    ) {
                        Text("Salva Comunque", color = TravelGreen)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showLowDistanceDialog = false
                            // Reset senza salvare
                            selectedTrip = null
                            routePoints = emptyList()
                            totalDistance = 0.0
                            elapsedTime = 0L
                        }
                    ) {
                        Text("Annulla", color = Color.Gray)
                    }
                },
                containerColor = Color.White
            )
        }
    }
}

// === COMPONENTI RIUTILIZZABILI (invariati) ===

@Composable
fun TripSelector(
    trips: List<Trip>,
    onTripSelected: (Trip) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Seleziona un viaggio da tracciare",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TravelGreen
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (trips.isEmpty()) {
                Text(
                    text = "Nessun viaggio disponibile. Crea prima un nuovo viaggio!",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                trips.forEach { trip ->
                    TripSelectorItem(trip = trip, onClick = { onTripSelected(trip) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TripSelectorItem(trip: Trip, onClick: () -> Unit) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = TravelGreen.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.destination,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = "${dateFormatter.format(trip.startDate)} - ${dateFormatter.format(trip.endDate)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Surface(
                color = TravelGreen,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = trip.tripType,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun TripInfoPanel(
    trip: Trip,
    isTracking: Boolean,
    distance: Double,
    elapsedTime: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTracking) TravelGreen.copy(alpha = 0.1f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = trip.destination,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text(
                        text = trip.tripType,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                if (isTracking) {
                    Surface(
                        color = Color.Red,
                        shape = CircleShape,
                        modifier = Modifier.size(12.dp)
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatisticItem(
                    label = "Distanza",
                    value = "${String.format("%.2f", distance)} km",
                    icon = "🗺️"
                )

                val hours = elapsedTime / 3600
                val minutes = (elapsedTime % 3600) / 60
                val seconds = elapsedTime % 60
                StatisticItem(
                    label = "Tempo",
                    value = String.format("%02d:%02d:%02d", hours, minutes, seconds),
                    icon = "⏱️"
                )
            }
        }
    }
}

@Composable
fun StatisticItem(label: String, value: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 24.sp)
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = TravelGreen
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun TrackingControls(
    isTracking: Boolean,
    hasPermission: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            if (!isTracking) {
                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TravelGreen)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Inizia Tracciamento",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onStopClick,
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ferma e Salva",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}