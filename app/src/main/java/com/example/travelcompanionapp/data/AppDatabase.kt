package com.example.travelcompanionapp.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import android.content.Context
import java.util.Date
/**
 * La classe del database principale dell'applicazione, che estende RoomDatabase.
 *
 * Il parametro 'entities' elenca tutte le Entity (tabelle) del database.
 * 'version' deve essere incrementato ogni volta che la struttura delle Entity cambia.
 * Si usano TypeConverters per permettere a Room di salvare oggetti non primitivi (come Date).
 */
@Database(entities = [Trip::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class) // Collega i TypeConverters al database
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    // === AGGIUNTA DEL SINGLETON QUI SOTTO ===
    companion object {
        // L'annotazione @Volatile assicura che il valore sia sempre aggiornato
        // e lo rende immediatamente visibile a tutti i thread.
        @Volatile
        private var Instance: AppDatabase? = null

        // Funzione per ottenere l'unica istanza del database.
        fun getDatabase(context: Context): AppDatabase {
            // Se l'istanza è null, la crea, altrimenti restituisce quella esistente.
            return Instance ?: synchronized(this) {
                // Costruisce il database
                Room.databaseBuilder(context, AppDatabase::class.java, "trip_database")
                    // NOTA: Con il Singleton non serve più .allowMainThreadQueries()
                    // perché le query Room avverranno su un thread in background di Room/Coroutine.
                    .build()
                    .also { Instance = it }
            }
        }
    }
    // === FINE AGGIUNTA DEL SINGLETON ===
}

/**
 * Type Converters per istruire Room su come convertire tipi di oggetti complessi
 * (come java.util.Date) in tipi che il database può comprendere (come Long/numeri).
 */
class Converters {
    // Converte un Long (timestamp) in un oggetto Date
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    // Converte un oggetto Date in un Long (timestamp)
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
