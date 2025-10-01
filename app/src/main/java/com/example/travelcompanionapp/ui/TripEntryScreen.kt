package com.example.travelcompanionapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.travelcompanionapp.viewmodel.TripViewModel

/**
 * Schermata per l'inserimento o la modifica di un viaggio.
 *
 * @param viewModel Il ViewModel per accedere ai dati del form.
 * @param onNavigateBack Funzione callback per tornare alla schermata precedente.
 */
@Composable
fun TripEntryScreen(
    viewModel: TripViewModel,
    onNavigateBack: () -> Unit
) {
    // Collezioniamo lo StateFlow del form dal ViewModel.
    // Ogni volta che lo stato cambia, il Composable viene ricomposto (aggiornato).
    val uiState by viewModel.tripDetailsUiState.collectAsState()

    Scaffold(
        topBar = {
            // TODO: Aggiungere una TopAppBar con un pulsante Indietro e un titolo dinamico
        }
    ) { innerPadding ->
        TripEntryBody(
            uiState = uiState,
            onDestinationChange = viewModel::updateDestination, // Collega l'input al ViewModel
            onSaveTrip = {
                viewModel.saveNewTrip()
                onNavigateBack() // Dopo il salvataggio, torna alla lista
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * Componente principale che contiene i campi di input del viaggio.
 */
@Composable
private fun TripEntryBody(
    uiState: com.example.travelcompanionapp.viewmodel.TripDetailsUiState,
    onDestinationChange: (String) -> Unit,
    onSaveTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Campo di input: Destinazione
        OutlinedTextField(
            value = uiState.destination,
            onValueChange = onDestinationChange, // Chiama la funzione nel ViewModel
            label = { Text("Destinazione") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // TODO: Aggiungere i campi per startDate, endDate e tripType

        Text(text = "Tipo di Viaggio: ${uiState.tripType}") // Placeholder per il tipo

        // Campo di input: Distanza (per i Multi-day trip)
        OutlinedTextField(
            value = uiState.totalDistanceKm,
            onValueChange = { /* TODO: Implementare updateTotalDistanceKm */ },
            label = { Text("Distanza Totale (Km)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Pulsante per salvare il viaggio
        Button(
            onClick = onSaveTrip,
            // enabled = uiState.isEntryValid, // Abilitare solo se il form è valido
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salva Viaggio")
        }
    }
}