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

    /**
     * Ottiene tutte le foto di un viaggio specifico.
     * Le foto sono ordinate dalla più recente alla più vecchia (timestamp DESC).
     *
     * @param tripId ID del viaggio
     * @return Flow che emette la lista aggiornata delle foto
     */
    @Query("SELECT * FROM trip_photos WHERE tripId = :tripId ORDER BY timestamp DESC")
    fun getPhotosForTrip(tripId: Int): Flow<List<TripPhoto>>

    /**
     * Ottiene una singola foto tramite ID.
     *
     * @param photoId ID della foto
     * @return La foto cercata o null se non esiste
     */
    @Query("SELECT * FROM trip_photos WHERE id = :photoId")
    suspend fun getPhotoById(photoId: Int): TripPhoto?

    /**
     * Inserisce una nuova foto nel database.
     *
     * @param photo Foto da inserire
     * @return L'ID della foto appena inserita
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: TripPhoto): Long

    /**
     * Aggiorna una foto esistente.
     * Utile per modificare la didascalia dopo lo scatto.
     *
     * @param photo Foto con i dati aggiornati
     */
    @Update
    suspend fun update(photo: TripPhoto)

    /**
     * Cancella una foto dal database.
     * NOTA: Questo cancella solo il record dal database, non il file fisico.
     * Il file deve essere eliminato separatamente dal file system.
     *
     * @param photo Foto da cancellare
     */
    @Delete
    suspend fun delete(photo: TripPhoto)

    /**
     * Cancella tutte le foto di un viaggio specifico.
     * Utile per pulizia dati o test.
     *
     * @param tripId ID del viaggio
     */
    @Query("DELETE FROM trip_photos WHERE tripId = :tripId")
    suspend fun deleteAllPhotosForTrip(tripId: Int)

    /**
     * Conta il numero di foto per un viaggio.
     * Utile per mostrare badge nella lista viaggi.
     *
     * @param tripId ID del viaggio
     * @return Numero di foto
     */
    @Query("SELECT COUNT(*) FROM trip_photos WHERE tripId = :tripId")
    suspend fun getPhotosCount(tripId: Int): Int

    /**
     * Ottiene le ultime N foto di un viaggio.
     * Utile per mostrare un'anteprima durante il tracking.
     *
     * @param tripId ID del viaggio
     * @param limit Numero massimo di foto da restituire
     * @return Flow con le ultime foto
     */
    @Query("SELECT * FROM trip_photos WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPhotosForTrip(tripId: Int, limit: Int = 3): Flow<List<TripPhoto>>

    /**
     * Ottiene tutte le foto con coordinate GPS.
     * Utile per visualizzarle su una mappa.
     *
     * @param tripId ID del viaggio
     * @return Flow con le foto che hanno coordinate valide
     */
    @Query("SELECT * FROM trip_photos WHERE tripId = :tripId AND latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY timestamp DESC")
    fun getPhotosWithLocation(tripId: Int): Flow<List<TripPhoto>>
}