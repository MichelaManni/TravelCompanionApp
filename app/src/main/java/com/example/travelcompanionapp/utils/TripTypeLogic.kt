package com.example.travelcompanionapp.utils

import com.example.travelcompanionapp.data.Trip
import java.util.concurrent.TimeUnit

/**
 * Logica e regole per i diversi tipi di viaggio.
 *
 * TIPOLOGIE DI VIAGGIO E LORO SIGNIFICATO:
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

    //costanti testuali che rappresentano le tre principali tipologie di viaggio
    const val LOCAL_TRIP = "Local trip"
    const val DAY_TRIP = "Day trip"
    const val MULTI_DAY_TRIP = "Multi-day trip"

    //limiti di distanza in chilometri per differenziare i tipi di viaggio
    const val LOCAL_TRIP_MAX_DISTANCE = 50.0
    const val DAY_TRIP_MAX_DISTANCE = 200.0

    //funzione che verifica se un viaggio rispetta le regole del tipo dichiarato
    fun validateTrip(trip: Trip): String? {
        val durationDays = calculateDurationDays(trip) //calcola la durata del viaggio in giorni

        return when (trip.tripType) {
            LOCAL_TRIP -> {
                //un viaggio locale non deve durare più di un giorno
                if (durationDays > 1) {
                    "⚠️ Un viaggio locale non può durare più di 1 giorno"
                } else {
                    null
                }
            }

            DAY_TRIP -> {
                //una gita giornaliera deve durare esattamente un giorno
                if (durationDays != 1) {
                    "⚠️ Una gita giornaliera deve durare esattamente 1 giorno"
                } else {
                    null
                }
            }

            MULTI_DAY_TRIP -> {
                //un viaggio multi-giorno deve durare almeno due giorni
                if (durationDays < 2) {
                    "⚠️ Un viaggio multi-giorno deve durare almeno 2 giorni"
                } else {
                    null
                }
            }

            else -> "⚠️ Tipo di viaggio non riconosciuto" //messaggio di fallback per tipi sconosciuti
        }
    }

    //funzione che suggerisce automaticamente il tipo di viaggio in base alle date e alla distanza percorsa
    fun suggestTripType(
        startDate: java.util.Date, //data di inizio del viaggio
        endDate: java.util.Date, //data di fine del viaggio
        distanceKm: Double = 0.0 //distanza percorsa (opzionale)
    ): String {
        //calcola la durata del viaggio in giorni interi
        val durationDays = TimeUnit.MILLISECONDS.toDays(
            endDate.time - startDate.time
        ).toInt() + 1

        return when {
            //se dura più di un giorno viene classificato automaticamente come multi-day trip
            durationDays > 1 -> MULTI_DAY_TRIP

            //se dura un giorno solo e la distanza è inferiore a 50 km è un local trip
            durationDays == 1 && distanceKm < LOCAL_TRIP_MAX_DISTANCE -> LOCAL_TRIP

            //altrimenti viene considerato day trip
            else -> DAY_TRIP
        }
    }

    //funzione che calcola la durata del viaggio in giorni
    fun calculateDurationDays(trip: Trip): Int {
        return TimeUnit.MILLISECONDS.toDays(
            trip.endDate.time - trip.startDate.time
        ).toInt() + 1 //aggiunge 1 per includere il giorno iniziale
    }

    //funzione che restituisce una descrizione testuale del tipo di viaggio
    fun getTripTypeDescription(tripType: String): String {
        return when (tripType) {
            LOCAL_TRIP -> "Viaggio nella tua zona (max 1 giorno, < 50 km)"
            DAY_TRIP -> "Gita giornaliera fuori porta (1 giorno, 50-200 km)"
            MULTI_DAY_TRIP -> "Viaggio di più giorni (2+ giorni)"
            else -> "Tipo di viaggio sconosciuto"
        }
    }

    //funzione che restituisce un’emoji corrispondente al tipo di viaggio per uso visivo nell’interfaccia
    fun getTripTypeEmoji(tripType: String): String {
        return when (tripType) {
            LOCAL_TRIP -> "🏙️"
            DAY_TRIP -> "🚗"
            MULTI_DAY_TRIP -> "✈️"
            else -> "🗺️"
        }
    }

}