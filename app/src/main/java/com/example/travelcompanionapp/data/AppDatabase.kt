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
@Database(entities = [Trip::class], version = 2, exportSchema = false)// Configura il database: entità (Trip), versione (1), schema non esportato
@TypeConverters(Converters::class) // Collega i TypeConverters al database
abstract class AppDatabase : RoomDatabase() { // Definizione della classe astratta del database
    abstract fun tripDao(): TripDao  // Metodo astratto per ottenere l'istanza del DAO

    // AGGIUNTA DEL SINGLETON
    companion object { // Blocco companion object per il pattern Singleton
        // L'annotazione @Volatile assicura che il valore sia sempre aggiornato
        // e lo rende immediatamente visibile a tutti i thread.
        @Volatile
        private var Instance: AppDatabase? = null  // Istanza singola del database, inizialmente null

        // Funzione per ottenere l'unica istanza del database.
        fun getDatabase(context: Context): AppDatabase {  // Metodo statico per accedere al database
            // Se l'istanza è null, la crea, altrimenti restituisce quella esistente.
            return Instance ?: synchronized(this) { // Usa l'operatore Elvis (?:) per restituire 'Instance' se non null, altrimenti esegue il blocco synchronized
                // Il blocco synchronized previene l'accesso da più thread contemporaneamente
                // Costruisce il database
                Room.databaseBuilder(context, AppDatabase::class.java, "trip_database") // Crea il database builder
                    .build()// Costruisce l'istanza del database
                    .also { Instance = it }// Assegna l'istanza appena creata alla variabile 'Instance'
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
    @TypeConverter//definisce questo metodo come un Type Converter
    fun fromTimestamp(value: Long?): Date? {// Metodo per convertire Long (timestamp) in Date
        return value?.let { Date(it) }// Se 'value' non è null, crea una nuova Date con quel timestamp
    }

    // Converte un oggetto Date in un Long (timestamp)
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {// Metodo per convertire Date in Long (timestamp)
        return date?.time// Restituisce il timestamp (millisecondi) della Date, se non null
    }
}
