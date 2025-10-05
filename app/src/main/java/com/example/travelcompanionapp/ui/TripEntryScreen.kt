package com.example.travelcompanionapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
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
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMapSelection: () -> Unit
) {
    val uiState by viewModel.tripDetailsUiState.collectAsState()

    // Lottie Animation per il titolo
    val headerComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.nuovo_viaggio_menu)
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
                        // Lottie Animation accanto al titolo
                        LottieAnimation(
                            composition = headerComposition,
                            progress = headerProgress,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Nuovo Viaggio",
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
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Campo Destinazione (scrittura manuale)
        OutlinedTextField(
            value = uiState.destination,
            onValueChange = onDestinationChange,
            label = { Text("Destinazione", color = Color.Black) },
            placeholder = { Text("Es: Roma, Milano, Firenze...", color = Color.Gray) },
            supportingText = {
                if (uiState.destinationLat != null) {
                    Text("📍 Posizione da mappa", color = TravelGreen)
                } else {
                    Text("Scrivi il nome o usa la mappa", color = Color.Gray)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = TravelGreen,
                unfocusedBorderColor = Color.Gray,
                cursorColor = TravelGreen
            )
        )

        // Pulsante per selezionare dalla mappa
        Button(
            onClick = onOpenMapSelection,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(),
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(TravelGreen))
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = TravelGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Seleziona dalla Mappa",
                color = TravelGreen,
                fontWeight = FontWeight.Medium
            )
        }

        // Mostra coordinate se selezionate dalla mappa
        if (uiState.destinationLat != null && uiState.destinationLng != null) {
            Surface(
                color = TravelGreen.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = TravelGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GPS: ${String.format("%.4f", uiState.destinationLat)}, ${String.format("%.4f", uiState.destinationLng)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TravelGreen
                        )
                    }
                }
            }
        }

        // Campo Data Inizio
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
                        tint = TravelGreen
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Black,
                focusedBorderColor = TravelGreen,
                unfocusedBorderColor = Color.Gray
            )
        )

        // Campo Data Fine
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
                        tint = TravelGreen
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                disabledTextColor = Color.Black,
                focusedBorderColor = TravelGreen,
                unfocusedBorderColor = Color.Gray
            )
        )

        // Dropdown Tipo di Viaggio
        TripTypeDropdown(
            selectedType = uiState.tripType,
            onTypeSelected = onTripTypeChange
        )

        Spacer(modifier = Modifier.weight(1f))

        // Pulsante Salva (stesso stile del menu)
        Button(
            onClick = onSaveTrip,
            enabled = uiState.isEntryValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TravelGreen,
                disabledContainerColor = Color.Gray
            )
        ) {
            Text(
                text = "Salva Viaggio",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Dialog Data Inizio
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
                    Text("OK", color = TravelGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(
                state = startDatePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = TravelGreen,
                    todayContentColor = TravelGreen,
                    todayDateBorderColor = TravelGreen
                )
            )
        }
    }

    // Dialog Data Fine
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
                    Text("OK", color = TravelGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(
                state = endDatePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = TravelGreen,
                    todayContentColor = TravelGreen,
                    todayDateBorderColor = TravelGreen
                )
            )
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
                focusedBorderColor = TravelGreen,
                unfocusedBorderColor = Color.Gray
            )
        )

        ExposedDropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            modifier = Modifier.background(Color.White)
        ) {
            TRIP_TYPES.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption, color = Color.Black) },
                    onClick = {
                        onTypeSelected(selectionOption)
                        expanded.value = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    colors = MenuDefaults.itemColors(
                        textColor = Color.Black
                    )
                )
            }
        }
    }
}