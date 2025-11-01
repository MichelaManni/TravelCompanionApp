package com.example.travelcompanionapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Definizione dell'entità del database (tabella) per un Viaggio.
 *
 * ⭐ AGGIORNAMENTO SISTEMA NOTE:
 * - Campo "notes" rinominato in "description"
 * - La "description" è una descrizione generale inserita alla creazione
 * - Le note DURANTE il viaggio sono nella tabella separata TripNote
 *
 * Questo design permette:
 * - Descrizione generale statica (es: "Vacanza estiva in famiglia")
 * - Note multiple dinamiche durante il viaggio (es: "Fermata pranzo", "Panorama mozzafiato")
 */
@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // === DETTAGLI OBBLIGATORI DEL VIAGGIO ===
    val destination: String,

    // Coordinate GPS della destinazione (dalla mappa)
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,

    // Date del viaggio
    val startDate: Date,
    val endDate: Date,

    // Tipo di viaggio (Local trip, Day trip, Multi-day trip)
    val tripType: String,

    // === DATI DI TRACCIAMENTO ===
    val totalDistanceKm: Double = 0.0,
    val isCompleted: Boolean = false,
    val status: String = "Pianificato", // Pianificato, In corso, Completato

    // ⭐ RINOMINATO: notes → description
    // Descrizione generale del viaggio inserita alla creazione
    // Es: "Weekend romantico", "Viaggio di lavoro", "Vacanza con amici"
    val description: String = ""
)