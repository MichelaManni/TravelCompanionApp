package com.example.travelcompanionapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) per l'entità Trip.
 * Definisce i metodi che il resto dell'app utilizzerà per leggere e scrivere i dati.
 */
@Dao//contrassegna l'interfaccia come un DAO di Room
interface TripDao {

    // Ottiene tutti i viaggi, ordinati per data di inizio (più recenti in cima).
    // Flow per gli aggiornamenti in tempo reale nell'interfaccia utente di Compose.
    @Query("SELECT * FROM trips ORDER BY startDate DESC")// Query SQL per selezionare tutti i viaggi ordinati per data di inizio decrescente
    fun getAllTrips(): Flow<List<Trip>>// Funzione che restituisce un Flow di liste di Trip (reattività)

    // Ottiene un singolo viaggio tramite ID.
    @Query("SELECT * FROM trips WHERE id = :tripId")// Query SQL per selezionare il viaggio con un ID specifico
    suspend fun getTripById(tripId: Int): Trip?// Funzione di sospensione (async) che restituisce un singolo Trip o null

    // Inserisce un nuovo viaggio. Se c'è un conflitto, ignora.
    @Insert(onConflict = OnConflictStrategy.IGNORE)//per l'inserimento, ignora se la chiave primaria è già presente
    suspend fun insert(trip: Trip)// Funzione di sospensione per l'inserimento asincrono di un Trip

    // Aggiorna un viaggio esistente.
    @Update
    suspend fun update(trip: Trip) // Funzione di sospensione per l'aggiornamento asincrono di un Trip

    // Cancella un viaggio.
    @Delete
    suspend fun delete(trip: Trip)// Funzione di sospensione per la cancellazione asincrona di un Trip

    // Ottiene i viaggi completati per le statistiche (Display Charts).
    @Query("SELECT * FROM trips WHERE isCompleted = 1 ORDER BY startDate DESC") // Query SQL per selezionare solo i viaggi con isCompleted = true (1 in SQLite)
    fun getCompletedTrips(): Flow<List<Trip>>// Funzione che restituisce un Flow di liste di Trip completati

    /**
     * Ottiene tutti i viaggi in modo sincrono (per uso in WorkManager).
     *
     * A differenza di getAllTrips() che restituisce un Flow (reattivo),
     * questa funzione restituisce direttamente la lista dei viaggi.
     *
     * Usata da ReminderWorker per controllare l'ultimo viaggio.
     */
    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    suspend fun getAllTripsSync(): List<Trip>
}
