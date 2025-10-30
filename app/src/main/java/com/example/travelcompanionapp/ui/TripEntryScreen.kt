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
 * L'utente deve:
 * 1. Selezionare la destinazione dalla mappa
 * 2. Scegliere le date di inizio e fine
 * 3. Selezionare il tipo di viaggio
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEntryScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMapSelection: () -> Unit
) {
    // Osserviamo lo stato del form dal ViewModel
    val uiState by viewModel.tripDetailsUiState.collectAsState()

    // Lottie Animation per il titolo (icona animata)
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
                        // Icona animata accanto al titolo
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
 * Ora la destinazione si seleziona SOLO dalla mappa!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEntryForm(
    uiState: TripDetailsUiState,
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

        // === SEZIONE DESTINAZIONE ===

        // Se la destinazione NON è ancora stata selezionata
        if (uiState.destination.isBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = TravelGreen.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = TravelGreen,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Nessuna destinazione selezionata",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tocca il pulsante qui sotto per scegliere un luogo dalla mappa",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            // Se la destinazione È stata selezionata, mostriamo le info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icona posizione
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = TravelGreen,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Destinazione",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Nome del luogo selezionato
                        Text(
                            text = uiState.destination,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }

                    // Icona checkmark verde (conferma)
                    Surface(
                        color = TravelGreen,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "✓",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // Pulsante per aprire/cambiare la selezione dalla mappa
        Button(
            onClick = onOpenMapSelection,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.destination.isBlank()) TravelGreen else Color.LightGray
            )
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (uiState.destination.isBlank()) {
                    "Scegli Destinazione dalla Mappa"
                } else {
                    "Cambia Destinazione"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // === SEZIONE DATE ===

        // Campo Data Inizio
        OutlinedTextField(
            value = uiState.startDate,
            onValueChange = { },
            label = { Text("Data Inizio", color = Color.Black) },
            placeholder = { Text("Seleziona data", color = Color.Gray) },
            readOnly = true, // Non editabile direttamente, si apre il DatePicker
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

        // === SEZIONE TIPO VIAGGIO ===

        // Dropdown per scegliere il tipo di viaggio
        TripTypeDropdown(
            selectedType = uiState.tripType,
            onTypeSelected = onTripTypeChange
        )

        Spacer(modifier = Modifier.weight(1f))

        // === PULSANTE SALVA ===

        // Abilitato solo se tutti i campi sono compilati
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

    // === DIALOG PER LA SELEZIONE DELLE DATE ===

    // Dialog Data Inizio
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Quando l'utente conferma la data
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
 * Dropdown menu per la selezione del tipo di viaggio.
 * Mostra le 3 opzioni: Local trip, Day trip, Multi-day trip
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