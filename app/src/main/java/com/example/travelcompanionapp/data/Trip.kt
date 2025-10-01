package com.example.travelcompanionapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Definizione dell'entità del database (tabella) per un Viaggio.
 *
 * Il progetto "Travel Companion" richiede di memorizzare i piani di viaggio
 * (destinazione, date, tipo di viaggio). Questa Entity rappresenta un singolo piano/viaggio.
 */
@Entity(tableName = "trips")
data class Trip(
    // Chiave primaria con autoGenerate = true per l'incremento automatico
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Dettagli del viaggio richiesti dalle specifiche
    val destination: String, // Esempio: "Parigi"
    val startDate: Date,     // Data di inizio del viaggio
    val endDate: Date,       // Data di fine del viaggio
    val tripType: String,    // Tipo di viaggio: "Local trip", "Day trip", "Multi-day trip"

    // Dati per i calcoli e le visualizzazioni (es. Bar Chart / Timeline)
    val totalDistanceKm: Double = 0.0, // Distanza totale per i "Multi-day trip"
    val isCompleted: Boolean = false   // Stato per filtrare i viaggi passati/futuri
)

/**
 * Poiché Room non supporta nativamente gli oggetti complessi (come java.util.Date),
 * e per gestire i dati GPS (che saranno complessi), è necessario un TypeConverter.
 * Per ora, definiamo solo il TypeConverter per Date, essenziale per l'Entity Trip.
 */
// NON è necessario creare un file .kt separato per TypeConverter in questo primo step,
// ma dovrai aggiungerli al file AppDatabase nel prossimo step!
