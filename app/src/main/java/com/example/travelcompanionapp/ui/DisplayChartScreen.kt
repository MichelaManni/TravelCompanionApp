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
 * FUNZIONALITÀ IMPLEMENTATE:
 * 1. Grafico a barre: Distanza totale per mese (ultimi 12 mesi)
 * 2. Mappa: Mostra tutte le destinazioni visitate con markers
 * 3. Statistiche generali: Totale km, numero viaggi
 */
@OptIn(ExperimentalMaterial3Api::class) //indica l'uso di API sperimentali di Material 3 (CenterAlignedTopAppBar)
@Composable
fun DisplayChartsScreen(
    viewModel: TripViewModel, //viewModel = ViewModel che fornisce i dati dei viaggi
    // Contiene i viaggi completati come Flow reattivo
    // Il ViewModel gestisce la logica di business e l'accesso ai dati
    onNavigateBack: () -> Unit // Callback chiamata quando l'utente preme il pulsante "indietro"
) {
    // Osserva i viaggi completati dal ViewModel
    val completedTrips by viewModel.completedTrips.collectAsState(initial = emptyList())
    // 1. viewModel.completedTrips = Flow<List<Trip>> flusso reattivo che emette liste di viaggi completati
    // 2. .collectAsState() = converte il Flow in State per Compose. Compose osserva questo State e ricompone quando cambia
    // 3. initial = emptyList() = valore iniziale prima che arrivi il primo dato
    //    All'inizio la lista è vuota, poi viene popolata dal database
    // 4. "by" = delegazione di proprietà
    //    completedTrips è direttamente la List<Trip>, non State<List<Trip>>

    // Lottie Animation per il titolo
    val headerComposition by rememberLottieComposition(
        //animazione per l'icona
        LottieCompositionSpec.RawRes(R.raw.display_charts_menu)
    )
    val headerProgress by animateLottieCompositionAsState(
        //anima animazione lottie della topbar
        composition = headerComposition,
        iterations = LottieConstants.IterateForever
    )

    Scaffold(
        // Scaffold = componente Material che fornisce la struttura base di una schermata
        //include: TopBar, BottomBar, FAB, Drawer, SnackBar
        //gestisce automaticamente il padding per questi elementi
        containerColor = Color.White, //colore di sfondo
        topBar = { //barra superiore schermata
            CenterAlignedTopAppBar( //topbar con titolo centrato
                title = {
                    Row( //per disporre animazione e testo orizzontale
                        verticalAlignment = Alignment.CenterVertically, //centra verticalmente animazione e testo
                        horizontalArrangement = Arrangement.Center //centra orizzontalmente contenuto row
                    ) {
                        LottieAnimation( //icona animata a sinistra del titolo
                            composition = headerComposition,
                            progress = headerProgress,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp)) //spazio tra icona e testo
                        Text(
                            "Display Charts",
                            color = TravelGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                navigationIcon = { //icona di navigazione a sinistra della topbar
                    IconButton(onClick = onNavigateBack) { //componente che visualizza icona vettoriale
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, //<-
                            contentDescription = "Torna Indietro",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors( //schema colori topbar
                    containerColor = Color.White//colore sfondo
                )
            )
        }
    ) { paddingValues -> //contenuto principale dello scaffold,paddingValues = padding da applicare per evitare sovrapposizioni con TopBar

        // Se non ci sono viaggi completati
        if (completedTrips.isEmpty()) {
            EmptyChartsScreen() //mostra schermata vupta con messaggio
        } else {
            // Altrimenti mostra le statistiche e i grafici
            ChartsContent( //componente che contiene tutti i grafici e statistiche
                trips = completedTrips, //passsa lista dei viaggi completati
                modifier = Modifier.padding(paddingValues) //applica paddingvalues per evitare sovrapposizioni con topbar
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
    trips: List<Trip>, //lista dei viaggi completati da visualizzare
    modifier: Modifier = Modifier // modifier = personalizzazioni dall'esterno
) {
    val scrollState = rememberScrollState() //rememberScrollState() = crea uno stato che traccia la posizione dello scroll
    // mantiene posizione anche durante le ricomposizioni

    Column( //dispone componenti verticalmente
        modifier = modifier
            .fillMaxSize() //occupa tutto lo spazio disponibile
            .verticalScroll(scrollState) //rende column scrollabile verticalmente, scrollstate è stato x tracciare posizione
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) //inserisce automaticamente spazio tra ogni elemento
    ) {
        //  1. STATISTICHE GENERALI
        GeneralStatisticsCard(trips = trips) //card con km tot, num viaggi

        // 2. GRAFICO A BARRE: Distanza per mese
        MonthlyDistanceChart(trips = trips)

        //  3. MAPPA: Destinazioni visitate
        DestinationsMapCard(trips = trips) // Google Maps con marker per ogni destinazione visitata

        // 4. BONUS: Lista viaggi con dettagli
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
fun GeneralStatisticsCard(trips: List<Trip>) { //lista dei viaggi completati
    // Calcola statistiche
    val totalDistance = trips.sumOf { it.totalDistanceKm } //trips.sumOf { it.totalDistanceKm } = somma tutti i totalDistanceKm
    val totalTrips = trips.size //numero elementi nella lista


    Card( //contenitore Material con bordi arrotondati ed elevazione
        modifier = Modifier.fillMaxWidth(), //occupa tutta la larghezza disponibile
        colors = CardDefaults.cardColors( //colori card
            containerColor = TravelGreen.copy(alpha = 0.1f) //colore sfondo, molto trasparente
        ),
        shape = RoundedCornerShape(16.dp) //bordi arrotondati
    ) {
        Column( //dispone contenuti verticalmente
            modifier = Modifier.padding(20.dp)
        ) {
            // Titolo
            Row( //riga per icona + testo titola
                verticalAlignment = Alignment.CenterVertically //centra verticalmente
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart, //icona frafico a barre
                    contentDescription = null,
                    tint = TravelGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp)) //spazio tra icona e testo
                Text(
                    text = "📊 Statistiche Generali",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TravelGreen
                )
            }

            Spacer(modifier = Modifier.height(20.dp)) //spazio tra titolo e statistiche

            // Tre statistiche in riga
            Row( //dispone statistiche orizzontalmente
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly // // SpaceEvenly = distribuisce lo spazio uniformemente
            ) {
                DisplayChartStatItem( //Componente riutilizzabile per una statistica
                    icon = "🗺️",
                    value = String.format("%.1f km", totalDistance), // formatta numero con 1 cifra decimale
                    label = "Distanza Totale"
                )

                DisplayChartStatItem(
                    icon = "✈️",
                    value = "$totalTrips", //converte numero in stringa
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
fun DisplayChartStatItem(icon: String, value: String, label: String) { //icon=emoji, valure= valore numerico, label = etichetta descrittiva
    Column( //dispone icona , vbalore e label verticlamente
        horizontalAlignment = Alignment.CenterHorizontally, //centra orizzontalmente tutti gli elementi
        modifier = Modifier.padding(8.dp)
    ) {
        Text(text = icon, fontSize = 32.sp) //emoji
        Spacer(modifier = Modifier.height(8.dp)) //spazio tra icona e valore
        Text(
            text = value, //valore numerico
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TravelGreen
        )
        Text(
            text = label, //label descrittiva
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
    val calendar = Calendar.getInstance() //oggetto calendario

    // Genera lista ultimi 12 mesi
    val last12Months = (0..11).map { monthsAgo ->// Crea un range da 0 a 11 (12 mesi) e per ogni numero esegue la lambda
        val cal = Calendar.getInstance() //crea una nuova istanza del calendario
        cal.add(Calendar.MONTH, -monthsAgo) //sottrae il numero di mesi (0, 1, 2... 11) per tornare indietro nel tempo
        SimpleDateFormat("MMM yy", Locale.ITALIAN).format(cal.time) to cal // Formatta la data come "Gen 25" e crea una coppia (etichetta, calendario)
    }.reversed() //inverte lista per mesi dal piu vecchio al + recente

    // Calcola distanza per ogni mese
    val monthlyData = last12Months.map { (label, cal) -> // Per ogni mese nella lista dei 12 mesi
        val month = cal.get(Calendar.MONTH) //Estrae il numero del mese (0=Gennaio, 11=Dicembre)
        val year = cal.get(Calendar.YEAR)//estrae anno (2025)

        val distance = trips //tutti i viaggi
            .filter { trip -> //filtra solo i viaggi che soddisfano la condizione
                val tripCal = Calendar.getInstance() //crea un calendario per il viaggio
                tripCal.time = trip.endDate //imposta data di fine del viaggio
                tripCal.get(Calendar.MONTH) == month && //verifica che il mese del viaggio corrisponda
                        tripCal.get(Calendar.YEAR) == year //verifica che l'anno del viaggio corrisponda
            }
            .sumOf { it.totalDistanceKm } // Somma tutte le distanze dei viaggi filtrati

        label to distance // Crea una coppia (etichetta mese, distanza totale)
    }

    val maxDistance = monthlyData.maxOfOrNull { it.second } ?: 1.0 // Trova la distanza massima tra tutti i mesi, se vuota usa 1.0

    Card( //contenitore card
        modifier = Modifier.fillMaxWidth(), //occupa tutta la larghezza disponibile
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp), //ombra
        shape = RoundedCornerShape(16.dp) //angoli arrotondati
    ) {
        Column( //dispone elementi verticalmente
            modifier = Modifier.padding(20.dp) //spazio interno
        ) {
            // Titolo
            Row( //dispone elementi orizzontalmente
                verticalAlignment = Alignment.CenterVertically //allinea verticalmente al centro
            ) {
                Icon(
                    imageVector = Icons.Filled.ShowChart, //icona di un grafico
                    contentDescription = null,
                    tint = TravelGreen,
                    modifier = Modifier.size(28.dp) //dimensione icona
                )
                Spacer(modifier = Modifier.width(12.dp)) //spazio verticale
                Text(
                    text = "📈 Distanza per Mese",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TravelGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp)) //spazio verticale

            Text( //sottotitolo
                text = "Ultimi 12 mesi",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            //GRAFICO A BARRE
            monthlyData.forEach { (month, distance) -> // Per ogni coppia (mese, distanza) nella lista
                MonthlyBarItem( //chiama funzione che crea singola barra
                    month = month, //etichetta mese
                    distance = distance, //distanza totale
                    maxDistance = maxDistance //distanza massima
                )
                Spacer(modifier = Modifier.height(12.dp)) // Spazio tra una barra e l'altra
            }
        }
    }
}

/**
 * Singola barra del grafico mensile.
 */
@Composable
fun MonthlyBarItem(month: String, distance: Double, maxDistance: Double) { //etichetta mmese, distanza e distanza max
    Column { //elementi in verticale
        Row( //elementi in orizzontale
            modifier = Modifier.fillMaxWidth(), //tutta la larghezza
            horizontalArrangement = Arrangement.SpaceBetween, //spazio tra gli elementi
            verticalAlignment = Alignment.CenterVertically  // Allinea verticalmente al centro
        ) {
            Text( //etichetta mese
                text = month,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.width(60.dp) //larghezza fissa
            )

            // Barra colorata
            Box( //contenitore per barra
                modifier = Modifier
                    .weight(1f) //occupa tutto spazio rimanente
                    .height(24.dp) //alytezza barra
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp)) //Sfondo grigio chiaro trasparente con angoli arrotondati
            ) {
                if (distance > 0) { //se c'è distanza da mostrare
                    val percentage = (distance / maxDistance).coerceIn(0.0, 1.0).toFloat() //calcola la percentuale rispetto al massimo, limitata tra 0 e 1
                    Box( // Barra verde che rappresenta la distanza
                        modifier = Modifier
                            .fillMaxHeight() // Occupa tutta l'altezza del contenitore
                            .fillMaxWidth(percentage) // Occupa una larghezza proporzionale alla percentuale
                            .background(TravelGreen, RoundedCornerShape(12.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp)) //spazio tra la barra e il valore

            Text( //valore numerico della distanza
                text = "${String.format("%.0f", distance)} km",  // Formatta il numero senza decimali e aggiunge "km"
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (distance > 0) TravelGreen else Color.Gray,  // Verde se c'è distanza, grigio se zero
                modifier = Modifier.width(70.dp)// Larghezza fissa di 70 dp per allineare tutti i valori
            )
        }
    }
}

/**
 * Card con mappa che mostra tutte le destinazioni visitate.
 * Ogni viaggio con coordinate GPS viene mostrato come marker.
 */
@Composable
fun DestinationsMapCard(trips: List<Trip>) { //riceve la lista di tutti i viaggi
    // Filtra viaggi con coordinate GPS valide
    val tripsWithGPS = trips.filter { //filtra solo i viaggi che hanno coordinate
        it.destinationLat != null && it.destinationLng != null // Verifica che latitudine e longitudine non siano nulle/
    }

    Card( //contenitore
        modifier = Modifier.fillMaxWidth(), // Occupa tutta la larghezza
        colors = CardDefaults.cardColors( // Imposta i colori
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp), //ombra
        shape = RoundedCornerShape(16.dp)
    ) {
        Column( //dispone elementi verticalmente
            modifier = Modifier.padding(20.dp)
        ) {
            // Titolo
            Row( //dispone elementi orizzontali
                verticalAlignment = Alignment.CenterVertically //allinea verticlamente al centro
            ) {
                Icon( //icona della mappa
                    imageVector = Icons.Filled.Map,
                    contentDescription = null, //nessuna descrizione
                    tint = TravelGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp)) //spazio
                Column { //colonna x titolo e sottotitolo
                    Text(
                        text = "🗺️ Mappa Destinazioni", //titolo
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TravelGreen
                    )
                    Text( //sottotitolo con conteggio
                        text = "${tripsWithGPS.size} destinazioni registrate",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) //spazio verticale

            if (tripsWithGPS.isEmpty()) { //se non ci sono viaggi con gps
                // Messaggio se nessun viaggio ha GPS
                Box( //contenitore
                    modifier = Modifier
                        .fillMaxWidth() //occupa tutta la larghezza
                        .height(300.dp), //altezza fissa
                    contentAlignment = Alignment.Center //centra contenuto
                ) {
                    Column( //elementiv erticalmente
                        horizontalAlignment = Alignment.CenterHorizontally //allinea orizzontalmente al centro
                    ) {
                        Text(text = "📍", fontSize = 48.sp) //emoji
                        Spacer(modifier = Modifier.height(8.dp)) //spazio
                        Text( //messaggio
                            text = "Nessuna destinazione con GPS",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else { //se ci sono viaggi con gps
                // Calcola centro della mappa (media coordinate)
                val centerLat = tripsWithGPS.mapNotNull { it.destinationLat }.average() // Calcola la media di tutte le latitudini (ignora i null)
                val centerLng = tripsWithGPS.mapNotNull { it.destinationLng }.average() // Calcola la media di tutte le longitudini (ignora i null)


                val cameraPositionState = rememberCameraPositionState { // Memorizza la posizione della camera della mappa
                    position = CameraPosition.fromLatLngZoom(  // Imposta la posizione iniziale
                        LatLng(centerLat, centerLng), // Coordinate del centro (media di tutte le destinazioni)
                        if (tripsWithGPS.size == 1) 10f else 5f  // Zoom: 10 se c'è una sola destinazione, 5 se ce ne sono multiple
                    )
                }

                // Mappa con markers
                GoogleMap( // Componente della mappa di Google
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    cameraPositionState = cameraPositionState, // Posizione e zoom della camera
                    uiSettings = MapUiSettings(  // Impostazioni dell'interfaccia della mappa
                        zoomControlsEnabled = true,// Mostra i pulsanti +/- per lo zoom
                        zoomGesturesEnabled = true,// Abilita il pinch-to-zoom con le dita
                        scrollGesturesEnabled = true // Abilita lo scorrimento con le dita
                    )
                ) {
                    tripsWithGPS.forEach { trip -> // Per ogni viaggio con coordinate GPS
                        Marker( // Crea un marker (pin) sulla mappa
                            state = rememberMarkerState(// Memorizza lo stato del marker
                                position = LatLng(trip.destinationLat!!, trip.destinationLng!!) // Posizione del marker (!! = sicuro che non è null)
                            ),
                            title = trip.destination,// Titolo mostrato quando si clicca sul marker
                            snippet = "${String.format("%.1f", trip.totalDistanceKm)} km"  // Sottotitolo con la distanza (1 decimale)
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
fun CompletedTripsListCard(trips: List<Trip>) { //lista di tutti i viaggi
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)// Crea un formattatore per le date in formato italiano (giorno/mese/anno)

    Card(  // Contenitore con bordi arrotondati
        modifier = Modifier.fillMaxWidth(), // Occupa tutta la larghezza
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp), // Ombra di 4 dp
        shape = RoundedCornerShape(16.dp)
    ) {
        Column( //dispone gli elementi verticalmente
            modifier = Modifier.padding(20.dp)
        ) {
            // Titolo
            Row( // Dispone gli elementi orizzontalmente
                verticalAlignment = Alignment.CenterVertically // Allinea verticalmente al centro
            ) {
                Icon( //icona di spunta
                    imageVector = Icons.Filled.CheckCircle, // Icona cerchio con segno di spunta
                    contentDescription = null,
                    tint = TravelGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp)) //spazio
                Text(
                    text = "✅ Viaggi Completati", //testo
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TravelGreen
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) //spazio verticale

            // Lista viaggi
            trips.sortedByDescending { it.endDate }.forEach { trip -> // Ordina i viaggi per data decrescente (più recenti prima) e per ognuno
                CompletedTripItem(trip = trip, dateFormatter = dateFormatter)// Chiama la funzione che crea un singolo elemento
                Spacer(modifier = Modifier.height(8.dp)) // Spazio di 8 dp tra un viaggio e l'altro
            }
        }
    }
}

/**
 * Item singolo viaggio completato.
 */
@Composable
fun CompletedTripItem(trip: Trip, dateFormatter: SimpleDateFormat) { // Riceve il viaggio e il formattatore di date
    Surface( //superficie con sfondo colorato
        color = TravelGreen.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row( //dispone gli elementi orizzontalmente
            modifier = Modifier.padding(12.dp), //spazio interno
            verticalAlignment = Alignment.CenterVertically //allinea verticlamente
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

            Column(modifier = Modifier.weight(1f)) {// Colonna che occupa tutto lo spazio rimanente
                Text( //nome destinazione
                    text = trip.destination,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text( //data
                    text = dateFormatter.format(trip.endDate),// Formatta la data di fine
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Distanza
            if (trip.totalDistanceKm > 0) { // Se il viaggio ha una distanza maggiore di zero
                Surface(
                    color = TravelGreen.copy(alpha = 0.2f),// Verde chiaro con 20% di opacità
                    shape = RoundedCornerShape(12.dp) //angoli molto arrotondati
                ) {
                    Text(
                        text = "${String.format("%.1f", trip.totalDistanceKm)} km", // Formatta con 1 decimale e aggiunge "km"
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), // Spazio interno
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
fun EmptyChartsScreen() { //no parametri
    Box( //contenitore che centra il contenuto
        modifier = Modifier
            .fillMaxSize()// Occupa tutto lo spazio disponibile
            .padding(32.dp),
        contentAlignment = Alignment.Center // Centra il contenuto sia orizzontalmente che verticalmente
    ) {
        Column( //dispone elementi verticlamente
            horizontalAlignment = Alignment.CenterHorizontally, // Allinea orizzontalmente al centro
            verticalArrangement = Arrangement.Center // Allinea verticalmente al centro
        ) {
            Text(text = "📊", fontSize = 80.sp) //emoji
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Nessun Dato Disponibile", //testo principale
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text( //sottotitolo
                text = "Completa il tuo primo viaggio per vedere le statistiche!",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
}