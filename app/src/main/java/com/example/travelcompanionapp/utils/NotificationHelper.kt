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

    // ID univoci per i canali di notifica
    private const val CHANNEL_REMINDERS_ID = "travel_reminders"
    private const val CHANNEL_ACTIVITY_ID = "activity_recognition"

    // ID univoci per le notifiche (per poterle aggiornare/cancellare)
    private const val NOTIFICATION_REMINDER_ID = 1001
    private const val NOTIFICATION_ACTIVITY_ID = 1002

    /**
     * Crea i canali di notifica necessari.
     * Deve essere chiamato all'avvio dell'app (in Application.onCreate).
     *
     * I canali permettono all'utente di gestire le notifiche per categoria.
     */
    fun createNotificationChannels(context: Context) {
        // I canali esistono solo da Android 8.0 (Oreo) in poi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            // Canale 1: Reminder per registrare viaggi
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Promemoria Viaggi", // Nome visibile all'utente
                NotificationManager.IMPORTANCE_DEFAULT // Importanza media
            ).apply {
                description = "Notifiche per ricordarti di registrare i tuoi viaggi"
            }

            // Canale 2: Rilevamento attività (movimento)
            val activityChannel = NotificationChannel(
                CHANNEL_ACTIVITY_ID,
                "Rilevamento Movimento", // Nome visibile all'utente
                NotificationManager.IMPORTANCE_HIGH // Importanza alta (con suono)
            ).apply {
                description = "Notifiche quando rileviamo che sei in movimento"
            }

            // Registra i canali nel sistema
            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(activityChannel)
        }
    }

    /**
     * Invia una notifica reminder per registrare un viaggio.
     *
     * Viene chiamata dal ReminderWorker quando l'utente non ha viaggi recenti.
     *
     * @param context Contesto dell'applicazione
     * @param daysSinceLastTrip Numero di giorni dall'ultimo viaggio
     */
    fun sendTripReminderNotification(context: Context, daysSinceLastTrip: Int) {
        // ⭐ CONTROLLO ESPLICITO DEL PERMESSO POST_NOTIFICATIONS
        if (!hasNotificationPermission(context)) {
            println(" Permesso POST_NOTIFICATIONS non concesso")
            return
        }

        // Intent per aprire l'app quando l'utente clicca la notifica
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // PendingIntent (wrapper per eseguire l'intent quando l'utente clicca)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE è obbligatorio da Android 12+
        )

        // Costruisce la notifica
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Icona piccola (obbligatoria)
            .setContentTitle("🗺️ Tempo di viaggiare!") // Titolo
            .setContentText("Non registri un viaggio da $daysSinceLastTrip giorni. Dove sei stato?") // Testo
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Priorità media
            .setContentIntent(pendingIntent) // Azione quando si clicca
            .setAutoCancel(true) // Si chiude automaticamente quando cliccata
            .build()

        // ⭐ CHIAMATA CON TRY-CATCH PER GESTIRE SecurityException
        try {
            // Invia la notifica usando NotificationManager
            NotificationManagerCompat.from(context).notify(NOTIFICATION_REMINDER_ID, notification)
            println("✅ Notifica reminder inviata con successo")
        } catch (e: SecurityException) {
            // Gestisce l'eccezione se il permesso non è concesso
            println("❌ SecurityException: impossibile inviare notifica reminder")
            e.printStackTrace()
        }
    }

    /**
     * Invia una notifica quando rileva che l'utente è in movimento.
     *
     * Viene chiamata dall'ActivityRecognitionReceiver quando rileva movimento.
     *
     * @param context Contesto dell'applicazione
     * @param activityType Tipo di attività rilevata (es: "guidando", "camminando")
     */
    fun sendActivityDetectedNotification(context: Context, activityType: String) {
        // ⭐ CONTROLLO ESPLICITO DEL PERMESSO POST_NOTIFICATIONS
        if (!hasNotificationPermission(context)) {
            println("Permesso POST_NOTIFICATIONS non concesso")
            return
        }

        // Intent per aprire direttamente la schermata di tracking
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Puoi aggiungere extra per navigare direttamente al tracking
            putExtra("navigate_to", "tracking")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Costruisce la notifica
        val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVITY_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🚗 Movimento Rilevato!")
            .setContentText("Sembra che tu stia $activityType. Vuoi iniziare a registrare?")
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Priorità alta (con suono)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Inizia Tracking", // Pulsante nella notifica
                pendingIntent
            )
            .build()

        // ⭐ CHIAMATA CON TRY-CATCH PER GESTIRE SecurityException
        try {
            // Invia la notifica
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ACTIVITY_ID, notification)
            println("Notifica activity rilevata inviata con successo")
        } catch (e: SecurityException) {
            // Gestisce l'eccezione se il permesso non è concesso
            println("SecurityException: impossibile inviare notifica activity")
            e.printStackTrace()
        }
    }

    /**
     * Controlla se l'app ha il permesso per inviare notifiche.
     *
     * Da Android 13 (API 33+) serve un permesso runtime.
     *
     * @param context Contesto dell'applicazione
     * @return true se il permesso è garantito, false altrimenti
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: controlla permesso POST_NOTIFICATIONS
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 e precedenti: notifiche sempre permesse
            true
        }
    }
}