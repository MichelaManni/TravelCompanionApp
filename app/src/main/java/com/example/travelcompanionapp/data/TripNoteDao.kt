package com.example.travelcompanionapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) per l'entità TripNote.
 *
 * Definisce le operazioni CRUD (Create, Read, Update, Delete) per le note
 * aggiunte durante i viaggi.
 */
@Dao
interface TripNoteDao {

    /**
     * Ottiene tutte le note di un viaggio specifico.
     * Le note sono ordinate dalla più recente alla più vecchia (timestamp DESC).
     *
     * @param tripId ID del viaggio
     * @return Flow che emette la lista aggiornata delle note
     */
    @Query("SELECT * FROM trip_notes WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getNotesForTrip(tripId: Int): Flow<List<TripNote>>

    /**
     * Ottiene una singola nota tramite ID.
     *
     * @param noteId ID della nota
     * @return La nota cercata o null se non esiste
     */
    @Query("SELECT * FROM trip_notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Int): TripNote?

    /**
     * Inserisce una nuova nota nel database.
     *
     * @param note Nota da inserire
     * @return L'ID della nota appena inserita
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: TripNote): Long

    /**
     * Aggiorna una nota esistente.
     *
     * @param note Nota con i dati aggiornati
     */
    @Update
    suspend fun update(note: TripNote)

    /**
     * Cancella una nota dal database.
     *
     * @param note Nota da cancellare
     */
    @Delete
    suspend fun delete(note: TripNote)

    /**
     * Cancella tutte le note di un viaggio specifico.
     * Utile per pulizia dati o test.
     *
     * @param tripId ID del viaggio
     */
    @Query("DELETE FROM trip_notes WHERE tripId = :tripId")
    suspend fun deleteAllNotesForTrip(tripId: Int)

    /**
     * Conta il numero di note per un viaggio.
     * Utile per mostrare badge nella lista viaggi.
     *
     * @param tripId ID del viaggio
     * @return Numero di note
     */
    @Query("SELECT COUNT(*) FROM trip_notes WHERE tripId = :tripId")
    suspend fun getNotesCount(tripId: Int): Int

    /**
     * Ottiene le ultime N note di un viaggio.
     * Utile per mostrare un'anteprima durante il tracking.
     *
     * @param tripId ID del viaggio
     * @param limit Numero massimo di note da restituire
     * @return Flow con le ultime note
     */
    @Query("SELECT * FROM trip_notes WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentNotesForTrip(tripId: Int, limit: Int = 3): Flow<List<TripNote>>
}