package com.example.travelcompanionapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.travelcompanionapp.viewmodel.TripViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.Locale
import android.location.Geocoder

/**
 * Schermata per la selezione della destinazione tramite OpenStreetMap (OSMDroid).
 * L'utente trascina la mappa e il punto centrale è la destinazione.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSelectionScreen(
    // Funzione per restituire il nome e le coordinate al ViewModel
    onDestinationSelected: (String, Double, Double) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Inizializza la configurazione di OSMDroid (necessario prima di creare la MapView)
    // Questo è il modo standard per configurare la cache dei tile
    Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))

    // Stato per la posizione del Marker/Centro (GeoPoint)
    val defaultLocation = GeoPoint(45.4642, 9.1900) // Esempio: Milano
    var mapCenter by remember { mutableStateOf(defaultLocation) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Seleziona Destinazione Mappa") }) },
        bottomBar = {
            Button(
                onClick = {
                    // 1. Usa Android Geocoder per ottenere un nome leggibile dalle coordinate
                    val lat = mapCenter.latitude
                    val lng = mapCenter.longitude

                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lng, 1)

                    val addressName = if (addresses != null && addresses.isNotEmpty()) {
                        addresses[0].getAddressLine(0) ?: "Posizione Selezionata"
                    } else {
                        "Posizione (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
                    }

                    // 2. Passa i dati al callback del ViewModel e torna indietro
                    onDestinationSelected(addressName, lat, lng)
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("Conferma Posizione")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Usa AndroidView per integrare la MapView tradizionale di OSMDroid
            AndroidView(
                factory = {
                    MapView(it).apply {
                        // Imposta la sorgente delle tile (es. Mapnik, che è lo standard OSM)
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)

                        // Imposta la posizione iniziale
                        controller.setZoom(15.0)
                        controller.setCenter(defaultLocation)

                        // Aggiunge un listener per aggiornare la posizione centrale
                        // ogni volta che la mappa si muove (o si ferma).
                        addMapListener(object : org.osmdroid.events.MapListener {
                            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                                // Aggiorna lo stato Compose ogni volta che la mappa si muove
                                mapCenter = mapCenter
                                return true
                            }
                            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                                mapCenter = mapCenter
                                return true
                            }
                        })
                    }
                },
                update = { map ->
                    // Questa funzione garantisce che l'oggetto GeoPoint sia sempre aggiornato
                    // con il centro corrente della mappa.
                    mapCenter = map.mapCenter as GeoPoint
                },
                modifier = Modifier.fillMaxSize()
            )

            // Marker centrale fisso (per indicare il punto di selezione)
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = "Marker di Selezione",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center) // Centra l'icona sullo schermo
                    .offset(y = (-24).dp) // Solleva leggermente l'icona per centrarla sulla punta
            )
        }
    }
}