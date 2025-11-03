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
 * ⭐ COMPONIBILE PER SCATTARE FOTO DURANTE IL VIAGGIO
 *
 * Questo componibile riutilizzabile gestisce:
 * - Richiesta permessi camera
 * - Apertura della fotocamera
 * - Salvataggio della foto con coordinate GPS
 * - Aggiunta di didascalia opzionale
 * - Visualizzazione anteprima ultime foto scattate
 *
 * Può essere utilizzato in TripTrackingScreen o altre schermate.
 *
 * @param tripId ID del viaggio corrente
 * @param currentLocation Posizione GPS corrente (se disponibile)
 * @param viewModel ViewModel per salvare le foto nel database
 */
@Composable
fun PhotoCaptureSection(
    tripId: Int,
    currentLocation: Location?,
    viewModel: TripViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Stato per l'URI dell'ultima foto scattata
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Stato per mostrare il dialog di aggiunta didascalia
    var showCaptionDialog by remember { mutableStateOf(false) }

    // Stato temporaneo per il percorso della foto appena scattata
    var tempPhotoPath by remember { mutableStateOf<String?>(null) }

    // Osserva le ultime 3 foto del viaggio per l'anteprima
    val recentPhotos by viewModel.getRecentPhotosForTrip(tripId).collectAsState(initial = emptyList())

    /**
     * LAUNCHER PER RICHIEDERE IL PERMESSO CAMERA
     */
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Se il permesso non è concesso, mostra un messaggio
            // In una app reale, potresti mostrare un Dialog per spiegare perché serve il permesso
        }
    }

    /**
     * LAUNCHER PER SCATTARE LA FOTO
     */
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            // La foto è stata scattata con successo!
            // Ora salviamo il file in modo permanente
            scope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    PhotoHelper.saveImageFromUri(context, photoUri!!, tripId)
                }

                if (savedPath != null) {
                    tempPhotoPath = savedPath
                    // Mostra il dialog per aggiungere una didascalia
                    showCaptionDialog = true
                }
            }
        }
    }

    /**
     * FUNZIONE PER AVVIARE LA FOTOCAMERA
     */
    fun launchCamera() {
        // Prima controlla se abbiamo il permesso
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Abbiamo il permesso, possiamo procedere
                try {
                    // Crea un file temporaneo per la foto
                    val photoFile = PhotoHelper.createImageFile(context, tripId)

                    // Crea un URI utilizzando il FileProvider
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )

                    photoUri = uri
                    takePictureLauncher.launch(uri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            else -> {
                // Non abbiamo il permesso, lo richiediamo
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * FUNZIONE PER SALVARE LA FOTO NEL DATABASE
     */
    suspend fun savePhotoToDatabase(caption: String?) {
        if (tempPhotoPath == null) return

        // Ottieni il nome della posizione dal GPS se disponibile
        val locationName = currentLocation?.let { location ->
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    )

                    addresses?.firstOrNull()?.let { address ->
                        buildString {
                            address.locality?.let { append(it).append(", ") }
                            address.countryName?.let { append(it) }
                        }.trim().removeSuffix(",")
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }

        // Crea l'oggetto TripPhoto
        val photo = TripPhoto(
            tripId = tripId,
            filePath = tempPhotoPath!!,
            timestamp = Date(),
            caption = caption?.takeIf { it.isNotBlank() },
            latitude = currentLocation?.latitude,
            longitude = currentLocation?.longitude,
            locationName = locationName
        )

        // Salva nel database
        viewModel.insertPhoto(photo)

        // Reset stato temporaneo
        tempPhotoPath = null
    }

    // === UI SECTION ===

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Titolo sezione
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = TravelGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Foto",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TravelGreen
                    )
                }

                // Badge con numero foto
                if (recentPhotos.isNotEmpty()) {
                    Surface(
                        color = TravelGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${recentPhotos.size}+",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TravelGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pulsante per scattare foto
            Button(
                onClick = { launchCamera() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TravelGreen
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.AddAPhoto,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scatta Foto")
            }

            // Anteprima ultime foto (se presenti)
            if (recentPhotos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Ultime foto:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentPhotos) { photo ->
                        PhotoThumbnailSmall(photo = photo)
                    }
                }
            }
        }
    }

    // === DIALOG PER AGGIUNGERE DIDASCALIA ===

    if (showCaptionDialog) {
        var caption by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                // Salva senza didascalia se l'utente chiude il dialog
                scope.launch {
                    savePhotoToDatabase(null)
                }
                showCaptionDialog = false
            },
            title = {
                Text("Aggiungi didascalia")
            },
            text = {
                Column {
                    Text(
                        "Vuoi aggiungere una descrizione alla foto?",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Didascalia (opzionale)") },
                        placeholder = { Text("Es: Panorama mozzafiato!") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            savePhotoToDatabase(caption)
                        }
                        showCaptionDialog = false
                    }
                ) {
                    Text("Salva", color = TravelGreen)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            savePhotoToDatabase(null)
                        }
                        showCaptionDialog = false
                    }
                ) {
                    Text("Salta", color = Color.Gray)
                }
            }
        )
    }
}

/**
 * Miniatura piccola per le anteprime nella sezione foto.
 */
@Composable
fun PhotoThumbnailSmall(photo: TripPhoto) {
    Card(
        modifier = Modifier.size(80.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        val bitmap = remember(photo.filePath) {
            PhotoHelper.loadThumbnail(photo.filePath, 160, 160)
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = photo.caption ?: "Foto del viaggio",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.BrokenImage,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }
    }
}