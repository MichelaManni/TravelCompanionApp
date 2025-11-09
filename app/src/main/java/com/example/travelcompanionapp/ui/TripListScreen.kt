package com.example.travelcompanionapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcompanionapp.data.Trip
import com.example.travelcompanionapp.viewmodel.TripViewModel
import com.example.travelcompanionapp.R
import com.airbnb.lottie.compose.*
import java.text.SimpleDateFormat
import java.util.*


/**
 * Schermata principale per visualizzare la lista dei viaggi salvati.
 * Mostra tutti i viaggi con possibilità di filtro per tipo.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    viewModel: TripViewModel, //viewmodel che gestisce i dati dei viaggi e lo stato della lista
    onNavigateBack: () -> Unit, //funzione di callback per tornare alla schermata precedente
    onTripClick: (Trip) -> Unit, //callback che riceve il viaggio cliccato per aprire i dettagli
    modifier: Modifier = Modifier //modifier esterno per padding o estensioni grafiche
) {
    val uiState by viewModel.uiState.collectAsState() //osserva lo stato ui del viewmodel e ricompone la schermata quando cambia

    var selectedFilter by remember { mutableStateOf<String?>(null) } //mantiene il tipo di viaggio filtrato (null mostra tutti)
    var showFilterMenu by remember { mutableStateOf(false) } //gestisce la visibilità del menu a tendina dei filtri

    val headerComposition by rememberLottieComposition( //carica la composizione lottie per l’animazione del titolo
        LottieCompositionSpec.RawRes(R.raw.miei_viaggi_menu)
    )
    val headerProgress by animateLottieCompositionAsState( //gestisce lo stato di animazione continuo del titolo
        composition = headerComposition,
        iterations = LottieConstants.IterateForever
    )

    //applica il filtro in base al tipo di viaggio selezionato
    val filteredTrips = if (selectedFilter == null) {
        uiState.tripList //se nessun filtro attivo, mostra tutti i viaggi
    } else {
        uiState.tripList.filter { it.tripType == selectedFilter } //altrimenti mostra solo quelli con il tipo selezionato
    }

    Scaffold(
        containerColor = Color.White, //sfondo generale bianco
        topBar = {
            CenterAlignedTopAppBar( //barra superiore centrata
                title = {
                    Row( //riga che contiene animazione e testo
                        verticalAlignment = Alignment.CenterVertically, //centra verticalmente gli elementi
                        horizontalArrangement = Arrangement.Center //centra orizzontalmente
                    ) {
                        LottieAnimation( //riproduce l’animazione lottie come icona titolo
                            composition = headerComposition,
                            progress = headerProgress,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp)) //spazio tra icona e testo
                        Text(
                            "I Miei Viaggi", //titolo della schermata
                            color = TravelGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { //bottone per tornare alla schermata precedente
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, //icona freccia indietro
                            contentDescription = "Torna Indietro", //descrizione per accessibilità
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) { //bottone per aprire il menu dei filtri
                        Icon(
                            imageVector = Icons.Filled.FilterList, //icona filtro
                            contentDescription = "Filtri", //descrizione accessibilità
                            tint = TravelGreen
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
            modifier = modifier
                .fillMaxSize() //occupa tutta l’altezza disponibile
                .padding(paddingValues) //rispetta i padding forniti dallo scaffold
        ) {
            if (selectedFilter != null) { //se esiste un filtro attivo
                Surface(
                    color = TravelGreen.copy(alpha = 0.1f), //sfondo leggero per distinguere il chip filtro
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth() //larghezza piena
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, //testo a sinistra e bottone a destra
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtro: $selectedFilter", //mostra tipo di viaggio filtrato
                            color = TravelGreen,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = { selectedFilter = null }) { //bottone per rimuovere il filtro
                            Text("Rimuovi", color = TravelGreen)
                        }
                    }
                }
            }

            if (uiState.isLoading) { //mostra caricamento se i dati non sono ancora pronti
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center //centra il loader
                ) {
                    CircularProgressIndicator(color = TravelGreen) //indicatore di caricamento circolare
                }
            } else if (filteredTrips.isEmpty()) { //mostra schermata vuota se non ci sono viaggi
                EmptyTripsList(hasFilter = selectedFilter != null)
            } else {
                LazyColumn( //lista scorrevole per i viaggi
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp), //margini interni della lista
                    verticalArrangement = Arrangement.spacedBy(12.dp) //spazio tra gli elementi
                ) {
                    items(filteredTrips) { trip -> //itera sui viaggi filtrati
                        TripCard( //mostra una card per ogni viaggio
                            trip = trip, //oggetto viaggio corrente
                            onClick = { onTripClick(trip) } //richiama callback con viaggio selezionato
                        )
                    }
                }
            }
        }

        DropdownMenu( //menu a tendina per selezionare il filtro
            expanded = showFilterMenu, //visibilità controllata da stato locale
            onDismissRequest = { showFilterMenu = false }, //chiude il menu se l’utente clicca fuori
            modifier = Modifier.background(Color.White)
        ) {
            DropdownMenuItem( //opzione per mostrare tutti i viaggi
                text = { Text("Tutti i viaggi", color = Color.Black) },
                onClick = {
                    selectedFilter = null //rimuove filtro
                    showFilterMenu = false //chiude il menu
                }
            )
            Divider() //linea di separazione tra le opzioni
            listOf("Local trip", "Day trip", "Multi-day trip").forEach { tripType -> //crea dinamicamente le voci filtro
                DropdownMenuItem(
                    text = { Text(tripType, color = Color.Black) }, //mostra nome del tipo
                    onClick = {
                        selectedFilter = tripType //applica filtro selezionato
                        showFilterMenu = false //chiude menu
                    }
                )
            }
        }
    }
}


/**
 * Rappresenta singolo viaggio all'interno della lista principale
ogni card mostra le informazioni principali del viaggio:
destinazione, date, tipo e stato;
eventuale durata gps e distanza;
una breve descrizione se disponibile.
 */
@Composable
fun TripCard(
    trip: Trip, //oggetto dati che rappresenta un singolo viaggio salvato
    onClick: () -> Unit, //callback eseguita quando l’utente clicca sulla card
    modifier: Modifier = Modifier //modifier per personalizzare margini o dimensioni esterne
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN) //formatter locale per gestire e mostrare le date in formato italiano

    Card(
        modifier = modifier
            .fillMaxWidth() //la card occupa tutta la larghezza disponibile
            .clickable(onClick = onClick), //rende la card cliccabile per aprire il dettaglio
        colors = CardDefaults.cardColors(
            containerColor = Color.White //sfondo bianco
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), //aggiunge ombra
        shape = RoundedCornerShape(12.dp) //angoli arrotondati
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth() //colonna interna che occupa tutta la larghezza
                .padding(16.dp) //padding interno per distanziare il contenuto dai bordi
        ) {
            //riga 1: destinazione del viaggio con icona e titolo
            Row(
                verticalAlignment = Alignment.CenterVertically //centra icona e testo verticalmente
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn, //icona localizzazione per rappresentare la destinazione
                    contentDescription = null,
                    tint = TravelGreen,
                    modifier = Modifier.size(24.dp) //dimensione compatta dell’icona
                )
                Spacer(modifier = Modifier.width(8.dp)) //spazio tra icona e testo
                Text(
                    text = trip.destination, //mostra il nome della destinazione
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp)) //spazio verticale tra righe

            //riga 2: mostra il range di date del viaggio
            Row(
                modifier = Modifier.fillMaxWidth(), //riga a larghezza piena
                horizontalArrangement = Arrangement.SpaceBetween, //distribuisce i contenuti agli estremi se necessario
                verticalAlignment = Alignment.CenterVertically //centra il testo verticalmente
            ) {
                Column(modifier = Modifier.weight(1f)) { //colonna che si adatta allo spazio disponibile
                    Text(
                        text = trip.getFormattedDateRange(), //usa metodo del modello per ottenere la stringa date già formattata
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (trip.hasDifferentActualDates()) { //cambia colore se le date effettive sono diverse da quelle pianificate
                            Color(0xFF2E7D32) //verde per evidenziare variazioni
                        } else {
                            Color.Gray //grigio per date standard
                        }
                    )

                    if (trip.isCompleted && trip.totalTrackingDurationMs > 0) { //mostra solo se il viaggio è completato e il gps ha registrato durata
                        Spacer(modifier = Modifier.height(2.dp)) //piccolo spazio prima del testo
                        Text(
                            text = "GPS attivo: ${trip.getFormattedDuration()}", //mostra durata totale registrata dal gps
                            style = MaterialTheme.typography.bodySmall, //stile piccolo per informazione aggiuntiva
                            color = Color(0xFF4CAF50), //verde chiaro per indicare attività positiva
                            fontWeight = FontWeight.Medium //peso medio per distinguere dal testo principale
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp)) //spazio tra sezione date e successiva

            //riga 3: mostra tipo di viaggio e stato attuale
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, //distribuisce tipo e badge stato agli estremi
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = TravelGreen.copy(alpha = 0.2f), //sfondo leggero per evidenziare tipo viaggio
                    shape = RoundedCornerShape(16.dp) //forma arrotondata
                ) {
                    Text(
                        text = trip.tripType, //mostra la categoria del viaggio
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), //padding interno
                        style = MaterialTheme.typography.bodySmall,
                        color = TravelGreen,
                        fontWeight = FontWeight.Medium
                    )
                }

                StatusBadge(status = trip.status) //mostra badge colorato per stato (pianificato, in corso, completato)
            }

            //riga 3.5: mostra descrizione solo se è stata compilata
            if (trip.description.isNotBlank()) { //condizione per non occupare spazio se campo vuoto
                Spacer(modifier = Modifier.height(12.dp)) //distanza tra blocchi
                Surface(
                    color = TravelGreen.copy(alpha = 0.05f), //sfondo molto tenue per differenziare la sezione descrizione
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth() //occupa tutta la larghezza della card
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp), //padding interno della card secondaria
                        verticalAlignment = Alignment.Top //allinea in alto icona e testo
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notes, //icona per rappresentare le note/descrizione
                            contentDescription = null,
                            tint = TravelGreen,
                            modifier = Modifier.size(16.dp) //dimensione ridotta per coerenza con testo piccolo
                        )
                        Spacer(modifier = Modifier.width(8.dp)) //spazio tra icona e testo
                        Text(
                            text = trip.description, //testo della descrizione inserita dall’utente
                            style = MaterialTheme.typography.bodySmall, //stile piccolo per testo secondario
                            color = Color.DarkGray, //colore neutro ma leggibile
                            maxLines = 3, //limita la visualizzazione a tre righe
                            modifier = Modifier.weight(1f) //consente al testo di occupare lo spazio rimanente
                        )
                    }
                }
            }

            if (trip.totalDistanceKm > 0) { //mostra la distanza solo se è maggiore di zero
                Spacer(modifier = Modifier.height(8.dp)) //piccolo spazio sopra la riga distanza
                Text(
                    text = "Distanza: ${String.format("%.1f", trip.totalDistanceKm)} km", //mostra la distanza percorsa formattata con una cifra decimale
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (trip.destinationLat != null && trip.destinationLng != null) { //verifica che ci siano coordinate valide
                Spacer(modifier = Modifier.height(4.dp)) //spazio leggero tra le righe
                Text(
                    text = "GPS: ${String.format("%.4f", trip.destinationLat)}, ${String.format("%.4f", trip.destinationLng)}", //mostra coordinate con quattro decimali
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray //colore neutro per informazione tecnica
                )
            }
        }
    }
}

/**
 * mostra un piccolo badge colorato che indica lo stato attuale del viaggio.
 * il colore e il testo del badge cambiano in base allo stato del viaggio:
 * - verde per "Completato"
 * - arancione per "In corso"
 * - grigio per "Pianificato" o altri stati non riconosciuti
 * utilizza un componente Surface con forma arrotondata e testo centrato.
 */
@Composable
fun StatusBadge(status: String) {
    //sceglie il colore di base in base allo stato del viaggio
    val backgroundColor = when (status) {
        "Completato" -> Color(0xFF4CAF50) //verde per indicare completamento
        "In corso" -> Color(0xFFFFA726) //arancione per viaggio attivo
        else -> Color(0xFF90A4AE) //grigio per stato pianificato o sconosciuto
    }

    Surface(
        color = backgroundColor.copy(alpha = 0.2f), //usa una versione trasparente
        shape = RoundedCornerShape(16.dp) //bordo arrotondato
    ) {
        Text(
            text = status, //mostra lo stato come testo leggibile
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), //padding interno per dare spessore visivo al badge
            style = MaterialTheme.typography.bodySmall,
            color = backgroundColor, //colore del testo identico alla versione piena del colore di stato
            fontWeight = FontWeight.Bold //testo in grassetto per evidenziare lo stato
        )
    }
}


/**
 * mostra una schermata di stato vuoto quando non ci sono viaggi da visualizzare.
 * viene utilizzata in TripListScreen per gestire due casi distinti:
 * - quando non ci sono viaggi salvati (nessun contenuto)
 * - quando un filtro è attivo ma non restituisce risultati
 */
@Composable
fun EmptyTripsList(hasFilter: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize() //riempie l’intero spazio disponibile sullo schermo
            .padding(32.dp), //aggiunge margine interno per distanziare il contenuto dai bordi
        contentAlignment = Alignment.Center //centra verticalmente e orizzontalmente il contenuto
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally, //centra tutti gli elementi orizzontalmente
            verticalArrangement = Arrangement.Center //distribuisce gli elementi in verticale al centro del box
        ) {
            //emoji principale che cambia in base al contesto (filtrato o no)
            Text(
                text = if (hasFilter) "🔍" else "✈️", //mostra lente di ingrandimento se è attivo un filtro, altrimenti aereo
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp)) //spazio tra emoji e messaggio principale

            //messaggio principale che informa l’utente della mancanza di risultati
            Text(
                text = if (hasFilter) {
                    "Nessun viaggio trovato" //testo mostrato quando il filtro non produce risultati
                } else {
                    "Nessun viaggio salvato" //testo mostrato quando non ci sono viaggi registrati
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp)) //spazio tra messaggio principale e quello secondario

            //messaggio secondario con suggerimento per l’utente
            Text(
                text = if (hasFilter) {
                    "Prova a rimuovere il filtro" //invita a disattivare il filtro per visualizzare altri viaggi
                } else {
                    "Crea il tuo primo viaggio dal menu principale" //messaggio guida per utenti nuovi
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}
