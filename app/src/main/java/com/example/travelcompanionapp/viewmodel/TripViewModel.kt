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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Formato standard per visualizzare la data nell'UI: Es. 31/12/2025
private const val DATE_FORMAT = "dd/MM/yyyy"

/**
 * Funzione di utilità per convertire una Stringa (es. "31/12/2025") in un oggetto Date.
 */
fun convertDateStringToDate(dateString: String): Date? {
    return try {
        SimpleDateFormat(DATE_FORMAT, Locale.ITALIAN).parse(dateString)
    } catch (e: Exception) {
        null // Ritorna null se la conversione fallisce
    }
}


/**
 * 1. STATO UI PER LA LISTA DEI VIAGGI
 */
data class TripUiState(
    val tripList: List<Trip> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * 2. STATO UI PER L'INSERIMENTO DI UN SINGOLO VIAGGIO
 */
data class TripDetailsUiState(
    val destination: String = "",
    // ⭐ AGGIORNATO: Campi per le coordinate (possono essere null se non selezionate da mappa)
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,
    // Contengono stringhe formattate (es. "01/11/2025")
    val startDate: String = "",
    val endDate: String = "",
    val tripType: String = "Multi-day trip",
    // Campo stringa per la distanza (input utente)
    val totalDistanceStr: String = "",
    // Campo double per la distanza (salvataggio nel DB)
    val totalDistanceKm: Double = 0.0,
    val isEntryValid: Boolean = false // Indica se il form è pronto per il salvataggio
)


/**
 * ViewModel principale dell'applicazione.
 */
class TripViewModel(private val tripRepository: TripRepository) : ViewModel() {

    // ⭐ CORREZIONE 1: Risolve l'errore 'getAllTripsStream' e la catena Flow/stateIn
    val uiState: StateFlow<TripUiState> = tripRepository.getAllTripsStream()
        .map { TripUiState(it, false) } // Mappa List<Trip> in TripUiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TripUiState(isLoading = true)
        )

    // Stato per il form di inserimento (EntryScreen)
    private val _tripDetailsUiState = MutableStateFlow(TripDetailsUiState())
    val tripDetailsUiState: StateFlow<TripDetailsUiState> = _tripDetailsUiState.asStateFlow()

    // --- METODI PER AGGIORNARE LO STATO DEL FORM ---

    fun updateDestination(destination: String) {
        _tripDetailsUiState.update { it.copy(destination = destination) }
        validateEntry()
    }

    // ⭐ NUOVO METODO: Aggiorna la destinazione e le coordinate selezionate dalla mappa.
    fun updateDestinationCoordinates(name: String, lat: Double, lng: Double) {
        _tripDetailsUiState.update { currentState ->
            currentState.copy(
                destination = name,
                destinationLat = lat,
                destinationLng = lng
            )
        }
        validateEntry()
    }

    fun updateStartDate(date: String) {
        _tripDetailsUiState.update { it.copy(startDate = date) }
        validateEntry()
    }

    fun updateEndDate(date: String) {
        _tripDetailsUiState.update { it.copy(endDate = date) }
        validateEntry()
    }

    fun updateTripType(type: String) {
        _tripDetailsUiState.update { it.copy(tripType = type) }
        validateEntry()
    }

    // Gestione della distanza come Stringa
    fun updateTotalDistanceStr(distanceStr: String) {
        val distance = distanceStr.toDoubleOrNull() ?: 0.0
        _tripDetailsUiState.update {
            it.copy(
                totalDistanceStr = distanceStr,
                totalDistanceKm = distance
            )
        }
        validateEntry()
    }


    private fun validateEntry() {
        val currentState = _tripDetailsUiState.value
        val isValid = currentState.destination.isNotBlank() &&
                currentState.startDate.isNotBlank() &&
                currentState.endDate.isNotBlank()
        // Nota: le coordinate possono essere null, quindi non le usiamo per la validazione base

        _tripDetailsUiState.update { it.copy(isEntryValid = isValid) }
    }


    /**
     * Funzione per salvare il viaggio nel database.
     */
    fun saveTrip() {
        val currentState = _tripDetailsUiState.value

        // Conversione delle stringhe di data in oggetti Date per la Entity
        // Gestiamo il caso in cui la conversione fallisca o la data sia vuota (anche se la validazione dovrebbe prevenirlo)
        val finalStartDate = convertDateStringToDate(currentState.startDate) ?: Date()
        val finalEndDate = convertDateStringToDate(currentState.endDate) ?: Date()


        // Costruzione della nuova Entity Trip
        val newTrip = Trip(
            destination = currentState.destination,
            // ⭐ CORREZIONE 2: Usa i campi delle coordinate aggiunti a TripDetailsUiState
            destinationLat = currentState.destinationLat,
            destinationLng = currentState.destinationLng,
            // ⭐ FINE CORREZIONE 2
            startDate = finalStartDate,
            endDate = finalEndDate,
            tripType = currentState.tripType,
            totalDistanceKm = currentState.totalDistanceKm
        )

        viewModelScope.launch {
            tripRepository.insertTrip(newTrip)
            // Resetta lo stato del form dopo il salvataggio
            _tripDetailsUiState.value = TripDetailsUiState()
        }
    }


    /**
     * Factory per creare il ViewModel.
     */
    companion object {
        fun Factory(repository: TripRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TripViewModel::class.java))
                return TripViewModel(repository) as T
            }
        }
    }
}