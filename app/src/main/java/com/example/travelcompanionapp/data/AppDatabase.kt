package com.example.travelcompanionapp.data //pacchetto data

import androidx.room.Database //import annotazione @Database di Room per definire il database
import androidx.room.Room //import classe Room che serve per creare e gestire il database
import androidx.room.RoomDatabase //import classe base RoomDatabase che il database deve estendere
import androidx.room.TypeConverter //annotazione per i convertitori di tipo personalizzati
import androidx.room.TypeConverters //annotazione per registrare i convertitori nel database
import androidx.room.migration.Migration //classe per definire le migrazioni del database (quando cambia la struttura)
import androidx.sqlite.db.SupportSQLiteDatabase //interfaccia del database SQLite sottostante
import android.content.Context //Context di Android (serve per accedere alle risorse dell'app)
import java.util.Date //per gestire data e orari

/**
 * Classe del database principale dell'applicazione, che estende RoomDatabase.
 * STORICO VERSIONI:
 * - v1: Versione iniziale
 * - v2: Aggiunte coordinate GPS
 * - v3: Aggiunto campo notes
 * - v4: Sistema note durante viaggio (notes→description + TripNote)
 * - v5: Sistema foto durante viaggio (TripPhoto)
 * - v6: Date effettive di tracking (actualStartDate, actualEndDate, totalTrackingDurationMs)
 */
@Database( //indica a Room che questa è la classe del database
    entities = [Trip::class, TripNote::class, TripPhoto::class], //lista delle tables nel databes
    version = 6, //numero di versione del databse attualmente a 6, ogni volta che si modifica struttura del database è da aumentare
    exportSchema = false //non serve esportare il database in un file
)
@TypeConverters(Converters::class) //indica a Room di usare classe Converters x convertire tipi di dati complessi
abstract class AppDatabase : RoomDatabase() { //abstract class che estende RoomDatabse, abstract perchè Room genererà automaticamente l'implementazione
    //restituisce il DAO(data access object) x i viaggi, DAO contiene i metodi x leggere / scrivere viaggi nel database
    abstract fun tripDao(): TripDao
    abstract fun tripNoteDao(): TripNoteDao //restituisce il DAO per le note dei viaggi
    abstract fun tripPhotoDao(): TripPhotoDao // restituisce il DAO per le foto dei viaggi

    companion object { // "companion object" è un oggetto singleton associato alla classe
        //contiene metodi e proprietà "statici" accessibili senza creare un'istanza
        @Volatile  //annotazione che garantisce che la variabile sia sempre aggiornata tra thread diversi
        //importante x il pattern Singleton in ambiente multi-thread
        private var Instance: AppDatabase? = null
        // Variabile che contiene l'unica istanza del database (Singleton Pattern)
        // "private" = accessibile solo da questa classe
        // "var" = variabile modificabile
        // "AppDatabase?" = può essere null all'inizio

        //MIGRAZIONE DA VERS 2 A VERS 3===
        // Aggiunge il campo "notes" alla tabella "trips"
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            // crea un oggetto Migration che va dalla versione 2 alla versione 3
            // "object :" crea un'istanza anonima di Migration
            // (2, 3) = da versione 2 a versione 3

            override fun migrate(database: SupportSQLiteDatabase) {
                // sovrascrive il metodo migrate che contiene le istruzioni SQL per la migrazione
                // "database" è il database su cui eseguire le modifiche


                database.execSQL(
                    // esegue un comando SQL direttamente sul database
                    "ALTER TABLE trips ADD COLUMN notes TEXT NOT NULL DEFAULT ''"
                    // SQL: Aggiunge una nuova colonna chiamata "notes" alla tabella "trips"
                    // - ALTER TABLE trips = modifica la tabella "trips"
                    // - ADD COLUMN notes = aggiunge una colonna chiamata "notes"
                    // - TEXT = tipo di dato testo
                    // - NOT NULL = non può essere vuota (deve avere sempre un valore)
                    // - DEFAULT '' = valore predefinito è una stringa vuota
                )
            }
        }

        // MIGRAZIONE DA VERSIONE 3 A VERSIONE 4
        //prima notes era inteso come nota unica aggiunta nel momento di creazione di un nuovo viaggio (non conforme alle specifiche)
        //ora quel parametro diventa una descrizione del viaggio e vengono aggiunte le vere note
        // Questa migrazione:
        // 1. Rinomina "notes" in "description" nella tabella trips ->
        // 2. Crea la nuova tabella "trip_notes" per le note durante il viaggio
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {

                //PASSO 1: Creare tabella temporanea con la nuova struttura
                //crea nuova tabella chiamata "trips_new" con la struttura aggiornata
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

                // PASSO 2: copiare i dati dalla vecchia alla nuova tabella
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

                // PASSO 3:eliminare la vecchia tabella
                database.execSQL("DROP TABLE trips")

                // PASSO 4: rinominare la nuova tabella
                database.execSQL("ALTER TABLE trips_new RENAME TO trips")

                // PASSO 5: creare la nuova tabella trip_notes
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

                // PASSO 6: creare indice per velocizzare le query su tripId
                database.execSQL("""
                    CREATE INDEX index_trip_notes_tripId ON trip_notes(tripId)
                """.trimIndent())
                //crea un indice sulla colonna "tripId" della tabella "trip_notes"
                //indice velocizza le ricerche (es: "trova tutte le note del viaggio 5")
            }
        }

        // MIGRAZIONE DA VERSIONE 4 A VERSIONE 5
        // questa migrazione crea la nuova tabella "trip_photos" per le foto durante il viaggio
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) { //metodo con le istruzioni per la migrazione

                // PASSO 1: creare la nuova tabella trip_photos per le foto scattate durante i viaggi
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

                // PASSO 2: creare indice per velocizzare le query su tripId
                database.execSQL("""
                    CREATE INDEX index_trip_photos_tripId ON trip_photos(tripId)
                """.trimIndent())
            }
        }

        // MIGRAZIONE DA VERSIONE 5 A VERSIONE
        // Questa migrazione:
        // 1. aggiunge campi per le date effettive di tracking GPS
        // 2. aggiunge campo per la durata totale del tracking
        //
        // MOTIVAZIONE:
        // - startDate/endDate = date pianificate dall'utente
        // - actualStartDate/actualEndDate = quando il GPS era effettivamente attivo
        // - totalTrackingDurationMs = durata totale con GPS acceso
        //
        // questo permette di distinguere tra:
        // "Viaggio pianificato 10-15 Gen" vs "Effettivamente tracciato 12-13 Gen"
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // PASSO 1: Aggiunge campo per data inizio tracking effettivo
                // NULL = viaggio non ancora tracciato
                database.execSQL("""
                    ALTER TABLE trips 
                    ADD COLUMN actualStartDate INTEGER
                """.trimIndent())

                // PASSO 2: Aggiunge campo per data fine tracking effettivo
                // NULL = viaggio non completato o in pausa
                database.execSQL("""
                    ALTER TABLE trips 
                    ADD COLUMN actualEndDate INTEGER
                """.trimIndent())

                // PASSO 3: Aggiunge campo per durata totale tracking in millisecondi
                // DEFAULT 0 = nessun tracking ancora effettuato
                database.execSQL("""
                    ALTER TABLE trips 
                    ADD COLUMN totalTrackingDurationMs INTEGER NOT NULL DEFAULT 0
                """.trimIndent()) // memorizza per quanto tempo il GPS è stato attivo durante il viaggio
            }
        }


         // funzione c ottenere l'unica istanza del database (Singleton Pattern).
         //se l'istanza è null, la crea; altrimenti restituisce quella esistente.
        fun getDatabase(context: Context): AppDatabase {
             //funzione pubblica che restituisce l'istanza del database
             // "context" è il contesto dell'app Android (serve per accedere alle risorse)
             // restituisce un oggetto di tipo AppDatabase

            return Instance ?: synchronized(this) { //se Instance non è null, restituiscila; altrimenti esegui il blocco
                //synchronized(this) = blocca l'accesso multi-thread
                //solo un thread alla volta può eseguire questo codice
                //importante per evitare che due parti dell'app creino il database contemporaneamente

                Room.databaseBuilder( //crea builder per costruire database Room

                    context,//passa contesto dell'app
                    AppDatabase::class.java, //specifica la classe del databse
                    "trip_database" //nome del file del databse sul dispositivo
                )
                    .addMigrations( //aggiunge le migrazioni al database
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .build() //costruisce effettivamente il database con tutte le configurazioni
                    .also { Instance = it }  // "also" esegue un'azione aggiuntiva sull'oggetto appena creato
                //salva l'istanza appena creata nella variabile "Instance"
                //così la prossima volta restituiamo questa istanza senza ricrearla
            }
        }
    }
}


 //Type Converters per istruire Room su come convertire tipi di oggetti complessi
 // (come java.util.Date) in tipi che il database può comprendere (come Long/numeri).
class Converters {
    // Converte un Long (timestamp) in un oggetto Date
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? { //funzione che prende un Long (opzionale) e restituisce un Date (opzionale)
        // "Long?" = può essere null
        // "Date?" = può restituire null
        return value?.let { Date(it) } //se "value" non è null (?.), crea un nuovo oggetto Date con quel timestamp
        //altrimenti restituisce null
    }

    // Converte un oggetto Date in un Long (timestamp)
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {  //funzione che prende un Date (opzionale) e restituisce un Long (opzionale)
        return date?.time ///se "date" non è null, restituisce il timestamp (proprietà .time)
        //altrimenti restituisce null
    }
}