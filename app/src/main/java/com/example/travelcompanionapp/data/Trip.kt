package com.example.travelcompanionapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Definizione dell'entità del database (tabella) per un Viaggio.
 *
 * ⭐ AGGIORNAMENTO SISTEMA DATE E TRACKING:
 * - Campo "notes" rinominato in "description"
 * - La "description" è una descrizione generale inserita alla creazione
 * - Le note DURANTE il viaggio sono nella tabella separata TripNote
 * - Aggiunte date EFFETTIVE di tracking (actualStartDate, actualEndDate)
 * - Aggiunta durata totale di tracking GPS (totalTrackingDurationMs)
 *
 * DISTINZIONE IMPORTANTE:
 * - startDate/endDate: Date PIANIFICATE dall'utente in TripEntryScreen
 * - actualStartDate/actualEndDate: Date EFFETTIVE quando il GPS era attivo
 *
 * Questo design permette:
 * - Pianificazione: "Viaggio previsto dal 10 al 15 Gennaio"
 * - Tracking reale: "Effettivamente tracciato il 12-13 Gennaio"
 * - Visualizzazione corretta: Mostra date effettive per viaggi completati
 */
@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // === DETTAGLI OBBLIGATORI DEL VIAGGIO (PIANIFICAZIONE) ===
    val destination: String,

    // Coordinate GPS della destinazione (dalla mappa)
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,

    // Date PIANIFICATE del viaggio (inserite dall'utente in TripEntryScreen)
    val startDate: Date,
    val endDate: Date,

    // Tipo di viaggio (Local trip, Day trip, Multi-day trip)
    val tripType: String,

    // === DATI DI TRACCIAMENTO EFFETTIVO ===
    val totalDistanceKm: Double = 0.0,
    val isCompleted: Boolean = false,
    val status: String = "Pianificato", // Pianificato, In corso, Completato

    // ⭐ NUOVI CAMPI: Date EFFETTIVE di tracking GPS
    // Quando l'utente ha premuto "Start Tracking"
    val actualStartDate: Date? = null,

    // Quando l'utente ha premuto "Stop Tracking" (ultima volta)
    val actualEndDate: Date? = null,

    // Durata totale con GPS attivo (in millisecondi)
    // Utile per mostrare "GPS attivo per 6h 30m"
    val totalTrackingDurationMs: Long = 0,

    // Descrizione generale del viaggio inserita alla creazione
    // Es: "Weekend romantico", "Viaggio di lavoro", "Vacanza con amici"
    val description: String = ""
) {
    /**
     * Restituisce le date da mostrare nell'interfaccia utente.
     *
     * LOGICA:
     * - Se il viaggio è completato e ha date effettive → mostra quelle
     * - Altrimenti → mostra le date pianificate
     *
     * @return Pair di (dataInizio, dataFine)
     */
    fun getDisplayDates(): Pair<Date, Date> {
        return if (isCompleted && actualStartDate != null && actualEndDate != null) {
            actualStartDate to actualEndDate
        } else {
            startDate to endDate
        }
    }

    /**
     * Restituisce una stringa formattata con le date appropriate.
     * Aggiunge "(effettivo)" se le date reali differiscono da quelle pianificate.
     *
     * ESEMPI:
     * - Pianificato non completato: "10/01/2025 - 15/01/2025"
     * - Completato con date uguali: "10/01/2025 - 15/01/2025"
     * - Completato con date diverse: "12/01/2025 - 13/01/2025 (effettivo)"
     *
     * @return String con range date formattato
     */
    fun getFormattedDateRange(): String {
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
        val (start, end) = getDisplayDates()

        return if (isCompleted && actualStartDate != null && actualEndDate != null) {
            val actual = "${dateFormatter.format(start)} - ${dateFormatter.format(end)}"

            // Controlla se le date effettive sono diverse da quelle pianificate
            val isDifferent = actualStartDate != startDate || actualEndDate != endDate

            if (isDifferent) {
                "$actual (effettivo)"
            } else {
                actual
            }
        } else {
            "${dateFormatter.format(start)} - ${dateFormatter.format(end)}"
        }
    }

    /**
     * Calcola la durata del tracking GPS in formato leggibile.
     *
     * ESEMPI:
     * - 125 minuti → "2h 5m"
     * - 45 minuti → "45m"
     * - 30 secondi → "< 1m"
     *
     * @return String con durata formattata
     */
    fun getFormattedDuration(): String {
        if (totalTrackingDurationMs == 0L) return "0m"

        val hours = totalTrackingDurationMs / (1000 * 60 * 60)
        val minutes = (totalTrackingDurationMs / (1000 * 60)) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    /**
     * Verifica se le date effettive sono diverse da quelle pianificate.
     * Utile per mostrare badge o avvisi nell'UI.
     *
     * @return true se il viaggio è stato tracciato in date diverse dal piano
     */
    fun hasDifferentActualDates(): Boolean {
        if (!isCompleted || actualStartDate == null || actualEndDate == null) {
            return false
        }
        return actualStartDate != startDate || actualEndDate != endDate
    }
}