package com.example.travelcompanionapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.travelcompanionapp.MainActivity
import com.example.travelcompanionapp.R

/**
 * Helper per gestire le notifiche dell'app.
 *
 * Funzionalità:
 * 1. Crea canali di notifica (obbligatorio per Android 8+)
 * 2. Invia notifiche per reminder viaggi
 * 3. Invia notifiche per activity recognition
 */
object NotificationHelper {

    //identificatori univoci per i canali di notifica
    private const val CHANNEL_REMINDERS_ID = "travel_reminders" //canale per i promemoria dei viaggi
    private const val CHANNEL_ACTIVITY_ID = "activity_recognition" //canale per il rilevamento del movimento

    //identificatori numerici per le notifiche, servono per aggiornarle o cancellarle in modo indipendente
    private const val NOTIFICATION_REMINDER_ID = 1001 //id per la notifica dei promemoria
    private const val NOTIFICATION_ACTIVITY_ID = 1002 //id per la notifica di attività rilevata

    //funzione che crea i canali di notifica obbligatori da android 8 (oreo) in poi
    fun createNotificationChannels(context: Context) {
        //verifica che la versione di android supporti i canali di notifica
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager //ottiene il notification manager dal sistema

            //crea il canale per i promemoria dei viaggi
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID, //id del canale
                "Promemoria Viaggi", //nome visibile all’utente nelle impostazioni
                NotificationManager.IMPORTANCE_DEFAULT //livello di importanza medio (nessun suono forte)
            ).apply {
                description = "Notifiche per ricordarti di registrare i tuoi viaggi" //descrizione mostrata nelle impostazioni
            }

            //crea il canale per il rilevamento di movimento
            val activityChannel = NotificationChannel(
                CHANNEL_ACTIVITY_ID, //id del canale
                "Rilevamento Movimento", //nome visibile all’utente
                NotificationManager.IMPORTANCE_HIGH //importanza alta, con suono e vibrazione
            ).apply {
                description = "Notifiche quando rileviamo che sei in movimento" //descrizione del canale
            }

            //registra i due canali nel sistema
            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(activityChannel)
        }
    }

    //funzione che invia una notifica di promemoria per registrare un viaggio
    fun sendTripReminderNotification(context: Context, daysSinceLastTrip: Int) {
        //controlla se l’app ha il permesso di inviare notifiche (necessario da android 13)
        if (!hasNotificationPermission(context)) {
            println(" Permesso POST_NOTIFICATIONS non concesso") //messaggio di avviso in log
            return //interrompe l’esecuzione se il permesso manca
        }

        //crea un intent per aprire l’app principale quando l’utente clicca la notifica
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK //assicura che venga aperta una nuova istanza dell’attività principale
        }

        //crea un pendingintent che incapsula l’intent per essere eseguito dal sistema
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE //richiesto da android 12+ per motivi di sicurezza
        )

        //costruisce la notifica visiva con titolo, testo e comportamento
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) //icona della notifica (obbligatoria)
            .setContentTitle("🗺️ Tempo di viaggiare!") //titolo mostrato all’utente
            .setContentText("Non registri un viaggio da $daysSinceLastTrip giorni. Dove sei stato?") //testo dinamico che mostra i giorni trascorsi
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) //livello di priorità media
            .setContentIntent(pendingIntent) //definisce cosa accade quando si clicca
            .setAutoCancel(true) //chiude automaticamente la notifica dopo il click
            .build()

        //blocca in try-catch per evitare crash in caso di permessi mancanti
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_REMINDER_ID, notification) //invia la notifica al sistema
            println("✅ Notifica reminder inviata con successo") //log di conferma
        } catch (e: SecurityException) {
            println("❌ SecurityException: impossibile inviare notifica reminder") //messaggio di errore
            e.printStackTrace()
        }
    }

    //funzione che invia una notifica quando viene rilevato che l’utente è in movimento
    fun sendActivityDetectedNotification(context: Context, activityType: String) {
        //controllo del permesso per inviare notifiche
        if (!hasNotificationPermission(context)) {
            println("Permesso POST_NOTIFICATIONS non concesso") //avviso in log
            return
        }

        //crea un intent per aprire direttamente la schermata di tracking nell’app principale
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK //apre la mainactivity resettando lo stack
            putExtra("navigate_to", "tracking") //aggiunge un extra per navigare direttamente al tracking
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE //flag richiesto per sicurezza
        )

        //costruisce la notifica che informa l’utente che è stato rilevato un movimento
        val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVITY_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) //icona visiva
            .setContentTitle("🚗 Movimento Rilevato!") //titolo
            .setContentText("Sembra che tu stia $activityType. Vuoi iniziare a registrare?") //testo dinamico basato sul tipo di attività
            .setPriority(NotificationCompat.PRIORITY_HIGH) //alta priorità per attirare attenzione
            .setContentIntent(pendingIntent) //azione principale al tocco
            .setAutoCancel(true) //rimuove la notifica al click
            .addAction(
                R.drawable.ic_launcher_foreground, //icona per l’azione aggiuntiva
                "Inizia Tracking", //testo del pulsante nella notifica
                pendingIntent //azione associata
            )
            .build()


        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ACTIVITY_ID, notification) //invia la notifica
            println("Notifica activity rilevata inviata con successo") //messaggio di successo nel log
        } catch (e: SecurityException) {
            println("SecurityException: impossibile inviare notifica activity") //messaggio di errore in log
            e.printStackTrace()
        }
    }

    //funzione che verifica se l’app ha il permesso per inviare notifiche
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED //ritorna true se il permesso è concesso
        } else {
            //per versioni precedenti le notifiche sono sempre abilitate
            true
        }
    }
}