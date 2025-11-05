package com.example.travelcompanionapp.utils

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.travelcompanionapp.receivers.ActivityRecognitionReceiver
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

/**
 * Helper per gestire l'Activity Recognition API di Google.
 *
 * Funzionalità:
 * - Rileva automaticamente quando l'utente inizia a muoversi (camminare, guidare, in bici)
 * - Invia notifiche per chiedere se vuole iniziare il tracking
 *
 * Conforme alle specifiche del progetto:
 * "Using the Activity Recognition API, the app detects when the user is moving
 *  and either prompts them to start logging a journey or begins recording automatically."
 */
object ActivityRecognitionHelper {

    // Client per l'Activity Recognition API
    private lateinit var activityRecognitionClient: ActivityRecognitionClient

    /**
     * Avvia il monitoraggio delle attività dell'utente.
     *
     * Rileva quando l'utente:
     * - Inizia a camminare (WALKING)
     * - Inizia a guidare (IN_VEHICLE)
     * - Inizia ad andare in bici (ON_BICYCLE)
     *
     * @param context Contesto dell'applicazione
     * @return true se avviato con successo, false se permessi mancanti
     */
    fun startActivityRecognition(context: Context): Boolean {
        // ⭐ CONTROLLO ESPLICITO DEI PERMESSI
        if (!hasActivityRecognitionPermission(context)) {
            println("❌ Permesso ACTIVITY_RECOGNITION non concesso")
            return false
        }

        // Inizializza il client
        activityRecognitionClient = ActivityRecognition.getClient(context)

        // Crea PendingIntent per ricevere gli aggiornamenti
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE // FLAG_MUTABLE perché riceve dati
        )

        // Definisce le transizioni da monitorare
        val transitions = listOf(
            // Rileva quando l'utente INIZIA a camminare
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),

            // Rileva quando l'utente INIZIA a guidare
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),

            // Rileva quando l'utente INIZIA ad andare in bici
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        // Crea la richiesta di transizioni
        val request = ActivityTransitionRequest(transitions)

        // ⭐ CHIAMATA CON TRY-CATCH PER GESTIRE SecurityException
        try {
            // Registra il listener per le transizioni
            activityRecognitionClient.requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    // Monitoraggio avviato con successo
                    println("✅ Activity Recognition avviato con successo")
                }
                .addOnFailureListener { e ->
                    // Errore nell'avvio
                    println("❌ Errore Activity Recognition: ${e.message}")
                    e.printStackTrace()
                }
        } catch (e: SecurityException) {
            // Gestisce l'eccezione se il permesso non è concesso
            println("❌ SecurityException: permesso non concesso")
            e.printStackTrace()
            return false
        }

        return true
    }

    /**
     * Ferma il monitoraggio delle attività.
     *
     * @param context Contesto dell'applicazione
     */
    fun stopActivityRecognition(context: Context) {
        // Controlla che il client sia stato inizializzato
        if (!::activityRecognitionClient.isInitialized) {
            println("⚠️ ActivityRecognitionClient non inizializzato")
            return
        }

        // ⭐ CONTROLLO ESPLICITO DEI PERMESSI
        if (!hasActivityRecognitionPermission(context)) {
            println("❌ Permesso ACTIVITY_RECOGNITION non concesso")
            return
        }

        val intent = Intent(context, ActivityRecognitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )

        // ⭐ CHIAMATA CON TRY-CATCH PER GESTIRE SecurityException
        try {
            // Rimuove il listener
            activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent)
                .addOnSuccessListener {
                    println("✅ Activity Recognition fermato con successo")
                }
                .addOnFailureListener { e ->
                    println("❌ Errore fermando Activity Recognition: ${e.message}")
                    e.printStackTrace()
                }
        } catch (e: SecurityException) {
            // Gestisce l'eccezione se il permesso non è concesso
            println("❌ SecurityException durante stop: permesso non concesso")
            e.printStackTrace()
        }
    }

    /**
     * Controlla se l'app ha il permesso per l'Activity Recognition.
     *
     * @param context Contesto dell'applicazione
     * @return true se il permesso è garantito, false altrimenti
     */
    fun hasActivityRecognitionPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: serve permesso ACTIVITY_RECOGNITION
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 9 e precedenti: non serve permesso
            true
        }
    }

    /**
     * Converte il tipo di attività rilevata in una stringa leggibile.
     *
     * @param activityType Codice dell'attività (es: DetectedActivity.WALKING)
     * @return Stringa descrittiva (es: "camminando")
     */
    fun getActivityName(activityType: Int): String {
        return when (activityType) {
            DetectedActivity.IN_VEHICLE -> "guidando"
            DetectedActivity.ON_BICYCLE -> "in bicicletta"
            DetectedActivity.WALKING -> "camminando"
            DetectedActivity.RUNNING -> "correndo"
            DetectedActivity.STILL -> "fermo"
            else -> "in movimento"
        }
    }
}