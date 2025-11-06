package com.example.travelcompanionapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.travelcompanionapp.data.Trip
import com.example.travelcompanionapp.data.TripNote
import com.example.travelcompanionapp.data.TripPhoto
import com.example.travelcompanionapp.utils.PhotoHelper
import com.example.travelcompanionapp.viewmodel.TripViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.Divider

/**
 * Schermata di dettaglio per un viaggio specifico.
 *
 * ⭐ AGGIORNATA: Ora mostra anche le foto oltre alle note
 *
 * Mostra:
 * - Informazioni complete del viaggio
 * - Descrizione generale
 * - TUTTE le foto del viaggio in una galleria
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
    val context = LocalContext.current

    // Osserva tutte le note del viaggio
    val notes by viewModel.getNotesForTrip(trip.id).collectAsState(initial = emptyList())

    // ⭐ NUOVO: Osserva tutte le foto del viaggio
    val photos by viewModel.getPhotosForTrip(trip.id).collectAsState(initial = emptyList())

    // Stato per mostrare la foto ingrandita
    var selectedPhoto by remember { mutableStateOf<TripPhoto?>(null) }

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
            // === DATE DETTAGLIATE ===
            item {
                TripDatesSection(trip = trip)
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

            // === ⭐ SEZIONE FOTO ===
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📷 Foto del Viaggio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TravelGreen
                    )

                    if (photos.isNotEmpty()) {
                        Surface(
                            color = TravelGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${photos.size}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                color = TravelGreen
                            )
                        }
                    }
                }
            }

            // Galleria foto o messaggio vuoto
            if (photos.isEmpty()) {
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
                            Text(text = "📷", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nessuna Foto",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Le foto scattate durante il viaggio appariranno qui",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                // Galleria foto con scroll orizzontale
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(photos) { photo ->
                            PhotoThumbnail(
                                photo = photo,
                                onClick = { selectedPhoto = photo }
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

    // ⭐ Dialog per mostrare la foto ingrandita
    selectedPhoto?.let { photo ->
        PhotoViewerDialog(
            photo = photo,
            onDismiss = { selectedPhoto = null }
        )
    }
}
@Composable
fun TripDatesSection(trip: Trip) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Titolo sezione
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = TravelGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "📅 Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date pianificate (sempre visibili)
            DateRow(
                label = "Pianificato:",
                startDate = trip.startDate,
                endDate = trip.endDate,
                color = Color.Gray,
                dateFormatter = dateFormatter
            )

            // Date effettive (solo se completato)
            if (trip.isCompleted && trip.actualStartDate != null && trip.actualEndDate != null) {
                Spacer(modifier = Modifier.height(8.dp))

                // Linea separatore
                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)

                Spacer(modifier = Modifier.height(8.dp))

                DateRow(
                    label = "Effettivo:",
                    startDate = trip.actualStartDate!!,
                    endDate = trip.actualEndDate!!,
                    color = TravelGreen,
                    dateFormatter = dateFormatter,
                    isBold = true
                )

                // Mostra durata tracking
                if (trip.totalTrackingDurationMs > 0) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = TravelGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Durata GPS attivo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                Text(
                                    trip.getFormattedDuration(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TravelGreen
                                )
                            }
                        }
                    }
                }

                // Badge se date diverse
                if (trip.hasDifferentActualDates()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Le date effettive di tracking sono diverse da quelle pianificate",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            } else if (trip.status == "In corso") {
                // Viaggio in corso ma non ancora completato
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = Color(0xFFFFA726),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Viaggio in corso",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFA726)
                            )
                            if (trip.actualStartDate != null) {
                                Text(
                                    "Iniziato il ${dateFormatter.format(trip.actualStartDate)}",
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
}

/**
 * Helper composable per mostrare una riga di date
 */
@Composable
fun DateRow(
    label: String,
    startDate: Date,
    endDate: Date,
    color: Color,
    dateFormatter: SimpleDateFormat,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            "${dateFormatter.format(startDate)} - ${dateFormatter.format(endDate)}",
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
    }
}
/**
 * ⭐ NUOVO: Componibile per mostrare una miniatura della foto.
 * Quando cliccata, apre la foto ingrandita.
 */
@Composable
fun PhotoThumbnail(
    photo: TripPhoto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Carica e mostra la miniatura
            val bitmap = remember(photo.filePath) {
                PhotoHelper.loadThumbnail(photo.filePath, 240, 240)
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = photo.caption ?: "Foto del viaggio",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Overlay scuro se c'è una didascalia
                if (photo.caption != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = photo.caption!!,
                            modifier = Modifier.padding(6.dp),
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 2
                        )
                    }
                }
            } else {
                // Placeholder se l'immagine non può essere caricata
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.BrokenImage,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

/**
 * ⭐ NUOVO: Dialog per mostrare una foto a schermo intero.
 */
@Composable
fun PhotoViewerDialog(
    photo: TripPhoto,
    onDismiss: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy 'alle' HH:mm", Locale.ITALIAN)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Carica l'immagine a risoluzione più alta
                val bitmap = remember(photo.filePath) {
                    PhotoHelper.loadThumbnail(photo.filePath, 1080, 1920)
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = photo.caption ?: "Foto del viaggio",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrokenImage,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info sulla foto
                Text(
                    text = dateFormatter.format(photo.timestamp),
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                if (photo.caption != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = photo.caption!!,
                        fontSize = 16.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (photo.locationName != null) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                            text = photo.locationName!!,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsante chiudi
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TravelGreen
                    )
                ) {
                    Text("Chiudi")
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