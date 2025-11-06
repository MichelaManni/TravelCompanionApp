package com.example.travelcompanionapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.travelcompanionapp.data.TripNote
import com.example.travelcompanionapp.viewmodel.TripViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Schermata per il tracciamento GPS di un viaggio con Google Maps.
 *
 * ⭐ NUOVE FUNZIONALITÀ:
 * - Floating Action Button per aggiungere note durante il viaggio
 * - Visualizzazione ultime 3 note
 * - Dialog per inserire il testo della nota con GPS automatico
 * - Geocoding inverso per ottenere il nome del luogo
 *
 * CONFORME ALLE SPECIFICHE:
 * "Allow users to attach photos (via the camera) and notes to specific moments
 *  or locations during the journey"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripTrackingScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    // Stato locale per il tracciamento
    var selectedTrip by remember { mutableStateOf<Trip?>(null) }
    var isTracking by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var trackingStartTime by remember { mutableStateOf<Long?>(null) }
    var showStopDialog by remember { mutableStateOf(false) }
    var showQuickTripDialog by remember { mutableStateOf(false) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var totalDistance by remember { mutableStateOf(0.0) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // ⭐ NUOVO: Stati per il sistema note
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }

    // ⭐ NUOVO: Osserva le ultime 3 note del viaggio selezionato
    val recentNotes by selectedTrip?.let { trip ->
        viewModel.getRecentNotesForTrip(trip.id).collectAsState(initial = emptyList())
    } ?: remember { mutableStateOf(emptyList()) }

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

    // Aggiorna la camera quando cambia la posizione
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

                        if (routePoints.isNotEmpty()) {
                            val lastPoint = routePoints.last()
                            val results = FloatArray(1)
                            Location.distanceBetween(
                                lastPoint.latitude, lastPoint.longitude,
                                newPoint.latitude, newPoint.longitude,
                                results
                            )
                            totalDistance += results[0] / 1000.0
                        }

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

        trackingStartTime = System.currentTimeMillis()

        selectedTrip?.let { trip ->
            val updatedTrip = if (trip.actualStartDate == null) {
                trip.copy(
                    status = "In corso",
                    actualStartDate = Date()
                )
            } else {
                trip.copy(status = "In corso")
            }
            viewModel.updateTrip(updatedTrip)
            selectedTrip = updatedTrip
        }

        isTracking = true
        elapsedTime = 0L

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).setMinUpdateIntervalMillis(2000).build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } catch (e: SecurityException) {
        }
    }

    fun saveTrip(completeTrip: Boolean = true) {
        selectedTrip?.let { trip ->
            val trackingEndTime = System.currentTimeMillis()
            val sessionDuration = trackingStartTime?.let {
                trackingEndTime - it
            } ?: 0L
            val newTotalDuration = trip.totalTrackingDurationMs + sessionDuration

            val updatedTrip = if (completeTrip) {
                trip.copy(
                    totalDistanceKm = totalDistance,
                    status = "Completato",
                    isCompleted = true,
                    actualEndDate = Date(),
                    totalTrackingDurationMs = newTotalDuration
                )
            } else {
                trip.copy(
                    totalDistanceKm = totalDistance,
                    status = "In corso",
                    totalTrackingDurationMs = newTotalDuration
                )
            }

            viewModel.updateTrip(updatedTrip)

            if (completeTrip) {
                selectedTrip = null
                routePoints = emptyList()
                totalDistance = 0.0
            } else {
                selectedTrip = updatedTrip
            }

            elapsedTime = 0L
            trackingStartTime = null
        }
    }
    // Funzione per fermare il tracciamento
    fun stopTracking() {
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        showStopDialog = true
    }


    // ⭐ NUOVA FUNZIONE: Ottiene il nome del luogo dalle coordinate
    suspend fun getLocationName(lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    buildString {
                        address.thoroughfare?.let { append("$it, ") }
                        address.locality?.let { append(it) }
                    }.trim().removeSuffix(",")
                } else {
                    "Lat ${String.format("%.4f", lat)}, Lng ${String.format("%.4f", lng)}"
                }
            } catch (e: Exception) {
                "Lat ${String.format("%.4f", lat)}, Lng ${String.format("%.4f", lng)}"
            }
        }
    }

    // ⭐ NUOVA FUNZIONE: Salva la nota nel database
    fun saveNote() {
        if (noteText.isBlank() || selectedTrip == null) return

        scope.launch {
            val location = currentLocation
            val locationName = if (location != null) {
                getLocationName(location.latitude, location.longitude)
            } else null

            val note = TripNote(
                tripId = selectedTrip!!.id,
                text = noteText,
                timestamp = Date(),
                latitude = location?.latitude,
                longitude = location?.longitude,
                locationName = locationName
            )

            viewModel.insertNote(note)
            noteText = ""
            showAddNoteDialog = false
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
    // Dialog Pausa/Completa
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = {
                showStopDialog = false
                isTracking = true
                if (hasLocationPermission) {
                    val locationRequest = LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY, 5000
                    ).setMinUpdateIntervalMillis(2000).build()
                    try {
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                    } catch (e: SecurityException) {}
                }
            },
            title = { Text("Ferma il tracciamento", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Cosa vuoi fare con questo viaggio?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Pausa: Ferma il GPS ma potrai riprendere dopo", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("• Completa: Chiudi definitivamente il viaggio", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveTrip(completeTrip = true)
                        showStopDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Completa viaggio")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    saveTrip(completeTrip = false)
                    showStopDialog = false
                }) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Metti in pausa")
                }
            }
        )
    }

// Dialog Viaggio Rapido
    if (showQuickTripDialog) {
        var quickDestination by remember { mutableStateOf("") }
        var quickTripType by remember { mutableStateOf("Day trip") }

        AlertDialog(
            onDismissRequest = { showQuickTripDialog = false },
            title = { Text("Nuovo viaggio rapido", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Crea e inizia subito un viaggio con le date di oggi")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = quickDestination,
                        onValueChange = { quickDestination = it },
                        label = { Text("Destinazione") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tipo di viaggio:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Local trip", "Day trip", "Multi-day trip").forEach { type ->
                            FilterChip(
                                selected = quickTripType == type,
                                onClick = { quickTripType = type },
                                label = { Text(type.replace(" trip", ""), fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.createQuickTrip(quickDestination, quickTripType, "Viaggio rapido")
                            delay(300)
                            val trips = viewModel.uiState.value.tripList
                            selectedTrip = trips.maxByOrNull { it.id }
                            showQuickTripDialog = false
                        }
                    },
                    enabled = quickDestination.isNotBlank()
                ) { Text("Inizia ora") }
            },
            dismissButton = {
                TextButton(onClick = { showQuickTripDialog = false }) { Text("Annulla") }
            }
        )
    }
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
        },
        // ⭐ Floating Action Button rimosso per evitare sovrapposizioni
        // Il pulsante per aggiungere note è ora integrato nella UI
    ) { paddingValues ->

        // ✅ SOLUZIONE: Box per contenere tutto con layout verticale fisso
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 🔹 PARTE SCROLLABILE: Contiene le info che possono crescere
                // Occupa lo spazio necessario ma lascia sempre spazio alla mappa
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false) // Non forza a riempire tutto lo spazio
                ) {
                    // Selettore viaggio (solo se non sta tracciando e nessun viaggio selezionato)
                    if (!isTracking && selectedTrip == null) {
                        item {
                            TripSelector(
                                trips = uiState.tripList.filter { !it.isCompleted },
                                viewModel = viewModel,
                                onTripSelected = { selectedTrip = it },
                                onCreateQuickTrip = { showQuickTripDialog = true }
                            )
                        }
                    }

                    // Pannello informazioni viaggio
                    selectedTrip?.let { trip ->
                        item {
                            TripInfoPanel(
                                trip = trip,
                                isTracking = isTracking,
                                distance = totalDistance,
                                elapsedTime = elapsedTime
                            )
                        }

                        // 🔹 NUOVO: Pulsante per aggiungere nota (stesso stile delle foto)
                        item {
                            AddNoteButton(
                                onClick = { showAddNoteDialog = true }
                            )
                        }

                        // ⭐ NUOVO: Mostra le ultime 3 note
                        if (recentNotes.isNotEmpty()) {
                            item {
                                RecentNotesPanel(notes = recentNotes)
                            }
                        }

                        // Sezione Foto
                        item {
                            PhotoCaptureSection(
                                tripId = trip.id,
                                currentLocation = currentLocation,
                                viewModel = viewModel
                            )
                        }
                    }
                }

                // 🔹 MAPPA: Altezza fissa per garantire che sia sempre visibile
                // Anche quando ci sono molte note/foto sopra
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp) // Altezza fissa: la mappa sarà sempre 300dp
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
                        currentLocation?.let { location ->
                            Marker(
                                state = rememberMarkerState(
                                    position = LatLng(location.latitude, location.longitude)
                                ),
                                title = "Posizione Attuale"
                            )
                        }

                        if (routePoints.isNotEmpty()) {
                            Polyline(
                                points = routePoints,
                                color = TravelGreen,
                                width = 8f
                            )
                        }
                    }

                    // Messaggio permessi GPS se mancanti
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

                // 🔹 PULSANTI DI CONTROLLO: Sempre visibili in fondo
                // Altezza fissa per evitare deformazioni
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

        // ⭐ NUOVO: Dialog per aggiungere nota
        if (showAddNoteDialog) {
            AddNoteDialog(
                noteText = noteText,
                onNoteTextChange = { noteText = it },
                currentLocation = currentLocation,
                onDismiss = {
                    showAddNoteDialog = false
                    noteText = ""
                },
                onSave = { saveNote() }
            )
        }
    }
}

// ⭐ NUOVO: Panel che mostra le ultime 3 note
@Composable
fun RecentNotesPanel(notes: List<TripNote>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝 Note Recenti",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TravelGreen
                )
                Text(
                    text = "${notes.size}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            notes.forEach { note ->
                NoteItem(note = note)
                if (note != notes.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// ⭐ NUOVO: Item singola nota
@Composable
fun NoteItem(note: TripNote) {
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.ITALIAN)

    Surface(
        color = TravelGreen.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.text,
                    fontSize = 14.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🕐 ${timeFormatter.format(note.timestamp)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    if (note.locationName != null) {
                        Text(
                            text = " • 📍 ${note.locationName}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

// ⭐ NUOVO: Dialog per aggiungere una nota
@Composable
fun AddNoteDialog(
    noteText: String,
    onNoteTextChange: (String) -> Unit,
    currentLocation: Location?,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = TravelGreen,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Aggiungi Nota",
                fontWeight = FontWeight.Bold,

                )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = onNoteTextChange,
                    placeholder = {
                        Text("Es: Fermata pranzo, panorama bellissimo...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TravelGreen,
                        unfocusedBorderColor = Color.Gray,
                        unfocusedTextColor = Color.Black,
                        focusedTextColor = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Info GPS
                if (currentLocation != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = TravelGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Posizione GPS registrata",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                } else {
                    Text(
                        text = "⚠️ GPS non disponibile",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = TravelGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.ITALIAN).format(Date()),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = noteText.isNotBlank()
            ) {
                Text("Salva", color = TravelGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = Color.DarkGray)
            }
        },
        containerColor = Color.White
    )
}

// === COMPONENTI ESISTENTI (invariati) ===

// 🔹 NUOVO: Pulsante per aggiungere nota con lo stesso stile della sezione foto
@Composable
fun AddNoteButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📝 Aggiungi Note",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TravelGreen
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TravelGreen
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Aggiungi Nota",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Scrivi una Nota",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// === COMPONENTI ESISTENTI (invariati) ===

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
    // ✅ SOLUZIONE: Surface con altezza fissa per evitare deformazioni
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp), // Altezza fissa per stabilità
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
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