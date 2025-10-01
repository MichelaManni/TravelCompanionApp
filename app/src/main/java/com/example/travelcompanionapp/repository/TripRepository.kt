package com.example.travelcompanionapp.repository

import com.example.travelcompanionapp.data.Trip
import com.example.travelcompanionapp.data.TripDao

/**
 * Repository per gestire le operazioni sui dati dei viaggi.
 *
 * Questo strato (layer) astrae la fonte dei dati (attualmente solo il database Room)
 * dal resto dell'applicazione (ViewModel).
 *
 * @param tripDao Il Data Access Object (DAO) fornito, necessario per interagire con Room.
 */
class TripRepository(private val tripDao: TripDao) {

    // Ottiene tutti i viaggi come un Flow, garantendo aggiornamenti in tempo reale all'UI.
    // L'uso di Flow è standard con Jetpack Compose e Room per la reattività.
    val allTrips = tripDao.getAllTrips()

    // Ottiene solo i viaggi completati per le statistiche.
    val completedTrips = tripDao.getCompletedTrips()

    /**
     * Inserisce un nuovo viaggio nel database.
     * Deve essere chiamato da una coroutine (o blocco 'suspend') perché l'operazione
     * di scrittura su DB è potenzialmente lenta e non può bloccare il thread principale.
     */
    suspend fun insertTrip(trip: Trip) {
        tripDao.insert(trip)
    }

    /**
     * Aggiorna un viaggio esistente nel database.
     * Funzione di sospensione per l'esecuzione asincrona.
     */
    suspend fun updateTrip(trip: Trip) {
        tripDao.update(trip)
    }

    /**
     * Cancella un viaggio dal database.
     * Funzione di sospensione per l'esecuzione asincrona.
     */
    suspend fun deleteTrip(trip: Trip) {
        tripDao.delete(trip)
    }

    /**
     * Ottiene un viaggio specifico tramite ID.
     * Funzione di sospensione per l'esecuzione asincrona.
     */
    suspend fun getTripById(id: Int): Trip? {
        return tripDao.getTripById(id)
    }
}
