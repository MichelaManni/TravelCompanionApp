package com.example.travelcompanionapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.travelcompanionapp.viewmodel.TripViewModel
import com.example.travelcompanionapp.viewmodel.TripDetailsUiState
import java.text.SimpleDateFormat
import java.util.*

// Tipi di viaggio obbligatori
val TRIP_TYPES = listOf("Local trip", "Day trip", "Multi-day trip")

/**
 * Schermata per l'inserimento di un nuovo viaggio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEntryScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMapSelection: () -> Unit
) {
    val uiState by viewModel.tripDetailsUiState.collectAsState()

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Inserisci nuovo viaggio",
                        color = Color.Black
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
        TripEntryForm(
            uiState = uiState,
            onDestinationChange = viewModel::updateDestination,
            onStartDateChange = viewModel::updateStartDate,
            onEndDateChange = viewModel::updateEndDate,
            onTripTypeChange = viewModel::updateTripType,
            onSaveTrip = {
                viewModel.saveTrip()
                onNavigateBack()
            },
            onOpenMapSelection = onNavigateToMapSelection,
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        )
    }
}

/**
 * Form per l'inserimento dei dati del viaggio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEntryForm(
    uiState: TripDetailsUiState,
    onDestinationChange: (String) -> Unit,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onTripTypeChange: (String) -> Unit,
    onSaveTrip: () -> Unit,
    onOpenMapSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Stati per i DatePicker
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Campo Destinazione (con selezione mappa)
        OutlinedTextField(
            value = uiState.destination,
            onValueChange = onDestinationChange,
            label = { Text("Destinazione", color = Color.Black) },
            placeholder = { Text("Seleziona dalla mappa o digita", color = Color.Gray) },
            trailingIcon = {
                IconButton(onClick = onOpenMapSelection) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = "Seleziona Mappa",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray
            )
        )

        // Campo Data Inizio (con DatePicker)
        OutlinedTextField(
            value = uiState.startDate,
            onValueChange = { },
            label = { Text("Data Inizio", color = Color.Black) },
            placeholder = { Text("Seleziona data", color = Color.Gray) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showStartDatePicker = true }) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = "Seleziona Data Inizio",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showStartDatePicker = true },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Black,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray
            )
        )

        // Campo Data Fine (con DatePicker)
        OutlinedTextField(
            value = uiState.endDate,
            onValueChange = { },
            label = { Text("Data Fine", color = Color.Black) },
            placeholder = { Text("Seleziona data", color = Color.Gray) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showEndDatePicker = true }) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = "Seleziona Data Fine",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEndDatePicker = true },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Black,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray
            )
        )

        // Dropdown Tipo di Viaggio
        TripTypeDropdown(
            selectedType = uiState.tripType,
            onTypeSelected = onTripTypeChange
        )

        // Mostra le coordinate se selezionate dalla mappa
        if (uiState.destinationLat != null && uiState.destinationLng != null) {
            Text(
                text = "Coordinate: ${String.format("%.4f", uiState.destinationLat)}, ${String.format("%.4f", uiState.destinationLng)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Pulsante Salva
        Button(
            onClick = onSaveTrip,
            enabled = uiState.isEntryValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Salva Viaggio")
        }
    }

    // Dialog per Data Inizio
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
                            onStartDateChange(formatter.format(date))
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    // Dialog per Data Fine
    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endDatePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
                            onEndDateChange(formatter.format(date))
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }
}

/**
 * Dropdown per la selezione del tipo di viaggio.
 */
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
        OutlinedTextField(
            modifier = Modifier
                .exposedDropdownSize(true)
                .fillMaxWidth()
                .menuAnchor(),
            readOnly = true,
            value = selectedType,
            onValueChange = {},
            label = { Text("Tipo di Viaggio", color = Color.Black) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray
            )
        )

        ExposedDropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            TRIP_TYPES.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption, color = Color.Black) },
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