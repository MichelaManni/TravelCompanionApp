package com.example.travelcompanionapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.travelcompanionapp.data.TripPhoto
import com.example.travelcompanionapp.utils.PhotoHelper
import com.example.travelcompanionapp.viewmodel.TripViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
/**
 *  PER SCATTARE FOTO DURANTE IL VIAGGIO
 *
 * gestisce:
 * - Richiesta permessi camera
 * - Apertura della fotocamera
 * - Salvataggio della foto con coordinate GPS
 * - Aggiunta di didascalia opzional
 * - Visualizzazione anteprima ultime foto scattate
 *
 *  utilizzato in TripTrackingScreen
 */
@Composable //elemento grafico riutilizzabile
fun PhotoCaptureSection(
    tripId: Int, // ID del viaggio a cui associare le foto
    currentLocation: Location?, // Posizione GPS corrente (può essere null se GPS non disponibile)
    viewModel: TripViewModel // ViewModel per accedere al database
) {
    val context = LocalContext.current // contesto corrente dell'applicazione
    val scope = rememberCoroutineScope() // Crea uno scope per lanciare coroutine

    // Stato per l'URI dell'ultima foto scattata
    var photoUri by remember { mutableStateOf<Uri?>(null) } // Variabile che memorizza il percorso temporaneo della foto

    // Stato per mostrare il dialog di aggiunta didascalia
    var showCaptionDialog by remember { mutableStateOf(false) } // Flag per mostrare/nascondere il dialog

    // Stato temporaneo per il percorso della foto appena scattata
    var tempPhotoPath by remember { mutableStateOf<String?>(null) } // Percorso permanente della foto salvata

    // Osserva le ultime 3 foto del viaggio per l'anteprima
    val recentPhotos by viewModel.getRecentPhotosForTrip(tripId).collectAsState(initial = emptyList()) // Flow che osserva le foto dal database

    /**
     * LAUNCHER PER RICHIEDERE IL PERMESSO CAMERA
     */
    val cameraPermissionLauncher = rememberLauncherForActivityResult( // Crea un launcher per richiedere permessi
        contract = ActivityResultContracts.RequestPermission() // Contratto per richiedere un singolo permesso
    ) { isGranted -> // Callback chiamato con il risultato (true se concesso, false altrimenti)
        if (!isGranted) { // Se il permesso non è stato concesso

        }
    }

    /**
     * LAUNCHER PER SCATTARE LA FOTO
     */
    val takePictureLauncher = rememberLauncherForActivityResult( // Crea un launcher per scattare foto
        contract = ActivityResultContracts.TakePicture() // Contratto per scattare una foto
    ) { success -> // Callback chiamato con il risultato (true se foto scattata, false altrimenti)
        if (success && photoUri != null) { // Se la foto è stata scattata con successo e abbiamo l'URI
            // salviamo il file in modo permanente
            scope.launch { // Lancia una coroutine
                val savedPath = withContext(Dispatchers.IO) { // Esegue nel thread IO
                    PhotoHelper.saveImageFromUri(context, photoUri!!, tripId) // Salva l'immagine dall'URI temporaneo a percorso permanente
                }

                if (savedPath != null) { // Se il salvataggio è andato a buon fine
                    tempPhotoPath = savedPath // Memorizza il percorso permanente
                    // Mostra il dialog per aggiungere una didascalia
                    showCaptionDialog = true // Attiva il flag per mostrare il dialog
                }
            }
        }
    }

    /**
     * FUNZIONE PER AVVIARE LA FOTOCAMERA
     */
    fun launchCamera() { // Funzione che avvia la fotocamera
        // Prima controlla se abbiamo il permesso
        when { // Struttura when per gestire diversi casi
            ContextCompat.checkSelfPermission( // Controlla se abbiamo il permesso
                context, // Contesto dell'applicazione
                Manifest.permission.CAMERA // Permesso della fotocamera
            ) == PackageManager.PERMISSION_GRANTED -> { // Se abbiamo già il permesso
                // Abbiamo il permesso, possiamo procedere
                try {
                    // Crea un file temporaneo per la foto
                    val photoFile = PhotoHelper.createImageFile(context, tripId) // Crea un file vuoto per la foto

                    // Crea un URI utilizzando il FileProvider
                    val uri = FileProvider.getUriForFile( // Crea un URI sicuro per il file
                        context, // Contesto
                        "${context.packageName}.fileprovider", // Authority del FileProvider (definito in AndroidManifest.xml)
                        photoFile // File da condividere
                    )

                    photoUri = uri // Memorizza l'URI
                    takePictureLauncher.launch(uri) // Lancia la fotocamera passando l'URI dove salvare la foto
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            else -> { // Se non abbiamo il permesso
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA) // Richiede il permesso della fotocamera
            }
        }
    }

    /**
     * FUNZIONE PER SALVARE LA FOTO NEL DATABASE
     */
    suspend fun savePhotoToDatabase(caption: String?) { // Funzione sospendibile per salvare nel database
        if (tempPhotoPath == null) return // Se non c'è un percorso foto, esce dalla funzione

        // Ottieni il nome della posizione dal GPS se disponibile
        val locationName = currentLocation?.let { location -> // Se abbiamo la posizione GPS
            withContext(Dispatchers.IO) { // Esegue nel thread IO
                try {
                    val geocoder = Geocoder(context, Locale.getDefault()) // Crea un geocoder
                    val addresses = geocoder.getFromLocation( // Converte coordinate in indirizzi
                        location.latitude, // Latitudine
                        location.longitude, // Longitudine
                        1 // Numero massimo di risultati
                    )

                    addresses?.firstOrNull()?.let { address -> // Se abbiamo trovato un indirizzo
                        buildString { // Costruisce una stringa
                            address.locality?.let { append(it).append(", ") } // Aggiunge la città
                            address.countryName?.let { append(it) } // Aggiunge il paese
                        }.trim().removeSuffix(",") // Rimuove spazi e virgola finale
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }

        // Crea l'oggetto TripPhoto
        val photo = TripPhoto( // Crea un nuovo oggetto foto
            tripId = tripId,
            filePath = tempPhotoPath!!, // Percorso del file (!! = sicuro che non è null)
            timestamp = Date(), // Data e ora corrente
            caption = caption?.takeIf { it.isNotBlank() }, // Didascalia (null se vuota)
            latitude = currentLocation?.latitude, // Latitudine (null se GPS non disponibile)
            longitude = currentLocation?.longitude, // Longitudine (null se GPS non disponibile)
            locationName = locationName // Nome del luogo (null se non trovato)
        )

        // Salva nel database
        viewModel.insertPhoto(photo) // Chiama il ViewModel per salvare nel database

        // Reset stato temporaneo
        tempPhotoPath = null // Resetta il percorso temporaneo
    }

    //  UI SECTION
    Card( // Contenitore con bordi arrotondati
        modifier = Modifier.fillMaxWidth(), // Occupa tutta la larghezza
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp), // Ombra leggera
        shape = RoundedCornerShape(12.dp) // Angoli arrotondati
    ) {
        Column( //elementi verticalmente
            modifier = Modifier.padding(16.dp) // Spazio interno
        ) {
            // Titolo
            Row( //  elementi orizzontalmente
                modifier = Modifier.fillMaxWidth(), // Occupa tutta la larghezza
                horizontalArrangement = Arrangement.SpaceBetween, // Distribuisce spazio tra gli elementi
                verticalAlignment = Alignment.CenterVertically // Allinea verticalmente al centro
            ) {
                Row( // Sotto-riga per icona e testo
                    verticalAlignment = Alignment.CenterVertically // Allinea verticalmente al centro
                ) {
                    Icon( // Icona della fotocamera
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = TravelGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Spazio
                    Text(
                        text = "Foto", //titolo
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TravelGreen
                    )
                }

                // Badge con numero foto
                if (recentPhotos.isNotEmpty()) { // Se ci sono foto da mostrare
                    Surface( // Superficie con sfondo colorato
                        color = TravelGreen.copy(alpha = 0.2f), // Verde chiaro (20% di opacità)
                        shape = RoundedCornerShape(12.dp) // Angoli molto arrotondati
                    ) {
                        Text(
                            text = "${recentPhotos.size}+", // Mostra il numero di foto (es. "3+")
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), // Spazio interno
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TravelGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp)) // Spazio verticale

            // Pulsante per scattare foto
            Button( // Pulsante Material Design
                onClick = { launchCamera() }, // Quando si clicca, avvia la fotocamera
                modifier = Modifier.fillMaxWidth(), // Occupa tutta la larghezza
                colors = ButtonDefaults.buttonColors(
                    containerColor = TravelGreen
                )
            ) {
                Icon( // Icona nel pulsante
                    imageVector = Icons.Filled.AddAPhoto, // Icona fotocamera con +
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp)) // Spazio tra icona e testo
                Text("Scatta Foto") // Testo del pulsante
            }

            // Anteprima ultime foto (se presenti)
            if (recentPhotos.isNotEmpty()) { // Se ci sono foto da mostrare
                Spacer(modifier = Modifier.height(12.dp)) // Spazio verticale

                Text(
                    text = "Ultime foto:", // Etichetta
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp)) // Spazio verticale

                LazyRow( // Riga scorrevole orizzontalmente
                    horizontalArrangement = Arrangement.spacedBy(8.dp) // Spazio tra ogni foto
                ) {
                    items(recentPhotos) { photo -> // Per ogni foto nella lista
                        PhotoThumbnailSmall(photo = photo) // Mostra la miniatura della foto
                    }
                }
            }
        }
    }

    //  DIALOG PER AGGIUNGERE DIDASCALIA
    if (showCaptionDialog) { // Se il flag è attivo, mostra il dialog
        var caption by remember { mutableStateOf("") } // Variabile per memorizzare il testo della didascalia

        AlertDialog( // Dialog di Material Design
            onDismissRequest = { // Funzione chiamata quando l'utente chiude il dialog (tocca fuori o preme indietro)
                // Salva senza didascalia se l'utente chiude il dialog
                scope.launch { // Lancia una coroutine
                    savePhotoToDatabase(null) // Salva la foto senza didascalia
                }
                showCaptionDialog = false // Nasconde il dialog
            },
            title = { // Titolo del dialog
                Text("Aggiungi didascalia") // Testo del titolo
            },
            text = { // Contenuto del dialog
                Column { // elementi verticalmente
                    Text(
                        "Vuoi aggiungere una descrizione alla foto?",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp)) // Spazio
                    OutlinedTextField( // Campo di testo con bordo
                        value = caption, // Valore corrente del campo
                        onValueChange = { caption = it }, // Aggiorna il valore quando l'utente scrive
                        label = { Text("Didascalia (opzionale)") }, // Etichetta del campo
                        placeholder = { Text("Es: Panorama mozzafiato!") }, // Testo di esempio mostrato quando il campo è vuoto
                        modifier = Modifier.fillMaxWidth(), // Occupa tutta la larghezza
                        maxLines = 3 // Massimo 3 righe di testo
                    )
                }
            },
            confirmButton = { // Pulsante di conferma
                TextButton( // Pulsante di testo
                    onClick = { // Quando si clicca
                        scope.launch { // Lancia una coroutine
                            savePhotoToDatabase(caption) // Salva la foto con la didascalia
                        }
                        showCaptionDialog = false // Nasconde il dialog
                    }
                ) {
                    Text("Salva", color = TravelGreen) // Testo verde
                }
            },
            dismissButton = { // Pulsante per saltare
                TextButton( // Pulsante di testo
                    onClick = { // Quando si clicca
                        scope.launch { // Lancia una coroutine
                            savePhotoToDatabase(null) // Salva la foto senza didascalia
                        }
                        showCaptionDialog = false // Nasconde il dialog
                    }
                ) {
                    Text("Salta", color = Color.Gray) // Testo grigio
                }
            }
        )
    }
}

/**
 * Miniatura piccola per le anteprime nella sezione foto.
 */
@Composable // elemento grafico riutilizzabile
fun PhotoThumbnailSmall(photo: TripPhoto) { // Riceve un oggetto TripPhoto come parametro
    Card( // Contenitore con bordi arrotondati
        modifier = Modifier.size(80.dp), // Dimensione fissa
        shape = RoundedCornerShape(8.dp), // Angoli arrotondati
        elevation = CardDefaults.cardElevation(2.dp) // Ombra leggera
    ) {
        val bitmap = remember(photo.filePath) { // Memorizza il bitmap (ricrea solo se il percorso cambia)
            PhotoHelper.loadThumbnail(photo.filePath, 160, 160) // Carica una miniatura 160x160 pixel del file
        }

        if (bitmap != null) { // Se il caricamento è andato a buon fine
            Image( // Componente per mostrare immagini
                bitmap = bitmap.asImageBitmap(), // Converte il Bitmap Android in ImageBitmap di Compose
                contentDescription = photo.caption ?: "Foto del viaggio",
                modifier = Modifier.fillMaxSize() // Occupa tutto lo spazio della Card
            )
        } else { // Se il caricamento è fallito
            Box( // Contenitore che centra il contenuto
                modifier = Modifier.fillMaxSize(), // Occupa tutto lo spazio
                contentAlignment = Alignment.Center // Centra il contenuto
            ) {
                Icon( // Icona di errore
                    imageVector = Icons.Filled.BrokenImage, // Icona immagine rotta
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }
    }
}