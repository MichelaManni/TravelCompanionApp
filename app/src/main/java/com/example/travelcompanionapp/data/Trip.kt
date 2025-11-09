package com.example.travelcompanionapp.data

import androidx.room.Entity //annotazione @Entity che indica a Room che questa classe è una tabella del database
import androidx.room.PrimaryKey //@PrimaryKey per definire la chiave primaria della tabella
import java.text.SimpleDateFormat //per formattare le date in stringhe leggibili
import java.util.Date //classe Date per gestire date e orari
import java.util.Locale

/**
 * Definizione dell'entità del database (tabella) per un Viaggio.
 *
 *
 * - La "description" è una descrizione generale inserita alla creazione
 * - Le note DURANTE il viaggio sono nella tabella separata TripNote
 * - Aggiunte date EFFETTIVE di tracking (actualStartDate, actualEndDate)
 * - Aggiunta durata totale di tracking GPS (totalTrackingDurationMs)
 *
 * DISTINZIONE IMPORTANTE:
 * - startDate/endDate: Date PIANIFICATE dall'utente in TripEntryScreen
 * - actualStartDate/actualEndDate: Date EFFETTIVE quando il GPS era attivo
 */
@Entity(tableName = "trips") //ogni proprietà della classe diventa una COLONNA della tabella
data class Trip(
    @PrimaryKey(autoGenerate = true) //chiave primaria è id
    val id: Int = 0,

    //Dettagli obbligatori del viaggio

    val destination: String,

    // Coordinate GPS della destinazione (dalla mappa)
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,

    // Date PIANIFICATE del viaggio (inserite dall'utente in TripEntryScreen)
    val startDate: Date,
    val endDate: Date,

    // Tipo di viaggio (Local trip, Day trip, Multi-day trip)
    val tripType: String,

    //  DATI DI TRACCIAMENTO EFFETTIVO
    val totalDistanceKm: Double = 0.0, //distanza totale percorsa durante il viaggio in km, valore pred è 0.0 (viaggio non iniziato)
    val isCompleted: Boolean = false,
    val status: String = "Pianificato", // Pianificato, In corso, Completato, pianificato è il valore predefinito

    //Date EFFETTIVE di tracking GPS
    // Quando l'utente ha premuto "Start Tracking"
    val actualStartDate: Date? = null,

    // Quando l'utente ha premuto "Stop Tracking" (ultima volta)
    val actualEndDate: Date? = null,

    // Durata totale con GPS attivo (in millisecondi)
    val totalTrackingDurationMs: Long = 0,

    // Descrizione generale del viaggio inserita alla creazione
    // Es: "Weekend romantico", "Viaggio di lavoro", "Vacanza con amici"
    val description: String = ""
) {

     //Restituisce le date da mostrare nell'interfaccia utente.
     //- Se il viaggio è completato e ha date effettive → mostra quelle
     // - Altrimenti → mostra le date pianificate
     // @return Pair di (dataInizio, dataFine)

    fun getDisplayDates(): Pair<Date, Date> { //restituisce una coppia (Pair) di due Date
        // serve per decidere quali date mostrare all'utente

        return if (isCompleted && actualStartDate != null && actualEndDate != null) { //se il viaggio è completato (isCompleted = true)
            //e se esistono le date effettive (actualStartDate e actualEndDate non sono null)
            actualStartDate to actualEndDate //restituisce le date EFFETTIVE
        } else {
            //altrimenti (viaggio non completato O date effettive non disponibili):
            startDate to endDate //restituisci le date PIANIFICATE

        }
    }


     //restituisce una stringa formattata con le date appropriate.
     //ggiunge "(effettivo)" se le date reali differiscono da quelle pianificate.
     // return String con range date formattato
    fun getFormattedDateRange(): String { //restituisce una stringa con le date formattate
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN) //formattatore di date con formato italiano
        val (start, end) = getDisplayDates() //chiama getDisplayDates() x ottenere le date da mostrare
         // "val (start, end)" = destrutturazione della Pair
         // estrae i due valori dalla coppia in due variabili separate

        return if (isCompleted && actualStartDate != null && actualEndDate != null) { //se viaggio è completed e ha date effettoive

            //crea strubga con date formattate. dateFormatter.format(start) = converte la data in stringa (es: "12/01/2025")
            // "${...}" = template string (inserisce il valore nella stringa)
            //es risultato: "12/01/2025 - 13/01/2025"
            val actual = "${dateFormatter.format(start)} - ${dateFormatter.format(end)}"

            //controlla se le date effettive sono diverse da quelle pianificate
            val isDifferent = actualStartDate != startDate || actualEndDate != endDate

            if (isDifferent) { //se sono diverse
                "$actual (effettivo)" //aggiunge "(effettivo)" alla stringa
            } else { // date uguali
                actual //restituisce solo le date senza aggiunta
            }
        } else { //se viaggio non completato o senza date effettiva
            "${dateFormatter.format(start)} - ${dateFormatter.format(end)}" //restituisce le date pianificate
        }
    }

     //calcola la durata del tracking GPS in formato leggibile.
    //ESEMPI:
     //- 125 minuti → "2h 5m"
     //- 45 minuti → "45m"
      //- 30 secondi → "< 1m"
    fun getFormattedDuration(): String { //converte i millisecondi in una stringa leggibile
        if (totalTrackingDurationMs == 0L) return "0m" //se la durata è 0, restituisce "0m" (0 minuti). OL = num Long

        //calcola ore e minuti da millisecondi

        val hours = totalTrackingDurationMs / (1000 * 60 * 60)
         //calcola  ore dividendo i millisecondi per il numero di millisecondi in un'ora
         // 1000 ms = 1 secondo
         // 1000 * 60 = 60.000 ms = 1 minuto
         // 1000 * 60 * 60 = 3.600.000 ms = 1 ora
         // Esempio: 7.200.000 ms / 3.600.000 = 2 ore

        val minutes = (totalTrackingDurationMs / (1000 * 60)) % 60
         //calcola i minuti rimanenti (escluse le ore)
         //prima divide per millisecondi in un minuto (ottiene minuti totali)
         // "% 60" = modulo 60 (resto della divisione per 60)
         //es: 7.500.000 ms → 125 minuti totali → 125 % 60 = 5 minuti
         // (2 ore e 5 minuti)

        return when {
            hours > 0 -> "${hours}h ${minutes}m" //se ci sono ore (hours > 0):
            //restituisce formato "Xh Ym" (es: "2h 5m")

            minutes > 0 -> "${minutes}m" //se ci sono solo minuti (hours = 0, minutes > 0):
            // Restituisce formato "Ym" (es: "45m")

            else -> "< 1m" //altrimenti (meno di 1 minuto) restituisce "< 1m" (meno di 1 minuto)
        }
    }


     //verifica se le date effettive sono diverse da quelle pianificate.
    fun hasDifferentActualDates(): Boolean {
        if (!isCompleted || actualStartDate == null || actualEndDate == null) { //se il viaggio NON è completato (!isCompleted)
            // O se le date effettive sono null:
            return false //date non sono diverse perché non ci sono date effettive
        }
        return actualStartDate != startDate || actualEndDate != endDate
         //controlla se almeno una data effettiva è diversa da quella pianificata
         //restituisce true se sono diverse, false se sono uguali

    }
}