package com.example.travelcompanionapp

import android.app.Application
import android.content.Context
import androidx.work.*
import com.example.travelcompanionapp.data.AppDatabase
import com.example.travelcompanionapp.repository.TripRepository
import com.example.travelcompanionapp.utils.ActivityRecognitionHelper
import com.example.travelcompanionapp.utils.NotificationHelper
import com.example.travelcompanionapp.workers.ReminderWorker
import java.util.concurrent.TimeUnit

/**
 * Classe Application personalizzata.
 *
 * ⭐ AGGIORNATA per supportare Background Jobs:
 * 1. Crea canali di notifica
 * 2. Avvia WorkManager per reminder periodici
 * 3. Avvia Activity Recognition per rilevare movimento
 */
class TravelCompanionApplication : Application() {

    // Database e Repository (già esistenti)
    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        TripRepository(
            tripDao = database.tripDao(),
            tripNoteDao = database.tripNoteDao(),
            tripPhotoDao = database.tripPhotoDao()
        )
    }

    companion object {
        lateinit var context: Context
    }

    /**
     * Chiamato all'avvio dell'app (prima di qualsiasi Activity).
     * Qui inizializziamo tutti i servizi background.
     */
    override fun onCreate() {
        super.onCreate()
        context = applicationContext

        // ⭐ 1. Crea i canali di notifica (obbligatorio per Android 8+)
        NotificationHelper.createNotificationChannels(this)

        // ⭐ 2. Avvia il WorkManager per i reminder periodici
        setupReminderWorker()

        // ⭐ 3. Avvia l'Activity Recognition per rilevare movimento
        setupActivityRecognition()
    }

    /**
     * Configura il WorkManager per eseguire ReminderWorker ogni 24 ore.
     *
     * Il worker controlla se l'utente ha viaggi recenti e invia
     * una notifica reminder se necessario.
     */
    private fun setupReminderWorker() {
        // Definisce i vincoli per l'esecuzione del worker
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Non serve connessione
            .setRequiresBatteryNotLow(true) // Esegui solo se batteria ok
            .build()

        // Crea una richiesta periodica (ogni 24 ore)
        val reminderWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            repeatInterval = 24, // Ogni 24 ore
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS) // Prima esecuzione dopo 1 ora
            .build()

        // Registra il worker con WorkManager
        // KEEP = mantiene il worker anche se l'app viene aggiornata
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TripReminderWorker", // Nome univoco
            ExistingPeriodicWorkPolicy.KEEP, // Non sovrascrive se esiste già
            reminderWorkRequest
        )

        println("✅ ReminderWorker configurato (ogni 24 ore)")
    }

    /**
     * Avvia l'Activity Recognition per rilevare automaticamente movimento.
     *
     * Il sistema invierà notifiche quando rileva che l'utente
     * sta camminando, guidando o andando in bici.
     */
    private fun setupActivityRecognition() {
        // Controlla se ha i permessi necessari
        if (ActivityRecognitionHelper.hasActivityRecognitionPermission(this)) {
            // Avvia il monitoraggio
            val started = ActivityRecognitionHelper.startActivityRecognition(this)

            if (started) {
                println("✅ Activity Recognition avviato")
            } else {
                println("❌ Activity Recognition non avviato (permessi mancanti)")
            }
        } else {
            println("⚠️ Activity Recognition: permesso non concesso")
            // Il permesso verrà richiesto quando l'utente apre l'app
        }
    }
}