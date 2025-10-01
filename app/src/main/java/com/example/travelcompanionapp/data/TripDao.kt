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
@Dao
interface TripDao {

    // Ottiene tutti i viaggi, ordinati per data di inizio (più recenti in cima).
    // Usiamo Flow per gli aggiornamenti in tempo reale nell'interfaccia utente di Compose.
    @Query("SELECT * FROM trips ORDER BY startDate DESC")
    fun getAllTrips(): Flow<List<Trip>>

    // Ottiene un singolo viaggio tramite ID.
    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Int): Trip?

    // Inserisce un nuovo viaggio. Se c'è un conflitto, ignora.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(trip: Trip)

    // Aggiorna un viaggio esistente.
    @Update
    suspend fun update(trip: Trip)

    // Cancella un viaggio.
    @Delete
    suspend fun delete(trip: Trip)

    // Ottiene i viaggi completati per le statistiche (Display Charts).
    @Query("SELECT * FROM trips WHERE isCompleted = 1 ORDER BY startDate DESC")
    fun getCompletedTrips(): Flow<List<Trip>>
}
