package com.example.travelcompanionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.travelcompanionapp.data.Trip
import com.example.travelcompanionapp.data.TripNote
import com.example.travelcompanionapp.data.TripPhoto
import com.example.travelcompanionapp.repository.TripRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel per gestire lo stato dell'interfaccia utente e le operazioni sui viaggi.
 * Comunica con il Repository e fornisce i dati alle UI attraverso Flow e StateFlow.
 *
 * ⭐ AGGIORNAMENTO:
 * - Aggiunta gestione completa delle foto
 */
class TripViewModel(private val repository: TripRepository) : ViewModel() {

    // === STATO GENERALE DELL'APP (lista viaggi) ===
    /**
     * StateFlow che emette lo stato attuale dell'UI per la lista viaggi.
     * Viene osservato dalle schermate per reagire ai cambiamenti.
     */
    val uiState: StateFlow<TripUiState> = repository.getAllTripsStream()
        .map { trips -> TripUiState(tripList = trips) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TripUiState()
        )

    // === STATO DEI DETTAGLI DEL VIAGGIO (per inserimento/modifica) ===
    /**
     * MutableStateFlow che contiene i dettagli del viaggio in fase di inserimento/modifica.
     */
    private val _tripDetailsUiState = MutableStateFlow(TripDetailsUiState())
    val tripDetailsUiState: StateFlow<TripDetailsUiState> = _tripDetailsUiState.asStateFlow()

    // === FUNZIONI PER AGGIORNARE I CAMPI DEL FORM ===

    /**
     * Aggiorna il campo destinazione.
     */
    fun updateDestination(destination: String) {
        _tripDetailsUiState.update { currentState ->
            currentState.copy(
                destination = destination,
                isEntryValid = validateInput(
                    destination = destination,
                    startDate = currentState.startDate,
                    endDate = currentState.endDate,
                    tripType = currentState.tripType
                )
            )
        }
    }

    /**
     * Aggiorna le coordinate GPS della destinazione (da mappa).
     */
    fun updateDestinationCoordinates(name: String, lat: Double, lng: Double) {
        _tripDetailsUiState.update { currentState ->
            currentState.copy(
                destination = name,
                destinationLat = lat,
                destinationLng = lng,
                isEntryValid = validateInput(
                    destination = name,
                    startDate = currentState.startDate,
                    endDate = currentState.endDate,
                    tripType = currentState.tripType
                )
            )
        }
    }

    /**
     * Aggiorna la data di inizio.
     */
    fun updateStartDate(startDate: String) {
        _tripDetailsUiState.update { currentState ->
            currentState.copy(
                startDate = startDate,
                isEntryValid = validateInput(
                    destination = currentState.destination,
                    startDate = startDate,
                    endDate = currentState.endDate,
                    tripType = currentState.tripType
                )
            )
        }
    }

    /**
     * Aggiorna la data di fine.
     */
    fun updateEndDate(endDate: String) {
        _tripDetailsUiState.update { currentState ->
            currentState.copy(
                endDate = endDate,
                isEntryValid = validateInput(
                    destination = currentState.destination,
                    startDate = currentState.startDate,
                    endDate = endDate,
                    tripType = currentState.tripType
                )
            )
        }
    }

    /**
     * Aggiorna il tipo di viaggio.
     */
    fun updateTripType(tripType: String) {
        _tripDetailsUiState.update { currentState ->
            currentState.copy(
                tripType = tripType,
                isEntryValid = validateInput(
                    destination = currentState.destination,
                    startDate = currentState.startDate,
                    endDate = currentState.endDate,
                    tripType = tripType
                )
            )
        }
    }

    /**
     * Aggiorna la descrizione generale del viaggio.
     * La descrizione è opzionale, quindi non influenza la validazione del form.
     */
    fun updateDescription(description: String) {
        _tripDetailsUiState.update { currentState ->
            currentState.copy(description = description)
        }
    }

    // === OPERAZIONI SUL DATABASE (VIAGGI) ===

    /**
     * Salva un nuovo viaggio nel database.
     * Converte i dati dal form in un oggetto Trip e lo inserisce tramite il repository.
     */
    fun saveTrip() {
        val currentState = _tripDetailsUiState.value

        if (!currentState.isEntryValid) return

        // Formattatore per convertire le stringhe in Date
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)

        viewModelScope.launch {
            try {
                val trip = Trip(
                    destination = currentState.destination,
                    destinationLat = currentState.destinationLat,
                    destinationLng = currentState.destinationLng,
                    startDate = dateFormatter.parse(currentState.startDate) ?: Date(),
                    endDate = dateFormatter.parse(currentState.endDate) ?: Date(),
                    tripType = currentState.tripType,
                    status = "Pianificato", // Stato iniziale
                    description = currentState.description
                )
                repository.insertTrip(trip)

                // Reset del form dopo il salvataggio
                _tripDetailsUiState.value = TripDetailsUiState()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Aggiorna un viaggio esistente nel database.
     * Usata ad esempio per salvare la distanza percorsa dopo il tracking.
     */
    fun updateTrip(trip: Trip) {
        viewModelScope.launch {
            try {
                repository.updateTrip(trip)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Cancella un viaggio dal database.
     */
    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
        }
    }

    // === OPERAZIONI SULLE NOTE ===

    /**
     * Ottiene tutte le note di un viaggio specifico.
     *
     * @param tripId ID del viaggio
     * @return Flow che emette la lista aggiornata delle note
     */
    fun getNotesForTrip(tripId: Int): Flow<List<TripNote>> {
        return repository.getNotesForTrip(tripId)
    }

    /**
     * Ottiene le ultime 3 note di un viaggio.
     * Utile per mostrare un'anteprima durante il tracking.
     *
     * @param tripId ID del viaggio
     * @return Flow con le ultime 3 note
     */
    fun getRecentNotesForTrip(tripId: Int): Flow<List<TripNote>> {
        return repository.getRecentNotesForTrip(tripId, limit = 3)
    }

    /**
     * Inserisce una nuova nota nel database.
     *
     * @param note Nota da inserire
     */
    fun insertNote(note: TripNote) {
        viewModelScope.launch {
            try {
                repository.insertNote(note)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Cancella una nota dal database.
     *
     * @param note Nota da cancellare
     */
    fun deleteNote(note: TripNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // === ⭐ OPERAZIONI SULLE FOTO (NUOVE) ===

    /**
     * Ottiene tutte le foto di un viaggio specifico.
     *
     * @param tripId ID del viaggio
     * @return Flow che emette la lista aggiornata delle foto
     */
    fun getPhotosForTrip(tripId: Int): Flow<List<TripPhoto>> {
        return repository.getPhotosForTrip(tripId)
    }

    /**
     * Ottiene le ultime 3 foto di un viaggio.
     * Utile per mostrare un'anteprima durante il tracking.
     *
     * @param tripId ID del viaggio
     * @return Flow con le ultime 3 foto
     */
    fun getRecentPhotosForTrip(tripId: Int): Flow<List<TripPhoto>> {
        return repository.getRecentPhotosForTrip(tripId, limit = 3)
    }

    /**
     * Ottiene tutte le foto con coordinate GPS di un viaggio.
     * Utile per visualizzarle su una mappa.
     *
     * @param tripId ID del viaggio
     * @return Flow con le foto che hanno coordinate valide
     */
    fun getPhotosWithLocation(tripId: Int): Flow<List<TripPhoto>> {
        return repository.getPhotosWithLocation(tripId)
    }

    /**
     * Inserisce una nuova foto nel database.
     *
     * @param photo Foto da inserire
     */
    fun insertPhoto(photo: TripPhoto) {
        viewModelScope.launch {
            try {
                repository.insertPhoto(photo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Aggiorna una foto esistente.
     * Utile per modificare la didascalia.
     *
     * @param photo Foto con i dati aggiornati
     */
    fun updatePhoto(photo: TripPhoto) {
        viewModelScope.launch {
            repository.updatePhoto(photo)
        }
    }

    /**
     * Cancella una foto dal database.
     * NOTA: Non elimina il file fisico, solo il record dal database.
     *
     * @param photo Foto da cancellare
     */
    fun deletePhoto(photo: TripPhoto) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
        }
    }

    /**
     * Ottiene il numero di foto per un viaggio.
     *
     * @param tripId ID del viaggio
     * @return Numero di foto
     */
    suspend fun getPhotosCount(tripId: Int): Int {
        return repository.getPhotosCount(tripId)
    }

    // === VALIDAZIONE INPUT ===

    /**
     * Valida che tutti i campi obbligatori siano compilati.
     * NOTA: La descrizione NON è obbligatoria, quindi non fa parte della validazione
     */
    private fun validateInput(
        destination: String,
        startDate: String,
        endDate: String,
        tripType: String
    ): Boolean {
        return destination.isNotBlank() &&
                startDate.isNotBlank() &&
                endDate.isNotBlank() &&
                tripType.isNotBlank()
    }

    // === FACTORY PER CREARE IL VIEWMODEL ===

    companion object {
        fun Factory(repository: TripRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TripViewModel(repository)
            }
        }
    }
}

// === DATA CLASSES PER LO STATO DELL'UI ===

/**
 * Stato generale dell'applicazione (lista viaggi).
 */
data class TripUiState(
    val tripList: List<Trip> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * Stato dei dettagli del viaggio in inserimento/modifica.
 */
data class TripDetailsUiState(
    val destination: String = "",
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,
    val startDate: String = "",
    val endDate: String = "",
    val tripType: String = "",
    val description: String = "",
    val isEntryValid: Boolean = false
)