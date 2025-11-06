// ====================================================================
// NUOVO COMPONENTE TripSelector CON PRIORITÀ TEMPORALE
// ====================================================================
//
// Sostituisci il componente TripSelector esistente in TripTrackingScreen.kt
// con questo nuovo che implementa:
// 1. Raggruppamento per stato temporale (Attivi, Prossimi/Recenti, Altri)
// 2. Pulsante per creare viaggio rapido
// 3. Indicatori visivi di priorità
// 4. Sezione espandibile per viaggi fuori range
// ====================================================================

package com.example.travelcompanionapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import com.example.travelcompanionapp.viewmodel.TripTemporalStatus
import com.example.travelcompanionapp.viewmodel.TripViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * ⭐ COMPONENTE AGGIORNATO: Selettore viaggio intelligente con priorità temporale
 *
 * Raggruppa i viaggi per rilevanza temporale:
 * - 🟢 Viaggi ATTIVI (in corso oggi)
 * - 🟡 Viaggi PROSSIMI/RECENTI (±3 giorni)
 * - ⚪ Altri viaggi (espandibile)
 *
 * Include anche pulsante per creare viaggio rapido.
 */
@Composable
fun TripSelector(
    trips: List<Trip>,
    viewModel: TripViewModel,
    onTripSelected: (Trip) -> Unit,
    onCreateQuickTrip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Seleziona il viaggio da tracciare",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Pulsante per creare viaggio rapido
        OutlinedButton(
            onClick = onCreateQuickTrip,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFFE8F5E9),
                contentColor = Color(0xFF2E7D32)
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF4CAF50))
            )
        ) {
            Icon(Icons.Default.AddCircle, "Nuovo", tint = Color(0xFF4CAF50))
            Spacer(Modifier.width(8.dp))
            Text(
                "Inizia un nuovo viaggio ora",
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Raggruppa viaggi per stato temporale
        val activeTrips = trips.filter {
            viewModel.getTripTemporalStatus(it) == TripTemporalStatus.ACTIVE
        }
        val upcomingTrips = trips.filter {
            viewModel.getTripTemporalStatus(it) == TripTemporalStatus.UPCOMING
        }
        val recentTrips = trips.filter {
            viewModel.getTripTemporalStatus(it) == TripTemporalStatus.RECENT
        }
        val otherTrips = trips.filter {
            val status = viewModel.getTripTemporalStatus(it)
            status == TripTemporalStatus.FUTURE || status == TripTemporalStatus.PAST
        }

        // Sezione viaggi attivi (priorità alta)
        if (activeTrips.isNotEmpty()) {
            TripGroupHeader(
                text = "🟢 Viaggi in corso",
                color = Color(0xFF4CAF50),
                count = activeTrips.size
            )
            activeTrips.forEach { trip ->
                TripCard(
                    trip = trip,
                    viewModel = viewModel,
                    onTripSelected = onTripSelected,
                    isPriority = true
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Sezione viaggi prossimi/recenti
        if (upcomingTrips.isNotEmpty() || recentTrips.isNotEmpty()) {
            TripGroupHeader(
                text = "🟡 Viaggi recenti o prossimi",
                color = Color(0xFFFFA726),
                count = upcomingTrips.size + recentTrips.size
            )
            upcomingTrips.forEach { trip ->
                TripCard(
                    trip = trip,
                    viewModel = viewModel,
                    onTripSelected = onTripSelected,
                    isPriority = false
                )
            }
            recentTrips.forEach { trip ->
                TripCard(
                    trip = trip,
                    viewModel = viewModel,
                    onTripSelected = onTripSelected,
                    isPriority = false
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Sezione altri viaggi (espandibile)
        if (otherTrips.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }

            Surface(
                onClick = { expanded = !expanded },
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⚪ Altri viaggi (${otherTrips.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Comprimi" else "Espandi",
                        tint = Color.Gray
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    otherTrips.forEach { trip ->
                        TripCard(
                            trip = trip,
                            viewModel = viewModel,
                            onTripSelected = onTripSelected,
                            isPriority = false,
                            dimmed = true
                        )
                    }
                }
            }
        }

        // Messaggio se non ci sono viaggi
        if (trips.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.EventNote,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Nessun viaggio pianificato",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Crea un viaggio rapido per iniziare!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * Header per raggruppamento viaggi
 */
@Composable
fun TripGroupHeader(text: String, color: Color, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = MaterialTheme.shapes.small,
            color = color.copy(alpha = 0.2f)
        ) {
            Text(
                "$count",
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Card per singolo viaggio con indicatori di stato temporale
 */
@Composable
fun TripCard(
    trip: Trip,
    viewModel: TripViewModel,
    onTripSelected: (Trip) -> Unit,
    isPriority: Boolean,
    dimmed: Boolean = false
) {
    val status = viewModel.getTripTemporalStatus(trip)
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

    // Calcola info stato
    val (statusText, statusColor) = when (status) {
        TripTemporalStatus.ACTIVE -> {
            "In corso oggi" to Color(0xFF4CAF50)
        }
        TripTemporalStatus.UPCOMING -> {
            val days = viewModel.getDaysUntilTripStart(trip)
            "Inizia ${if (days == 0) "oggi" else if (days == 1) "domani" else "tra $days giorni"}" to Color(0xFFFFA726)
        }
        TripTemporalStatus.RECENT -> {
            val days = viewModel.getDaysSinceTripEnd(trip)
            "Finito ${if (days == 0) "oggi" else if (days == 1) "ieri" else "$days giorni fa"}" to Color(0xFFFFA726)
        }
        TripTemporalStatus.FUTURE -> {
            val days = viewModel.getDaysUntilTripStart(trip)
            "Tra $days giorni" to Color.Gray
        }
        TripTemporalStatus.PAST -> {
            "Passato" to Color.Gray
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPriority -> Color(0xFFE8F5E9)
                dimmed -> Color(0xFFF5F5F5)
                else -> Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPriority) 4.dp else 1.dp
        ),
        onClick = { onTripSelected(trip) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Destinazione
                Text(
                    trip.destination,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isPriority) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = if (isPriority) 16.sp else 15.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Date pianificate
                Text(
                    "${dateFormatter.format(trip.startDate)} - ${dateFormatter.format(trip.endDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Badge stato
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            statusText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        trip.tripType,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Seleziona",
                tint = if (isPriority) Color(0xFF4CAF50) else Color.Gray
            )
        }
    }
}