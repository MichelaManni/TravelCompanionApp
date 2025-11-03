package com.example.travelcompanionapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class per gestire le operazioni sulle foto.
 *
 * Fornisce funzioni per:
 * - Creare file temporanei per la camera
 * - Salvare e comprimere immagini
 * - Ruotare immagini in base ai metadati EXIF
 * - Eliminare file fisici
 *
 * ⭐ DESIGN SEMPLICE E COMPRENSIBILE:
 * Ogni funzione ha un solo scopo e commenti chiari per studenti principianti.
 */
object PhotoHelper {

    /**
     * Crea un file temporaneo nella directory delle foto dell'app
     * per salvare la foto scattata dalla camera.
     *
     * Il file viene creato nella cartella privata dell'app:
     * /Android/data/com.example.travelcompanionapp/files/Pictures/
     *
     * @param context Context dell'applicazione
     * @param tripId ID del viaggio (usato per organizzare i file)
     * @return File temporaneo dove salvare la foto
     */
    fun createImageFile(context: Context, tripId: Int): File {
        // Crea un nome file unico usando timestamp
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())
        val fileName = "trip_${tripId}_photo_${timeStamp}.jpg"

        // Directory per salvare le foto (cartella privata dell'app)
        val storageDir = File(context.filesDir, "Pictures")

        // Crea la directory se non esiste
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        // Crea e restituisce il file
        return File(storageDir, fileName)
    }

    /**
     * Salva un'immagine da un Uri in un file permanente.
     * Comprime l'immagine per risparmiare spazio.
     *
     * @param context Context dell'applicazione
     * @param sourceUri Uri dell'immagine da salvare
     * @param tripId ID del viaggio
     * @return Percorso del file salvato
     */
    fun saveImageFromUri(context: Context, sourceUri: Uri, tripId: Int): String? {
        return try {
            // Crea il file di destinazione
            val destinationFile = createImageFile(context, tripId)

            // Leggi l'immagine dall'Uri
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Ruota l'immagine se necessario (alcune foto dalla camera sono ruotate)
                val rotatedBitmap = rotateImageIfRequired(context, sourceUri, bitmap)

                // Salva l'immagine compressa
                saveCompressedImage(rotatedBitmap, destinationFile)

                // Libera la memoria
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                }
                rotatedBitmap.recycle()

                // Restituisci il percorso del file salvato
                destinationFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Salva un Bitmap in un file comprimendolo.
     * Usa compressione JPEG al 80% per un buon compromesso qualità/dimensione.
     *
     * @param bitmap Immagine da salvare
     * @param file File di destinazione
     */
    private fun saveCompressedImage(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { outputStream ->
            // Comprimi l'immagine al 80% di qualità
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        }
    }

    /**
     * Ruota un'immagine in base ai metadati EXIF.
     *
     * Quando scatti una foto con il telefono tenuto verticalmente,
     * l'immagine potrebbe essere salvata ruotata. Questa funzione
     * legge i metadati EXIF e ruota l'immagine nella direzione corretta.
     *
     * @param context Context dell'applicazione
     * @param uri Uri dell'immagine
     * @param bitmap Bitmap da ruotare
     * @return Bitmap ruotato correttamente
     */
    private fun rotateImageIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            // Leggi i metadati EXIF dell'immagine
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()

            // Ottieni l'orientamento dall'EXIF
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            // Calcola l'angolo di rotazione necessario
            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            // Se non serve rotazione, restituisci l'immagine originale
            if (rotation == 0f) {
                return bitmap
            }

            // Crea una matrice di rotazione
            val matrix = Matrix()
            matrix.postRotate(rotation)

            // Applica la rotazione e restituisci la nuova immagine
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Elimina un file fisico dal dispositivo.
     *
     * IMPORTANTE: Questa funzione elimina il file FISICO, non solo il record dal database.
     * Usala quando cancelli una foto che l'utente non vuole più.
     *
     * @param filePath Percorso completo del file da eliminare
     * @return true se l'eliminazione ha successo, false altrimenti
     */
    fun deletePhotoFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Ottiene le dimensioni di un'immagine senza caricarla completamente in memoria.
     * Utile per ottimizzare le prestazioni quando si mostrano miniature.
     *
     * @param filePath Percorso del file immagine
     * @return Pair<width, height> oppure null se errore
     */
    fun getImageDimensions(filePath: String): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                // inJustDecodeBounds = true significa "leggi solo le dimensioni, non caricare l'immagine"
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Carica un'immagine ottimizzata per la visualizzazione in miniatura.
     * Riduce la risoluzione per risparmiare memoria.
     *
     * @param filePath Percorso del file immagine
     * @param targetWidth Larghezza desiderata in pixel
     * @param targetHeight Altezza desiderata in pixel
     * @return Bitmap ridimensionato
     */
    fun loadThumbnail(filePath: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            // Prima ottieni le dimensioni originali
            val dimensions = getImageDimensions(filePath) ?: return null

            // Calcola il fattore di ridimensionamento
            val sampleSize = calculateSampleSize(
                dimensions.first,
                dimensions.second,
                targetWidth,
                targetHeight
            )

            // Carica l'immagine ridimensionata
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            BitmapFactory.decodeFile(filePath, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Calcola il fattore di ridimensionamento ottimale per un'immagine.
     *
     * @param originalWidth Larghezza originale
     * @param originalHeight Altezza originale
     * @param targetWidth Larghezza desiderata
     * @param targetHeight Altezza desiderata
     * @return Fattore di ridimensionamento (potenza di 2)
     */
    private fun calculateSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var sampleSize = 1

        if (originalHeight > targetHeight || originalWidth > targetWidth) {
            val halfHeight = originalHeight / 2
            val halfWidth = originalWidth / 2

            // Calcola il più grande sampleSize (potenza di 2) che mantiene dimensioni >= target
            while (halfHeight / sampleSize >= targetHeight && halfWidth / sampleSize >= targetWidth) {
                sampleSize *= 2
            }
        }

        return sampleSize
    }

    /**
     * Verifica se un file foto esiste fisicamente sul dispositivo.
     *
     * @param filePath Percorso del file
     * @return true se il file esiste, false altrimenti
     */
    fun photoFileExists(filePath: String): Boolean {
        return try {
            File(filePath).exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ottiene la dimensione di un file foto in KB.
     *
     * @param filePath Percorso del file
     * @return Dimensione in KB, oppure 0 se errore
     */
    fun getPhotoFileSizeKB(filePath: String): Long {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.length() / 1024 // Converti byte in KB
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
}