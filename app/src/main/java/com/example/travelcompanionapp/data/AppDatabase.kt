package com.example.travelcompanionapp.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import java.util.Date

/**
 * La classe del database principale dell'applicazione, che estende RoomDatabase.
 *
 * ⭐ AGGIORNAMENTO VERSIONE 4 -> 5:
 * - Aggiunta nuova tabella "trip_photos" per le foto durante il viaggio
 *
 * STORICO VERSIONI:
 * - v1: Versione iniziale
 * - v2: Aggiunte coordinate GPS
 * - v3: Aggiunto campo notes
 * - v4: Sistema note durante viaggio (notes→description + TripNote)
 * - v5: Sistema foto durante viaggio (TripPhoto)
 */
@Database(
    entities = [Trip::class, TripNote::class, TripPhoto::class], // ⭐ Aggiunta TripPhoto
    version = 5, // ⭐ INCREMENTATA DA 4 A 5
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun tripNoteDao(): TripNoteDao
    abstract fun tripPhotoDao(): TripPhotoDao // ⭐ Nuovo DAO per le foto

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        // === MIGRAZIONE DA VERSIONE 2 A VERSIONE 3 ===
        // Aggiunge il campo "notes" alla tabella "trips"
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE trips ADD COLUMN notes TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // === MIGRAZIONE DA VERSIONE 3 A VERSIONE 4 ===
        // Questa migrazione:
        // 1. Rinomina "notes" in "description" nella tabella trips
        // 2. Crea la nuova tabella "trip_notes" per le note durante il viaggio
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {

                // PASSO 1: Creare tabella temporanea con la nuova struttura
                database.execSQL("""
                    CREATE TABLE trips_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        destination TEXT NOT NULL,
                        destinationLat REAL,
                        destinationLng REAL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        tripType TEXT NOT NULL,
                        totalDistanceKm REAL NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())

                // PASSO 2: Copiare i dati dalla vecchia alla nuova tabella
                // "notes" viene copiato in "description"
                database.execSQL("""
                    INSERT INTO trips_new (
                        id, destination, destinationLat, destinationLng,
                        startDate, endDate, tripType, totalDistanceKm,
                        isCompleted, status, description
                    )
                    SELECT 
                        id, destination, destinationLat, destinationLng,
                        startDate, endDate, tripType, totalDistanceKm,
                        isCompleted, status, notes
                    FROM trips
                """.trimIndent())

                // PASSO 3: Eliminare la vecchia tabella
                database.execSQL("DROP TABLE trips")

                // PASSO 4: Rinominare la nuova tabella
                database.execSQL("ALTER TABLE trips_new RENAME TO trips")

                // PASSO 5: Creare la nuova tabella trip_notes
                database.execSQL("""
                    CREATE TABLE trip_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tripId INTEGER NOT NULL,
                        text TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        locationName TEXT,
                        FOREIGN KEY(tripId) REFERENCES trips(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // PASSO 6: Creare indice per velocizzare le query su tripId
                database.execSQL("""
                    CREATE INDEX index_trip_notes_tripId ON trip_notes(tripId)
                """.trimIndent())
            }
        }

        // === ⭐ MIGRAZIONE DA VERSIONE 4 A VERSIONE 5 ===
        // Questa migrazione:
        // 1. Crea la nuova tabella "trip_photos" per le foto durante il viaggio
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // PASSO 1: Creare la nuova tabella trip_photos
                database.execSQL("""
                    CREATE TABLE trip_photos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tripId INTEGER NOT NULL,
                        filePath TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        caption TEXT,
                        latitude REAL,
                        longitude REAL,
                        locationName TEXT,
                        FOREIGN KEY(tripId) REFERENCES trips(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // PASSO 2: Creare indice per velocizzare le query su tripId
                database.execSQL("""
                    CREATE INDEX index_trip_photos_tripId ON trip_photos(tripId)
                """.trimIndent())
            }
        }

        /**
         * Funzione per ottenere l'unica istanza del database (Singleton Pattern).
         * Se l'istanza è null, la crea; altrimenti restituisce quella esistente.
         */
        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "trip_database"
                )
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5 // ⭐ Aggiunta nuova migrazione
                    )
                    .build()
                    .also { Instance = it }
            }
        }
    }
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