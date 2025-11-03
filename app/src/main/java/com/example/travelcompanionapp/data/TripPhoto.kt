package com.example.travelcompanionapp.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entità per le foto scattate durante un viaggio.
 *
 * Ogni foto rappresenta un "momento specifico" del viaggio e può includere:
 * - Percorso del file immagine nel dispositivo
 * - Timestamp (quando è stata scattata)
 * - Coordinate GPS (dove è stata scattata)
 * - Nome della posizione (es: "Roma, Piazza Navona")
 * - Didascalia opzionale
 *
 * Relazione con Trip: Un viaggio può avere MOLTE foto (1-to-Many)
 *
 * ⭐ CONFORME ALLE SPECIFICHE:
 * "Allow users to attach photos (via the camera) and notes to specific moments
 *  or locations during the journey"
 */
@Entity(
    tableName = "trip_photos",
    // Chiave esterna per collegare la foto al viaggio
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE // Se cancelli il viaggio, cancella anche le foto
        )
    ],
    // Indice per velocizzare le query per tripId
    indices = [Index(value = ["tripId"])]
)
data class TripPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // ID del viaggio a cui appartiene questa foto
    val tripId: Int,

    // === CONTENUTO DELLA FOTO ===

    // Percorso del file immagine salvato nella memoria locale
    // Es: "/storage/emulated/0/Android/data/.../files/Pictures/trip_1_photo_123.jpg"
    val filePath: String,

    // Momento in cui è stata scattata la foto
    val timestamp: Date,

    // Didascalia opzionale aggiunta dall'utente
    // Es: "Tramonto sul Colosseo", "Pranzo tipico"
    val caption: String? = null,

    // === INFORMAZIONI DI POSIZIONE (OPZIONALI) ===

    // Coordinate GPS del punto in cui è stata scattata la foto
    // Questi campi sono opzionali perché:
    // 1. L'utente potrebbe non avere GPS attivo
    // 2. Potrebbe aggiungere foto anche dopo il viaggio
    val latitude: Double? = null,
    val longitude: Double? = null,

    // Nome del luogo ottenuto tramite geocoding inverso
    // Es: "Roma, Via del Corso" oppure "Milano, Duomo"
    val locationName: String? = null
)