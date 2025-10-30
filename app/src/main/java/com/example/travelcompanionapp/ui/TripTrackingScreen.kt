package com.example.travelcompanionapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.*
import com.example.travelcompanionapp.R
import com.example.travelcompanionapp.data.Trip
import com.example.travelcompanionapp.viewmodel.TripViewModel
import com.google.android.gms.location.*
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Schermata per il tracciamento GPS di un viaggio.
 * Permette di:
 * - Selezionare un viaggio dalla lista
 * - Avviare/fermare il tracciamento GPS
 * - Visualizzare il percorso sulla mappa
 * - Calcolare la distanza percorsa
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
    var routePoints by remember { mutableStateOf<List<GeoPoint>>(emptyList()) }
    var totalDistance by remember { mutableStateOf(0.0) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Client per il servizio di localizzazione
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Controlla i permessi all'avvio
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Timer per il tempo trascorso
    LaunchedEffect(isTracking) {
        if (isTracking) {
            while (isTracking) {
                delay(1000) // Aggiorna ogni secondo
                elapsedTime += 1
            }
        }
    }

    // Launcher per richiedere i permessi di localizzazione
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
                        val newPoint = GeoPoint(location.latitude, location.longitude)

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

        // Richiesta di aggiornamenti GPS
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000 // Aggiorna ogni 5 secondi
        ).setMinUpdateIntervalMillis(2000).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } catch (e: SecurityException) {
            // Gestisci l'eccezione di permessi
        }
    }

    // Funzione per fermare il tracciamento e salvare
    fun stopTracking() {
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Salva i dati nel viaggio
        selectedTrip?.let { trip ->
            val updatedTrip = trip.copy(
                totalDistanceKm = totalDistance,
                status = "Completato",
                isCompleted = true
            )
            viewModel.updateTrip(updatedTrip)
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
            // Selettore viaggio (se non sta tracciando)
            if (!isTracking && selectedTrip == null) {
                TripSelector(
                    trips = uiState.tripList.filter { !it.isCompleted },
                    onTripSelected = { selectedTrip = it }
                )
            }

            // Pannello informazioni viaggio selezionato
            selectedTrip?.let { trip ->
                TripInfoPanel(
                    trip = trip,
                    isTracking = isTracking,
                    distance = totalDistance,
                    elapsedTime = elapsedTime
                )
            }

            // Mappa
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                TrackingMapView(
                    currentLocation = currentLocation,
                    routePoints = routePoints,
                    isTracking = isTracking
                )

                // Messaggio se non ci sono permessi
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
                            Text(
                                text = "📍",
                                fontSize = 48.sp
                            )
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
    }
}

/**
 * Selettore per scegliere quale viaggio tracciare.
 */
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
                    TripSelectorItem(
                        trip = trip,
                        onClick = { onTripSelected(trip) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Singolo elemento nella lista di selezione viaggi.
 */
@Composable
fun TripSelectorItem(
    trip: Trip,
    onClick: () -> Unit
) {
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

/**
 * Pannello con le informazioni del viaggio in corso.
 */
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
                // Distanza
                StatisticItem(
                    label = "Distanza",
                    value = "${String.format("%.2f", distance)} km",
                    icon = "🗺️"
                )

                // Tempo
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

/**
 * Elemento statistico (distanza, tempo, ecc).
 */
@Composable
fun StatisticItem(
    label: String,
    value: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

/**
 * Mappa che mostra la posizione corrente e il percorso.
 */
@Composable
fun TrackingMapView(
    currentLocation: Location?,
    routePoints: List<GeoPoint>,
    isTracking: Boolean
) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)

                // Zoom iniziale sull'Italia
                controller.setZoom(6.0)
                controller.setCenter(GeoPoint(42.5, 12.5))
            }
        },
        update = { mapView ->
            mapView.overlays.clear()

            // Mostra posizione corrente
            currentLocation?.let { location ->
                val currentPoint = GeoPoint(location.latitude, location.longitude)

                // Marker posizione corrente
                val marker = Marker(mapView).apply {
                    position = currentPoint
                    title = "Posizione Attuale"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(marker)

                // Centra sulla posizione corrente se sta tracciando
                if (isTracking) {
                    mapView.controller.animateTo(currentPoint)
                    mapView.controller.setZoom(15.0)
                }
            }

            // Disegna il percorso
            if (routePoints.isNotEmpty()) {
                val polyline = Polyline().apply {
                    setPoints(routePoints)
                    outlinePaint.color = android.graphics.Color.parseColor("#008080")
                    outlinePaint.strokeWidth = 8f
                }
                mapView.overlays.add(polyline)
            }

            mapView.invalidate()
        }
    )
}

/**
 * Pulsanti per controllare il tracciamento.
 */
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TravelGreen
                    )
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
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