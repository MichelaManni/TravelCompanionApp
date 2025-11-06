package com.example.travelcompanionapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.airbnb.lottie.compose.*
import com.example.travelcompanionapp.R
import com.example.travelcompanionapp.data.Trip
import com.example.travelcompanionapp.viewmodel.TripViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Schermata Display Charts - Visualizza statistiche e mappe dei viaggi completati.
 *
 * FUNZIONALITÀ IMPLEMENTATE:
 * 1. Grafico a barre: Distanza totale per mese (ultimi 12 mesi)
 * 2. Mappa: Mostra tutte le destinazioni visitate con markers
 * 3. Statistiche generali: Totale km, numero viaggi
 *
 * Conforme alle specifiche del progetto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayChartsScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    // Osserva i viaggi completati dal ViewModel
    val completedTrips by viewModel.completedTrips.collectAsState(initial = emptyList())

    // Lottie Animation per il titolo
    val headerComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.display_charts_menu)
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
                            "Display Charts",
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

        // Se non ci sono viaggi completati, mostra messaggio vuoto
        if (completedTrips.isEmpty()) {
            EmptyChartsScreen()
        } else {
            // Altrimenti mostra le statistiche e i grafici
            ChartsContent(
                trips = completedTrips,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

/**
 * Contenuto principale con grafici e statistiche.
 * Layout scrollabile verticalmente.
 */
@Composable
fun ChartsContent(
    trips: List<Trip>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // === 1. STATISTICHE GENERALI ===
        GeneralStatisticsCard(trips = trips)

        // === 2. GRAFICO A BARRE: Distanza per mese ===
        MonthlyDistanceChart(trips = trips)

        // === 3. MAPPA: Destinazioni visitate ===
        DestinationsMapCard(trips = trips)

        // === 4. BONUS: Lista viaggi con dettagli ===
        CompletedTripsListCard(trips = trips)

        // Spazio finale
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Card con statistiche generali sui viaggi completati.
 * Mostra: Totale km, Numero viaggi.
 */
@Composable
fun GeneralStatisticsCard(trips: List<Trip>) {
    // Calcola statistiche
    val totalDistance = trips.sumOf { it.totalDistanceKm }
    val totalTrips = trips.size


    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TravelGreen.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Titolo
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = TravelGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "📊 Statistiche Generali",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TravelGreen
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tre statistiche in riga
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DisplayChartStatItem(
                    icon = "🗺️",
                    value = String.format("%.1f km", totalDistance),
                    label = "Distanza Totale"
                )

                DisplayChartStatItem(
                    icon = "✈️",
                    value = "$totalTrips",
                    label = "Viaggi Completati"
                )
            }

        }
    }
}

/**
 * Item per statistica nella card Display Charts.
 * Nome univoco per evitare conflitti con TripTrackingScreen.
 */
@Composable
fun DisplayChartStatItem(icon: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(text = icon, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
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
 * Grafico a barre che mostra la distanza percorsa per mese.
 * Ultimi 12 mesi dal mese corrente.
 */
@Composable
fun MonthlyDistanceChart(trips: List<Trip>) {
    val calendar = Calendar.getInstance()

    // Genera lista ultimi 12 mesi
    val last12Months = (0..11).map { monthsAgo ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthsAgo)
        SimpleDateFormat("MMM yy", Locale.ITALIAN).format(cal.time) to cal
    }.reversed()

    // Calcola distanza per ogni mese
    val monthlyData = last12Months.map { (label, cal) ->
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)

        val distance = trips
            .filter { trip ->
                val tripCal = Calendar.getInstance()
                tripCal.time = trip.endDate
                tripCal.get(Calendar.MONTH) == month &&
                        tripCal.get(Calendar.YEAR) == year
            }
            .sumOf { it.totalDistanceKm }

        label to distance
    }

    val maxDistance = monthlyData.maxOfOrNull { it.second } ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Titolo
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.ShowChart,
                    contentDescription = null,
                    tint = TravelGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "📈 Distanza per Mese",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TravelGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ultimi 12 mesi",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Grafico a barre
            monthlyData.forEach { (month, distance) ->
                MonthlyBarItem(
                    month = month,
                    distance = distance,
                    maxDistance = maxDistance
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Singola barra del grafico mensile.
 */
@Composable
fun MonthlyBarItem(month: String, distance: Double, maxDistance: Double) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = month,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.width(60.dp)
            )

            // Barra colorata
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                if (distance > 0) {
                    val percentage = (distance / maxDistance).coerceIn(0.0, 1.0).toFloat()
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percentage)
                            .background(TravelGreen, RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${String.format("%.0f", distance)} km",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (distance > 0) TravelGreen else Color.Gray,
                modifier = Modifier.width(70.dp)
            )
        }
    }
}

/**
 * Card con mappa che mostra tutte le destinazioni visitate.
 * Ogni viaggio con coordinate GPS viene mostrato come marker.
 */
@Composable
fun DestinationsMapCard(trips: List<Trip>) {
    // Filtra viaggi con coordinate GPS valide
    val tripsWithGPS = trips.filter {
        it.destinationLat != null && it.destinationLng != null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Titolo
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Map,
                    contentDescription = null,
                    tint = TravelGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "🗺️ Mappa Destinazioni",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TravelGreen
                    )
                    Text(
                        text = "${tripsWithGPS.size} destinazioni registrate",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (tripsWithGPS.isEmpty()) {
                // Messaggio se nessun viaggio ha GPS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📍", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nessuna destinazione con GPS",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Calcola centro della mappa (media coordinate)
                val centerLat = tripsWithGPS.mapNotNull { it.destinationLat }.average()
                val centerLng = tripsWithGPS.mapNotNull { it.destinationLng }.average()

                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(
                        LatLng(centerLat, centerLng),
                        if (tripsWithGPS.size == 1) 10f else 5f
                    )
                }

                // Mappa con markers
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        zoomGesturesEnabled = true,
                        scrollGesturesEnabled = true
                    )
                ) {
                    tripsWithGPS.forEach { trip ->
                        Marker(
                            state = rememberMarkerState(
                                position = LatLng(trip.destinationLat!!, trip.destinationLng!!)
                            ),
                            title = trip.destination,
                            snippet = "${String.format("%.1f", trip.totalDistanceKm)} km"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card con lista compatta dei viaggi completati.
 * Mostra destinazione, data e distanza.
 */
@Composable
fun CompletedTripsListCard(trips: List<Trip>) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Titolo
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = TravelGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "✅ Viaggi Completati",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TravelGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista viaggi
            trips.sortedByDescending { it.endDate }.forEach { trip ->
                CompletedTripItem(trip = trip, dateFormatter = dateFormatter)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Item singolo viaggio completato.
 */
@Composable
fun CompletedTripItem(trip: Trip, dateFormatter: SimpleDateFormat) {
    Surface(
        color = TravelGreen.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icona tipo viaggio
            Text(
                text = when (trip.tripType) {
                    "Local trip" -> "🏙️"
                    "Day trip" -> "🚗"
                    "Multi-day trip" -> "✈️"
                    else -> "📍"
                },
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.destination,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = dateFormatter.format(trip.endDate),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Distanza
            if (trip.totalDistanceKm > 0) {
                Surface(
                    color = TravelGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", trip.totalDistanceKm)} km",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TravelGreen
                    )
                }
            }
        }
    }
}

/**
 * Schermata mostrata quando non ci sono viaggi completati.
 */
@Composable
fun EmptyChartsScreen() {
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
            Text(text = "📊", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Nessun Dato Disponibile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Completa il tuo primo viaggio per vedere le statistiche!",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
}