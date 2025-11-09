package com.example.travelcompanionapp.ui

import android.location.Geocoder //per convertire coordinate in indirizzi
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
import androidx.compose.ui.unit.sp //unità di misura scale-independent pixels per il testo
import com.google.android.gms.maps.model.CameraPosition //posizione della camera della mappa
import com.google.android.gms.maps.model.LatLng // classe per coordinate geografiche (latitudine/longitudine)
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

val TravelGreen = Color(0xFF008080) //colore verde

/**
 * Schermata per selezionare una destinazione dalla mappa Google Maps.
 * L'utente può:
 * Toccare un punto sulla mappa
 *Il sistema converte automaticamente le coordinate in un indirizzo reale (Geocoding)
 */
@OptIn(ExperimentalMaterial3Api::class) //usa API sperimentali di Material3
@Composable //elemento grafico riutilizzabile
fun MapSelectionScreen(
    onDestinationSelected: (name: String, lat: Double, lng: Double) -> Unit, // Funzione callback chiamata quando l'utente conferma una destinazione
    onNavigateBack: () -> Unit // Funzione callback per tornare alla schermata precedente
) {
    val context = LocalContext.current // Ottiene il contesto corrente dell'applicazione (necessario per accedere a risorse Android)
    val scope = rememberCoroutineScope() // Crea uno scope per lanciare coroutine (operazioni asincrone)

    // Stato per la posizione selezionata sulla mappa
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) } // Variabile che memorizza le coordinate selezionate (null se nessuna selezione)

    // Nome del luogo convertito dalle coordinate
    var locationName by remember { mutableStateOf<String?>(null) } // Variabile che memorizza il nome del luogo (null se non ancora convertito)

    // Indica se stiamo caricando il nome del luogo
    var isLoadingAddress by remember { mutableStateOf(false) } // Flag che indica se la conversione coordinate->indirizzo è in corso

    // Camera position iniziale: centro Italia
    val cameraPositionState = rememberCameraPositionState { // Memorizza la posizione e lo zoom della camera della mappa
        position = CameraPosition.fromLatLngZoom(LatLng(42.5, 12.5), 6f) // Imposta posizione iniziale al centro Italia con zoom 6
    }

    /**
     * Funzione che converte le coordinate geografiche in un indirizzo leggibile.
     * Usa il servizio Geocoder di Android per ottenere il nome del luogo.
     */
    suspend fun getAddressFromLocation(latLng: LatLng): String { // Funzione sospendibile (può essere eseguita in background)
        return withContext(Dispatchers.IO) { // Esegue il codice nel thread IO (operazioni di rete/disco)
            try {
                // GEOCODER è il servizio Android per convertire coordinate in indirizzi
                val geocoder = Geocoder(context, Locale.getDefault()) // Crea un geocoder con le impostazioni regionali del dispositivo

                // Otteniamo fino a 1 risultato per le coordinate specificate
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) // Richiede l'indirizzo per le coordinate date

                if (!addresses.isNullOrEmpty()) { // Se abbiamo trovato almeno un indirizzo
                    val address = addresses[0] // Prende il primo risultato

                    // Costruiamo un nome leggibile dal risultato

                    buildString { // Costruisce una stringa concatenando vari pezzi
                        // Prova a prendere il nome della località
                        address.featureName?.let { // Se c'è un nome specifico del luogo
                            if (it != address.locality && !it.matches(Regex("\\d+"))) { // Se il nome non è uguale alla città e non è solo un numero
                                append(it) // Aggiunge il nome del luogo
                                append(", ") // Aggiunge una virgola
                            }
                        }

                        // Aggiungi la città
                        address.locality?.let { // Se c'è il nome della città
                            append(it) // Aggiunge la città
                            append(", ") // Aggiunge una virgola
                        }

                        // Aggiungi la provincia/regione se la città non è presente
                        if (address.locality == null) { // Se non abbiamo trovato la città
                            address.subAdminArea?.let { // Prova con la provincia/regione
                                append(it) // Aggiunge la provincia
                                append(", ") // Aggiunge una virgola
                            }
                        }

                        // Aggiungi sempre il paese
                        address.countryName?.let { append(it) } // Aggiunge il nome del paese
                    }.trim().removeSuffix(",") // Rimuove spazi e l'eventuale virgola finale
                } else {
                    // Se non troviamo un indirizzo, usiamo le coordinate
                    "Lat ${String.format("%.4f", latLng.latitude)}, Lng ${String.format("%.4f", latLng.longitude)}" // Formatta le coordinate con 4 decimali
                }
            } catch (e: Exception) {
                // In caso di errore usiamo le coordinate
                "Lat ${String.format("%.4f", latLng.latitude)}, Lng ${String.format("%.4f", latLng.longitude)}" // Restituisce le coordinate come fallback
            }
        }
    }

    Scaffold( // Struttura base di Material Design con TopBar, BottomBar e contenuto
        containerColor = Color.White, // Colore di sfondo
        topBar = { // Definisce la barra superiore
            CenterAlignedTopAppBar( // Barra superiore con titolo centrato
                title = { // titolo
                    Text(
                        "Seleziona Destinazione",
                        color = TravelGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = { // Definisce l'icona di navigazione a sinistra
                    IconButton(onClick = onNavigateBack) { // Pulsante che chiama la funzione per tornare indietro
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Icona freccia indietro
                            contentDescription = "Torna Indietro",
                            tint = Color.Black
                        )
                    }
                },
                actions = { // Definisce le azioni a destra nella barra
                    // Mostra il pulsante di conferma solo se c'è una posizione selezionata
                    if (selectedLocation != null && locationName != null) { // Controlla se abbiamo sia coordinate che nome
                        IconButton(
                            onClick = { // Quando si clicca il pulsante di conferma
                                // Quando l'utente conferma, passiamo i dati al chiamante
                                selectedLocation?.let { location -> // Se c'è una posizione selezionata
                                    onDestinationSelected(
                                        locationName!!, // Nome del luogo (!! = sicuro che non è null)
                                        location.latitude, // Latitudine
                                        location.longitude // Longitudine
                                    )
                                    onNavigateBack() // Torniamo alla schermata precedente
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check, // Icona segno di spunta
                                contentDescription = "Conferma Selezione",
                                tint = TravelGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors( // Imposta i colori della barra
                    containerColor = Color.White // Sfondo
                )
            )
        }
    ) { paddingValues -> // paddingValues contiene i margini necessari per non sovrapporsi alla TopBar
        Column( // Dispone gli elementi verticalmente
            modifier = Modifier
                .fillMaxSize() // Occupa tutto lo spazio disponibile
                .padding(paddingValues) // Applica i margini per evitare la sovrapposizione con la TopBar
        ) {
            // Card informativa con il nome del luogo selezionato
            if (selectedLocation != null) { // Se c'è una posizione selezionata
                Card( // Contenitore con bordi arrotondati
                    modifier = Modifier
                        .fillMaxWidth() // Occupa tutta la larghezza
                        .padding(horizontal = 16.dp, vertical = 8.dp), // Margine esterno: 16 dp orizzontale, 8 dp verticale
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Ombra leggera
                    shape = RoundedCornerShape(12.dp) // Angoli arrotondati
                ) {
                    Column( // Dispone gli elementi verticalmente
                        modifier = Modifier.padding(16.dp), // Spazio interno
                        horizontalAlignment = Alignment.Start // Allinea a sinistra
                    ) {
                        // Etichetta
                        Text(
                            text = "📍 Destinazione Selezionata",
                            style = MaterialTheme.typography.labelMedium,
                            color = TravelGreen,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp)) // Spazio verticale

                        // Nome del luogo o indicatore di caricamento
                        if (isLoadingAddress) { // Se stiamo caricando l'indirizzo
                            Row( // Dispone gli elementi orizzontalmente
                                verticalAlignment = Alignment.CenterVertically // Allinea verticalmente al centro
                            ) {
                                CircularProgressIndicator( // Indicatore di caricamento circolare
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp, // Spessore della linea
                                    color = TravelGreen // Colore verde
                                )
                                Spacer(modifier = Modifier.width(8.dp)) // Spazio orizzontale
                                Text(
                                    text = "Ricerca indirizzo...", // Testo di caricamento
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        } else if (locationName != null) { // Se abbiamo trovato il nome del luogo
                            // Mostra il nome del luogo trovato
                            Text(
                                text = locationName!!, // Mostra il nome (!! = sicuro che non è null)
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )

                            Spacer(modifier = Modifier.height(8.dp)) // Spazio verticale
                            // Istruzioni per confermare
                            Text(
                                text = "Tocca ✓ in alto per confermare", // Istruzioni per l'utente
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray // Colore grigio
                            )
                        }
                    }
                }
            }

            // Google Map - Occupa tutto lo spazio rimanente
            GoogleMap( // Componente della mappa di Google
                modifier = Modifier
                    .fillMaxWidth() // Occupa tutta la larghezza
                    .weight(1f), // Prende tutto lo spazio verticale disponibile (peso 1)
                cameraPositionState = cameraPositionState, // Imposta posizione e zoom della camera
                properties = MapProperties( // Proprietà della mappa
                    isMyLocationEnabled = false // Disabilita il pulsante "mostra la mia posizione"
                ),
                uiSettings = MapUiSettings( // Impostazioni dell'interfaccia utente
                    zoomControlsEnabled = true, // Mostra i pulsanti +/- per lo zoom
                    myLocationButtonEnabled = false // Nasconde il pulsante per centrare sulla posizione corrente
                ),
                onMapClick = { latLng -> // Funzione chiamata quando l'utente tocca la mappa
                    // Quando l'utente tocca sulla mappa, aggiorniamo immediatamente
                    selectedLocation = latLng // Salva le coordinate toccate
                    isLoadingAddress = true // Imposta flag di caricamento
                    locationName = null // Resetta il nome del luogo

                    // Avvia la conversione coordinate → indirizzo in background
                    scope.launch { // Lancia una coroutine
                        val address = getAddressFromLocation(latLng) // Converte coordinate in indirizzo
                        locationName = address // Salva il nome trovato
                        isLoadingAddress = false // Disattiva flag di caricamento
                    }
                }
            ) {
                // Marker sulla posizione selezionata (se presente)
                selectedLocation?.let { location -> // Se c'è una posizione selezionata
                    // Creiamo uno stato per il marker che può essere trascinato
                    val markerState = rememberMarkerState(position = location) // Crea stato del marker con la posizione

                    // Aggiorna il marker state quando la location cambia (dal click sulla mappa)
                    LaunchedEffect(location) { // Effetto lanciato quando 'location' cambia
                        if (markerState.position != location) { // Se la posizione del marker è diversa da quella selezionata
                            markerState.position = location // Aggiorna la posizione del marker
                        }
                    }

                    // Monitora quando il marker viene trascinato dall'utente
                    LaunchedEffect(Unit) { // Effetto lanciato una sola volta
                        snapshotFlow { markerState.position } // Osserva i cambiamenti della posizione del marker
                            .collect { newPosition -> // Per ogni nuova posizione
                                // Se la posizione è diversa da quella selezionata,
                                // significa che l'utente ha trascinato il marker
                                if (newPosition != location) { // Se il marker è stato spostato
                                    // Aspetta un po' per essere sicuri che il drag sia finito
                                    kotlinx.coroutines.delay(500) // Aspetta 500 millisecondi

                                    // Verifica che la posizione sia ancora la stessa (non sta più trascinando)
                                    if (markerState.position == newPosition) { // Se la posizione è stabile
                                        selectedLocation = newPosition // Aggiorna la posizione selezionata
                                        isLoadingAddress = true // Imposta flag di caricamento
                                        locationName = null // Resetta il nome del luogo

                                        // Avvia la conversione coordinate → indirizzo in background
                                        val address = getAddressFromLocation(newPosition) // Converte coordinate in indirizzo
                                        locationName = address // Salva il nome trovato
                                        isLoadingAddress = false // Disattiva flag di caricamento
                                    }
                                }
                            }
                    }

                    // Marker rosso trascinabile
                    Marker( // Crea un marker sulla mappa
                        state = markerState, // Passa lo stato del marker
                        title = "Destinazione", // Titolo mostrato quando si tocca il marker
                        draggable = true // Permette di trascinare il marker con il dito
                    )
                }
            }

            // Card con istruzioni iniziali (visibile solo se non c'è nulla selezionato)
            if (selectedLocation == null) { // Se non c'è ancora una posizione selezionata
                Card( // Contenitore con bordi arrotondati
                    modifier = Modifier
                        .fillMaxWidth() // Occupa tutta la larghezza
                        .padding(16.dp), // Margine esterno
                    colors = CardDefaults.cardColors(
                        containerColor = TravelGreen.copy(alpha = 0.1f) // Verde chiaro trasparente (10% di opacità)
                    ),
                    shape = RoundedCornerShape(12.dp) // Angoli arrotondati
                ) {
                    Row( // Dispone gli elementi orizzontalmente
                        modifier = Modifier.padding(16.dp), // Spazio interno
                        verticalAlignment = Alignment.CenterVertically // Allinea verticalmente al centro
                    ) {
                        Text(
                            text = "👆",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 12.dp) // Spazio a destra
                        )
                        Column { // Dispone gli elementi verticalmente
                            Text(
                                text = "Tocca la mappa per selezionare un luogo", // Istruzioni per l'utente
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}