package com.example.travelcompanionapp.utils

import com.example.travelcompanionapp.data.Trip
import java.util.concurrent.TimeUnit

/**
 * Logica e regole per i diversi tipi di viaggio.
 *
 * ⭐ TIPOLOGIE DI VIAGGIO E LORO SIGNIFICATO:
 *
 * 1. LOCAL TRIP (Viaggio Locale)
 *    - Durata: Massimo 1 giorno
 *    - Distanza: < 50 km
 *    - Esempi: Gita in città, visita museo, shopping
 *
 * 2. DAY TRIP (Gita Giornaliera)
 *    - Durata: 1 giorno
 *    - Distanza: 50-200 km
 *    - Esempi: Gita fuori porta, visita città vicina
 *
 * 3. MULTI-DAY TRIP (Viaggio Multi-giorno)
 *    - Durata: 2+ giorni
 *    - Distanza: Qualsiasi
 *    - Esempi: Vacanza, viaggio di lavoro prolungato
 */
object TripTypeLogic {

    // Costanti per i tipi di viaggio
    const val LOCAL_TRIP = "Local trip"
    const val DAY_TRIP = "Day trip"
    const val MULTI_DAY_TRIP = "Multi-day trip"

    // Limiti di distanza (in km)
    const val LOCAL_TRIP_MAX_DISTANCE = 50.0
    const val DAY_TRIP_MAX_DISTANCE = 200.0

    /**
     * Valida se un viaggio rispetta le regole del suo tipo.
     *
     * @param trip Il viaggio da validare
     * @return Messaggio di errore se non valido, null se tutto ok
     */
    fun validateTrip(trip: Trip): String? {
        val durationDays = calculateDurationDays(trip)

        return when (trip.tripType) {
            LOCAL_TRIP -> {
                // Local trip deve essere di massimo 1 giorno
                if (durationDays > 1) {
                    "⚠️ Un viaggio locale non può durare più di 1 giorno"
                } else {
                    null
                }
            }

            DAY_TRIP -> {
                // Day trip deve essere esattamente 1 giorno
                if (durationDays != 1) {
                    "⚠️ Una gita giornaliera deve durare esattamente 1 giorno"
                } else {
                    null
                }
            }

            MULTI_DAY_TRIP -> {
                // Multi-day trip deve essere almeno 2 giorni
                if (durationDays < 2) {
                    "⚠️ Un viaggio multi-giorno deve durare almeno 2 giorni"
                } else {
                    null
                }
            }

            else -> "⚠️ Tipo di viaggio non riconosciuto"
        }
    }

    /**
     * Suggerisce il tipo di viaggio appropriato in base a date e distanza.
     *
     * @param startDate Data inizio
     * @param endDate Data fine
     * @param distanceKm Distanza percorsa (opzionale)
     * @return Tipo di viaggio suggerito
     */
    fun suggestTripType(
        startDate: java.util.Date,
        endDate: java.util.Date,
        distanceKm: Double = 0.0
    ): String {
        val durationDays = TimeUnit.MILLISECONDS.toDays(
            endDate.time - startDate.time
        ).toInt() + 1

        return when {
            // Se è più di un giorno, sicuramente multi-day
            durationDays > 1 -> MULTI_DAY_TRIP

            // Se è 1 giorno e distanza < 50km, è local trip
            durationDays == 1 && distanceKm < LOCAL_TRIP_MAX_DISTANCE -> LOCAL_TRIP

            // Altrimenti day trip
            else -> DAY_TRIP
        }
    }

    /**
     * Calcola la durata in giorni di un viaggio.
     */
    fun calculateDurationDays(trip: Trip): Int {
        return TimeUnit.MILLISECONDS.toDays(
            trip.endDate.time - trip.startDate.time
        ).toInt() + 1
    }

    /**
     * Restituisce una descrizione del tipo di viaggio.
     */
    fun getTripTypeDescription(tripType: String): String {
        return when (tripType) {
            LOCAL_TRIP -> "Viaggio nella tua zona (max 1 giorno, < 50 km)"
            DAY_TRIP -> "Gita giornaliera fuori porta (1 giorno, 50-200 km)"
            MULTI_DAY_TRIP -> "Viaggio di più giorni (2+ giorni)"
            else -> "Tipo di viaggio sconosciuto"
        }
    }

    /**
     * Restituisce l'emoji appropriata per il tipo di viaggio.
     */
    fun getTripTypeEmoji(tripType: String): String {
        return when (tripType) {
            LOCAL_TRIP -> "🏙️"
            DAY_TRIP -> "🚗"
            MULTI_DAY_TRIP -> "✈️"
            else -> "🗺️"
        }
    }

    /**
     * Controlla se la distanza è appropriata per il tipo di viaggio.
     * Restituisce un avviso se la distanza sembra anomala.
     */
    fun checkDistanceWarning(trip: Trip): String? {
        if (trip.totalDistanceKm == 0.0) return null

        return when (trip.tripType) {
            LOCAL_TRIP -> {
                if (trip.totalDistanceKm > LOCAL_TRIP_MAX_DISTANCE) {
                    "💡 Hai percorso più di 50 km. Forse era una 'Gita giornaliera'?"
                } else {
                    null
                }
            }

            DAY_TRIP -> {
                when {
                    trip.totalDistanceKm < LOCAL_TRIP_MAX_DISTANCE -> {
                        "💡 Meno di 50 km. Forse era un 'Viaggio locale'?"
                    }
                    trip.totalDistanceKm > DAY_TRIP_MAX_DISTANCE -> {
                        "💡 Più di 200 km in un giorno. Ottimo viaggio!"
                    }
                    else -> null
                }
            }

            else -> null
        }
    }

    /**
     * Restituisce statistiche e badge in base al tipo e performance del viaggio.
     */
    fun getTripBadges(trip: Trip): List<String> {
        val badges = mutableListOf<String>()
        val durationDays = calculateDurationDays(trip)

        // Badge per distanza
        when {
            trip.totalDistanceKm > 1000 -> badges.add("🏆 Esploratore Instancabile")
            trip.totalDistanceKm > 500 -> badges.add("⭐ Grande Viaggiatore")
            trip.totalDistanceKm > 200 -> badges.add("✨ Avventuriero")
        }

        // Badge per durata
        when {
            durationDays > 30 -> badges.add("🌍 Nomade Digitale")
            durationDays > 14 -> badges.add("🎒 Vagabondo")
            durationDays > 7 -> badges.add("⛺ Esploratore")
        }

        // Badge per tipo specifico
        when (trip.tripType) {
            LOCAL_TRIP -> {
                if (trip.totalDistanceKm > 30) {
                    badges.add("🚶 Camminatore Urbano")
                }
            }
            DAY_TRIP -> {
                if (trip.totalDistanceKm > 150) {
                    badges.add("🚗 Re della Strada")
                }
            }
            MULTI_DAY_TRIP -> {
                val avgDistancePerDay = trip.totalDistanceKm / durationDays
                if (avgDistancePerDay > 100) {
                    badges.add("⚡ Velocista")
                }
            }
        }

        return badges
    }

    /**
     * Calcola suggerimenti in base al tipo di viaggio e alle performance.
     */
    fun getTripSuggestions(trip: Trip): List<String> {
        val suggestions = mutableListOf<String>()
        val durationDays = calculateDurationDays(trip)

        when (trip.tripType) {
            LOCAL_TRIP -> {
                suggestions.add("💡 Perfetto per esplorare la tua città!")
                if (trip.totalDistanceKm < 10) {
                    suggestions.add("🚶 Considera di camminare di più per scoprire angoli nascosti")
                }
            }

            DAY_TRIP -> {
                suggestions.add("🗓️ Ideale per una giornata di esplorazione!")
                if (trip.totalDistanceKm < 50) {
                    suggestions.add("🚗 Prova a visitare località più lontane la prossima volta")
                }
            }

            MULTI_DAY_TRIP -> {
                val avgDistancePerDay = if (durationDays > 0) {
                    trip.totalDistanceKm / durationDays
                } else {
                    0.0
                }

                suggestions.add("✈️ Grande avventura di $durationDays giorni!")

                if (avgDistancePerDay < 50) {
                    suggestions.add("🏖️ Sembra un viaggio rilassante. Perfetto!")
                } else if (avgDistancePerDay > 150) {
                    suggestions.add("⚡ Wow! Hai macinato km ogni giorno!")
                }
            }
        }

        return suggestions
    }
}