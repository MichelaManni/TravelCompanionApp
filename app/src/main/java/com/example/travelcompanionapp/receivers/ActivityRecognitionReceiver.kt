package com.example.travelcompanionapp.receivers //"receivers" sono componenti Android che ricevono notifiche dal sistema

import android.content.BroadcastReceiver //componente base per ricevere eventi di sistema
import android.content.Context //Context per accedere alle risorse dell'applicazione
import android.content.Intent //oggetto che contiene i dati dell'evento ricevuto
import com.example.travelcompanionapp.utils.ActivityRecognitionHelper
import com.example.travelcompanionapp.utils.NotificationHelper
import com.google.android.gms.location.ActivityTransitionResult //classe di Google Play Services che contiene i risultati del riconoscimento attività

/**
 * BroadcastReceiver che riceve gli eventi dall'Activity Recognition API.
 *
 * COS'È UN BROADCASTRECEIVER?
 * Oggetto che riceve messaggi dal sistema operativo Android.
 * In questo caso, riceve notifiche quando l'utente inizia un'attività fisica
 * (camminare, correre, guidare, andare in bici).
 *
 * COME FUNZIONA?
 * 1. L'app si registra per ricevere notifiche di movimento
 * 2. Google Play Services monitora i sensori del telefono in background
 * 3. Quando rileva che l'utente ha iniziato a muoversi, invia un Intent
 * 4. Questo BroadcastReceiver riceve l'Intent e lo processa
 * 5. L'app mostra una notifica per chiedere se vuole tracciare
 *
 * Viene chiamato automaticamente dal sistema quando l'utente inizia
 * un'attività monitorata (camminare, guidare, andare in bici).
 *
 */
class ActivityRecognitionReceiver : BroadcastReceiver() { //estende BroadcastReceiver, la classe base per ricevere broadcast dal sistema


    //Metodo chiamato quando arriva un evento di transizione.
    // Parametri:
    //   - context: permette di accedere alle risorse dell'app (database, preferences, ecc.)
    //   - intent: contiene i dati dell'evento (che attività è stata rilevata)

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
                    // event.activityType = codice numerico che identifica l'attività
                    //
                    // CODICI DELL'API:
                    // - 0 = IN_VEHICLE (in veicolo, guidando)
                    // - 1 = ON_BICYCLE (in bicicletta)
                    // - 2 = ON_FOOT (a piedi, camminando o correndo)
                    // - 3 = STILL (fermo)
                    // - 7 = WALKING (camminando)
                    // - 8 = RUNNING (correndo)

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

                        }
                    }
                }
            }
        }
    }

}