package com.example.travelcompanionapp.ui
//riepilogo funzioni presenti nel file:
//- triptrackingScreen: schermata principale che gestisce l’intero flusso di tracciamento gps del viaggio, include mappa, note, foto, dialog e controlli
//- recentNotesPanel: pannello che mostra le note più recenti relative al viaggio attivo, con intestazione e separatore
//- noteItem: componente singolo per visualizzare una singola nota con testo, orario e posizione
//- addNoteDialog: finestra di dialogo che permette di scrivere e salvare una nuova nota durante il viaggio
//- addNoteButton: card con pulsante per aprire il dialog di aggiunta nota, evidenziato graficamente nella schermata
//- tripInfoPanel: pannello informativo che mostra dettagli del viaggio selezionato (destinazione, tipo, distanza percorsa e tempo totale)
//- statisticItem: elemento compatto che mostra un singolo valore statistico con etichetta e icona (usato nel tripInfoPanel)
//- trackingControls: sezione inferiore con i pulsanti principali per avviare o fermare il tracciamento gps, che cambiano colore e testo in base allo stato
//ogni funzione è indipendente e costruita come composable

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
 * schermata principale dedicata al tracciamento gps del viaggio.
 * integra google maps, il sistema di localizzazione android e funzioni personalizzate per gestire:
 * - l’avvio, la pausa e il completamento di un viaggio
 * - la registrazione in tempo reale della distanza percorsa
 * - la memorizzazione dei punti del percorso su mappa
 * - la possibilità di aggiungere note e foto durante il tragitto
 * - la gestione dei permessi di localizzazione e dei dialog di sistema
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripTrackingScreen(
    viewModel: TripViewModel, //viewmodel responsabile della logica del viaggio e delle note
    onNavigateBack: () -> Unit //callback invocata per tornare alla schermata precedente
) {
    val context = LocalContext.current //ottiene il contesto corrente dell’app (necessario per gps e geocoder)
    val scope = rememberCoroutineScope() //crea uno scope coroutine legato al ciclo di vita composable
    val uiState by viewModel.uiState.collectAsState() //osserva lo stato globale dei viaggi dal viewmodel

    //stati principali per il tracciamento gps
    var selectedTrip by remember { mutableStateOf<Trip?>(null) } //viaggio attualmente selezionato
    var isTracking by remember { mutableStateOf(false) } //true se il gps è attivo
    var currentLocation by remember { mutableStateOf<Location?>(null) } //posizione corrente restituita dai servizi di localizzazione
    var trackingStartTime by remember { mutableStateOf<Long?>(null) } //timestamp di avvio del tracciamento
    var showStopDialog by remember { mutableStateOf(false) } //mostra dialog per fermare o mettere in pausa il viaggio
    var showQuickTripDialog by remember { mutableStateOf(false) } //mostra dialog per creare un viaggio rapido
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) } //lista ordinata dei punti gps raccolti
    var totalDistance by remember { mutableStateOf(0.0) } //distanza totale percorsa in km
    var elapsedTime by remember { mutableStateOf(0L) } //tempo trascorso espresso in secondi
    var hasLocationPermission by remember { mutableStateOf(false) } //true se i permessi gps sono concessi

    //stati per il sistema di note
    var showAddNoteDialog by remember { mutableStateOf(false) } //controlla la visibilità del dialog per aggiungere una nota
    var noteText by remember { mutableStateOf("") } //testo inserito dall’utente per la nuova nota

    //osserva le ultime tre note relative al viaggio selezionato (aggiornamento automatico tramite flow)
    val recentNotes by selectedTrip?.let { trip ->
        viewModel.getRecentNotesForTrip(trip.id).collectAsState(initial = emptyList()) //recupera le note dal database
    } ?: remember { mutableStateOf(emptyList()) } //se nessun viaggio è selezionato, mantiene lista vuota

    //imposta posizione iniziale della mappa centrata sull’italia
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(42.5, 12.5), 6f) //latitudine e longitudine centrali dell’italia
    }

    //inizializza il client fused location per ricevere aggiornamenti di posizione
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    //controlla all’avvio se i permessi di localizzazione sono già stati concessi
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    //effetto che aggiorna il contatore del tempo durante il tracking
    LaunchedEffect(isTracking) {
        if (isTracking) { //se il tracciamento è attivo
            while (isTracking) { //ciclo continuo finché è attivo
                delay(1000) //attende un secondo
                elapsedTime += 1 //incrementa il tempo di un secondo
            }
        }
    }

    //aggiorna automaticamente la visuale della mappa seguendo la posizione in tempo reale
    LaunchedEffect(currentLocation, isTracking) {
        if (isTracking && currentLocation != null) { //esegue solo se gps attivo e posizione valida
            val newPosition = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(newPosition, 15f) //porta la camera sulla posizione attuale
        }
    }

    //launcher che richiede i permessi di localizzazione all’utente
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = //aggiorna flag solo se almeno un permesso è stato accettato
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    //callback che riceve gli aggiornamenti continui di posizione dal gps
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location -> //usa l’ultima posizione disponibile
                    currentLocation = location //aggiorna stato posizione

                    if (isTracking) { //solo se tracciamento attivo
                        val newPoint = LatLng(location.latitude, location.longitude) //crea nuovo punto da aggiungere al percorso

                        if (routePoints.isNotEmpty()) { //calcola distanza solo se non è il primo punto
                            val lastPoint = routePoints.last() //recupera ultimo punto registrato
                            val results = FloatArray(1)
                            Location.distanceBetween(
                                lastPoint.latitude, lastPoint.longitude,
                                newPoint.latitude, newPoint.longitude,
                                results
                            )
                            totalDistance += results[0] / 1000.0 //converte i metri in chilometri e li somma al totale
                        }

                        routePoints = routePoints + newPoint //aggiunge il nuovo punto alla lista del percorso
                    }
                }
            }
        }
    }

    //funzione che avvia il tracciamento gps e aggiorna lo stato del viaggio
    fun startTracking() {
        if (!hasLocationPermission) { //se mancano i permessi gps li richiede
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        if (selectedTrip == null) return //non può partire senza un viaggio selezionato

        trackingStartTime = System.currentTimeMillis() //salva il momento esatto di avvio

        //aggiorna il viaggio selezionato impostando lo stato "in corso"
        selectedTrip?.let { trip ->
            val updatedTrip = if (trip.actualStartDate == null) {
                trip.copy(
                    status = "In corso", //aggiorna lo stato
                    actualStartDate = Date() //imposta data effettiva di inizio se non era presente
                )
            } else {
                trip.copy(status = "In corso") //mantiene la data ma aggiorna lo stato
            }
            viewModel.updateTrip(updatedTrip) //salva aggiornamento nel database
            selectedTrip = updatedTrip //aggiorna localmente
        }

        isTracking = true //attiva flag di tracciamento
        elapsedTime = 0L //azzera tempo
        routePoints = emptyList() //svuota punti precedenti

        //crea richiesta di aggiornamento posizione ad alta frequenza
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, //massima precisione
            5000 //intervallo medio di aggiornamento (5 secondi)
        ).setMinUpdateIntervalMillis(2000).build() //minimo 2 secondi tra gli update

        try {
            fusedLocationClient.requestLocationUpdates( //avvia gli aggiornamenti in tempo reale
                locationRequest,
                locationCallback,
                null
            )
        } catch (e: SecurityException) {
            //non fa nulla se i permessi sono stati negati nel frattempo
        }
    }

    //funzione per salvare lo stato del viaggio, sia in pausa che completato
    fun saveTrip(completeTrip: Boolean = true) {
        selectedTrip?.let { trip ->
            val trackingEndTime = System.currentTimeMillis() //timestamp di fine sessione
            val sessionDuration = trackingStartTime?.let {
                trackingEndTime - it //durata della sessione corrente in ms
            } ?: 0L
            val newTotalDuration = trip.totalTrackingDurationMs + sessionDuration //aggiunge la sessione alla durata totale

            //crea una nuova versione del viaggio aggiornata in base all’azione scelta
            val updatedTrip = if (completeTrip) { //se l’utente decide di completare
                trip.copy(
                    totalDistanceKm = totalDistance, //salva distanza percorsa
                    status = "Completato", //aggiorna stato
                    isCompleted = true, //marca come completato
                    actualEndDate = Date(), //data di fine effettiva
                    totalTrackingDurationMs = newTotalDuration //durata totale aggiornata
                )
            } else { //se viene solo messo in pausa
                trip.copy(
                    totalDistanceKm = totalDistance,
                    status = "In corso", //mantiene stato “in corso”
                    totalTrackingDurationMs = newTotalDuration
                )
            }

            viewModel.updateTrip(updatedTrip) //salva aggiornamento nel database

            if (completeTrip) { //se è completato, resetta tutto
                selectedTrip = null
                routePoints = emptyList()
                totalDistance = 0.0
            } else {
                selectedTrip = updatedTrip //mantiene viaggio se solo in pausa
            }

            elapsedTime = 0L //azzera tempo trascorso
            trackingStartTime = null //azzera tempo di partenza
        }
    }

    //funzione che interrompe temporaneamente il tracciamento e mostra un dialog di scelta
    fun stopTracking() {
        isTracking = false //ferma aggiornamenti interni
        fusedLocationClient.removeLocationUpdates(locationCallback) //rimuove listener dal gps
        showStopDialog = true //apre dialog per completare o mettere in pausa
    }

    //funzione sospesa che converte coordinate gps in un nome leggibile del luogo
    suspend fun getLocationName(lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) { //sposta l’esecuzione su thread i/o per non bloccare la ui
            try {
                val geocoder = Geocoder(context, Locale.getDefault()) //inizializza geocoder con lingua di sistema
                val addresses = geocoder.getFromLocation(lat, lng, 1) //richiede un solo risultato per coordinate date

                if (!addresses.isNullOrEmpty()) { //verifica che sia stato trovato almeno un indirizzo
                    val address = addresses[0] //usa il primo risultato ottenuto
                    buildString { //costruisce la stringa da restituire
                        address.thoroughfare?.let { append("$it, ") } //aggiunge via se disponibile
                        address.locality?.let { append(it) } //aggiunge città o località
                    }.trim().removeSuffix(",") //rimuove eventuale virgola finale e spazi extra
                } else {
                    "Lat ${String.format("%.4f", lat)}, Lng ${String.format("%.4f", lng)}" //valore di fallback se non c’è indirizzo
                }
            } catch (e: Exception) {
                "Lat ${String.format("%.4f", lat)}, Lng ${String.format("%.4f", lng)}" //ritorna coordinate se il geocoder genera eccezione
            }
        }
    }

    //funzione per salvare una nuova nota associata al viaggio selezionato
    fun saveNote() {
        if (noteText.isBlank() || selectedTrip == null) return //evita salvataggio se testo vuoto o nessun viaggio selezionato

        scope.launch { //lancia coroutine per gestire salvataggio asincrono
            val location = currentLocation //ottiene la posizione corrente
            val locationName = if (location != null) {
                getLocationName(location.latitude, location.longitude) //ottiene nome luogo tramite geocoder
            } else null

            val note = TripNote( //crea oggetto nota con i dettagli correnti
                tripId = selectedTrip!!.id, //associa nota al viaggio attivo
                text = noteText, //inserisce testo digitato
                timestamp = Date(), //imposta ora attuale come data creazione
                latitude = location?.latitude, //coord. lat opzionale
                longitude = location?.longitude, //coord. lng opzionale
                locationName = locationName //nome luogo leggibile
            )

            viewModel.insertNote(note) //inserisce la nota nel database tramite viewmodel
            noteText = "" //svuota campo testo dopo salvataggio
            showAddNoteDialog = false //chiude dialog di inserimento
        }
    }

    //carica l’animazione lottie mostrata nella top bar
    val headerComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.inizia_viaggio_menu)
    )
    val headerProgress by animateLottieCompositionAsState(
        composition = headerComposition,
        iterations = LottieConstants.IterateForever //fa girare l’animazione all’infinito
    )

    //dialog mostrato quando l’utente interrompe il tracciamento
    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { //chiusura dialog senza conferma
                showStopDialog = false
                isTracking = true //ripristina tracciamento se non confermato
                if (hasLocationPermission) { //se permessi concessi, riattiva gps
                    val locationRequest = LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY, 5000 //intervallo 5s
                    ).setMinUpdateIntervalMillis(2000).build() //aggiornamenti ogni 2s
                    try {
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                    } catch (e: SecurityException) {}
                }
            },
            title = { Text("Ferma il tracciamento", fontWeight = FontWeight.Bold) }, //titolo dialog
            text = {
                Column {
                    Text("Cosa vuoi fare con questo viaggio?") //testo informativo
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Pausa: Ferma il GPS ma potrai riprendere dopo", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text("• Completa: Chiudi definitivamente il viaggio", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = { //conferma completamento
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
                OutlinedButton(onClick = { //opzione pausa
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

    //dialog per creare un nuovo viaggio con le date di oggi
    if (showQuickTripDialog) {
        var quickDestination by remember { mutableStateOf("") } //destinazione scritta dall’utente
        var quickTripType by remember { mutableStateOf("Day trip") } //tipo viaggio predefinito

        AlertDialog(
            onDismissRequest = { showQuickTripDialog = false }, //chiude finestra
            title = { Text("Nuovo viaggio rapido", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Crea e inizia subito un viaggio con le date di oggi") //spiega funzione
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField( //campo input per destinazione
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
                                selected = quickTripType == type, //chip selezionato
                                onClick = { quickTripType = type }, //aggiorna tipo
                                label = { Text(type.replace(" trip", ""), fontSize = 12.sp) }, //testo semplificato
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { //crea viaggio e lo seleziona
                        scope.launch {
                            viewModel.createQuickTrip(quickDestination, quickTripType, "Viaggio rapido") //crea nel db
                            delay(300) //breve pausa per garantire update ui
                            val trips = viewModel.uiState.value.tripList
                            selectedTrip = trips.maxByOrNull { it.id } //imposta ultimo viaggio creato
                            showQuickTripDialog = false
                        }
                    },
                    enabled = quickDestination.isNotBlank() //disabilita se campo vuoto
                ) { Text("Inizia ora") }
            },
            dismissButton = {
                TextButton(onClick = { showQuickTripDialog = false }) { Text("Annulla") }
            }
        )
    }

    //scaffold principale che contiene la mappa e i vari pannelli di controllo
    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { //titolo con animazione lottie e testo
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
                    IconButton(onClick = onNavigateBack) { //freccia indietro
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna Indietro",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->

        //contenitore principale dell’intera interfaccia
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                //colonna scorrevole con pannelli dinamici
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    //mostra selettore viaggi se non c’è un viaggio attivo
                    if (!isTracking && selectedTrip == null) {
                        item {
                            TripSelector(
                                trips = uiState.tripList.filter { !it.isCompleted }, //filtra solo viaggi non completati
                                viewModel = viewModel,
                                onTripSelected = { selectedTrip = it }, //seleziona viaggio
                                onCreateQuickTrip = { showQuickTripDialog = true } //apre creazione rapida
                            )
                        }
                    }

                    //se è presente un viaggio selezionato, mostra i pannelli correlati
                    selectedTrip?.let { trip ->
                        item {
                            TripInfoPanel( //mostra info generali viaggio
                                trip = trip,
                                isTracking = isTracking,
                                distance = totalDistance,
                                elapsedTime = elapsedTime
                            )
                        }
                        item { AddNoteButton(onClick = { showAddNoteDialog = true }) } //bottone aggiungi nota
                        if (recentNotes.isNotEmpty()) { //mostra note recenti se presenti
                            item { RecentNotesPanel(notes = recentNotes) }
                        }
                        item { //mostra sezione per catturare foto
                            PhotoCaptureSection(
                                tripId = trip.id,
                                currentLocation = currentLocation,
                                viewModel = viewModel
                            )
                        }
                    }
                }

                //sezione inferiore con mappa google e percorso
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = hasLocationPermission), //abilita punto blu
                        uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = hasLocationPermission)
                    ) {
                        currentLocation?.let { location -> //marker posizione utente
                            Marker(
                                state = rememberMarkerState(position = LatLng(location.latitude, location.longitude)),
                                title = "Posizione Attuale"
                            )
                        }

                        if (routePoints.isNotEmpty()) { //disegna il percorso se ci sono punti
                            Polyline(points = routePoints, color = TravelGreen, width = 8f)
                        }
                    }

                    //messaggio se mancano permessi gps
                    if (!hasLocationPermission) {
                        Card(
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "📍", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Permesso GPS Richiesto", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Per tracciare il viaggio serve accedere alla posizione",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                //pulsanti di controllo del tracciamento (start/stop)
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

        //dialog per aggiungere una nuova nota durante il viaggio
        if (showAddNoteDialog) {
            AddNoteDialog(
                noteText = noteText, //testo nota corrente
                onNoteTextChange = { noteText = it }, //aggiorna input utente
                currentLocation = currentLocation, //posizione associata
                onDismiss = {
                    showAddNoteDialog = false
                    noteText = "" //reset campo
                },
                onSave = { saveNote() } //salvataggio finale
            )
        }
    }
}


/**mostra un pannello che elenca le note più recenti
 * associate al viaggio attualmente selezionato e tracciato.
 * ogni nota include il testo inserito, il timestamp e, se disponibile, il nome del luogo
 * o le coordinate gps della posizione in cui è stata registrata.
 */
@Composable
fun RecentNotesPanel(notes: List<TripNote>) {
    Card(
        modifier = Modifier
            .fillMaxWidth() //la card occupa tutta la larghezza del contenitore
            .padding(horizontal = 16.dp, vertical = 8.dp), //spaziatura per separarla visivamente dagli altri elementi
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF7F7F7) //colore di sfondo chiaro per differenziarla dal contenuto principale
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), //ombra leggera
        shape = RoundedCornerShape(12.dp) //angoli arrotondati
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth() //colonna che occupa tutta la larghezza disponibile
                .padding(16.dp), //padding interno per distanziare il contenuto dai bordi della card
            verticalArrangement = Arrangement.spacedBy(8.dp) //distanza costante tra una nota e l’altra
        ) {
            //intestazione del pannello con titolo e icona
            Row(
                verticalAlignment = Alignment.CenterVertically, //centra verticalmente il titolo e l’icona
                horizontalArrangement = Arrangement.Start //mantiene contenuto allineato a sinistra
            ) {
                Icon(
                    imageVector = Icons.Default.Notes, //icona
                    contentDescription = null,
                    tint = TravelGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp)) //spazio orizzontale tra icona e testo
                Text(
                    "Note recenti", //titolo della sezione
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Divider(color = Color.LightGray.copy(alpha = 0.6f)) //linea sottile per separare titolo e contenuto

            //mostra le note in ordine, con un limite definito (le più recenti sono passate dal chiamante)
            notes.forEach { note ->
                NoteItem(note = note) //richiama una funzione composable dedicata a mostrare una singola nota
            }

            //mostra un messaggio di fallback se la lista è vuota
            if (notes.isEmpty()) {
                Text(
                    "Nessuna nota disponibile", //messaggio informativo
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray, //colore neutro
                    modifier = Modifier.align(Alignment.CenterHorizontally) //centra orizzontalmente il messaggio
                )
            }
        }
    }
}


//rappresenta un singolo elemento della lista delle note recenti associate a un viaggio
//serve a mostrare il testo della nota, l’orario di creazione e il luogo (se disponibile)
//viene utilizzata all’interno del pannello RecentNotesPanel
@Composable
fun NoteItem(note: TripNote) {
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.ITALIAN) //formatter per visualizzare solo ore e minuti in formato italiano

    Surface(
        color = TravelGreen.copy(alpha = 0.05f), //colore di sfondo semitrasparente
        shape = RoundedCornerShape(8.dp), //angoli arrotondati
        modifier = Modifier.fillMaxWidth() //fa sì che l’elemento occupi l’intera larghezza disponibile
    ) {
        Row(
            modifier = Modifier.padding(12.dp), //inserisce margine interno per distanziare il contenuto dai bordi
            verticalAlignment = Alignment.Top //allinea tutto in alto verticalmente
        ) {
            Column(modifier = Modifier.weight(1f)) { //colonna che contiene il testo e i metadati della nota, occupa tutto lo spazio disponibile
                Text(
                    text = note.text, //mostra il testo principale della nota salvata
                    fontSize = 14.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp)) //aggiunge uno spazio verticale tra il testo della nota e le informazioni successive

                Row(
                    verticalAlignment = Alignment.CenterVertically //allinea le informazioni aggiuntive (tempo e luogo) al centro
                ) {
                    Text(
                        text = "🕐 ${timeFormatter.format(note.timestamp)}", //mostra l’ora di creazione formattata della nota
                        fontSize = 12.sp, //testo più piccolo perché informazione secondaria
                        color = Color.Gray
                    )

                    if (note.locationName != null) { //se esiste un nome di località associato alla nota
                        Text(
                            text = " • 📍 ${note.locationName}", //aggiunge separatore e icona segnaposto prima del nome del luogo
                            fontSize = 12.sp, //dimensione uguale all’orario
                            color = Color.Gray, //colore secondario
                            maxLines = 1 //limita il testo a una sola riga
                        )
                    }
                }
            }
        }
    }
}


// mostra un dialog che consente all’utente di aggiungere una nuova nota durante il viaggio
//per raccogliere testo, mostrare la posizione corrente e permettere il salvataggio della nota
//è costruita su un AlertDialog di material design
@Composable
fun AddNoteDialog(
    noteText: String, //testo attualmente inserito dall’utente nel campo nota
    onNoteTextChange: (String) -> Unit, //callback che aggiorna lo stato del testo man mano che l’utente digita
    currentLocation: Location?, //posizione gps attuale da mostrare come riferimento
    onDismiss: () -> Unit, //callback eseguita quando il dialog viene chiuso senza salvare
    onSave: () -> Unit //callback eseguita quando l’utente conferma il salvataggio della nota
) {
    AlertDialog(
        onDismissRequest = onDismiss, //chiude il dialog quando l’utente tocca fuori o preme “indietro”
        title = {
            Text(
                text = "Aggiungi nota", //titolo principale del dialog
                fontWeight = FontWeight.Bold,
                color = TravelGreen
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = noteText, //mostra il testo corrente digitato
                    onValueChange = onNoteTextChange, //aggiorna lo stato a ogni modifica dell’utente
                    label = { Text("Scrivi una nota") }, //etichetta del campo input
                    modifier = Modifier
                        .fillMaxWidth(), //campo che si estende per tutta la larghezza del dialog
                    maxLines = 5 //limita a 5 righe
                )

                Spacer(modifier = Modifier.height(12.dp)) //spazio tra campo di testo e posizione

                if (currentLocation != null) { //se la posizione è disponibile la mostra
                    Text(
                        text = "📍 Posizione corrente: " +
                                "Lat ${String.format("%.4f", currentLocation.latitude)}, " +
                                "Lng ${String.format("%.4f", currentLocation.longitude)}", //mostra coordinate formattate
                        fontSize = 12.sp, //testo piccolo per informazione secondaria
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave, //esegue la callback di salvataggio della nota
                enabled = noteText.isNotBlank(), //attivo solo se l’utente ha scritto qualcosa
                colors = ButtonDefaults.buttonColors(containerColor = TravelGreen) //colore coerente con tema principale
            ) {
                Icon(
                    imageVector = Icons.Default.Save, //icona di salvataggio
                    contentDescription = "Salva nota", //testo per accessibilità
                    tint = Color.White //colore icona bianco su sfondo verde
                )
                Spacer(modifier = Modifier.width(6.dp)) //spazio tra icona e testo
                Text("Salva", color = Color.White) //testo pulsante
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { //pulsante per chiudere il dialog senza salvare
                Text(
                    text = "Annulla", //testo pulsante di chiusura
                    color = Color.Gray //colore neutro per indicare azione secondaria
                )
            }
        }
    )
}


// mostra un pulsante che consente di aprire il dialog per aggiungere una nuova nota
//viene utilizzata nella schermata di tracciamento per permettere all’utente di scrivere e salvare appunti relativi al viaggio in corso
//la struttura è composta da una card contenente un titolo descrittivo e un pulsante di azione principale
@Composable
fun AddNoteButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth() //la card occupa tutta la larghezza disponibile
            .padding(horizontal = 16.dp, vertical = 8.dp), //aggiunge spaziatura esterna per separarla dagli altri elementi
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp) //applica un’ombra leggera
    ) {
        Column(modifier = Modifier.padding(16.dp)) { //colonna interna con margine per disporre titolo e pulsante
            Text(
                text = "📝 Aggiungi Note", //titolo della sezione che spiega l’azione
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TravelGreen
            )

            Spacer(modifier = Modifier.height(12.dp)) //spazio verticale tra titolo e pulsante

            Button(
                onClick = onClick, //esegue la funzione passata come parametro quando l’utente preme il pulsante
                modifier = Modifier
                    .fillMaxWidth() //fa sì che il pulsante occupi tutta la larghezza della card
                    .height(48.dp), //imposta un’altezza fissa
                colors = ButtonDefaults.buttonColors(
                    containerColor = TravelGreen //colore di sfondo verde per rendere il pulsante ben visibile
                ),
                shape = RoundedCornerShape(8.dp) //angoli leggermente arrotondati
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit, //icona a forma di matita
                    contentDescription = "Aggiungi Nota",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp)) //spazio orizzontale tra icona e testo del pulsante
                Text(
                    text = "Scrivi una Nota", //testo descrittivo dell’azione del pulsante
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

//mostra un pannello informativo con i dati principali del viaggio selezionato
//visualizza la destinazione, il tipo di viaggio, lo stato di tracciamento (con un indicatore rosso se attivo)
//e due statistiche principali: distanza percorsa e tempo trascorso
@Composable
fun TripInfoPanel(
    trip: Trip, //oggetto che contiene tutte le informazioni del viaggio corrente
    isTracking: Boolean, //flag che indica se il tracciamento gps è attualmente attivo
    distance: Double, //distanza totale percorsa in chilometri, aggiornata dinamicamente
    elapsedTime: Long //tempo trascorso in secondi dall’inizio del tracciamento
) {
    Card(
        modifier = Modifier
            .fillMaxWidth() //la card occupa l’intera larghezza del contenitore
            .padding(16.dp), //aggiunge spazio esterno per separarla dagli altri elementi
        colors = CardDefaults.cardColors(
            containerColor = if (isTracking) TravelGreen.copy(alpha = 0.1f) else Color.White //colore di sfondo verde chiaro se tracking attivo, bianco altrimenti
        ),
        elevation = CardDefaults.cardElevation(4.dp) //applica ombra
    ) {
        Column(modifier = Modifier.padding(16.dp)) { //contenuto principale disposto in colonna con margine interno
            Row(
                modifier = Modifier.fillMaxWidth(), //riga che si estende per tutta la larghezza
                horizontalArrangement = Arrangement.SpaceBetween, //distribuisce gli elementi agli estremi della riga
                verticalAlignment = Alignment.CenterVertically //allinea testo e indicatori verticalmente al centro
            ) {
                Column { //colonna sinistra con informazioni principali del viaggio
                    Text(
                        text = trip.destination, //mostra la destinazione del viaggio
                        fontWeight = FontWeight.Bold, //grassetto per evidenziare la meta
                        fontSize = 18.sp, //dimensione maggiore per il titolo principale
                        color = Color.Black
                    )
                    Text(
                        text = trip.tripType, //mostra il tipo di viaggio (es. Day trip, Multi-day trip, ecc.)
                        fontSize = 14.sp, //testo più piccolo per informazione secondaria
                        color = Color.Gray
                    )
                }

                if (isTracking) { //se il tracciamento è attivo mostra un piccolo indicatore visivo
                    Surface(
                        color = Color.Red, //colore rosso per indicare attività in corso
                        shape = CircleShape, //forma circolare come indicatore di stato
                        modifier = Modifier.size(12.dp) //dimensione piccola ma ben visibile
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(12.dp)) //spazio verticale tra intestazione e sezione statistiche

            Row(
                modifier = Modifier.fillMaxWidth(), //riga per le statistiche disposte orizzontalmente
                horizontalArrangement = Arrangement.SpaceAround //distribuisce in modo uniforme le due statistiche
            ) {
                StatisticItem(
                    label = "Distanza", //etichetta per la prima statistica
                    value = "${String.format("%.2f", distance)} km", //mostra distanza percorsa con due decimali
                    icon = "🗺️" //emoji mappa
                )

                val hours = elapsedTime / 3600 //calcola le ore dal tempo totale in secondi
                val minutes = (elapsedTime % 3600) / 60 //calcola i minuti rimanenti
                val seconds = elapsedTime % 60 //calcola i secondi rimanenti
                StatisticItem(
                    label = "Tempo", //etichetta per la seconda statistica
                    value = String.format("%02d:%02d:%02d", hours, minutes, seconds), //mostra tempo in formato hh:mm:ss
                    icon = "⏱️" //emoji cronometro per rappresentare la durata
                )
            }
        }
    }
}

// mostra una singola statistica del pannello informazioni viaggio
//è usata in TripInfoPanel per visualizzare in modo compatto un valore numerico con un’icona e una breve etichetta descrittiva
@Composable
fun StatisticItem(label: String, value: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { //colonna che allinea tutti gli elementi al centro orizzontalmente
        Text(text = icon, fontSize = 24.sp) //mostra l’icona o emoji rappresentativa della statistica (es. mappa o cronometro)
        Text(
            text = value, //valore numerico o temporale da mostrare (es. “3.42 km” o “00:25:14”)
            fontWeight = FontWeight.Bold, //grassetto per enfatizzare il valore principale
            fontSize = 18.sp,
            color = TravelGreen
        )
        Text(
            text = label, //etichetta che descrive il tipo di statistica (es. “Distanza”, “Tempo”)
            fontSize = 12.sp, //testo piccolo
            color = Color.Gray
        )
    }
}

//mostra i controlli principali per avviare o fermare il tracciamento gps del viaggio
//viene visualizzata nella parte inferiore della schermata di tracciamento e cambia contenuto dinamicamente in base allo stato attuale
//se il tracciamento non è attivo mostra un pulsante verde “inizia tracciamento”, altrimenti un pulsante rosso “ferma e salva”
@Composable
fun TrackingControls(
    isTracking: Boolean, //true se il tracciamento gps è attualmente attivo
    hasPermission: Boolean, //true se i permessi di localizzazione sono stati concessi
    onStartClick: () -> Unit, //callback eseguita quando l’utente avvia il tracciamento
    onStopClick: () -> Unit //callback eseguita quando l’utente ferma il tracciamento
) {
    //surface con altezza fissa per evitare che i pulsanti causino salti visivi quando cambia lo stato
    Surface(
        modifier = Modifier
            .fillMaxWidth() //occupa tutta la larghezza disponibile
            .height(88.dp), //altezza fissa per mantenere layout stabile
        color = Color.White, //sfondo bianco per separare visivamente i controlli dal resto della schermata
        shadowElevation = 8.dp //ombra per far apparire il pannello leggermente rialzato
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize() //la riga si espande per tutto lo spazio disponibile della surface
                .padding(16.dp), //padding interno per dare respiro agli elementi
            horizontalArrangement = Arrangement.Center, //centra orizzontalmente i pulsanti
            verticalAlignment = Alignment.CenterVertically //allinea verticalmente al centro
        ) {
            if (!isTracking) { //se il tracciamento non è attivo mostra il pulsante di avvio
                Button(
                    onClick = onStartClick, //esegue la funzione per iniziare il tracciamento
                    modifier = Modifier
                        .height(56.dp) //altezza del pulsante principale
                        .fillMaxWidth(0.8f), //occupa l’80% della larghezza del contenitore
                    shape = RoundedCornerShape(28.dp), //angoli molto arrotondati
                    colors = ButtonDefaults.buttonColors(containerColor = TravelGreen) //colore di sfondo verde
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow, //icona “play” per indicare avvio
                        contentDescription = null,
                        modifier = Modifier.size(24.dp) //dimensione icona proporzionata al testo
                    )
                    Spacer(modifier = Modifier.width(8.dp)) //spazio tra icona e testo
                    Text(
                        text = "Inizia Tracciamento", //testo del pulsante principale
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else { //se il tracciamento è già in corso mostra il pulsante di stop
                Button(
                    onClick = onStopClick, //esegue la funzione che interrompe il tracciamento e apre il dialog di conferma
                    modifier = Modifier
                        .height(56.dp) //stessa altezza del pulsante verde per uniformità
                        .fillMaxWidth(0.8f), //stessa larghezza per stabilità visiva
                    shape = RoundedCornerShape(28.dp), //angoli coerenti con lo stile del pulsante precedente
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red) //colore rosso per indicare azione di stop o chiusura
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop, //icona “stop” per indicare interruzione
                        contentDescription = null, //icona decorativa
                        modifier = Modifier.size(24.dp) //dimensione coerente con l’altra icona
                    )
                    Spacer(modifier = Modifier.width(8.dp)) //spazio orizzontale tra icona e testo
                    Text(
                        text = "Ferma e Salva", //testo esplicativo che chiarisce la doppia azione (stop + salvataggio)
                        fontSize = 16.sp, //dimensione media
                        fontWeight = FontWeight.Bold //grassetto per dare enfasi all’azione critica
                    )
                }
            }
        }
    }
}
