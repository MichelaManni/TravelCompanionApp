//gestisce la schermata di dettaglio di un viaggio con foto, note e informazioni
package com.example.travelcompanionapp.ui //package dell'interfaccia utente

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

@OptIn(ExperimentalMaterial3Api::class) //abilita api material3 sperimentali
@Composable //funzione disegna UI reattiva
fun TripDetailScreen( //mostra i dettagli completi del viaggio selezionato
    trip: Trip, //viaggio da visualizzare
    viewModel: TripViewModel, //fornitore di dati e logica
    onNavigateBack: () -> Unit //callback per tornare indietro
) {
    val context = LocalContext.current //contesto corrente

    val notes by viewModel.getNotesForTrip(trip.id).collectAsState(initial = emptyList()) //osserva flusso note e lo espone come stato
    val photos by viewModel.getPhotosForTrip(trip.id).collectAsState(initial = emptyList()) //osserva flusso foto e lo espone come stato

    var selectedPhoto by remember { mutableStateOf<TripPhoto?>(null) } //stato per tenere la foto selezionata

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN) //formatter per giorno/mese/anno
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.ITALIAN) //formatter per ore e minuti

    Scaffold( //layout con top bar e contenuto
        containerColor = Color.White, //colore di sfondo scaffolding
        topBar = { // top bar
            CenterAlignedTopAppBar( //barra con titolo centrato
                title = {
                    Text(
                        "Dettagli Viaggio",
                        color = TravelGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                navigationIcon = { //slot icona navigazione
                    IconButton(onClick = onNavigateBack) { //bottone che invoca la callback
                        Icon( //icona grafica
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, //freccia indietro auto-mirrored
                            contentDescription = "Torna Indietro",
                            tint = Color.Black //colore icona
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors( //palette della top bar
                    containerColor = Color.White //sfondo bianco
                )
            )
        }
    ) { paddingValues -> //lambda contenuto con padding di sistema
        LazyColumn( //lista verticale con lazy rendering
            modifier = Modifier
                .fillMaxSize() //occupa tutto lo spazio
                .padding(paddingValues), //applica padding fornito da Scaffold
            contentPadding = PaddingValues(16.dp), //padding interno del contenuto
            verticalArrangement = Arrangement.spacedBy(16.dp) //spaziatura fissa tra item
        ) {
            item { //item card informazioni principali
                Card( //contenitore card
                    modifier = Modifier.fillMaxWidth(), //larghezza piena
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White //sfondo card
                    ),
                    elevation = CardDefaults.cardElevation(4.dp), //ombra card
                    shape = RoundedCornerShape(12.dp) //angoli arrotondati
                ) {
                    Column( //colonna interna
                        modifier = Modifier.padding(20.dp) //padding interno
                    ) {
                        Row( //riga con icona e titolo
                            verticalAlignment = Alignment.CenterVertically //allinea verticalmente centro
                        ) {
                            Icon( //icona posizione
                                imageVector = Icons.Filled.LocationOn, //vettore icona
                                contentDescription = null,
                                tint = TravelGreen,
                                modifier = Modifier.size(32.dp) //dimensione icona
                            )
                            Spacer(modifier = Modifier.width(12.dp)) //spazio tra icona e testo
                            Text( //testo destinazione
                                text = trip.destination, //valore destinazione
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp)) //spazio verticale
                        Divider(color = Color.LightGray) //divisore visivo
                        Spacer(modifier = Modifier.height(16.dp)) //spazio verticale

                        DetailRow( //riga dettaglio date pianificate
                            icon = Icons.Filled.CalendarToday, //icona calendario
                            label = "Date", //etichetta
                            value = "${dateFormatter.format(trip.startDate)} - ${dateFormatter.format(trip.endDate)}" //testo con date formattate
                        )

                        Spacer(modifier = Modifier.height(12.dp)) //spazio verticale

                        DetailRow( //riga tipo viaggio
                            icon = Icons.Filled.Category, //icona categoria
                            label = "Tipo", //etichetta
                            value = trip.tripType //valore tipo
                        )

                        Spacer(modifier = Modifier.height(12.dp)) //spazio verticale

                        DetailRow( //riga stato
                            icon = Icons.Filled.Flag, //icona
                            label = "Stato", //etichetta
                            value = trip.status //valore stato
                        )

                        if (trip.totalDistanceKm > 0) { //mostra distanza solo se > 0
                            Spacer(modifier = Modifier.height(12.dp)) //spazio
                            DetailRow( //riga distanza
                                icon = Icons.Filled.Route, //icona percorso
                                label = "Distanza", //etichetta
                                value = "${String.format("%.1f", trip.totalDistanceKm)} km" //valore formattato in km
                            )
                        }

                        if (trip.destinationLat != null && trip.destinationLng != null) { //mostra coordinate se presenti
                            Spacer(modifier = Modifier.height(12.dp)) //spazio
                            DetailRow( //riga coordinate
                                icon = Icons.Filled.GpsFixed, //icona gps
                                label = "GPS", //etichetta
                                value = "${String.format("%.4f", trip.destinationLat)}, ${String.format("%.4f", trip.destinationLng)}" //lat/lng con 4 decimali
                            )
                        }
                    }
                }
            }
            item { //item sezione date dettagliate
                TripDatesSection(trip = trip) //mostra sezione date pianificate/effettive
            }
            if (trip.description.isNotBlank()) { //mostra descrizione solo se non vuota
                item { //item card descrizione
                    Card(
                        modifier = Modifier.fillMaxWidth(), //larghezza card piena
                        colors = CardDefaults.cardColors(
                            containerColor = TravelGreen.copy(alpha = 0.05f) //sfondo verde tenue
                        ),
                        shape = RoundedCornerShape(12.dp) //angoli arrotondati
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp) //padding interno
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically //allineamento contenuti riga
                            ) {
                                Icon( //icona descrizione
                                    imageVector = Icons.Filled.Description, //icona
                                    contentDescription = null,
                                    tint = TravelGreen, //colore icona
                                    modifier = Modifier.size(24.dp) //dimensione icona
                                )
                                Spacer(modifier = Modifier.width(8.dp)) //spazio
                                Text( //titolo sezione
                                    text = "Descrizione", //testo titolo
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TravelGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp)) //spazio

                            Text( //testo descrizione
                                text = trip.description, //contenuto descrizione
                                fontSize = 16.sp, //dimensione
                                color = Color.DarkGray //colore testo
                            )
                        }
                    }
                }
            }

            item { //item intestazione sezione foto
                Row(
                    modifier = Modifier.fillMaxWidth(), //occupa tutta la riga
                    horizontalArrangement = Arrangement.SpaceBetween, //spaziatura tra titolo e badge
                    verticalAlignment = Alignment.CenterVertically //allinea verticalmente
                ) {
                    Text( //titolo sezione foto
                        text = "📷 Foto del Viaggio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TravelGreen
                    )

                    if (photos.isNotEmpty()) { //se ci sono foto mostra badge conteggio
                        Surface(
                            color = TravelGreen.copy(alpha = 0.2f), //sfondo badge
                            shape = RoundedCornerShape(12.dp) //angoli
                        ) {
                            Text(
                                text = "${photos.size}", //numero foto
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), //padding interno
                                fontWeight = FontWeight.Bold,
                                color = TravelGreen
                            )
                        }
                    }
                }
            }

            if (photos.isEmpty()) { //gestione stato vuoto foto
                item { //card vuota foto
                    Card(
                        modifier = Modifier.fillMaxWidth(), //larghezza piena
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White //sfondo
                        ),
                        elevation = CardDefaults.cardElevation(2.dp), //ombra lieve
                        shape = RoundedCornerShape(12.dp) //angoli arrotondati
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth() //occupa tutta la larghezza
                                .padding(32.dp), //padding interno
                            horizontalAlignment = Alignment.CenterHorizontally //centra contenuti
                        ) {
                            Text(text = "📷", fontSize = 48.sp) //emoji camera
                            Spacer(modifier = Modifier.height(16.dp)) //spazio
                            Text(
                                text = "Nessuna Foto", //titolo vuoto
                                fontWeight = FontWeight.Bold, //grassetto
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp)) //spazio
                            Text(
                                text = "Le foto scattate durante il viaggio appariranno qui", //messaggio
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else { //galleria foto
                item { //item con riga scorrevole
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp), //spazio tra miniature
                        modifier = Modifier.fillMaxWidth() //larghezza piena
                    ) {
                        items(photos) { photo -> //itera le foto
                            PhotoThumbnail( //mostra miniatura foto
                                photo = photo, //dati foto
                                onClick = { selectedPhoto = photo } //seleziona per dialog
                            )
                        }
                    }
                }
            }

            item { //item intestazione sezione note
                Row(
                    modifier = Modifier.fillMaxWidth(), //larghezza piena
                    horizontalArrangement = Arrangement.SpaceBetween, //spazia titolo/badge
                    verticalAlignment = Alignment.CenterVertically //allinea verticalmente
                ) {
                    Text( //titolo note
                        text = "📝 Note del Viaggio", //testo
                        fontWeight = FontWeight.Bold, //grassetto
                        fontSize = 20.sp, //dimensione
                        color = TravelGreen //colore
                    )

                    if (notes.isNotEmpty()) { //se ci sono note mostra badge
                        Surface(
                            color = TravelGreen.copy(alpha = 0.2f), //sfondo badge
                            shape = RoundedCornerShape(12.dp) //angoli
                        ) {
                            Text(
                                text = "${notes.size}", //conteggio note
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), //padding
                                fontWeight = FontWeight.Bold,
                                color = TravelGreen
                            )
                        }
                    }
                }
            }

            if (notes.isEmpty()) { //stato vuoto note
                item { //card vuota note
                    Card(
                        modifier = Modifier.fillMaxWidth(), //larghezza piena
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White //sfondo
                        ),
                        elevation = CardDefaults.cardElevation(2.dp), //ombra lieve
                        shape = RoundedCornerShape(12.dp) //angoli arrotondati
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth() //larghezza piena
                                .padding(32.dp), //padding
                            horizontalAlignment = Alignment.CenterHorizontally //centra
                        ) {
                            Text(text = "📝", fontSize = 48.sp) //emoji nota
                            Spacer(modifier = Modifier.height(16.dp)) //spazio
                            Text(
                                text = "Nessuna Nota", //titolo vuoto
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp)) //spazio
                            Text(
                                text = "Le note aggiunte durante il viaggio appariranno qui", //messaggio
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else { //lista note
                items(notes) { note -> //itera note
                    NoteDetailCard( //mostra card dettaglio nota
                        note = note, //dati nota
                        timeFormatter = timeFormatter //formatter per ora
                    )
                }
            }
        }
    }

    selectedPhoto?.let { photo -> //se è selezionata una foto mostra dialog
        PhotoViewerDialog(
            photo = photo, //foto da visualizzare
            onDismiss = { selectedPhoto = null } //chiusura dialog azzera selezione
        )
    }
}

@Composable //sezione date del viaggio (pianificate ed effettive) con badge informativi
fun TripDatesSection(trip: Trip) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN) //formatter per date

    Card( //contenitore sezione date
        modifier = Modifier.fillMaxWidth(), //larghezza piena
        colors = CardDefaults.cardColors(
            containerColor = Color.White //sfondo bianco
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) //ombra lieve
    ) {
        Column(modifier = Modifier.padding(16.dp)) { //contenuto con padding
            Row(verticalAlignment = Alignment.CenterVertically) { //titolo sezione
                Icon(
                    Icons.Default.CalendarMonth, //icona calendario
                    contentDescription = null,
                    tint = TravelGreen, //colore icona
                    modifier = Modifier.size(20.dp) //dimensione
                )
                Spacer(modifier = Modifier.width(8.dp)) //spazio
                Text(
                    "📅 Date", //titolo
                    style = MaterialTheme.typography.titleMedium, //stile titolo medio
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp)) //spazio

            DateRow( //righe date pianificate
                label = "Pianificato:", //etichetta
                startDate = trip.startDate, //data inizio pianificata
                endDate = trip.endDate, //data fine pianificata
                color = Color.Gray,
                dateFormatter = dateFormatter //formatter
            )

            if (trip.isCompleted && trip.actualStartDate != null && trip.actualEndDate != null) { //se viaggio completato e date effettive disponibili
                Spacer(modifier = Modifier.height(8.dp)) //spazio

                Divider(color = Color(0xFFE0E0E0), thickness = 1.dp) //divisore separatore

                Spacer(modifier = Modifier.height(8.dp)) //spazio

                DateRow( //riga date effettive
                    label = "Effettivo:", //etichetta
                    startDate = trip.actualStartDate!!, //inizio effettivo (!!= variabile non null)
                    endDate = trip.actualEndDate!!, //fine effettiva
                    color = TravelGreen, //colore evidenziato
                    dateFormatter = dateFormatter, //formatter
                    isBold = true //rende il testo in grassetto
                )

                if (trip.totalTrackingDurationMs > 0) { //mostra durata tracciamento se presente
                    Spacer(modifier = Modifier.height(8.dp)) //spazio

                    Surface( //riquadro durata gps
                        color = Color(0xFFE8F5E9), //verde chiaro
                        shape = RoundedCornerShape(8.dp), //angoli arrotondati
                        modifier = Modifier.fillMaxWidth() //larghezza piena
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp), //padding interno
                            verticalAlignment = Alignment.CenterVertically //allineamento verticale
                        ) {
                            Icon(
                                Icons.Default.Timer, //icona timer
                                contentDescription = null, //nessuna descrizione
                                tint = TravelGreen, //colore icona
                                modifier = Modifier.size(18.dp) //dimensione
                            )
                            Spacer(modifier = Modifier.width(8.dp)) //spazio
                            Column { //testi durata
                                Text(
                                    "Durata GPS attivo", //etichetta
                                    style = MaterialTheme.typography.labelSmall, //stile piccolo
                                    color = Color.Gray //colore testo
                                )
                                Text(
                                    trip.getFormattedDuration(), //durata formattata
                                    style = MaterialTheme.typography.titleMedium, //stile titolo medio
                                    fontWeight = FontWeight.Bold, //grassetto
                                    color = TravelGreen //colore
                                )
                            }
                        }
                    }
                }

                if (trip.hasDifferentActualDates()) { //se le date effettive differiscono da quelle pianificate
                    Spacer(modifier = Modifier.height(8.dp)) //spazio

                    Surface( //badge avviso
                        color = Color(0xFFFFF3E0), //arancione chiaro
                        shape = RoundedCornerShape(8.dp), //angoli
                        modifier = Modifier.fillMaxWidth() //larghezza piena
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp), //padding
                            verticalAlignment = Alignment.CenterVertically //allineamento
                        ) {
                            Icon(
                                Icons.Default.Info, //icona info
                                contentDescription = null,
                                tint = Color(0xFFFF9800), //arancione
                                modifier = Modifier.size(18.dp) //dimensione
                            )
                            Spacer(modifier = Modifier.width(8.dp)) //spazio
                            Text(
                                "Le date effettive di tracking sono diverse da quelle pianificate", //messaggio
                                style = MaterialTheme.typography.bodySmall, //stile piccolo
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            } else if (trip.status == "In corso") { //in corso ma non completato
                Spacer(modifier = Modifier.height(8.dp)) //spazio

                Surface( //badge stato in corso
                    color = Color(0xFFFFF8E1), //giallo chiaro
                    shape = RoundedCornerShape(8.dp), //angoli
                    modifier = Modifier.fillMaxWidth() //larghezza piena
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp), //padding
                        verticalAlignment = Alignment.CenterVertically //allineamento
                    ) {
                        Icon(
                            Icons.Default.DirectionsWalk, //icona camminata
                            contentDescription = null, //nessuna descrizione
                            tint = Color(0xFFFFA726), //arancione
                            modifier = Modifier.size(18.dp) //dimensione
                        )
                        Spacer(modifier = Modifier.width(8.dp)) //spazio
                        Column { //testi stato in corso
                            Text(
                                "Viaggio in corso", //titolo
                                style = MaterialTheme.typography.labelSmall, //stile piccolo
                                fontWeight = FontWeight.Bold, //grassetto
                                color = Color(0xFFFFA726)
                            )
                            if (trip.actualStartDate != null) { //se disponibile mostra data avvio
                                Text(
                                    "Iniziato il ${dateFormatter.format(trip.actualStartDate)}", //testo avvio
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

@Composable //riga con etichetta e intervallo date formattato
fun DateRow(
    label: String, //testo etichetta a sinistra
    startDate: Date, //data di inizio
    endDate: Date, //data di fine
    color: Color, //colore dei testi
    dateFormatter: SimpleDateFormat, //formatter per date
    isBold: Boolean = false //rende i testi in grassetto se true
) {
    Row(
        modifier = Modifier.fillMaxWidth(), //larghezza piena
        horizontalArrangement = Arrangement.SpaceBetween, //spazia etichetta e valore
        verticalAlignment = Alignment.CenterVertically //allinea verticalmente
    ) {
        Text(
            label, //mostra etichetta
            style = MaterialTheme.typography.bodyMedium, //stile corpo
            color = color,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal //peso condizionale
        )
        Text(
            "${dateFormatter.format(startDate)} - ${dateFormatter.format(endDate)}", //intervallo formattato
            style = MaterialTheme.typography.bodyMedium, //stile corpo
            color = color, //colore testo
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal //peso condizionale
        )
    }
}

@Composable //miniatura cliccabile di una foto del viaggio
fun PhotoThumbnail(
    photo: TripPhoto, //dati della foto (path, didascalia, posizione)
    onClick: () -> Unit //callback al tocco per aprire la foto
) {
    Card(
        modifier = Modifier
            .size(120.dp) //dimensione miniatura
            .clickable(onClick = onClick), //abilita click
        shape = RoundedCornerShape(12.dp), //angoli arrotondati
        elevation = CardDefaults.cardElevation(4.dp) //ombra card
    ) {
        Box(
            modifier = Modifier.fillMaxSize() //riempie la card
        ) {
            val bitmap = remember(photo.filePath) { //memoizza caricamento in base al path
                PhotoHelper.loadThumbnail(photo.filePath, 240, 240) //carica miniatura ridimensionata
            }

            if (bitmap != null) { //se caricata con successo
                Image(
                    bitmap = bitmap.asImageBitmap(), //bitmap convertito in imagebitmap
                    contentDescription = photo.caption ?: "Foto del viaggio", //descrizione accessibile
                    modifier = Modifier.fillMaxSize(), //riempie la card
                    contentScale = ContentScale.Crop //ritaglio per riempire
                )

                if (photo.caption != null) { //se presente didascalia mostra overlay
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth() //larghezza piena
                            .align(Alignment.BottomStart), //ancora in basso
                        color = Color.Black.copy(alpha = 0.6f) //overlay scuro
                    ) {
                        Text(
                            text = photo.caption!!, //testo didascalia
                            modifier = Modifier.padding(6.dp), //padding interno
                            fontSize = 11.sp, //dimensione piccola
                            color = Color.White, //colore testo
                            maxLines = 2 //limita righe
                        )
                    }
                }
            } else { //se fallisce il caricamento
                Column(
                    modifier = Modifier.fillMaxSize(), //riempie
                    horizontalAlignment = Alignment.CenterHorizontally, //centra orizzontale
                    verticalArrangement = Arrangement.Center //centra verticale
                ) {
                    Icon(
                        imageVector = Icons.Filled.BrokenImage, //icona immagine rotta
                        contentDescription = null, //nessuna descrizione
                        tint = Color.Gray, //colore grigio
                        modifier = Modifier.size(48.dp) //dimensione icona
                    )
                }
            }
        }
    }
}

@Composable //dialog per mostrare una foto a schermo intero con info
fun PhotoViewerDialog(
    photo: TripPhoto, //foto da visualizzare
    onDismiss: () -> Unit //callback chiusura dialog
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy 'alle' HH:mm", Locale.ITALIAN) //formatter data/ora

    Dialog(onDismissRequest = onDismiss) { //mostra dialog modale
        Card(
            modifier = Modifier
                .fillMaxWidth() //larghezza piena
                .wrapContentHeight(), //altezza in base al contenuto
            shape = RoundedCornerShape(16.dp), //angoli arrotondati
            colors = CardDefaults.cardColors(
                containerColor = Color.White //sfondo bianco
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp) //padding interno
            ) {
                val bitmap = remember(photo.filePath) { //memorizza caricamento in base al path
                    PhotoHelper.loadThumbnail(photo.filePath, 1080, 1920) //carica immagine grande
                }

                if (bitmap != null) { //se caricata correttamente
                    Image(
                        bitmap = bitmap.asImageBitmap(), //bitmap convertito
                        contentDescription = photo.caption ?: "Foto del viaggio", //descrizione
                        modifier = Modifier
                            .fillMaxWidth() //larghezza piena
                            .height(400.dp), //altezza fissa per anteprima grande
                        contentScale = ContentScale.Fit //scala contenuto mantenendo proporzioni
                    )
                } else { //placeholder se non caricata
                    Box(
                        modifier = Modifier
                            .fillMaxWidth() //larghezza piena
                            .height(400.dp), //altezza fissa
                        contentAlignment = Alignment.Center //centra icona
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrokenImage, //icona immagine rotta
                            contentDescription = null, //nessuna descrizione
                            tint = Color.Gray, //colore grigio
                            modifier = Modifier.size(72.dp) //dimensione icona
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) //spazio

                Text(
                    text = dateFormatter.format(photo.timestamp), //mostra data/ora scatto
                    fontSize = 14.sp, //dimensione
                    color = Color.Gray //colore
                )

                if (photo.caption != null) { //se presente didascalia
                    Spacer(modifier = Modifier.height(8.dp)) //spazio
                    Text(
                        text = photo.caption!!, //testo didascalia
                        fontSize = 16.sp, //dimensione
                        color = Color.Black, //colore
                        fontWeight = FontWeight.Medium //peso medio
                    )
                }

                if (photo.locationName != null) { //se presente luogo
                    Spacer(modifier = Modifier.height(8.dp)) //spazio
                    Row(
                        verticalAlignment = Alignment.CenterVertically //allineamento
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn, //icona posizione
                            contentDescription = null,
                            tint = TravelGreen, //colore icona
                            modifier = Modifier.size(16.dp) //dimensione
                        )
                        Spacer(modifier = Modifier.width(4.dp)) //spazio
                        Text(
                            text = photo.locationName!!, //nome luogo
                            fontSize = 14.sp, //dimensione
                            color = Color.Gray //colore
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) //spazio

                Button(
                    onClick = onDismiss, //chiude il dialog
                    modifier = Modifier.fillMaxWidth(), //bottone a larghezza piena
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TravelGreen //colore bottone
                    )
                ) {
                    Text("Chiudi") //etichetta bottone
                }
            }
        }
    }
}

@Composable //card che visualizza una singola nota con orario e posizione
fun NoteDetailCard(
    note: TripNote, //nota da mostrare
    timeFormatter: SimpleDateFormat //formatter dell'ora
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN) //formatter data

    Card(
        modifier = Modifier.fillMaxWidth(), //card a larghezza piena
        colors = CardDefaults.cardColors(
            containerColor = Color.White //sfondo bianco
        ),
        elevation = CardDefaults.cardElevation(2.dp), //ombra lieve
        shape = RoundedCornerShape(12.dp) //angoli arrotondati
    ) {
        Column(
            modifier = Modifier.padding(16.dp) //padding interno
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically //allineamento
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime, //icona orologio
                    contentDescription = null,
                    tint = TravelGreen, //colore icona
                    modifier = Modifier.size(18.dp) //dimensione
                )
                Spacer(modifier = Modifier.width(6.dp)) //spazio
                Text(
                    text = "${dateFormatter.format(note.timestamp)} alle ${timeFormatter.format(note.timestamp)}", //data e ora nota
                    fontSize = 13.sp, //dimensione testo
                    color = Color.Gray, //colore
                    fontWeight = FontWeight.Medium //peso medio
                )
            }

            Spacer(modifier = Modifier.height(12.dp)) //spazio

            Text(
                text = note.text, //contenuto della nota
                fontSize = 16.sp, //dimensione
                color = Color.Black //colore
            )

            if (note.locationName != null) { //se la nota ha luogo associato
                Spacer(modifier = Modifier.height(12.dp)) //spazio

                Surface(
                    color = TravelGreen.copy(alpha = 0.1f), //riquadro leggero
                    shape = RoundedCornerShape(8.dp) //angoli
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp), //padding
                        verticalAlignment = Alignment.CenterVertically //allineamento
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn, //icona posizione
                            contentDescription = null, //nessuna descrizione
                            tint = TravelGreen, //colore icona
                            modifier = Modifier.size(16.dp) //dimensione
                        )
                        Spacer(modifier = Modifier.width(6.dp)) //spazio
                        Text(
                            text = note.locationName!!, //nome luogo
                            fontSize = 13.sp, //dimensione
                            color = Color.DarkGray //colore
                        )
                    }
                }
            }
        }
    }
}

@Composable //riga con icona, etichetta e valore per dettaglio informativo
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector, //icona a sinistra
    label: String, //etichetta descrittiva
    value: String //valore mostrato a destra
) {
    Row(
        verticalAlignment = Alignment.CenterVertically //allineamento verticale
    ) {
        Icon(
            imageVector = icon, //icona
            contentDescription = null, //nessuna descrizione
            tint = TravelGreen, //colore verde tema
            modifier = Modifier.size(20.dp) //dimensione icona
        )
        Spacer(modifier = Modifier.width(12.dp)) //spazio tra icona e testi
        Column { //colonna testi
            Text(
                text = label, //etichetta
                fontSize = 12.sp, //dimensione piccola
                color = Color.Gray //colore grigio
            )
            Text(
                text = value, //valore
                fontSize = 16.sp, //dimensione
                color = Color.Black, //colore
                fontWeight = FontWeight.Medium //peso medio
            )
        }
    }
}
