package com.example.travelcompanionapp // Dichiarazione del package

import android.app.Application
import android.content.Context
import com.example.travelcompanionapp.data.AppDatabase
import com.example.travelcompanionapp.repository.TripRepository

/**
 * Classe Application personalizzata.
 * Viene eseguita prima di qualsiasi Activity o Service ed è il posto ideale
 * per inizializzare componenti che devono vivere per l'intera vita dell'app (es. Database Singleton).
 */
class TravelCompanionApplication : Application() { // Estende la classe Android Application

    // Ereditiamo il lazy-loading del database dal Singleton definito in AppDatabase.kt.
    // L'istanza del database viene creata solo al primo accesso (lazy).
    private val database by lazy { AppDatabase.getDatabase(this) } // Inizializzazione lazy del database Singleton usando il contesto dell'app

    // Ereditiamo il lazy-loading del repository, che dipende dal DAO del database.
    // Viene creata una singola istanza del repository per tutta la vita dell'applicazione.
    val repository by lazy { TripRepository(database.tripDao()) } // Inizializzazione lazy del Repository, iniettando il TripDao

    // Il companion object con il Context non è strettamente necessario se si usa 'this'
    // nell'inizializzazione lazy, ma è mantenuto qui per il riferimento.
    companion object {
        lateinit var context: Context // Variabile per tenere traccia del contesto dell'applicazione
    }

    override fun onCreate() {
        super.onCreate()
        // Imposta il contesto per l'uso nel Singleton del database (se necessario, ma AppDatabase.kt gestisce già il Singleton con 'this')
        context = applicationContext
    }
}