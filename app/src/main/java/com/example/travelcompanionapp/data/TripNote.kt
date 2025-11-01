package com.example.travelcompanionapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entità per le note aggiunte durante un viaggio.
 *
 * Ogni nota rappresenta un "momento specifico" del viaggio e può includere:
 * - Testo della nota
 * - Timestamp (quando è stata scritta)
 * - Coordinate GPS (dove è stata scritta)
 * - Nome della posizione (es: "Roma, Piazza Navona")
 *
 * Relazione con Trip: Un viaggio può avere MOLTE note (1-to-Many)
 *
 * ⭐ CONFORME ALLE SPECIFICHE:
 * "Allow users to attach photos (via the camera) and notes to specific moments
 *  or locations during the journey"
 */
@Entity(
    tableName = "trip_notes",
    // Chiave esterna per collegare la nota al viaggio
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE // Se cancelli il viaggio, cancella anche le note
        )
    ],
    // Indice per velocizzare le query per tripId
    indices = [Index(value = ["tripId"])]
)
data class TripNote(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // ID del viaggio a cui appartiene questa nota
    val tripId: Int,

    // === CONTENUTO DELLA NOTA ===

    // Testo della nota scritto dall'utente
    val text: String,

    // Momento in cui è stata creata la nota
    val timestamp: Date,

    // === INFORMAZIONI DI POSIZIONE (OPZIONALI) ===

    // Coordinate GPS del punto in cui è stata scritta la nota
    // Questi campi sono opzionali perché:
    // 1. L'utente potrebbe non avere GPS attivo
    // 2. Potrebbe aggiungere note anche dopo il viaggio
    val latitude: Double? = null,
    val longitude: Double? = null,

    // Nome del luogo ottenuto tramite geocoding inverso
    // Es: "Roma, Via del Corso" oppure "Milano, Duomo"
    val locationName: String? = null
)