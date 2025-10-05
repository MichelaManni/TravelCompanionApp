package com.example.travelcompanionapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Schermata per selezionare una destinazione dalla mappa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSelectionScreen(
    onDestinationSelected: (name: String, lat: Double, lng: Double) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Coordinate selezionate (inizialmente Roma come esempio)
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var destinationName by remember { mutableStateOf("") }

    // Configurazione OSMDroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Seleziona Destinazione",
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna Indietro",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    // Pulsante di conferma (visibile solo se una posizione è selezionata)
                    if (selectedLocation != null) {
                        IconButton(
                            onClick = {
                                selectedLocation?.let { location ->
                                    val name = if (destinationName.isBlank()) {
                                        "Posizione: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                                    } else {
                                        destinationName
                                    }
                                    onDestinationSelected(name, location.latitude, location.longitude)
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Conferma Selezione",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Campo per inserire il nome della destinazione
            if (selectedLocation != null) {
                OutlinedTextField(
                    value = destinationName,
                    onValueChange = { destinationName = it },
                    label = { Text("Nome Destinazione (opzionale)", color = Color.Black) },
                    placeholder = { Text("Es: Roma, Colosseo", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }

            // Mappa OSMDroid
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)

                        // Centra su Roma come default
                        val startPoint = GeoPoint(41.9028, 12.4964)
                        controller.setZoom(10.0)
                        controller.setCenter(startPoint)

                        // Marker per la posizione selezionata
                        val marker = Marker(this).apply {
                            title = "Destinazione Selezionata"
                            isDraggable = true
                        }

                        // Listener per il click sulla mappa
                        setOnTouchListener { _, event ->
                            if (event.action == android.view.MotionEvent.ACTION_UP) {
                                val projection = this.projection
                                val geoPoint = projection.fromPixels(
                                    event.x.toInt(),
                                    event.y.toInt()
                                ) as GeoPoint

                                selectedLocation = geoPoint

                                // Aggiorna il marker
                                marker.position = geoPoint
                                if (!overlays.contains(marker)) {
                                    overlays.add(marker)
                                }
                                invalidate()
                            }
                            false
                        }

                        // Listener per il drag del marker
                        marker.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                            override fun onMarkerDrag(marker: Marker?) {}
                            override fun onMarkerDragStart(marker: Marker?) {}
                            override fun onMarkerDragEnd(marker: Marker?) {
                                marker?.position?.let { position ->
                                    selectedLocation = position
                                }
                            }
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Informazioni sulla posizione selezionata
            if (selectedLocation != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Posizione Selezionata",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lat: ${String.format("%.4f", selectedLocation!!.latitude)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                        Text(
                            text = "Lng: ${String.format("%.4f", selectedLocation!!.longitude)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tocca l'icona ✓ in alto per confermare",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Istruzioni iniziali
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Tocca la mappa per selezionare una destinazione",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}