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

    //funzione principale che viene eseguita quando il worker parte
    override suspend fun doWork(): Result {
        return try {
            //ottiene un’istanza del database locale dell’app
            val database = AppDatabase.getDatabase(applicationContext)
            val tripDao = database.tripDao() //accede al dao dei viaggi

            //recupera tutti i viaggi dal database
            //usa una query sincrona perché il worker è già in un thread in background
            val allTrips = tripDao.getAllTripsSync()

            //se non esistono viaggi salvati, termina senza fare nulla
            if (allTrips.isEmpty()) {
                return Result.success()
            }

            //seleziona l’ultimo viaggio completato in base alla data di fine
            val lastTrip = allTrips
                .filter { it.isCompleted } //considera solo i viaggi contrassegnati come completati
                .maxByOrNull { it.endDate } //prende il più recente per data di fine

            //se non ci sono viaggi completati, termina senza notificare
            if (lastTrip == null) {
                return Result.success()
            }

            //calcola i giorni trascorsi dalla fine dell’ultimo viaggio
            val currentTime = System.currentTimeMillis()
            val lastTripTime = lastTrip.endDate.time
            val daysSinceLastTrip = TimeUnit.MILLISECONDS.toDays(currentTime - lastTripTime)

            //se sono passati almeno 3 giorni, valuta l’invio della notifica
            if (daysSinceLastTrip >= 3) {
                //controlla se l’app ha il permesso di inviare notifiche
                if (NotificationHelper.hasNotificationPermission(applicationContext)) {
                    //invoca la funzione helper per mostrare una notifica di promemoria
                    NotificationHelper.sendTripReminderNotification(
                        context = applicationContext,
                        daysSinceLastTrip = daysSinceLastTrip.toInt()
                    )
                }
            }

            //ritorna risultato di successo: il lavoro è stato completato correttamente
            Result.success()

        } catch (e: Exception) {
            //in caso di eccezioni logga l’errore e chiede al workmanager di riprovare
            e.printStackTrace()
            Result.retry()
        }
    }
}
