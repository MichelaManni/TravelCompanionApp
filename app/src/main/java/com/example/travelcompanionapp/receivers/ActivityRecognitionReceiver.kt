package com.example.travelcompanionapp.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.travelcompanionapp.utils.ActivityRecognitionHelper
import com.example.travelcompanionapp.utils.NotificationHelper
import com.google.android.gms.location.ActivityTransitionResult

/**
 * BroadcastReceiver che riceve gli eventi dall'Activity Recognition API.
 *
 * Viene chiamato automaticamente dal sistema quando l'utente inizia
 * un'attività monitorata (camminare, guidare, andare in bici).
 *
 * Conforme alle specifiche:
 * "The app detects when the user is moving and prompts them to start logging"
 */
class ActivityRecognitionReceiver : BroadcastReceiver() {

    /**
     * Metodo chiamato quando arriva un evento di transizione.
     *
     * @param context Contesto dell'applicazione
     * @param intent Intent contenente i dati dell'evento
     */
    override fun onReceive(context: Context, intent: Intent) {
        // Controlla se l'intent contiene risultati di transizione
        if (ActivityTransitionResult.hasResult(intent)) {
            // Estrae il risultato dall'intent
            val result = ActivityTransitionResult.extractResult(intent)

            // result può essere null in alcuni casi
            result?.let { transitionResult ->
                // Itera su tutti gli eventi di transizione ricevuti
                for (event in transitionResult.transitionEvents) {

                    // Ottiene il tipo di attività rilevata
                    val activityType = event.activityType

                    // Ottiene il tipo di transizione (ENTER = inizia, EXIT = finisce)
                    val transitionType = event.transitionType

                    // Converte il codice in nome leggibile
                    val activityName = ActivityRecognitionHelper.getActivityName(activityType)

                    // Log per debug
                    println("🚶 Attività rilevata: $activityName (tipo: $transitionType)")

                    // Se è una transizione ENTER (l'utente ha INIZIATO l'attività)
                    if (transitionType == com.google.android.gms.location.ActivityTransition.ACTIVITY_TRANSITION_ENTER) {

                        // Controlla che l'app abbia il permesso per le notifiche
                        if (NotificationHelper.hasNotificationPermission(context)) {

                            // Invia una notifica per chiedere se vuole tracciare
                            NotificationHelper.sendActivityDetectedNotification(
                                context = context,
                                activityType = activityName
                            )

                            // Salva nelle SharedPreferences l'ultima attività rilevata
                            // (utile per mostrare suggerimenti nell'UI)
                            saveLastDetectedActivity(context, activityName)
                        }
                    }
                }
            }
        }
    }

    /**
     * Salva l'ultima attività rilevata nelle SharedPreferences.
     *
     * Questo permette all'app di mostrare suggerimenti personalizzati
     * quando l'utente apre la schermata di tracking.
     *
     * @param context Contesto dell'applicazione
     * @param activityName Nome dell'attività rilevata
     */
    private fun saveLastDetectedActivity(context: Context, activityName: String) {
        val sharedPrefs = context.getSharedPreferences("travel_companion_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("last_detected_activity", activityName)
            putLong("last_activity_time", System.currentTimeMillis())
            apply()
        }
    }

    /**
     * FUNZIONE HELPER: Legge l'ultima attività rilevata.
     * Puoi usarla in TripTrackingScreen per mostrare suggerimenti.
     *
     * Esempio:
     * "Abbiamo rilevato che stai guidando. Vuoi iniziare il tracking?"
     */
    companion object {
        fun getLastDetectedActivity(context: Context): Pair<String?, Long>? {
            val sharedPrefs = context.getSharedPreferences("travel_companion_prefs", Context.MODE_PRIVATE)
            val activity = sharedPrefs.getString("last_detected_activity", null)
            val time = sharedPrefs.getLong("last_activity_time", 0)

            return if (activity != null && time > 0) {
                Pair(activity, time)
            } else {
                null
            }
        }
    }
}