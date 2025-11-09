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
 *  selettore di viaggio con priorità temporale.
 * viene utilizzata in TripTrackingScreen per permettere all’utente di scegliere un viaggio da monitorare.
 * i viaggi vengono automaticamente raggruppati in base alla loro rilevanza temporale:
 * - attivi oggi (stato "in corso")
 * - prossimi o recenti (±3 giorni dalla data corrente)
 * - altri viaggi (futuri lontani o già conclusi)
 * la funzione mostra anche un pulsante per creare rapidamente un nuovo viaggio
 * e una sezione espandibile per visualizzare i viaggi meno rilevanti.
 */
@Composable
fun TripSelector(
    trips: List<Trip>, //lista di viaggi da mostrare e raggruppare
    viewModel: TripViewModel, //viewmodel per calcolare lo stato temporale di ciascun viaggio
    onTripSelected: (Trip) -> Unit, //callback chiamata quando l’utente seleziona un viaggio
    onCreateQuickTrip: () -> Unit //callback per creare immediatamente un nuovo viaggio
) {
    Column(
        modifier = Modifier
            .fillMaxWidth() //colonna che occupa tutta la larghezza disponibile
            .padding(16.dp) //padding esterno per distanziare dai bordi
    ) {
        Text(
            "Seleziona il viaggio da tracciare", //titolo principale della sezione
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )

        Spacer(modifier = Modifier.height(12.dp)) //spazio tra titolo e pulsante

        OutlinedButton(
            onClick = onCreateQuickTrip, //avvia la creazione rapida di un viaggio
            modifier = Modifier.fillMaxWidth(), //pulsante largo quanto lo schermo
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFFE8F5E9),
                contentColor = Color(0xFF2E7D32)
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF4CAF50))
            )
        ) {
            Icon(Icons.Default.AddCircle, "Nuovo", tint = Color(0xFF4CAF50)) //icona per aggiungere nuovo viaggio
            Spacer(Modifier.width(8.dp)) //spazio tra icona e testo
            Text(
                "Inizia un nuovo viaggio ora", //testo chiaro che spiega l’azione
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp)) //spazio prima dei gruppi

        //filtra i viaggi in base allo stato temporale calcolato dal viewmodel
        val activeTrips = trips.filter { viewModel.getTripTemporalStatus(it) == TripTemporalStatus.ACTIVE } //viaggi attivi oggi
        val upcomingTrips = trips.filter { viewModel.getTripTemporalStatus(it) == TripTemporalStatus.UPCOMING } //viaggi che iniziano nei prossimi giorni
        val recentTrips = trips.filter { viewModel.getTripTemporalStatus(it) == TripTemporalStatus.RECENT } //viaggi terminati di recente
        val otherTrips = trips.filter { //viaggi fuori range di interesse
            val status = viewModel.getTripTemporalStatus(it)
            status == TripTemporalStatus.FUTURE || status == TripTemporalStatus.PAST
        }

        if (activeTrips.isNotEmpty()) { //se esistono viaggi attivi li mostra per primi
            TripGroupHeader( //intestazione per la sezione attiva
                text = "🟢 Viaggi in corso", //titolo della sezione
                color = Color(0xFF4CAF50), //colore verde coerente con stato attivo
                count = activeTrips.size //numero di viaggi attivi
            )
            activeTrips.forEach { trip -> //mostra ogni viaggio come card prioritaria
                TripCard(
                    trip = trip,
                    viewModel = viewModel,
                    onTripSelected = onTripSelected,
                    isPriority = true //indica che il viaggio ha priorità visiva
                )
            }
            Spacer(modifier = Modifier.height(8.dp)) //spazio prima della prossima sezione
        }

        if (upcomingTrips.isNotEmpty() || recentTrips.isNotEmpty()) { //se ci sono viaggi prossimi o recenti li mostra
            TripGroupHeader(
                text = "🟡 Viaggi recenti o prossimi", //titolo per gruppo intermedio
                color = Color(0xFFFFA726), //colore arancione per evidenziare attenzione
                count = upcomingTrips.size + recentTrips.size //totale elementi della sezione
            )
            upcomingTrips.forEach { trip -> //mostra i viaggi imminenti
                TripCard(
                    trip = trip,
                    viewModel = viewModel,
                    onTripSelected = onTripSelected,
                    isPriority = false //non priorità ma rilevanti
                )
            }
            recentTrips.forEach { trip -> //mostra viaggi terminati da poco
                TripCard(
                    trip = trip,
                    viewModel = viewModel,
                    onTripSelected = onTripSelected,
                    isPriority = false
                )
            }
            Spacer(modifier = Modifier.height(8.dp)) //spazio prima della prossima sezione
        }

        if (otherTrips.isNotEmpty()) { //se ci sono viaggi fuori dal range principale
            var expanded by remember { mutableStateOf(false) } //stato locale per espandere/comprimere la sezione

            Surface(
                onClick = { expanded = !expanded }, //clic per mostrare/nascondere la lista
                color = Color.Transparent, //nessuno sfondo per non sovrapporsi visivamente
                modifier = Modifier.fillMaxWidth() //larghezza completa
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp), //padding verticale per separare visivamente la sezione
                    horizontalArrangement = Arrangement.SpaceBetween, //testo e icona alle estremità opposte
                    verticalAlignment = Alignment.CenterVertically //centra verticalmente i contenuti
                ) {
                    Text(
                        "⚪ Altri viaggi (${otherTrips.size})", //etichetta con numero totale
                        style = MaterialTheme.typography.labelLarge, //stile piccolo ma leggibile
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon( //icona che cambia in base allo stato espanso
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, //freccia su/giù
                        contentDescription = if (expanded) "Comprimi" else "Espandi", //testo accessibilità
                        tint = Color.Gray
                    )
                }
            }

            AnimatedVisibility(visible = expanded) { //mostra la lista solo quando expanded è true
                Column {
                    Spacer(modifier = Modifier.height(4.dp)) //spazio sopra l’elenco
                    otherTrips.forEach { trip -> //mostra tutti i viaggi “altri”
                        TripCard(
                            trip = trip,
                            viewModel = viewModel,
                            onTripSelected = onTripSelected,
                            isPriority = false, //non ha priorità
                            dimmed = true //sfondo più chiaro per distinguerli
                        )
                    }
                }
            }
        }

        if (trips.isEmpty()) { //gestisce il caso in cui non esista alcun viaggio
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp), //margine sopra e sotto per centrare il messaggio
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0) //sfondo giallo chiaro
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp), //padding interno del messaggio
                    horizontalAlignment = Alignment.CenterHorizontally //centra gli elementi
                ) {
                    Icon(
                        Icons.Default.EventNote, //icona calendario come promemoria
                        contentDescription = null,
                        tint = Color(0xFFFF9800), //colore arancione
                        modifier = Modifier.size(48.dp) //icona grande e visibile
                    )
                    Spacer(modifier = Modifier.height(8.dp)) //spazio tra icona e testo
                    Text(
                        "Nessun viaggio pianificato", //messaggio principale informativo
                        style = MaterialTheme.typography.titleMedium, //titolo medio
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Crea un viaggio rapido per iniziare!", //suggerimento per incoraggiare l’azione
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray //colore secondario
                    )
                }
            }
        }
    }
}

/**
 * intestazione di gruppo dei viaggi.
 * per distinguere visivamente le varie sezioni temporali
 * come “viaggi in corso”, “viaggi prossimi o recenti”, ecc.
 * l’intestazione mostra un’etichetta colorata e il numero totale di viaggi nel gruppo.
 * utilizza una Row per allineare testo e contatore con un piccolo badge colorato.
 */
@Composable
fun TripGroupHeader(text: String, color: Color, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth() //la riga occupa tutta la larghezza disponibile
            .padding(vertical = 4.dp), //spazio verticale per separarla visivamente dagli altri elementi
        verticalAlignment = Alignment.CenterVertically //centra verticalmente testo e badge
    ) {
        Text(
            text, //mostra il titolo del gruppo (es. “🟢 Viaggi in corso”)
            style = MaterialTheme.typography.labelLarge,
            color = color, //colore coerente con il tipo di gruppo (verde, arancione, ecc.)
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(8.dp)) //spazio orizzontale tra testo e badge contatore
        Surface(
            shape = MaterialTheme.shapes.small, //forma arrotondata piccola per il badge
            color = color.copy(alpha = 0.2f) //sfondo semitrasparente
        ) {
            Text(
                "$count", //mostra il numero totale di viaggi nel gruppo
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), //padding interno per dimensionare il badge
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


/**
rappresenta la card di un singolo viaggio all’interno del selettore.
 *per mostrare ogni viaggio con il suo stato temporale e informazioni principali.
 * include:
 * - la destinazione del viaggio
 * - le date pianificate
 * - un badge che descrive lo stato (es. "in corso oggi", "tra 2 giorni", "ieri", ecc.)
 * - il tipo di viaggio (local, day, multi-day)
 * la card cambia aspetto in base ai parametri:
 * - isPriority: evidenzia i viaggi attivi o urgenti con sfondo più visibile e ombra più marcata
 * - dimmed: rende la card più chiara e neutra per viaggi meno rilevanti
 */
@Composable
fun TripCard(
    trip: Trip, //oggetto che contiene tutti i dati del viaggio
    viewModel: TripViewModel, //viewmodel usato per calcolare stato temporale e informazioni derivate
    onTripSelected: (Trip) -> Unit, //callback eseguita quando la card viene cliccata
    isPriority: Boolean, //indica se il viaggio deve essere messo in evidenza (es. attivo oggi)
    dimmed: Boolean = false //opzionale: se true, la card appare visivamente attenuata (oscurato)
) {
    val status = viewModel.getTripTemporalStatus(trip) //recupera lo stato temporale corrente del viaggio (active, upcoming, recent, ecc.)
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN) //formatter per visualizzare le date in formato giorno/mese/anno

    //calcola testo e colore del badge in base allo stato temporale
    val (statusText, statusColor) = when (status) {
        TripTemporalStatus.ACTIVE -> { //se il viaggio è in corso oggi
            "In corso oggi" to Color(0xFF4CAF50) //testo e colore verde per indicare attività
        }
        TripTemporalStatus.UPCOMING -> { //se il viaggio deve iniziare a breve
            val days = viewModel.getDaysUntilTripStart(trip) //numero di giorni mancanti all’inizio
            "Inizia ${if (days == 0) "oggi" else if (days == 1) "domani" else "tra $days giorni"}" to Color(0xFFFFA726) //testo dinamico e colore arancione
        }
        TripTemporalStatus.RECENT -> { //se il viaggio è terminato di recente
            val days = viewModel.getDaysSinceTripEnd(trip) //giorni trascorsi dalla fine
            "Finito ${if (days == 0) "oggi" else if (days == 1) "ieri" else "$days giorni fa"}" to Color(0xFFFFA726) //testo dinamico e colore arancione
        }
        TripTemporalStatus.FUTURE -> { //se il viaggio è pianificato ma lontano nel tempo
            val days = viewModel.getDaysUntilTripStart(trip) //calcola giorni fino all’inizio
            "Tra $days giorni" to Color.Gray //colore neutro per viaggi futuri
        }
        TripTemporalStatus.PAST -> { //se il viaggio è già terminato da tempo
            "Passato" to Color.Gray //testo fisso e colore grigio per stato concluso
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth() //card a larghezza completa per allinearsi alla colonna principale
            .padding(vertical = 4.dp), //spazio verticale tra le card
        colors = CardDefaults.cardColors(
            containerColor = when { //sceglie il colore di sfondo in base ai parametri
                isPriority -> Color(0xFFE8F5E9) //verde chiaro per viaggi prioritari
                dimmed -> Color(0xFFF5F5F5) //grigio chiaro per viaggi secondari o lontani
                else -> Color.White //bianco standard per gli altri
            }
        ),
        elevation = CardDefaults.cardElevation( //gestisce l’ombra in base alla priorità
            defaultElevation = if (isPriority) 4.dp else 1.dp //più evidente per i viaggi prioritari
        ),
        onClick = { onTripSelected(trip) } //quando l’utente clicca sulla card, viene selezionato il viaggio
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth() //riga che contiene tutte le informazioni testuali e l’icona di selezione
                .padding(12.dp), //padding interno uniforme
            verticalAlignment = Alignment.CenterVertically //centra verticalmente testo e icona
        ) {
            Column(modifier = Modifier.weight(1f)) { //colonna per le informazioni principali del viaggio
                Text(
                    trip.destination, //nome della destinazione
                    style = MaterialTheme.typography.titleMedium, //stile medio per testo principale
                    fontWeight = if (isPriority) FontWeight.Bold else FontWeight.SemiBold, //più pesante se il viaggio è prioritario
                    fontSize = if (isPriority) 16.sp else 15.sp //leggera variazione di grandezza per enfasi visiva
                )

                Spacer(modifier = Modifier.height(2.dp)) //spazio tra destinazione e date

                Text(
                    "${dateFormatter.format(trip.startDate)} - ${dateFormatter.format(trip.endDate)}", //mostra le date del viaggio formattate
                    style = MaterialTheme.typography.bodySmall, //stile minore per dati temporali
                    color = Color.Gray, //colore neutro
                    fontSize = 12.sp //testo compatto per informazioni secondarie
                )

                Spacer(modifier = Modifier.height(4.dp)) //spazio tra date e badge di stato

                Row(verticalAlignment = Alignment.CenterVertically) { //riga contenente badge di stato e tipo viaggio
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall, //forma piccola e arrotondata per il badge
                        color = statusColor.copy(alpha = 0.15f) //sfondo leggero derivato dal colore di stato
                    ) {
                        Text(
                            statusText, //testo che descrive lo stato temporale
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), //padding
                            style = MaterialTheme.typography.labelSmall, //stile piccolo per badge
                            color = statusColor, //testo colorato in base allo stato
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp //dimensione piccola ma leggibile
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp)) //spazio orizzontale tra badge e testo tipo viaggio

                    Text(
                        trip.tripType, //mostra la categoria del viaggio (es. Local trip, Multi-day trip)
                        style = MaterialTheme.typography.labelSmall, //stile coerente con badge
                        color = Color.Gray,
                        fontSize = 11.sp //testo piccolo
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight, //icona a freccia per indicare che la card è selezionabile
                contentDescription = "Seleziona",
                tint = if (isPriority) Color(0xFF4CAF50) else Color.Gray //colore coerente con lo stato del viaggio
            )
        }
    }
}
