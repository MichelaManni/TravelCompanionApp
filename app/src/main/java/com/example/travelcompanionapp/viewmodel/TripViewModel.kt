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

private const val DATE_FORMAT = "dd/MM/yyyy"

/**
 * Converte una stringa data in un oggetto Date.
 */
fun convertDateStringToDate(dateString: String): Date? {
    return try {
        SimpleDateFormat(DATE_FORMAT, Locale.ITALIAN).parse(dateString)
    } catch (e: Exception) {
        null
    }
}

/**
 * Stato UI per la lista dei viaggi.
 */
data class TripUiState(
    val tripList: List<Trip> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Stato UI per l'inserimento di un viaggio.
 */
data class TripDetailsUiState(
    val destination: String = "",
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,
    val startDate: String = "",
    val endDate: String = "",
    val tripType: String = "Multi-day trip",
    val isEntryValid: Boolean = false
)

/**
 * ViewModel principale per la gestione dei viaggi.
 */
class TripViewModel(private val tripRepository: TripRepository) : ViewModel() {

    val uiState: StateFlow<TripUiState> = tripRepository.getAllTripsStream()
        .map { TripUiState(it, false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TripUiState(isLoading = true)
        )

    private val _tripDetailsUiState = MutableStateFlow(TripDetailsUiState())
    val tripDetailsUiState: StateFlow<TripDetailsUiState> = _tripDetailsUiState.asStateFlow()

    // --- METODI DI AGGIORNAMENTO ---

    fun updateDestination(destination: String) {
        // Quando l'utente scrive manualmente, azzera le coordinate
        _tripDetailsUiState.update {
            it.copy(
                destination = destination,
                destinationLat = null,
                destinationLng = null
            )
        }
        validateEntry()
    }

    /**
     * Aggiorna destinazione e coordinate dalla mappa.
     */
    fun updateDestinationCoordinates(name: String, lat: Double, lng: Double) {
        _tripDetailsUiState.update {
            it.copy(
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

    /**
     * Valida che tutti i campi obbligatori siano compilati.
     */
    private fun validateEntry() {
        val currentState = _tripDetailsUiState.value

        // Validazione base: destinazione e date devono essere presenti
        val isValid = currentState.destination.isNotBlank() &&
                currentState.startDate.isNotBlank() &&
                currentState.endDate.isNotBlank()

        // Validazione aggiuntiva: verifica che la data fine sia >= data inizio
        val datesValid = if (isValid) {
            val startDate = convertDateStringToDate(currentState.startDate)
            val endDate = convertDateStringToDate(currentState.endDate)
            startDate != null && endDate != null && !endDate.before(startDate)
        } else {
            false
        }

        _tripDetailsUiState.update { it.copy(isEntryValid = isValid && datesValid) }
    }

    /**
     * Salva il viaggio nel database.
     */
    fun saveTrip() {
        val currentState = _tripDetailsUiState.value

        if (!currentState.isEntryValid) return

        val startDate = convertDateStringToDate(currentState.startDate) ?: Date()
        val endDate = convertDateStringToDate(currentState.endDate) ?: Date()

        val newTrip = Trip(
            destination = currentState.destination,
            destinationLat = currentState.destinationLat,
            destinationLng = currentState.destinationLng,
            startDate = startDate,
            endDate = endDate,
            tripType = currentState.tripType,
            totalDistanceKm = 0.0, // Verrà calcolata durante il tracking GPS
            status = "Pianificato"
        )

        viewModelScope.launch {
            tripRepository.insertTrip(newTrip)
            // Reset del form
            _tripDetailsUiState.value = TripDetailsUiState()
        }
    }

    /**
     * Factory per creare il ViewModel.
     */
    companion object {
        fun Factory(repository: TripRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(TripViewModel::class.java))
                    return TripViewModel(repository) as T
                }
            }
    }
}