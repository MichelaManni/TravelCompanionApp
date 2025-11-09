package com.example.travelcompanionapp.repository //repository sono il ponte tra viewmodel e sorgenti dati

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
 * COS'È UN REPOSITORY?
 * Il Repository è un pattern architetturale che astrae la sorgente dei dati.
 *
 * PERCHÉ USARE UN REPOSITORY?
 * Senza Repository:
 * ViewModel → parla direttamente con → TripDao, TripNoteDao, TripPhotoDao
 * Con Repository:
 * ViewModel → parla solo con → TripRepository → gestisce → TripDao, TripNoteDao, TripPhotoDao
 */
class TripRepository(
    private val tripDao: TripDao,
    private val tripNoteDao: TripNoteDao,
    private val tripPhotoDao: TripPhotoDao
) {


    // OPERAZIONI SUI VIAGGI (Trip)

    //ritorna un Flow di tutti i viaggi.
     //Flow emette automaticamente nuovi valori quando i dati cambiano.
    fun getAllTripsStream(): Flow<List<Trip>> {
        return tripDao.getAllTrips()
    }

     //Ottiene solo i viaggi completati per le statistiche.
    val completedTrips = tripDao.getCompletedTrips()


     //Inserisce un nuovo viaggio nel database.
    suspend fun insertTrip(trip: Trip) {
        tripDao.insert(trip)
    }


     //aggiorna un viaggio esistente nel database.
    suspend fun updateTrip(trip: Trip) {
        tripDao.update(trip)
    }

    //cancella un viaggio dal database.
    suspend fun deleteTrip(trip: Trip) {
        tripDao.delete(trip)
    }


     //ottiene un viaggio specifico tramite ID (senza Flow).
    suspend fun getTripById(tripId: Int): Trip? {
        return tripDao.getTripById(tripId)
    }


    // OPERAZIONI SULLE NOTE (TripNote)

    //ottiene tutte le note di un viaggio specifico.
     //Le note sono ordinate dalla più recente alla più vecchia.
    fun getNotesForTrip(tripId: Int): Flow<List<TripNote>> {
        return tripNoteDao.getNotesForTrip(tripId)
    }

    //ottiene le ultime N note di un viaggio.
     //Utile per mostrare un'anteprima durante il tracking.
    fun getRecentNotesForTrip(tripId: Int, limit: Int = 3): Flow<List<TripNote>> {
        return tripNoteDao.getRecentNotesForTrip(tripId, limit)
    }

    //inserisce una nuova nota nel database.
    suspend fun insertNote(note: TripNote): Long {
        return tripNoteDao.insert(note)
    }

    //aggiorna una nota esistente.
    suspend fun updateNote(note: TripNote) {
        tripNoteDao.update(note)
    }

    //cancella una nota dal database.
    suspend fun deleteNote(note: TripNote) {
        tripNoteDao.delete(note)
    }

    // Ottiene il numero di note per un viaggio.
     //Utile per mostrare badge nella lista viaggi
    suspend fun getNotesCount(tripId: Int): Int {
        return tripNoteDao.getNotesCount(tripId)
    }

    //ottiene una singola nota tramite ID.
    suspend fun getNoteById(noteId: Int): TripNote? {
        return tripNoteDao.getNoteById(noteId)
    }

    //cancella tutte le note di un viaggio specifico.
    suspend fun deleteAllNotesForTrip(tripId: Int) {
        tripNoteDao.deleteAllNotesForTrip(tripId)
    }

    //  OPERAZIONI SULLE FOTO (TripPhoto)
    // Ottiene tutte le foto di un viaggio specifico.
     // Le foto sono ordinate dalla più recente alla più vecchia.
    fun getPhotosForTrip(tripId: Int): Flow<List<TripPhoto>> {
        return tripPhotoDao.getPhotosForTrip(tripId)
    }

    //Ottiene le ultime N foto di un viaggio.
     // Utile per mostrare un'anteprima durante il tracking.
    fun getRecentPhotosForTrip(tripId: Int, limit: Int = 3): Flow<List<TripPhoto>> {
        return tripPhotoDao.getRecentPhotosForTrip(tripId, limit)
    }

    //Ottiene tutte le foto con coordinate GPS di un viaggio.
     // Utile per visualizzarle su una mappa.
    fun getPhotosWithLocation(tripId: Int): Flow<List<TripPhoto>> {
        return tripPhotoDao.getPhotosWithLocation(tripId)
    }

    //Inserisce una nuova foto nel database.
    suspend fun insertPhoto(photo: TripPhoto): Long {
        return tripPhotoDao.insert(photo)
    }

    //Aggiorna una foto esistente.
     // Utile per modificare la didascalia.
    suspend fun updatePhoto(photo: TripPhoto) {
        tripPhotoDao.update(photo)
    }

    //Cancella una foto dal database.
     //NOTA: Non elimina il file fisico, solo il record dal database.
    suspend fun deletePhoto(photo: TripPhoto) {
        tripPhotoDao.delete(photo)
    }

    //Ottiene il numero di foto per un viaggio.
     // Utile per mostrare badge nella lista viaggi (es: "📷 12").
    suspend fun getPhotosCount(tripId: Int): Int {
        return tripPhotoDao.getPhotosCount(tripId)
    }

    //Ottiene una singola foto tramite ID.
    suspend fun getPhotoById(photoId: Int): TripPhoto? {
        return tripPhotoDao.getPhotoById(photoId)
    }

    //Cancella tutte le foto di un viaggio specifico.
     //NOTA: Non elimina i file fisici, solo i record dal database.
    suspend fun deleteAllPhotosForTrip(tripId: Int) {
        tripPhotoDao.deleteAllPhotosForTrip(tripId)
    }
}