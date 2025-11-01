package com.example.travelcompanionapp

import android.app.Application
import android.content.Context
import com.example.travelcompanionapp.data.AppDatabase
import com.example.travelcompanionapp.repository.TripRepository

/**
 * Classe Application personalizzata.
 * Viene eseguita prima di qualsiasi Activity o Service ed è il posto ideale
 * per inizializzare componenti che devono vivere per l'intera vita dell'app (es. Database Singleton).
 *
 * ⭐ AGGIORNAMENTO: Il repository ora include anche il TripNoteDao
 */
class TravelCompanionApplication : Application() {

    // Inizializzazione lazy del database Singleton usando il contesto dell'app
    private val database by lazy { AppDatabase.getDatabase(this) }

    // ⭐ AGGIORNATO: Il repository ora riceve ENTRAMBI i DAO (Trip e TripNote)
    val repository by lazy {
        TripRepository(
            tripDao = database.tripDao(),
            tripNoteDao = database.tripNoteDao() // ⭐ Aggiunto
        )
    }

    companion object {
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}