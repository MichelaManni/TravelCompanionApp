package com.example.travelcompanionapp.utils
//definisce un oggetto singleton che gestisce il rilevamento automatico delle attività dell’utente tramite l’activity recognition api di google
//serve per individuare quando l’utente inizia a muoversi (camminare, guidare, andare in bici) e notificare l’app per proporre l’avvio del tracciamento
//
//funzioni principali:
//
//- startActivityRecognition(context): avvia il monitoraggio delle attività dell’utente, controlla i permessi, registra le transizioni (walk, drive, bicycle) e gestisce eventuali errori di sicurezza
//- stopActivityRecognition(context): interrompe il monitoraggio, rimuovendo gli aggiornamenti precedentemente registrati dal client
//- hasActivityRecognitionPermission(context): verifica se l’app dispone del permesso ACTIVITY_RECOGNITION
//- getActivityName(activityType): converte i codici delle attività rilevate (es. DetectedActivity.WALKING) in stringhe leggibili come “camminando”, “guidando”, ecc.

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
 */
object ActivityRecognitionHelper {

    //variabile che memorizza l’istanza del client dell’api di activity recognition di google
    private lateinit var activityRecognitionClient: ActivityRecognitionClient

    //funzione che avvia il rilevamento automatico delle attività dell’utente
    fun startActivityRecognition(context: Context): Boolean {
        //controlla se l’app dispone del permesso per utilizzare il riconoscimento attività
        if (!hasActivityRecognitionPermission(context)) {
            println("❌ Permesso ACTIVITY_RECOGNITION non concesso") //stampa messaggio di errore se manca il permesso
            return false //interrompe l’esecuzione
        }

        //inizializza il client che gestisce le richieste di riconoscimento attività
        activityRecognitionClient = ActivityRecognition.getClient(context)

        //crea un intent per ricevere gli aggiornamenti tramite un broadcast receiver dedicato
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0, //request code
            intent, //intent da inviare al receiver
            PendingIntent.FLAG_MUTABLE //flag richiesto per consentire la modifica dei dati in arrivo
        )

        //definisce le transizioni di attività che si vogliono monitorare (camminare, guidare, andare in bici)
        val transitions = listOf(
            //rileva quando l’utente inizia a camminare
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),

            //rileva quando l’utente inizia a guidare
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),

            //rileva quando l’utente inizia ad andare in bicicletta
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_BICYCLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        )

        //crea la richiesta di riconoscimento che contiene la lista delle transizioni definite sopra
        val request = ActivityTransitionRequest(transitions)

        try {
            //registra il listener per ricevere notifiche quando avviene una transizione di attività
            activityRecognitionClient.requestActivityTransitionUpdates(request, pendingIntent)
                .addOnSuccessListener {
                    //callback chiamata se il monitoraggio è stato avviato correttamente
                    println("✅ Activity Recognition avviato con successo")
                }
                .addOnFailureListener { e ->
                    //callback chiamata se il monitoraggio fallisce
                    println("❌ Errore Activity Recognition: ${e.message}")
                    e.printStackTrace()
                }
        } catch (e: SecurityException) {
            //gestisce il caso in cui il permesso non sia concesso o venga revocato
            println("❌ SecurityException: permesso non concesso")
            e.printStackTrace()
            return false
        }

        return true //ritorna true se l’avvio è andato a buon fine
    }

    //funzione che interrompe il monitoraggio dell’attività fisica
    fun stopActivityRecognition(context: Context) {
        //verifica che il client sia stato inizializzato prima di tentare di fermare il monitoraggio
        if (!::activityRecognitionClient.isInitialized) {
            println("⚠️ ActivityRecognitionClient non inizializzato") //messaggio informativo
            return
        }

        //ricontrolla che il permesso sia ancora concesso prima di fermare il servizio
        if (!hasActivityRecognitionPermission(context)) {
            println("❌ Permesso ACTIVITY_RECOGNITION non concesso") //messaggio di errore se i permessi mancano
            return
        }

        //ricrea l’intent e il pendingintent con la stessa configurazione usata per l’avvio
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )

        //gestisce l’arresto del monitoraggio dentro un blocco di sicurezza
        try {
            //rimuove gli aggiornamenti precedentemente registrati
            activityRecognitionClient.removeActivityTransitionUpdates(pendingIntent)
                .addOnSuccessListener {
                    println("✅ Activity Recognition fermato con successo") //log di conferma
                }
                .addOnFailureListener { e ->
                    println("❌ Errore fermando Activity Recognition: ${e.message}") //messaggio di errore in caso di fallimento
                    e.printStackTrace()
                }
        } catch (e: SecurityException) {
            //gestisce l’errore nel caso in cui il permesso sia stato revocato durante l’esecuzione
            println("❌ SecurityException durante stop: permesso non concesso")
            e.printStackTrace()
        }
    }

    //funzione di utilità che controlla se l’app ha il permesso per accedere ai dati di activity recognition
    fun hasActivityRecognitionPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //da android 10 in poi serve il permesso ACTIVITY_RECOGNITION
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED //ritorna true se il permesso è attivo
        } else {
            //per versioni precedenti non è richiesto alcun permesso specifico
            true
        }
    }

    //funzione che converte il codice numerico di un’attività in una stringa leggibile
    fun getActivityName(activityType: Int): String {
        return when (activityType) {
            DetectedActivity.IN_VEHICLE -> "guidando" //attività di guida in veicolo
            DetectedActivity.ON_BICYCLE -> "in bicicletta" //attività in bicicletta
            DetectedActivity.WALKING -> "camminando" //attività di camminata
            DetectedActivity.RUNNING -> "correndo" //attività di corsa
            DetectedActivity.STILL -> "fermo" //nessun movimento rilevato
            else -> "in movimento" //valore predefinito per altri tipi non gestiti
        }
    }
}