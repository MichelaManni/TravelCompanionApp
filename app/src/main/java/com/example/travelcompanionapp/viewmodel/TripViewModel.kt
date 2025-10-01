package com.example.travelcompanionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.travelcompanionapp.data.Trip
import com.example.travelcompanionapp.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date // Necessario per Date
import java.time.Instant // Tipo moderno, ma useremo java.util.Date per coerenza con Trip.kt

/**
 * 1. STATO UI PER LA LISTA DEI VIAGGI (GIA' ESISTENTE)
 */
data class TripUiState(
    val tripList: List<Trip> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * 2. STATO UI PER L'INSERIMENTO DI UN SINGOLO VIAGGIO (NUOVO)
 * Contiene i dati che l'utente sta inserendo nel form.
 * Usiamo String per i campi di input, anche per le date, perché sono i tipi usati da TextField.
 */
data class TripDetailsUiState(
    val destination: String = "",
    val startDate: String = "", // Rappresentazione testuale della data
    val endDate: String = "",   // Rappresentazione testuale della data
    val tripType: String = "Multi-day trip", // Valore predefinito
    val totalDistanceKm: String = "0.0",
    val isEntryValid: Boolean = false
)

/**
 * ViewModel: Contiene la logica di business e mantiene lo stato dell'UI.
 */
class TripViewModel(private val tripRepository: TripRepository) : ViewModel() {

    // (CODICE ESISTENTE) StateFlow per la schermata della lista.
    val uiState: StateFlow<TripUiState> = tripRepository.allTrips
        .map { trips ->
            TripUiState(tripList = trips, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TripUiState(isLoading = true)
        )

    // (NUOVO) StateFlow per la schermata di inserimento.
    // Usiamo MutableStateFlow per permettere la scrittura.
    private val _tripDetailsUiState = MutableStateFlow(TripDetailsUiState())
    val tripDetailsUiState: StateFlow<TripDetailsUiState> = _tripDetailsUiState.asStateFlow()

    /**
     * Aggiorna il valore della destinazione nel form.
     */
    fun updateDestination(destination: String) {
        _tripDetailsUiState.update { currentState ->
            currentState.copy(destination = destination)
        }
        // TODO: Aggiungere qui la validazione del form se necessario
    }

    /**
     * Aggiorna il valore del tipo di viaggio.
     */
    fun updateTripType(type: String) {
        _tripDetailsUiState.update { currentState ->
            currentState.copy(tripType = type)
        }
    }

    // TODO: Implementare updateStartDate(date: Date) e updateEndDate(date: Date)
    // per gestire l'aggiornamento delle date dal DatePicker.

    /**
     * 1. Costruisce l'oggetto Trip dall'attuale TripDetailsUiState.
     * 2. Chiama il Repository per salvare i dati.
     */
    fun saveNewTrip() {
        // Estraiamo i dati dallo stato corrente.
        val currentState = _tripDetailsUiState.value

        // --- ESEMPIO DI CONVERSIONE E GESTIONE DATI ---
        // Attenzione: Qui c'è un placeholder! Le stringhe di data devono essere
        // convertite in oggetti Date. Per ora, useremo Date() per semplificare.

        val newTrip = Trip(
            destination = currentState.destination,
            // Sostituire con una corretta conversione da String a Date
            startDate = Date(),
            endDate = Date(),
            tripType = currentState.tripType,
            totalDistanceKm = currentState.totalDistanceKm.toDoubleOrNull() ?: 0.0
        )

        // Eseguiamo l'inserimento nel DB in background (coroutine).
        viewModelScope.launch {
            tripRepository.insertTrip(newTrip)
            // Dopo il salvataggio, possiamo resettare lo stato del form.
            _tripDetailsUiState.value = TripDetailsUiState()
        }
    }

    // (CODICE ESISTENTE) Funzione per inserire un nuovo viaggio nel database (usata da saveNewTrip).
    fun saveNewTrip(trip: Trip) {
        viewModelScope.launch {
            tripRepository.insertTrip(trip)
        }
    }

    /**
     * Factory per creare il ViewModel.
     * Rimane invariata ma ora gestisce anche la nuova logica.
     */
    companion object {
        fun Factory(repository: TripRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TripViewModel(repository) as T
            }
        }
    }
}