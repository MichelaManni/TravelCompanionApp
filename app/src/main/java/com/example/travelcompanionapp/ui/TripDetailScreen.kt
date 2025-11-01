package com.example.travelcompanionapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcompanionapp.data.Trip
import com.example.travelcompanionapp.data.TripNote
import com.example.travelcompanionapp.viewmodel.TripViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Schermata di dettaglio per un viaggio specifico.
 *
 * Mostra:
 * - Informazioni complete del viaggio
 * - Descrizione generale
 * - TUTTE le note cronologiche del viaggio
 * - Statistiche (distanza, date, tipo)
 *
 * Questa schermata si apre quando l'utente clicca su una card nella TripListScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    trip: Trip,
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    // Osserva tutte le note del viaggio
    val notes by viewModel.getNotesForTrip(trip.id).collectAsState(initial = emptyList())

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.ITALIAN)

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Dettagli Viaggio",
                        color = TravelGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === CARD INFORMAZIONI PRINCIPALI ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Destinazione
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = TravelGreen,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = trip.destination,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Date
                        DetailRow(
                            icon = Icons.Filled.CalendarToday,
                            label = "Date",
                            value = "${dateFormatter.format(trip.startDate)} - ${dateFormatter.format(trip.endDate)}"
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tipo viaggio
                        DetailRow(
                            icon = Icons.Filled.Category,
                            label = "Tipo",
                            value = trip.tripType
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stato
                        DetailRow(
                            icon = Icons.Filled.Flag,
                            label = "Stato",
                            value = trip.status
                        )

                        // Distanza (se presente)
                        if (trip.totalDistanceKm > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailRow(
                                icon = Icons.Filled.Route,
                                label = "Distanza",
                                value = "${String.format("%.1f", trip.totalDistanceKm)} km"
                            )
                        }

                        // Coordinate GPS (se presenti)
                        if (trip.destinationLat != null && trip.destinationLng != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailRow(
                                icon = Icons.Filled.GpsFixed,
                                label = "GPS",
                                value = "${String.format("%.4f", trip.destinationLat)}, ${String.format("%.4f", trip.destinationLng)}"
                            )
                        }
                    }
                }
            }

            // === DESCRIZIONE (se presente) ===
            if (trip.description.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = TravelGreen.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = TravelGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Descrizione",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TravelGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = trip.description,
                                fontSize = 16.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            // === SEZIONE NOTE ===
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📝 Note del Viaggio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TravelGreen
                    )

                    if (notes.isNotEmpty()) {
                        Surface(
                            color = TravelGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${notes.size}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                color = TravelGreen
                            )
                        }
                    }
                }
            }

            // Lista note o messaggio vuoto
            if (notes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📝", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nessuna Nota",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Le note aggiunte durante il viaggio appariranno qui",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                // Ogni nota in una card separata
                items(notes) { note ->
                    NoteDetailCard(
                        note = note,
                        timeFormatter = timeFormatter
                    )
                }
            }
        }
    }
}

/**
 * Card per visualizzare una singola nota in dettaglio.
 */
@Composable
fun NoteDetailCard(
    note: TripNote,
    timeFormatter: SimpleDateFormat
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    tint = TravelGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${dateFormatter.format(note.timestamp)} alle ${timeFormatter.format(note.timestamp)}",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Testo della nota
            Text(
                text = note.text,
                fontSize = 16.sp,
                color = Color.Black
            )

            // Posizione (se presente)
            if (note.locationName != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = TravelGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = TravelGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = note.locationName!!,
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

/**
 * Riga con icona, label e valore per i dettagli del viaggio.
 */
@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TravelGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 16.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium
            )
        }
    }
}