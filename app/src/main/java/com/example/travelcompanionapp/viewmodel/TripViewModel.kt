package com.example.travelcompanionapp.viewmodel//responsabile della logica tra repository e ui

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

// viewmodel gestisce tutti i dati relativi ai viaggi e fornisce stato e logica alla ui
//comunica con il repository per eseguire operazioni sul database e mantiene gli stati dell’interfaccia tramite flow e stateflow
//include funzioni per la creazione, modifica e cancellazione di viaggi, note e foto, oltre a metodi per gestire lo stato temporale dei viaggi
class TripViewModel(private val repository: TripRepository) : ViewModel() {

    //=== stato generale dell’app (lista viaggi) ===
    //stateflow che emette continuamente l’elenco aggiornato dei viaggi salvati
    val uiState: StateFlow<TripUiState> = repository.getAllTripsStream()
        .map { trips -> TripUiState(tripList = trips) } //mappa i dati provenienti dal repository nello stato dell’interfaccia
        .stateIn(
            scope = viewModelScope, //ambito del viewmodel per mantenere il flusso attivo finché la ui è in uso
            started = SharingStarted.WhileSubscribed(5000), //mantiene vivo il flusso per 5 secondi dopo l’ultima sottoscrizione
            initialValue = TripUiState() //stato iniziale vuoto
        )

    //flusso separato che contiene solo i viaggi completati
    val completedTrips: Flow<List<Trip>> = repository.completedTrips

    //=== stato dei dettagli del viaggio (per form di inserimento o modifica) ===
    //statoflow mutabile usato per aggiornare dinamicamente i campi del form
    private val _tripDetailsUiState = MutableStateFlow(TripDetailsUiState())
    val tripDetailsUiState: StateFlow<TripDetailsUiState> = _tripDetailsUiState.asStateFlow()

    //=== funzioni per aggiornare i campi del form ===

    //aggiorna il campo destinazione e ricalcola la validità dei dati
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

    //aggiorna i dati gps della destinazione selezionata da mappa
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

    //aggiorna la data di inizio viaggio e valida il form
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

    //aggiorna la data di fine viaggio e valida i dati
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

    //aggiorna il tipo di viaggio selezionato e valida l’input
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

    //aggiorna la descrizione del viaggio, campo opzionale che non influisce sulla validazione
    fun updateDescription(description: String) {
        _tripDetailsUiState.update { currentState ->
            currentState.copy(description = description)
        }
    }

    //=== operazioni sul database (viaggi) ===

    //salva un nuovo viaggio convertendo i dati del form in un oggetto trip
    fun saveTrip() {
        val currentState = _tripDetailsUiState.value

        if (!currentState.isEntryValid) return //interrompe se i dati non sono validi

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN) //formattatore per le date

        viewModelScope.launch {
            try {
                val trip = Trip(
                    destination = currentState.destination,
                    destinationLat = currentState.destinationLat,
                    destinationLng = currentState.destinationLng,
                    startDate = dateFormatter.parse(currentState.startDate) ?: Date(),
                    endDate = dateFormatter.parse(currentState.endDate) ?: Date(),
                    tripType = currentState.tripType,
                    status = "Pianificato", //stato iniziale del viaggio
                    description = currentState.description
                )
                repository.insertTrip(trip) //inserisce nel database

                _tripDetailsUiState.value = TripDetailsUiState() //reset del form dopo il salvataggio
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //crea e salva un viaggio rapido con data odierna (utile per tracking immediato)
    suspend fun createQuickTrip(
        destination: String,
        tripType: String,
        description: String = "Viaggio rapido"
    ) {
        val today = Date()
        val trip = Trip(
            destination = destination,
            startDate = today,
            endDate = today,
            tripType = tripType,
            status = "Pianificato",
            description = description
        )
        repository.insertTrip(trip)
    }

    //aggiorna i dati di un viaggio esistente
    fun updateTrip(trip: Trip) {
        viewModelScope.launch {
            try {
                repository.updateTrip(trip)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //elimina un viaggio dal database
    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
        }
    }

    //=== operazioni sulle note ===

    //restituisce tutte le note associate a un viaggio
    fun getNotesForTrip(tripId: Int): Flow<List<TripNote>> {
        return repository.getNotesForTrip(tripId)
    }

    //restituisce solo le ultime 3 note per anteprima rapida
    fun getRecentNotesForTrip(tripId: Int): Flow<List<TripNote>> {
        return repository.getRecentNotesForTrip(tripId, limit = 3)
    }

    //inserisce una nuova nota nel database
    fun insertNote(note: TripNote) {
        viewModelScope.launch {
            try {
                repository.insertNote(note)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //cancella una nota esistente
    fun deleteNote(note: TripNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    //=== operazioni sulle foto ===

    //restituisce tutte le foto associate a un viaggio
    fun getPhotosForTrip(tripId: Int): Flow<List<TripPhoto>> {
        return repository.getPhotosForTrip(tripId)
    }

    //restituisce le ultime 3 foto per anteprima
    fun getRecentPhotosForTrip(tripId: Int): Flow<List<TripPhoto>> {
        return repository.getRecentPhotosForTrip(tripId, limit = 3)
    }

    //restituisce le foto che contengono coordinate gps per la mappa
    fun getPhotosWithLocation(tripId: Int): Flow<List<TripPhoto>> {
        return repository.getPhotosWithLocation(tripId)
    }

    //inserisce una nuova foto nel database
    fun insertPhoto(photo: TripPhoto) {
        viewModelScope.launch {
            try {
                repository.insertPhoto(photo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //aggiorna i dati di una foto esistente (es. didascalia)
    fun updatePhoto(photo: TripPhoto) {
        viewModelScope.launch {
            repository.updatePhoto(photo)
        }
    }

    //elimina una foto dal database (non cancella il file fisico)
    fun deletePhoto(photo: TripPhoto) {
        viewModelScope.launch {
            repository.deletePhoto(photo)
        }
    }

    //ottiene il numero di foto associate a un determinato viaggio
    suspend fun getPhotosCount(tripId: Int): Int {
        return repository.getPhotosCount(tripId)
    }

    //=== gestione stato temporale dei viaggi ===

    //determina se un viaggio è in corso, futuro, recente o passato
    fun getTripTemporalStatus(trip: Trip): TripTemporalStatus {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val startCal = Calendar.getInstance().apply {
            time = trip.startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance().apply {
            time = trip.endDate
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val millisecondsInDay = 1000L * 60 * 60 * 24
        val daysUntilStart = ((startCal.timeInMillis - today.time) / millisecondsInDay).toInt()
        val daysSinceEnd = ((today.time - endCal.timeInMillis) / millisecondsInDay).toInt()

        return when {
            today >= trip.startDate && today <= endCal.time -> TripTemporalStatus.ACTIVE
            daysUntilStart in 0..3 -> TripTemporalStatus.UPCOMING
            daysSinceEnd in 0..3 -> TripTemporalStatus.RECENT
            today > endCal.time -> TripTemporalStatus.PAST
            else -> TripTemporalStatus.FUTURE
        }
    }

    //calcola i giorni che mancano all’inizio del viaggio
    fun getDaysUntilTripStart(trip: Trip): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startCal = Calendar.getInstance().apply {
            time = trip.startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val millisecondsInDay = 1000L * 60 * 60 * 24
        return ((startCal.timeInMillis - today.timeInMillis) / millisecondsInDay).toInt()
    }

    //calcola i giorni trascorsi dalla fine del viaggio
    fun getDaysSinceTripEnd(trip: Trip): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance().apply {
            time = trip.endDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val millisecondsInDay = 1000L * 60 * 60 * 24
        return ((today.timeInMillis - endCal.timeInMillis) / millisecondsInDay).toInt()
    }

    //=== validazione input ===
    //controlla che tutti i campi obbligatori del form siano compilati
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

    //=== factory per creare il viewmodel ===
    companion object {
        fun Factory(repository: TripRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TripViewModel(repository) //inizializza il viewmodel con il repository passato
            }
        }
    }
}

//=== data classes e enum per rappresentare lo stato dell’interfaccia ===

//stato generale dell’app: contiene la lista viaggi e flag di caricamento
data class TripUiState(
    val tripList: List<Trip> = emptyList(),
    val isLoading: Boolean = false
)

//stato dei dettagli di un viaggio in fase di inserimento o modifica
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

//enum che definisce lo stato temporale di un viaggio rispetto alla data corrente
enum class TripTemporalStatus {
    ACTIVE, //viaggio in corso
    UPCOMING, //viaggio imminente entro 3 giorni
    RECENT, //viaggio concluso da massimo 3 giorni
    FUTURE, //viaggio pianificato lontano
    PAST //viaggio terminato da tempo
}
