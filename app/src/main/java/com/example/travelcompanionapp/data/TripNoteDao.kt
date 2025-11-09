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
 *definisce le operazioni CRUD (Create, Read, Update, Delete) per le note
 * aggiunte durante i viaggi.
 */
@Dao
interface TripNoteDao {

     //ottiene tutte le note di un viaggio specifico.
     //note sono ordinate dalla più recente alla più vecchia (timestamp DESC).
    @Query("SELECT * FROM trip_notes WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getNotesForTrip(tripId: Int): Flow<List<TripNote>>


     //Ottiene una singola nota tramite ID.
    @Query("SELECT * FROM trip_notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Int): TripNote?


      //inserisce una nuova nota nel database.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: TripNote): Long


     //aggiorna una nota esistente.
    @Update
    suspend fun update(note: TripNote)


     //cancella una nota dal database.
    @Delete
    suspend fun delete(note: TripNote)

    //cancella tutte le note di un viaggio specifico.
    @Query("DELETE FROM trip_notes WHERE tripId = :tripId")
    suspend fun deleteAllNotesForTrip(tripId: Int)

     //conta il numero di note per un viaggio.
    @Query("SELECT COUNT(*) FROM trip_notes WHERE tripId = :tripId")
    suspend fun getNotesCount(tripId: Int): Int


     //ottiene le ultime N note di un viaggio.
     //per mostrare un'anteprima durante il tracking.
    @Query("SELECT * FROM trip_notes WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentNotesForTrip(tripId: Int, limit: Int = 3): Flow<List<TripNote>>
}