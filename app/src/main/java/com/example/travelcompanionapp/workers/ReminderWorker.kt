package com.example.travelcompanionapp.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.travelcompanionapp.data.AppDatabase
import com.example.travelcompanionapp.utils.NotificationHelper
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Worker che controlla periodicamente se l'utente ha viaggi recenti.
 *
 * Se l'ultimo viaggio è stato registrato più di 3 giorni fa,
 * invia una notifica reminder per invogliare l'utente a registrare nuovi viaggi.
 *
 * Eseguito da WorkManager ogni 24 ore.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    /**
     * Metodo principale eseguito dal Worker.
     *
     * Return:
     * - Result.success() se il lavoro è completato con successo
     * - Result.retry() se c'è stato un errore temporaneo
     * - Result.failure() se c'è stato un errore permanente
     */
    override suspend fun doWork(): Result {
        return try {
            // Ottiene il database
            val database = AppDatabase.getDatabase(applicationContext)
            val tripDao = database.tripDao()

            // Ottiene tutti i viaggi dal database
            // Nota: qui usiamo una query sincrona (non Flow)
            // perché WorkManager è già in background
            val allTrips = tripDao.getAllTripsSync()

            // Se non ci sono viaggi, non fare nulla
            if (allTrips.isEmpty()) {
                return Result.success()
            }

            // Trova l'ultimo viaggio completato (per data di fine)
            val lastTrip = allTrips
                .filter { it.isCompleted } // Solo viaggi completati
                .maxByOrNull { it.endDate } // Prende quello con data fine più recente

            // Se non ci sono viaggi completati, non fare nulla
            if (lastTrip == null) {
                return Result.success()
            }

            // Calcola quanti giorni sono passati dall'ultimo viaggio
            val currentTime = System.currentTimeMillis()
            val lastTripTime = lastTrip.endDate.time
            val daysSinceLastTrip = TimeUnit.MILLISECONDS.toDays(currentTime - lastTripTime)

            // Se sono passati più di 3 giorni, invia la notifica
            if (daysSinceLastTrip >= 3) {
                // Controlla che l'app abbia il permesso per le notifiche
                if (NotificationHelper.hasNotificationPermission(applicationContext)) {
                    NotificationHelper.sendTripReminderNotification(
                        context = applicationContext,
                        daysSinceLastTrip = daysSinceLastTrip.toInt()
                    )
                }
            }

            // Lavoro completato con successo
            Result.success()

        } catch (e: Exception) {
            // Se c'è un errore, prova a ripetere il lavoro
            e.printStackTrace()
            Result.retry()
        }
    }
}

/**
 * NOTA: Questa funzione di estensione è necessaria perché TripDao
 * ha solo metodi Flow (reattivi), ma WorkManager ha bisogno di una
 * query sincrona. Aggiungi questo metodo a TripDao:
 *
 * @Query("SELECT * FROM trips ORDER BY startDate DESC")
 * suspend fun getAllTripsSync(): List<Trip>
 */