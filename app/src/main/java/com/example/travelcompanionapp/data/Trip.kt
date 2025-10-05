package com.example.travelcompanionapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Definizione dell'entità del database (tabella) per un Viaggio.
 */
@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Dettagli del viaggio richiesti
    val destination: String,
    // ⭐ AGGIUNTO: Campi per le coordinate GPS
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,
    // ⭐ FINE AGGIUNTA
    val startDate: Date,
    val endDate: Date,
    val tripType: String,

    // Dati per i calcoli e le visualizzazioni
    val totalDistanceKm: Double = 0.0,
    val isCompleted: Boolean = false,
    val status: String = "Pianificato" // Es. Pianificato, In corso, Completato
)

