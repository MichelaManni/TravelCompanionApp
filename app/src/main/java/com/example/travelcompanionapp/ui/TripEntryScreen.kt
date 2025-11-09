package com.example.travelcompanionapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.travelcompanionapp.viewmodel.TripViewModel
import com.example.travelcompanionapp.viewmodel.TripDetailsUiState
import com.example.travelcompanionapp.utils.TripTypeLogic
import java.text.SimpleDateFormat
import java.util.*
import com.example.travelcompanionapp.R
import com.airbnb.lottie.compose.*

// Tipi di viaggio obbligatori
val TRIP_TYPES = listOf("Local trip", "Day trip", "Multi-day trip")

/**
 * Schermata per l'inserimento di un nuovo viaggio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEntryScreen(
    viewModel: TripViewModel, //viewmodel che gestisce lo stato e la logica del viaggio
    onNavigateBack: () -> Unit, //callback per tornare indietro alla schermata precedente
    onNavigateToMapSelection: () -> Unit //callback per navigare alla schermata di selezione mappa
) {
    //osserva lo stato corrente del form del viaggio dal viewmodel tramite flow convertito in stato composable
    val uiState by viewModel.tripDetailsUiState.collectAsState()

    //carica l'animazione lottie del titolo dal file raw specificato
    val headerComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.nuovo_viaggio_menu)
    )
    //anima continuamente l'animazione lottie del titolo
    val headerProgress by animateLottieCompositionAsState(
        composition = headerComposition,
        iterations = LottieConstants.IterateForever
    )

    Scaffold(
        containerColor = Color.White, //colore di sfondo principale della schermata
        topBar = {
            //barra superiore centrata con titolo e icona di navigazione
            CenterAlignedTopAppBar(
                title = {
                    //riga con animazione e testo del titolo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        //animazione lottie che funge da icona animata del titolo
                        LottieAnimation(
                            composition = headerComposition,
                            progress = headerProgress,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp)) //spazio tra animazione e testo
                        Text(
                            "Nuovo Viaggio", //titolo della schermata
                            color = TravelGreen, //colore personalizzato per il titolo
                            fontWeight = FontWeight.Bold, //testo in grassetto
                            fontSize = 22.sp //dimensione testo
                        )
                    }
                },
                navigationIcon = {
                    //icona di navigazione per tornare alla schermata precedente
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, //icona freccia indietro automatica
                            contentDescription = "Torna Indietro", //descrizione accessibilità
                            tint = Color.Black //colore icona
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White //sfondo bianco per la top bar
                )
            )
        }
    ) { paddingValues ->
        //contenuto principale della schermata che mostra il form di inserimento viaggio
        TripEntryForm(
            uiState = uiState, //stato attuale del form proveniente dal viewmodel
            onStartDateChange = viewModel::updateStartDate, //callback per aggiornare la data di inizio
            onEndDateChange = viewModel::updateEndDate, //callback per aggiornare la data di fine
            onTripTypeChange = viewModel::updateTripType, //callback per aggiornare il tipo di viaggio
            onDescriptionChange = viewModel::updateDescription, //callback per aggiornare la descrizione
            onSave = {
                //quando l’utente preme salva, viene richiamato il metodo per salvare il viaggio
                viewModel.saveTrip()
                //dopo il salvataggio, si torna alla schermata precedente
                onNavigateBack()
            },
            onNavigateToMapSelection = onNavigateToMapSelection, //callback per selezionare la destinazione dalla mappa
            modifier = Modifier.padding(paddingValues) //gestisce il padding generato dallo scaffold
        )
    }
}
/**
 * Form per l'inserimento dei dati del viaggio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEntryForm(
    uiState: TripDetailsUiState, //stato ui immutabile che contiene i campi del form e la validità
    onStartDateChange: (String) -> Unit, //callback per aggiornare la data di inizio nel viewmodel
    onEndDateChange: (String) -> Unit, //callback per aggiornare la data di fine nel viewmodel
    onTripTypeChange: (String) -> Unit, //callback per aggiornare il tipo di viaggio nel viewmodel
    onDescriptionChange: (String) -> Unit, //callback per aggiornare la descrizione nel viewmodel
    onSave: () -> Unit, //callback eseguita alla pressione del pulsante di salvataggio
    onNavigateToMapSelection: () -> Unit, //callback per aprire la schermata di selezione sulla mappa
    modifier: Modifier = Modifier //modifier esterno per applicare padding dal parent scaffold
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN) //formatter per visualizzare e salvare le date in formato locale

    var showStartDatePicker by remember { mutableStateOf(false) } //stato locale per mostrare/nascondere il date picker di inizio
    var showEndDatePicker by remember { mutableStateOf(false) } //stato locale per mostrare/nascondere il date picker di fine

    val scrollState = rememberScrollState() //stato di scroll per permettere lo scorrimento verticale del form lungo

    //memorizzazione del suggerimento tipo viaggio in base alle date; ricalcola solo quando cambiano start o end date
    val suggestedTripType = remember(uiState.startDate, uiState.endDate) {
        if (uiState.startDate.isNotBlank() && uiState.endDate.isNotBlank()) { //controlla che entrambe le date siano presenti
            try {
                val startDate = dateFormatter.parse(uiState.startDate) //parsing stringa in data per la data di inizio
                val endDate = dateFormatter.parse(uiState.endDate) //parsing stringa in data per la data di fine
                if (startDate != null && endDate != null) { //verifica parsing avvenuto correttamente
                    TripTypeLogic.suggestTripType(startDate, endDate) //usa logica centralizzata per stimare il tipo viaggio
                } else null //se parsing fallisce ritorna null per non mostrare suggerimenti
            } catch (e: Exception) {
                null //in caso di eccezione sul parsing evita di interrompere il flusso e disabilita il suggerimento
            }
        } else null //se mancano le date non mostra alcun suggerimento
    }

    //calcolo del warning quando il tipo selezionato non è coerente con la durata; dipende da start, end e tipo scelto
    val tripTypeWarning = remember(uiState.startDate, uiState.endDate, uiState.tripType) {
        if (uiState.startDate.isNotBlank() &&
            uiState.endDate.isNotBlank() &&
            uiState.tripType.isNotBlank()) { //richiede tutti i dati necessari
            try {
                val startDate = dateFormatter.parse(uiState.startDate)!! //parsing della data di inizio con not-null perché già validata
                val endDate = dateFormatter.parse(uiState.endDate)!! //parsing della data di fine con not-null
                val durationDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
                    endDate.time - startDate.time //differenza in millisecondi tra fine e inizio
                ).toInt() + 1 //converte in giorni e aggiunge 1 per includere il giorno di partenza

                when (uiState.tripType) { //verifica regole di coerenza per ciascun tipo
                    "Local trip" -> {
                        if (durationDays > 1) "Un viaggio locale dovrebbe durare max 1 giorno"
                        else null //nessun warning se la durata è coerente
                    }
                    "Day trip" -> {
                        if (durationDays != 1) "Una gita giornaliera dovrebbe durare esattamente 1 giorno"
                        else null
                    }
                    "Multi-day trip" -> {
                        if (durationDays < 2) "Un viaggio multi-giorno dovrebbe durare almeno 2 giorni"
                        else null
                    }
                    else -> null //nessuna regola applicata per valori inattesi
                }
            } catch (e: Exception) {
                null //in caso di date non parsabili
            }
        } else null //se mancano dati sufficienti non calcola warning
    }

    //colonna principale scrollabile che contiene tutte le sezioni del form
    Column(
        modifier = modifier
            .fillMaxSize() //occupa l’intera area disponibile
            .verticalScroll(scrollState) //abilita lo scroll verticale per evitare contenuti nascosti
            .padding(16.dp), //padding interno uniforme per respiro visivo
        verticalArrangement = Arrangement.spacedBy(16.dp) //spaziatura verticale costante tra i blocchi
    ) {

        //sezione destinazione: branch per stato vuoto (invita a scegliere) o selezionata (mostra riepilogo)
        if (uiState.destination.isBlank()) { //quando la destinazione non è ancora stata selezionata
            Card(
                modifier = Modifier.fillMaxWidth(), //la card si estende su tutta la larghezza
                colors = CardDefaults.cardColors(
                    containerColor = TravelGreen.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp) //angoli arrotondati
            ) {
                Column(
                    modifier = Modifier.padding(16.dp), //padding interno della card
                    horizontalAlignment = Alignment.CenterHorizontally //centra icona e testi
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn, //icona di posizione per contestualizzare l’azione
                        contentDescription = null,
                        tint = TravelGreen,
                        modifier = Modifier.size(48.dp) //dimensione ampia per enfasi
                    )
                    Spacer(modifier = Modifier.height(12.dp)) //spazio tra icona e titolo
                    Text(
                        "Seleziona Destinazione",
                        fontWeight = FontWeight.Bold,
                        color = TravelGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp)) //spazio tra titolo e descrizione
                    Text(
                        "Inizia scegliendo dove vuoi andare dalla mappa",
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp)) //spazio prima del bottone
                    Button(
                        onClick = onNavigateToMapSelection, //apre la schermata/flow per scegliere la destinazione su mappa
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TravelGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn, //ripete l’icona per coerenza visiva con l’azione
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp)) //spazio tra icona e testo del pulsante
                        Text("Apri Mappa") //etichetta chiara dell’azione principale
                    }
                }
            }
        } else { //destinazione presente: mostra riepilogo e possibilità di cambiarla
            Card(
                modifier = Modifier.fillMaxWidth(), //usa tutta la larghezza disponibile
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(2.dp), //leggera ombra per distinguere la card
                shape = RoundedCornerShape(12.dp) //angoli arrotondati
            ) {
                Row(
                    modifier = Modifier.padding(16.dp), //padding interno uniforme
                    verticalAlignment = Alignment.CenterVertically //centra icona e testi verticalmente
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn, //icona coerente con la sezione destinazione
                        contentDescription = null, //decorativa
                        tint = TravelGreen,
                        modifier = Modifier.size(32.dp) //dimensione media
                    )
                    Spacer(modifier = Modifier.width(12.dp)) //spazio tra icona e contenuto testuale
                    Column(modifier = Modifier.weight(1f)) { //colonna che occupa lo spazio rimanente
                        Text(
                            "Destinazione",
                            color = Color.Black
                        )
                        Text(
                            uiState.destination, //mostra dest scelto dall’utente
                            fontWeight = FontWeight.Bold, //evidenzia il valore selezionato
                            color = Color.Black
                        )
                    }
                    TextButton(onClick = onNavigateToMapSelection) { //azione per cambiare la destinazione
                        Text("Cambia", color = TravelGreen) //testo chiaro per rientrare nel flow di selezione
                    }
                }
            }
        }

        //sezione date con due card cliccabili affiancate
        Row(
            modifier = Modifier.fillMaxWidth(), //riga a larghezza piena per distribuire due card
            horizontalArrangement = Arrangement.spacedBy(8.dp) //spazio orizzontale tra le due card
        ) {
            //card data inizio: apre il date picker all'onClick
            Card(
                modifier = Modifier.weight(1f), //occupa metà della riga
                onClick = { showStartDatePicker = true }, //mostra il selettore della data di inizio
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(2.dp), //leggera ombra
                shape = RoundedCornerShape(12.dp) //angoli arrotondati coerenti
            ) {
                Column(modifier = Modifier.padding(12.dp)) { //contenuto interno della card
                    Icon(
                        imageVector = Icons.Filled.DateRange, //icona calendario per indicare la funzione
                        contentDescription = null,
                        tint = TravelGreen,
                        modifier = Modifier.size(24.dp) //dimensione compatta
                    )
                    Spacer(modifier = Modifier.height(8.dp)) //spazio tra icona e label
                    Text(
                        "Data Inizio",
                        color = Color.Black
                    )
                    Text(
                        if (uiState.startDate.isBlank()) "Seleziona" else uiState.startDate, //placeholder o valore selezionato
                        fontWeight = FontWeight.Medium, //peso medio per differenziarlo dalla label
                        color = if (uiState.startDate.isBlank()) Color.Gray else Color.Black //colore condizionale per stato vuoto/pieno
                    )
                }
            }

            //card data fine: simmetrica alla card di inizio
            Card(
                modifier = Modifier.weight(1f), //occupa metà della riga
                onClick = { showEndDatePicker = true }, //mostra il selettore della data di fine
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) { //contenuto interno della card
                    Icon(
                        imageVector = Icons.Filled.DateRange, //stessa icona per coerenza visiva
                        contentDescription = null,
                        tint = TravelGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Data Fine",
                        color = Color.Black
                    )
                    Text(
                        if (uiState.endDate.isBlank()) "Seleziona" else uiState.endDate, //placeholder o valore selezionato
                        fontWeight = FontWeight.Medium,
                        //fontSize non commentato per vincolo
                        color = if (uiState.endDate.isBlank()) Color.Gray else Color.Black //colore condizionale per stato vuoto/pieno
                    )
                }
            }
        }

        //card suggerimento tipo viaggio: appare solo quando esiste un suggerimento e non è stato ancora scelto un tipo
        if (suggestedTripType != null && uiState.tripType.isBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(), //card a tutta larghezza
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                ),
                shape = RoundedCornerShape(8.dp) //angoli leggermente arrotondati
            ) {
                Row(
                    modifier = Modifier.padding(12.dp), //padding interno
                    verticalAlignment = Alignment.CenterVertically //allineamento per icona e testo
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info, //icona informativa per suggerimento
                        contentDescription = null, //decorativa
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(20.dp) //dimensione piccola per non dominare la card
                    )
                    Spacer(modifier = Modifier.width(8.dp)) //spazio tra icona e testi
                    Column(modifier = Modifier.weight(1f)) { //testi che occupano lo spazio rimanente
                        Text(
                            "Suggerimento",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            "In base alle date, ti suggeriamo: ${TripTypeLogic.getTripTypeEmoji(suggestedTripType)} $suggestedTripType", //testo dinamico con emoji e tipo
                            color = Color(0xFF424242)
                        )
                    }
                }
            }
        }

        //card warning coerenza tipo viaggio: appare solo quando tripTypeWarning contiene un messaggio
        if (tripTypeWarning != null) {
            Card(
                modifier = Modifier.fillMaxWidth(), //card a tutta larghezza
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                shape = RoundedCornerShape(8.dp) //stesso stile del suggerimento per coerenza
            ) {
                Row(
                    modifier = Modifier.padding(12.dp), //padding interno
                    verticalAlignment = Alignment.Top //allinea l’emoji in alto per testo multilinea
                ) {
                    Text(
                        "⚠️", //emoji di avviso per attirare l’attenzione
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp)) //spazio tra emoji e testo del warning
                    Text(
                        tripTypeWarning, //messaggio di incoerenza calcolato in precedenza
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        //titolo sezione tipo di viaggio
        Text(
            "Tipo di Viaggio",
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        //lista dei tipi ammessi con card selezionabili; evidenzia quello selezionato
        TRIP_TYPES.forEach { tripType ->
            val isSelected = uiState.tripType == tripType //vero se l’utente ha selezionato questo tipo
            val emoji = TripTypeLogic.getTripTypeEmoji(tripType) //emoji contestuale per tipo
            val description = TripTypeLogic.getTripTypeDescription(tripType) //descrizione breve del tipo

            Card(
                modifier = Modifier.fillMaxWidth(), //card a tutta larghezza per area di tap ampia
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        TravelGreen.copy(alpha = 0.2f)
                    } else {
                        Color.White //sfondo neutro quando non selezionata
                    }
                ),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, TravelGreen) //bordo
                } else {
                    null //nessun bordo quando non selezionata
                },
                shape = RoundedCornerShape(12.dp), //angoli arrotondati
                onClick = { onTripTypeChange(tripType) } //al tap aggiorna il tipo nel viewmodel
            ) {
                Row(
                    modifier = Modifier.padding(16.dp), //padding interno per respiro
                    verticalAlignment = Alignment.CenterVertically //centra emoji e testi
                ) {
                    Text(
                        emoji, //mostra l’emoji del tipo per scan visivo rapido
                    )
                    Spacer(modifier = Modifier.width(12.dp)) //separa emoji dal testo
                    Column(modifier = Modifier.weight(1f)) { //colonna testuale che riempie lo spazio
                        Text(
                            tripType, //nome del tipo
                            fontWeight = FontWeight.Bold, //titolo della card
                            color = if (isSelected) TravelGreen else Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp)) //spazio tra titolo e descrizione
                        Text(
                            description, //descrizione breve
                            color = Color.Black
                        )
                    }
                    if (isSelected) { //solo se selezionato mostra un’icona di conferma/tema
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = TravelGreen,
                            modifier = Modifier.size(24.dp) //dimensione contenuta
                        )
                    }
                }
            }
        }

        //campo descrizione opzionale del viaggio
        OutlinedTextField(
            value = uiState.description, //valore corrente della descrizione
            onValueChange = onDescriptionChange, //propaga ogni modifica al viewmodel
            label = { Text("Descrizione (opzionale)") }, //label informativa sul campo
            placeholder = { Text("Es: Vacanza estiva con famiglia") }, //placeholder di esempio d’uso
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Notes, //icona note per indicare testo libero
                    contentDescription = null,
                    tint = TravelGreen
                )
            },
            modifier = Modifier.fillMaxWidth(), //campo a tutta larghezza per comodità di input
            maxLines = 4, //limita l’altezza del campo mantenendo leggibilità
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TravelGreen,
                focusedLabelColor = TravelGreen,
                cursorColor = TravelGreen
            )
        )

        //pulsante di salvataggio: abilitato solo se il form è valido
        Button(
            onClick = onSave, //esegue la logica di salvataggio fornita dal chiamante
            enabled = uiState.isEntryValid, //vincola l’interazione alla validità del form
            modifier = Modifier.fillMaxWidth(), //pulsante a tutta larghezza per priorità visiva
            colors = ButtonDefaults.buttonColors(
                containerColor = TravelGreen,
                disabledContainerColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp) //angoli arrotondati coerenti con lo stile
        ) {
            Text(
                if (uiState.isEntryValid) "Crea Viaggio" else "Compila tutti i campi obbligatori", //messaggio dinamico in base alla validità
                fontWeight = FontWeight.Bold, //enfasi sul testo del bottone
                modifier = Modifier.padding(vertical = 8.dp) //aumenta l’area cliccabile e l’altezza del bottone
            )
        }

        Spacer(modifier = Modifier.height(16.dp)) //spazio di fondo per evitare che il bottone aderisca al bordo inferiore
    }

    //dialog di selezione data inizio: mostrato quando showStartDatePicker è true
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState() //state del date picker per tracciare la selezione
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false }, //chiude il dialog senza modifiche
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis -> //se è stata selezionata una data
                            val date = Date(millis) //converte i millisecondi in oggetto Date
                            onStartDateChange(dateFormatter.format(date)) //propaga la data formattata al viewmodel
                        }
                        showStartDatePicker = false //chiude il dialog dopo la conferma
                    }
                ) {
                    Text("OK", color = TravelGreen, fontWeight = FontWeight.Bold) //testo pulsante conferma
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { //chiusura senza azione
                    Text("Annulla", color = Color.DarkGray) //testo pulsante annulla
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White
            )
        ) {
            DatePicker(
                state = datePickerState, //collega il componente al suo state
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    headlineContentColor = Color.Black,
                    weekdayContentColor = Color.Black,
                    subheadContentColor = Color.Black,
                    yearContentColor = Color.Black,
                    currentYearContentColor = TravelGreen,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = TravelGreen,
                    dayContentColor = Color.Black,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = TravelGreen,
                    todayContentColor = TravelGreen,
                    todayDateBorderColor = TravelGreen
                )
            )
        }
    }

    //dialog di selezione data fine: simmetrico a quello della data inizio
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState() //state del date picker per la data di fine
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false }, //chiude il dialog senza modifiche
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis -> //applica la data solo se selezionata
                            val date = Date(millis) //crea la Date dai millisecondi
                            onEndDateChange(dateFormatter.format(date)) //propaga la data al viewmodel
                        }
                        showEndDatePicker = false //chiude il dialog dopo la conferma
                    }
                ) {
                    Text("OK", color = TravelGreen, fontWeight = FontWeight.Bold) //testo pulsante conferma
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { //chiusura senza azione
                    Text("Annulla", color = Color.DarkGray) //testo pulsante annulla
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White
            )
        ) {
            DatePicker(
                state = datePickerState, //collega il componente al suo state
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    headlineContentColor = Color.Black,
                    weekdayContentColor = Color.Black,
                    subheadContentColor = Color.Black,
                    yearContentColor = Color.Black,
                    currentYearContentColor = TravelGreen,
                    selectedYearContentColor = Color.White,
                    selectedYearContainerColor = TravelGreen,
                    dayContentColor = Color.Black,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = TravelGreen,
                    todayContentColor = TravelGreen,
                    todayDateBorderColor = TravelGreen
                )
            )
        }
    }
}
