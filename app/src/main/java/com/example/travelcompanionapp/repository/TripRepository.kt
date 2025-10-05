package com.example.travelcompanionapp.repository

import com.example.travelcompanionapp.data.Trip
import com.example.travelcompanionapp.data.TripDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository per gestire le operazioni sui dati dei viaggi.
 */
class TripRepository(private val tripDao: TripDao) {

    // ⭐ CORREZIONE: Implementa la funzione che il ViewModel si aspetta.
    // Ritorna un Flow di tutti i viaggi, garantendo aggiornamenti in tempo reale all'UI.
    fun getAllTripsStream(): Flow<List<Trip>> {
        return tripDao.getAllTrips()
    }

    // Ottiene solo i viaggi completati per le statistiche.
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
}