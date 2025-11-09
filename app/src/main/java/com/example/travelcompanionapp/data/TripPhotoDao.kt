package com.example.travelcompanionapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) per l'entità TripPhoto.
 *
 * Definisce le operazioni CRUD (Create, Read, Update, Delete) per le foto
 * scattate durante i viaggi.
 */
@Dao
interface TripPhotoDao {


     //Ottiene tutte le foto di un viaggio specifico.
     //Le foto sono ordinate dalla più recente alla più vecchia (timestamp DESC).
    @Query("SELECT * FROM trip_photos WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getPhotosForTrip(tripId: Int): Flow<List<TripPhoto>>


     //Ottiene una singola foto tramite ID.
    @Query("SELECT * FROM trip_photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: Int): TripPhoto?


     // Inserisce una nuova foto nel database.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: TripPhoto): Long

    // Aggiorna una foto esistente.
    @Update
    suspend fun update(photo: TripPhoto)


     //Cancella una foto dal database.
     //NOTA: Questo cancella solo il record dal database, non il file fisico.
    @Delete
    suspend fun delete(photo: TripPhoto)

    // Cancella tutte le foto di un viaggio specifico.
    @Query("DELETE FROM trip_photos WHERE tripId = :tripId")
    suspend fun deleteAllPhotosForTrip(tripId: Int)

     //Conta il numero di foto per un viaggio.
     //Utile per mostrare badge nella lista viaggi.
    @Query("SELECT COUNT(*) FROM trip_photos WHERE tripId = :tripId")
    suspend fun getPhotosCount(tripId: Int): Int


     // Ottiene le ultime N foto di un viaggio.
     //Utile per mostrare un'anteprima durante il tracking.
    @Query("SELECT * FROM trip_photos WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPhotosForTrip(tripId: Int, limit: Int = 3): Flow<List<TripPhoto>>

    //Ottiene tutte le foto con coordinate GPS.
     //Utile per visualizzarle su una mappa.
    @Query("SELECT * FROM trip_photos WHERE tripId = :tripId AND latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY timestamp DESC")
    fun getPhotosWithLocation(tripId: Int): Flow<List<TripPhoto>>
}