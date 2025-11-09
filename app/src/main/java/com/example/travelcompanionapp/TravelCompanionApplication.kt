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


//classe application personalizzata che viene creata prima di qualsiasi activity
//serve per inizializzare elementi condivisi come database, repository e servizi background
class TravelCompanionApplication : Application() {

    //crea il database una sola volta usando lazy per evitare inizializzazione prematura
    private val database by lazy { AppDatabase.getDatabase(this) }

    //repository principale che fornisce accesso ai dao dei viaggi, note e foto
    val repository by lazy {
        TripRepository(
            tripDao = database.tripDao(),
            tripNoteDao = database.tripNoteDao(),
            tripPhotoDao = database.tripPhotoDao()
        )
    }

    //companion object per mantenere un riferimento globale al context dell’app
    companion object {
        lateinit var context: Context
    }

    //funzione chiamata automaticamente all’avvio dell’app
    //qui vengono configurati i canali di notifica, i worker periodici e l’activity recognition
    override fun onCreate() {
        super.onCreate()
        context = applicationContext //salva il contesto globale

        //1. crea i canali di notifica richiesti da android 8 e superiori
        NotificationHelper.createNotificationChannels(this)

        //2. imposta il workmanager per eseguire reminder giornalieri
        setupReminderWorker()

        //3. avvia il rilevamento automatico delle attività fisiche
        setupActivityRecognition()
    }

    //funzione privata che configura un worker per inviare promemoria ogni 24 ore
    private fun setupReminderWorker() {
        //definisce i vincoli di esecuzione: nessuna rete richiesta e batteria non bassa
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        //crea la richiesta periodica per il worker reminderworker
        val reminderWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints) //applica i vincoli definiti sopra
            .setInitialDelay(1, TimeUnit.HOURS) //avvio ritardato di 1 ora dopo l’apertura dell’app
            .build()

        //registra il worker con un nome univoco per evitare duplicati
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TripReminderWorker",
            ExistingPeriodicWorkPolicy.KEEP, //mantiene il worker esistente se già registrato
            reminderWorkRequest
        )

        println("✅ ReminderWorker configurato (ogni 24 ore)")
    }

    //funzione che avvia il sistema di activity recognition per rilevare movimento dell’utente
    private fun setupActivityRecognition() {
        //verifica se l’app possiede il permesso necessario
        if (ActivityRecognitionHelper.hasActivityRecognitionPermission(this)) {
            //avvia il monitoraggio delle transizioni di attività (camminare, guidare, bici)
            val started = ActivityRecognitionHelper.startActivityRecognition(this)

            if (started) {
                println("✅ Activity Recognition avviato")
            } else {
                println("❌ Activity Recognition non avviato (permessi mancanti)")
            }
        } else {
            //informa nel log che il permesso non è ancora stato concesso
            println("⚠️ Activity Recognition: permesso non concesso")
            //il permesso verrà richiesto in mainactivity tramite dialog di sistema
        }
    }
}