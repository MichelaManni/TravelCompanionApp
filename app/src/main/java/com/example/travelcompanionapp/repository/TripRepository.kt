package com.example.travelcompanionapp.repository

import com.example.travelcompanionapp.data.Trip
import com.example.travelcompanionapp.data.TripDao
import com.example.travelcompanionapp.data.TripNote
import com.example.travelcompanionapp.data.TripNoteDao
import com.example.travelcompanionapp.data.TripPhoto
import com.example.travelcompanionapp.data.TripPhotoDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository per gestire le operazioni sui dati dei viaggi, note e foto.
 *
 * ⭐ AGGIORNAMENTO: Aggiunta gestione delle foto durante il viaggio
 *
 * Il Repository astrae la sorgente dei dati (in questo caso Room Database)
 * e fornisce un'interfaccia pulita per il ViewModel.
 */
class TripRepository(
    private val tripDao: TripDao,
    private val tripNoteDao: TripNoteDao,
    private val tripPhotoDao: TripPhotoDao // ⭐ Nuovo DAO per le foto
) {

    // ============================================
    // OPERAZIONI SUI VIAGGI (Trip)
    // ============================================

    /**
     * Ritorna un Flow di tutti i viaggi.
     * Il Flow emette automaticamente nuovi valori quando i dati cambiano.
     */
    fun getAllTripsStream(): Flow<List<Trip>> {
        return tripDao.getAllTrips()
    }

    /**
     * Ottiene solo i viaggi completati per le statistiche.
     */
    val completedTrips = tripDao.getCompletedTrips()

    /**
     * Inserisce un nuovo viaggio nel database.
     */
    suspend fun insertTrip(trip: Trip) {
        tripDao.insert(trip)
    }

    /**
     * Aggiorna un viaggio esistente nel database.
     */
    suspend fun updateTrip(trip: Trip) {
        tripDao.update(trip)
    }

    /**
     * Cancella un viaggio dal database.
     * Le note e foto associate verranno cancellate automaticamente (CASCADE).
     */
    suspend fun deleteTrip(trip: Trip) {
        tripDao.delete(trip)
    }

    /**
     * Ottiene un viaggio specifico tramite ID (senza Flow).
     */
    suspend fun getTripById(tripId: Int): Trip? {
        return tripDao.getTripById(tripId)
    }

    // ============================================
    // OPERAZIONI SULLE NOTE (TripNote)
    // ============================================

    /**
     * Ottiene tutte le note di un viaggio specifico.
     * Le note sono ordinate dalla più recente alla più vecchia.
     *
     * @param tripId ID del viaggio
     * @return Flow che emette la lista aggiornata delle note
     */
    fun getNotesForTrip(tripId: Int): Flow<List<TripNote>> {
        return tripNoteDao.getNotesForTrip(tripId)
    }

    /**
     * Ottiene le ultime N note di un viaggio.
     * Utile per mostrare un'anteprima durante il tracking.
     *
     * @param tripId ID del viaggio
     * @param limit Numero massimo di note (default: 3)
     * @return Flow con le ultime note
     */
    fun getRecentNotesForTrip(tripId: Int, limit: Int = 3): Flow<List<TripNote>> {
        return tripNoteDao.getRecentNotesForTrip(tripId, limit)
    }

    /**
     * Inserisce una nuova nota nel database.
     *
     * @param note Nota da inserire
     * @return L'ID della nota appena inserita
     */
    suspend fun insertNote(note: TripNote): Long {
        return tripNoteDao.insert(note)
    }

    /**
     * Aggiorna una nota esistente.
     *
     * @param note Nota con i dati aggiornati
     */
    suspend fun updateNote(note: TripNote) {
        tripNoteDao.update(note)
    }

    /**
     * Cancella una nota dal database.
     *
     * @param note Nota da cancellare
     */
    suspend fun deleteNote(note: TripNote) {
        tripNoteDao.delete(note)
    }

    /**
     * Ottiene il numero di note per un viaggio.
     * Utile per mostrare badge nella lista viaggi (es: "📝 5").
     *
     * @param tripId ID del viaggio
     * @return Numero di note
     */
    suspend fun getNotesCount(tripId: Int): Int {
        return tripNoteDao.getNotesCount(tripId)
    }

    /**
     * Ottiene una singola nota tramite ID.
     *
     * @param noteId ID della nota
     * @return La nota cercata o null se non esiste
     */
    suspend fun getNoteById(noteId: Int): TripNote? {
        return tripNoteDao.getNoteById(noteId)
    }

    /**
     * Cancella tutte le note di un viaggio specifico.
     *
     * @param tripId ID del viaggio
     */
    suspend fun deleteAllNotesForTrip(tripId: Int) {
        tripNoteDao.deleteAllNotesForTrip(tripId)
    }

    // ============================================
    // ⭐ OPERAZIONI SULLE FOTO (TripPhoto)
    // ============================================

    /**
     * Ottiene tutte le foto di un viaggio specifico.
     * Le foto sono ordinate dalla più recente alla più vecchia.
     *
     * @param tripId ID del viaggio
     * @return Flow che emette la lista aggiornata delle foto
     */
    fun getPhotosForTrip(tripId: Int): Flow<List<TripPhoto>> {
        return tripPhotoDao.getPhotosForTrip(tripId)
    }

    /**
     * Ottiene le ultime N foto di un viaggio.
     * Utile per mostrare un'anteprima durante il tracking.
     *
     * @param tripId ID del viaggio
     * @param limit Numero massimo di foto (default: 3)
     * @return Flow con le ultime foto
     */
    fun getRecentPhotosForTrip(tripId: Int, limit: Int = 3): Flow<List<TripPhoto>> {
        return tripPhotoDao.getRecentPhotosForTrip(tripId, limit)
    }

    /**
     * Ottiene tutte le foto con coordinate GPS di un viaggio.
     * Utile per visualizzarle su una mappa.
     *
     * @param tripId ID del viaggio
     * @return Flow con le foto che hanno coordinate valide
     */
    fun getPhotosWithLocation(tripId: Int): Flow<List<TripPhoto>> {
        return tripPhotoDao.getPhotosWithLocation(tripId)
    }

    /**
     * Inserisce una nuova foto nel database.
     *
     * @param photo Foto da inserire
     * @return L'ID della foto appena inserita
     */
    suspend fun insertPhoto(photo: TripPhoto): Long {
        return tripPhotoDao.insert(photo)
    }

    /**
     * Aggiorna una foto esistente.
     * Utile per modificare la didascalia.
     *
     * @param photo Foto con i dati aggiornati
     */
    suspend fun updatePhoto(photo: TripPhoto) {
        tripPhotoDao.update(photo)
    }

    /**
     * Cancella una foto dal database.
     * NOTA: Non elimina il file fisico, solo il record dal database.
     *
     * @param photo Foto da cancellare
     */
    suspend fun deletePhoto(photo: TripPhoto) {
        tripPhotoDao.delete(photo)
    }

    /**
     * Ottiene il numero di foto per un viaggio.
     * Utile per mostrare badge nella lista viaggi (es: "📷 12").
     *
     * @param tripId ID del viaggio
     * @return Numero di foto
     */
    suspend fun getPhotosCount(tripId: Int): Int {
        return tripPhotoDao.getPhotosCount(tripId)
    }

    /**
     * Ottiene una singola foto tramite ID.
     *
     * @param photoId ID della foto
     * @return La foto cercata o null se non esiste
     */
    suspend fun getPhotoById(photoId: Int): TripPhoto? {
        return tripPhotoDao.getPhotoById(photoId)
    }

    /**
     * Cancella tutte le foto di un viaggio specifico.
     * NOTA: Non elimina i file fisici, solo i record dal database.
     *
     * @param tripId ID del viaggio
     */
    suspend fun deleteAllPhotosForTrip(tripId: Int) {
        tripPhotoDao.deleteAllPhotosForTrip(tripId)
    }
}