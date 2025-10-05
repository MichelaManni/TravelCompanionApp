package com.example.travelcompanionapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
// ⭐ NUOVO IMPORT
import androidx.compose.material.icons.filled.LocationOn
// ⭐ FINE NUOVO IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.travelcompanionapp.viewmodel.TripViewModel
import com.example.travelcompanionapp.viewmodel.TripDetailsUiState
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.travelcompanionapp.R
import com.airbnb.lottie.compose.*


// =========================

// Definiamo i tipi di viaggio obbligatori come costanti
val TRIP_TYPES = listOf("Local trip", "Day trip", "Multi-day trip")

/**
 * Schermata per l'inserimento o la modifica di un viaggio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEntryScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit,
    // ⭐ NUOVO PARAMETRO PER LA NAVIGAZIONE ALLA MAPPA
    onNavigateToMapSelection: () -> Unit
) {
    // Collezioniamo lo StateFlow del form dal ViewModel.
    val uiState by viewModel.tripDetailsUiState.collectAsState()

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pianifica un nuovo viaggio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna Indietro"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        TripEntryForm(
            uiState = uiState,
            onDestinationChange = viewModel::updateDestination,
            onStartDateChange = viewModel::updateStartDate,
            onEndDateChange = viewModel::updateEndDate,
            onTripTypeChange = viewModel::updateTripType,
            onTotalDistanceChange = viewModel::updateTotalDistanceStr,
            onSaveTrip = viewModel::saveTrip,
            // ⭐ PASSA IL NUOVO CALLBACK AL FORM
            onOpenMapSelection = onNavigateToMapSelection,
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        )
    }
}


/**
 * Form Composable per l'inserimento dei dati di un viaggio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEntryForm(
    uiState: TripDetailsUiState,
    onDestinationChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onTripTypeChange: (String) -> Unit,
    onTotalDistanceChange: (String) -> Unit,
    onSaveTrip: () -> Unit,
    // ⭐ NUOVO PARAMETRO
    onOpenMapSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp) // Spazio tra i componenti
    ) {
        // Campo di input: Destinazione (ORA CLICCABILE)
        OutlinedTextField(
            value = uiState.destination, // Valore attuale dal modello di stato
            onValueChange = onDestinationChange, // Permette l'inserimento manuale
            label = { Text("Destinazione") }, // Etichetta del campo
            placeholder = { Text("Seleziona dalla mappa o digita") },
            // ⭐ Icona e click per aprire la mappa
            trailingIcon = {
                IconButton(onClick = onOpenMapSelection) {
                    Icon(Icons.Filled.LocationOn, contentDescription = "Seleziona Mappa")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                // ⭐ Rende cliccabile tutto il campo per l'apertura della mappa
                .clickable { onOpenMapSelection() },
            singleLine = true // Una sola riga
        )

        // TODO: Aggiungere qui i campi per la selezione delle date (con DatePickerDialog)

        // Dropdown per il Tipo di Viaggio
        TripTypeDropdown(
            selectedType = uiState.tripType,
            onTypeSelected = onTripTypeChange
        )

        // Campo di input: Distanza (per i Multi-day trip)
        OutlinedTextField(
            // Il TextField deve visualizzare una Stringa. Usiamo il campo `totalDistanceStr`
            value = uiState.totalDistanceStr,

            // Chiama la funzione di aggiornamento nel ViewModel
            onValueChange = onTotalDistanceChange,

            label = { Text("Distanza Totale (Km)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Pulsante per salvare il viaggio
        Button(
            onClick = onSaveTrip, // Azione al click
            enabled = uiState.isEntryValid, // Abilitato solo a form valido
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Salva Viaggio") // Testo del pulsante
        }
    }
}


// Il codice di TripTypeDropdown rimane invariato.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripTypeDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val expanded = remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded.value,
        onExpandedChange = { expanded.value = !expanded.value },
        modifier = modifier.fillMaxWidth()
    ) {
        // TextField che mostra il valore selezionato
        OutlinedTextField(
            modifier = Modifier.exposedDropdownSize(true).fillMaxWidth().menuAnchor(),
            readOnly = true,
            value = selectedType,
            onValueChange = {},
            label = { Text("Tipo di Viaggio") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        // Il menu a tendina vero e proprio
        ExposedDropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            TRIP_TYPES.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onTypeSelected(selectionOption)
                        expanded.value = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}