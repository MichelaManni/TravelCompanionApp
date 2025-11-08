package com.example.travelcompanionapp.ui

import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

val TravelGreen = Color(0xFF008080)

/**
 * Schermata per selezionare una destinazione dalla mappa Google Maps.
 * L'utente può:
 * 1. Toccare un punto sulla mappa
 * 2. Trascinare il marker
 * 3. Il sistema converte automaticamente le coordinate in un indirizzo reale (Geocoding)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSelectionScreen(
    onDestinationSelected: (name: String, lat: Double, lng: Double) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Stato per la posizione selezionata sulla mappa
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    // Nome del luogo convertito dalle coordinate (es: "Roma, Italia")
    var locationName by remember { mutableStateOf<String?>(null) }

    // Indica se stiamo caricando il nome del luogo
    var isLoadingAddress by remember { mutableStateOf(false) }

    // Camera position iniziale: centro Italia
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(42.5, 12.5), 6f)
    }

    /**
     * Funzione che converte le coordinate geografiche in un indirizzo leggibile.
     * Usa il servizio Geocoder di Android per ottenere il nome del luogo.
     */
    suspend fun getAddressFromLocation(latLng: LatLng): String {
        return withContext(Dispatchers.IO) {
            try {
                // Geocoder è il servizio Android per convertire coordinate in indirizzi
                val geocoder = Geocoder(context, Locale.getDefault())

                // Otteniamo fino a 1 risultato per le coordinate specificate
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]

                    // Costruiamo un nome leggibile dal risultato
                    // Esempio: "Via Roma, Milano, Italia" oppure "Milano, Italia"
                    buildString {
                        // Prova a prendere il nome della località (es: "Colosseo", "Piazza Duomo")
                        address.featureName?.let {
                            if (it != address.locality && !it.matches(Regex("\\d+"))) {
                                append(it)
                                append(", ")
                            }
                        }

                        // Aggiungi la città
                        address.locality?.let {
                            append(it)
                            append(", ")
                        }

                        // Aggiungi la provincia/regione se la città non è presente
                        if (address.locality == null) {
                            address.subAdminArea?.let {
                                append(it)
                                append(", ")
                            }
                        }

                        // Aggiungi sempre il paese
                        address.countryName?.let { append(it) }
                    }.trim().removeSuffix(",")
                } else {
                    // Se non troviamo un indirizzo, usiamo le coordinate
                    "Lat ${String.format("%.4f", latLng.latitude)}, Lng ${String.format("%.4f", latLng.longitude)}"
                }
            } catch (e: Exception) {
                // In caso di errore (es: nessuna connessione), usiamo le coordinate
                "Lat ${String.format("%.4f", latLng.latitude)}, Lng ${String.format("%.4f", latLng.longitude)}"
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Seleziona Destinazione",
                        color = TravelGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
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
                actions = {
                    // Mostra il pulsante di conferma solo se c'è una posizione selezionata
                    if (selectedLocation != null && locationName != null) {
                        IconButton(
                            onClick = {
                                // Quando l'utente conferma, passiamo i dati al chiamante
                                selectedLocation?.let { location ->
                                    onDestinationSelected(
                                        locationName!!, // Nome del luogo
                                        location.latitude, // Latitudine
                                        location.longitude // Longitudine
                                    )
                                    onNavigateBack() // Torniamo alla schermata precedente
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Conferma Selezione",
                                tint = TravelGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
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
            // Card informativa con il nome del luogo selezionato
            if (selectedLocation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Etichetta
                        Text(
                            text = "📍 Destinazione Selezionata",
                            style = MaterialTheme.typography.labelMedium,
                            color = TravelGreen,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Nome del luogo o indicatore di caricamento
                        if (isLoadingAddress) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = TravelGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ricerca indirizzo...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        } else if (locationName != null) {
                            // Mostra il nome del luogo trovato
                            Text(
                                text = locationName!!,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Istruzioni per confermare
                            Text(
                                text = "Tocca ✓ in alto per confermare",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Google Map - Occupa tutto lo spazio rimanente
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Prende tutto lo spazio verticale disponibile
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = false
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true, // Pulsanti +/- per zoom
                    myLocationButtonEnabled = false
                ),
                onMapClick = { latLng ->
                    // Quando l'utente tocca sulla mappa, aggiorniamo immediatamente
                    selectedLocation = latLng
                    isLoadingAddress = true
                    locationName = null

                    // Avvia la conversione coordinate → indirizzo in background
                    scope.launch {
                        val address = getAddressFromLocation(latLng)
                        locationName = address
                        isLoadingAddress = false
                    }
                }
            ) {
                // Marker sulla posizione selezionata (se presente)
                selectedLocation?.let { location ->
                    // Creiamo uno stato per il marker che può essere trascinato
                    val markerState = rememberMarkerState(position = location)

                    // Aggiorna il marker state quando la location cambia (dal click sulla mappa)
                    LaunchedEffect(location) {
                        if (markerState.position != location) {
                            markerState.position = location
                        }
                    }

                    // Monitora quando il marker viene trascinato dall'utente
                    LaunchedEffect(Unit) {
                        snapshotFlow { markerState.position }
                            .collect { newPosition ->
                                // Se la posizione è diversa da quella selezionata,
                                // significa che l'utente ha trascinato il marker
                                if (newPosition != location) {
                                    // Aspetta un po' per essere sicuri che il drag sia finito
                                    kotlinx.coroutines.delay(500)

                                    // Verifica che la posizione sia ancora la stessa (non sta più trascinando)
                                    if (markerState.position == newPosition) {
                                        selectedLocation = newPosition
                                        isLoadingAddress = true
                                        locationName = null

                                        // Avvia la conversione coordinate → indirizzo in background
                                        val address = getAddressFromLocation(newPosition)
                                        locationName = address
                                        isLoadingAddress = false
                                    }
                                }
                            }
                    }

                    // Marker rosso trascinabile
                    Marker(
                        state = markerState,
                        title = "Destinazione",
                        draggable = true // Permette di trascinare il marker
                    )
                }
            }

            // Card con istruzioni iniziali (visibile solo se non c'è nulla selezionato)
            if (selectedLocation == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = TravelGreen.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👆",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "Tocca la mappa per selezionare un luogo",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Puoi anche trascinare il marker per spostarlo",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}