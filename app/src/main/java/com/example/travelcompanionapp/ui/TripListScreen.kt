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
 *
 * ⭐ AGGIORNAMENTO: Ora visualizza anche le note dei viaggi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    viewModel: TripViewModel, // ViewModel per accedere ai dati
    onNavigateBack: () -> Unit, // Funzione per tornare indietro
    onTripClick: (Trip) -> Unit, // Funzione chiamata quando clicchi su un viaggio
    modifier: Modifier = Modifier
) {
    // Collezioniamo lo stato dal ViewModel (contiene la lista viaggi)
    val uiState by viewModel.uiState.collectAsState()

    // Stato locale per il filtro selezionato (null = mostra tutti)
    var selectedFilter by remember { mutableStateOf<String?>(null) }

    // Stato per mostrare/nascondere il menu filtri
    var showFilterMenu by remember { mutableStateOf(false) }

    // Lottie Animation per il titolo (stessa usata nel menu)
    val headerComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.miei_viaggi_menu)
    )
    val headerProgress by animateLottieCompositionAsState(
        composition = headerComposition,
        iterations = LottieConstants.IterateForever
    )

    // Filtriamo la lista viaggi in base al tipo selezionato
    val filteredTrips = if (selectedFilter == null) {
        // Se nessun filtro, mostra tutti i viaggi
        uiState.tripList
    } else {
        // Altrimenti mostra solo i viaggi del tipo selezionato
        uiState.tripList.filter { it.tripType == selectedFilter }
    }

    Scaffold(
        containerColor = Color.White, // Sfondo bianco
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // Titolo con animazione Lottie
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
                            "I Miei Viaggi",
                            color = TravelGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                navigationIcon = {
                    // Pulsante per tornare indietro
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna Indietro",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    // Pulsante per aprire i filtri
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = "Filtri",
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
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chip per mostrare il filtro attivo
            if (selectedFilter != null) {
                Surface(
                    color = TravelGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtro: $selectedFilter",
                            color = TravelGreen,
                            fontWeight = FontWeight.Medium
                        )
                        // Pulsante per rimuovere il filtro
                        TextButton(onClick = { selectedFilter = null }) {
                            Text("Rimuovi", color = TravelGreen)
                        }
                    }
                }
            }

            // Contenuto principale: lista viaggi o messaggio vuoto
            if (uiState.isLoading) {
                // Mostra loading mentre carica i dati
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TravelGreen)
                }
            } else if (filteredTrips.isEmpty()) {
                // Mostra messaggio se non ci sono viaggi
                EmptyTripsList(hasFilter = selectedFilter != null)
            } else {
                // Mostra la lista dei viaggi
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Per ogni viaggio nella lista, crea una card
                    items(filteredTrips) { trip ->
                        TripCard(
                            trip = trip,
                            onClick = { onTripClick(trip) }
                        )
                    }
                }
            }
        }

        // Menu a tendina per i filtri
        DropdownMenu(
            expanded = showFilterMenu,
            onDismissRequest = { showFilterMenu = false },
            modifier = Modifier.background(Color.White)
        ) {
            // Opzione "Tutti i viaggi" (rimuove filtro)
            DropdownMenuItem(
                text = { Text("Tutti i viaggi", color = Color.Black) },
                onClick = {
                    selectedFilter = null
                    showFilterMenu = false
                }
            )
            Divider()
            // Opzioni per ogni tipo di viaggio
            listOf("Local trip", "Day trip", "Multi-day trip").forEach { tripType ->
                DropdownMenuItem(
                    text = { Text(tripType, color = Color.Black) },
                    onClick = {
                        selectedFilter = tripType
                        showFilterMenu = false
                    }
                )
            }
        }
    }
}

/**
 * Card che rappresenta un singolo viaggio nella lista.
 * Mostra destinazione, date, tipo, stato, descrizione e NUMERO DI NOTE.
 *
 * ⭐ AGGIORNAMENTO: Badge con numero di note
 */
@Composable
fun TripCard(
    trip: Trip,
    onClick: () -> Unit,
    noteCount: Int = 0 // ⭐ NUOVO parametro
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Rende la card cliccabile
        shape = RoundedCornerShape(12.dp), // Bordi arrotondati
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp // Ombra della card
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // RIGA 1: Destinazione + icona posizione
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = TravelGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trip.destination,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // RIGA 2: Date del viaggio
            Text(
                text = "${dateFormatter.format(trip.startDate)} - ${dateFormatter.format(trip.endDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // RIGA 3: Tipo viaggio e stato
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip per il tipo di viaggio
                Surface(
                    color = TravelGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = trip.tripType,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = TravelGreen,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Badge per lo stato del viaggio
                StatusBadge(status = trip.status)
            }

            // ⭐ RIGA 3.5: Descrizione (se presente)
            if (trip.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))

                // Card interna per la descrizione con sfondo leggero
                Surface(
                    color = TravelGreen.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Icona descrizione
                        Icon(
                            imageVector = Icons.Filled.Notes,
                            contentDescription = null,
                            tint = TravelGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Testo della descrizione (max 3 righe)
                        Text(
                            text = trip.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray,
                            maxLines = 3, // Mostra max 3 righe, poi "..."
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // RIGA 4: Distanza (se presente e > 0)
            if (trip.totalDistanceKm > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Distanza: ${String.format("%.1f", trip.totalDistanceKm)} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // RIGA 5: Coordinate GPS (se presenti)
            if (trip.destinationLat != null && trip.destinationLng != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "GPS: ${String.format("%.4f", trip.destinationLat)}, ${String.format("%.4f", trip.destinationLng)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Badge colorato che mostra lo stato del viaggio.
 * Colori diversi per stati diversi (Pianificato, In corso, Completato).
 */
@Composable
fun StatusBadge(status: String) {
    // Scegliamo il colore in base allo stato
    val backgroundColor = when (status) {
        "Completato" -> Color(0xFF4CAF50) // Verde
        "In corso" -> Color(0xFFFFA726) // Arancione
        else -> Color(0xFF90A4AE) // Grigio (Pianificato)
    }

    Surface(
        color = backgroundColor.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = backgroundColor,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Schermata vuota mostrata quando non ci sono viaggi.
 * Cambia messaggio in base alla presenza di filtri attivi.
 */
@Composable
fun EmptyTripsList(hasFilter: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Emoji grande
            Text(
                text = if (hasFilter) "🔍" else "✈️",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Messaggio principale
            Text(
                text = if (hasFilter) {
                    "Nessun viaggio trovato"
                } else {
                    "Nessun viaggio salvato"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Messaggio secondario
            Text(
                text = if (hasFilter) {
                    "Prova a rimuovere il filtro"
                } else {
                    "Crea il tuo primo viaggio dal menu principale"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}