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
 *
 * ⭐ AGGIORNAMENTI:
 * - Aggiunto scrolling verticale per evitare pulsanti nascosti
 * - Validazione intelligente del tipo di viaggio
 * - Suggerimenti automatici in base a date e distanza
 * - Warning se tipo viaggio non corrisponde alla durata
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
            onDescriptionChange = viewModel::updateDescription,
            onSave = {
                viewModel.saveTrip()
                onNavigateBack()
            },
            onNavigateToMapSelection = onNavigateToMapSelection,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

/**
 * Form per l'inserimento dei dati del viaggio.
 *
 * ⭐ SCROLLING ABILITATO: verticalScroll() sulla Column principale
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEntryForm(
    uiState: TripDetailsUiState,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
    onTripTypeChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onNavigateToMapSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Stato per scrolling
    val scrollState = rememberScrollState()

    // Calcola suggerimento tipo viaggio
    val suggestedTripType = remember(uiState.startDate, uiState.endDate) {
        if (uiState.startDate.isNotBlank() && uiState.endDate.isNotBlank()) {
            try {
                val startDate = dateFormatter.parse(uiState.startDate)
                val endDate = dateFormatter.parse(uiState.endDate)
                if (startDate != null && endDate != null) {
                    TripTypeLogic.suggestTripType(startDate, endDate)
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // Calcola warning se tipo non corrisponde
    val tripTypeWarning = remember(uiState.startDate, uiState.endDate, uiState.tripType) {
        if (uiState.startDate.isNotBlank() &&
            uiState.endDate.isNotBlank() &&
            uiState.tripType.isNotBlank()) {
            try {
                val startDate = dateFormatter.parse(uiState.startDate)!!
                val endDate = dateFormatter.parse(uiState.endDate)!!
                val durationDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
                    endDate.time - startDate.time
                ).toInt() + 1

                when (uiState.tripType) {
                    "Local trip" -> {
                        if (durationDays > 1) "Un viaggio locale dovrebbe durare max 1 giorno"
                        else null
                    }
                    "Day trip" -> {
                        if (durationDays != 1) "Una gita giornaliera dovrebbe durare esattamente 1 giorno"
                        else null
                    }
                    "Multi-day trip" -> {
                        if (durationDays < 2) "Un viaggio multi-giorno dovrebbe durare almeno 2 giorni"
                        else null
                    }
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // SCROLLING ABILITATO QUI
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // === SEZIONE DESTINAZIONE ===

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
                        "Seleziona Destinazione",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TravelGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Inizia scegliendo dove vuoi andare dalla mappa",
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToMapSelection,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TravelGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apri Mappa")
                    }
                }
            }
        } else {
            // Destinazione selezionata
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = TravelGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Destinazione",
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                        Text(
                            uiState.destination,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    TextButton(onClick = onNavigateToMapSelection) {
                        Text("Cambia", color = TravelGreen)
                    }
                }
            }
        }

        // === SEZIONE DATE ===

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Data Inizio - ✅ SFONDO BIANCO AGGIUNTO
            Card(
                modifier = Modifier.weight(1f),
                onClick = { showStartDatePicker = true },
                colors = CardDefaults.cardColors(
                    containerColor = Color.White // ✅ Sfondo bianco
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = TravelGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Data Inizio",
                        fontSize = 11.sp,
                        color = Color.Black
                    )
                    Text(
                        if (uiState.startDate.isBlank()) "Seleziona" else uiState.startDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (uiState.startDate.isBlank()) Color.Gray else Color.Black // ✅ Testo nero
                    )
                }
            }

            // Data Fine - ✅ SFONDO BIANCO AGGIUNTO
            Card(
                modifier = Modifier.weight(1f),
                onClick = { showEndDatePicker = true },
                colors = CardDefaults.cardColors(
                    containerColor = Color.White // ✅ Sfondo bianco
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = TravelGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Data Fine",
                        fontSize = 11.sp,
                        color = Color.Black
                    )
                    Text(
                        if (uiState.endDate.isBlank()) "Seleziona" else uiState.endDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (uiState.endDate.isBlank()) Color.Gray else Color.Black // ✅ Testo nero
                    )
                }
            }
        }

        // Suggerimento tipo viaggio
        if (suggestedTripType != null && uiState.tripType.isBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE3F2FD)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Suggerimento",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            "In base alle date, ti suggeriamo: ${TripTypeLogic.getTripTypeEmoji(suggestedTripType)} $suggestedTripType",
                            fontSize = 13.sp,
                            color = Color(0xFF424242)
                        )
                    }
                }
            }
        }

        // Warning tipo viaggio
        if (tripTypeWarning != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "⚠️",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tripTypeWarning,
                        fontSize = 13.sp,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        // === SEZIONE TIPO VIAGGIO ===

        Text(
            "Tipo di Viaggio",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.Black
        )

        TRIP_TYPES.forEach { tripType ->
            val isSelected = uiState.tripType == tripType
            val emoji = TripTypeLogic.getTripTypeEmoji(tripType)
            val description = TripTypeLogic.getTripTypeDescription(tripType)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        TravelGreen.copy(alpha = 0.2f)
                    } else {
                        Color.White
                    }
                ),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, TravelGreen)
                } else {
                    null
                },
                shape = RoundedCornerShape(12.dp),
                onClick = { onTripTypeChange(tripType) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        emoji,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tripType,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isSelected) TravelGreen else Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            description,
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = TravelGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // === SEZIONE DESCRIZIONE (OPZIONALE) ===

        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            label = { Text("Descrizione (opzionale)") },
            placeholder = { Text("Es: Vacanza estiva con famiglia") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Notes,
                    contentDescription = null,
                    tint = TravelGreen
                )
            },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TravelGreen,
                focusedLabelColor = TravelGreen,
                cursorColor = TravelGreen
            )
        )

        // === PULSANTE SALVA ===

        Button(
            onClick = onSave,
            enabled = uiState.isEntryValid,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = TravelGreen,
                disabledContainerColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (uiState.isEntryValid) "Crea Viaggio" else "Compila tutti i campi obbligatori",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Spazio finale per garantire che l'ultimo pulsante non sia mai nascosto
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Date Pickers con sfondo bianco e testo nero
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            onStartDateChange(dateFormatter.format(date))
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK", color = TravelGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Annulla", color = Color.DarkGray)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White
            )
        ) {
            DatePicker(
                state = datePickerState,
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

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            onEndDateChange(dateFormatter.format(date))
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK", color = TravelGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Annulla", color = Color.DarkGray)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White
            )
        ) {
            DatePicker(
                state = datePickerState,
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