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
 */
// gestisce tutte le operazioni relative alle immagini (foto scattate o salvate)

object PhotoHelper {

    //funzione che crea un file temporaneo per salvare una foto scattata con la fotocamera
    fun createImageFile(context: Context, tripId: Int): File {
        //crea un timestamp per rendere univoco il nome del file
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALIAN).format(Date())
        val fileName = "trip_${tripId}_photo_${timeStamp}.jpg" //costruisce il nome file con id viaggio e orario

        //ottiene la directory privata dell’app dove salvare le immagini
        val storageDir = File(context.filesDir, "Pictures")

        //crea la directory se non esiste già
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        //ritorna il file pronto per essere utilizzato dalla fotocamera
        return File(storageDir, fileName)
    }

    //funzione che salva un’immagine proveniente da un uri in un file permanente e la comprime
    fun saveImageFromUri(context: Context, sourceUri: Uri, tripId: Int): String? {
        return try {
            //crea il file di destinazione dove salvare la foto
            val destinationFile = createImageFile(context, tripId)

            //apre uno stream per leggere l’immagine da uri
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val bitmap = BitmapFactory.decodeStream(inputStream) //decodifica i byte in un bitmap
            inputStream?.close()

            if (bitmap != null) {
                //ruota l’immagine se necessario in base ai metadati exif
                val rotatedBitmap = rotateImageIfRequired(context, sourceUri, bitmap)

                //salva l’immagine ruotata e compressa
                saveCompressedImage(rotatedBitmap, destinationFile)

                //libera memoria dai bitmap temporanei
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                }
                rotatedBitmap.recycle()

                //ritorna il percorso assoluto del file salvato
                destinationFile.absolutePath
            } else {
                null //in caso di errore nella lettura restituisce null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    //funzione privata che salva un bitmap su file comprimendolo in jpeg all’80% di qualità
    private fun saveCompressedImage(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { outputStream ->
            //comprimi l’immagine e scrivila nel file di destinazione
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        }
    }

    //funzione privata che ruota un’immagine leggendo i metadati exif per correggere l’orientamento
    private fun rotateImageIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            //apre lo stream per leggere i metadati exif dell’immagine
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = inputStream?.let { ExifInterface(it) }
            inputStream?.close()

            //legge l’orientamento dal tag exif
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            //determina l’angolo di rotazione corretto
            val rotation = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            //se non è necessaria rotazione restituisce il bitmap originale
            if (rotation == 0f) {
                return bitmap
            }

            //crea una matrice di trasformazione per ruotare l’immagine
            val matrix = Matrix()
            matrix.postRotate(rotation)

            //applica la rotazione e restituisce il nuovo bitmap ruotato
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace() //stampa l’errore per debug
            bitmap //in caso di errore restituisce l’immagine originale
        }
    }

    //funzione che elimina fisicamente un file immagine dal dispositivo
    fun deletePhotoFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete() //elimina il file se esiste
            } else {
                false //ritorna false se il file non esiste
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    //funzione che legge le dimensioni di un’immagine senza caricarla completamente in memoria
    fun getImageDimensions(filePath: String): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                //inJustDecodeBounds=true consente di leggere solo larghezza e altezza senza decodificare l’immagine
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)
            Pair(options.outWidth, options.outHeight) //ritorna coppia larghezza e altezza
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    //funzione che carica un’immagine ridimensionata per l’anteprima o miniature
    fun loadThumbnail(filePath: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            //ottiene le dimensioni originali dell’immagine
            val dimensions = getImageDimensions(filePath) ?: return null

            //calcola il fattore di riduzione da applicare per il ridimensionamento
            val sampleSize = calculateSampleSize(
                dimensions.first,
                dimensions.second,
                targetWidth,
                targetHeight
            )

            //decodifica l’immagine usando il sampleSize calcolato
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            BitmapFactory.decodeFile(filePath, options)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    //funzione privata che calcola il fattore ottimale di ridimensionamento (potenza di 2)
    private fun calculateSampleSize(
        originalWidth: Int,
        originalHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var sampleSize = 1 //valore iniziale (nessuna riduzione)

        if (originalHeight > targetHeight || originalWidth > targetWidth) {
            val halfHeight = originalHeight / 2
            val halfWidth = originalWidth / 2

            //incrementa sampleSize fino a mantenere un’immagine non più piccola del target
            while (halfHeight / sampleSize >= targetHeight && halfWidth / sampleSize >= targetWidth) {
                sampleSize *= 2
            }
        }

        return sampleSize //ritorna il valore ottimale calcolato
    }

    //funzione che controlla se un file immagine esiste fisicamente nel percorso indicato
    fun photoFileExists(filePath: String): Boolean {
        return try {
            File(filePath).exists() //ritorna true se il file esiste
        } catch (e: Exception) {
            false
        }
    }

    //funzione che calcola la dimensione di un file foto in kilobyte
    fun getPhotoFileSizeKB(filePath: String): Long {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.length() / 1024 //divide per 1024 per convertire da byte a kilobyte
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
}