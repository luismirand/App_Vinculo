package com.unison.binku

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream

object ImageCompressor {

    /**
     * Comprime una imagen desde una URI y la devuelve como un Array de Bytes.
     * @param context Contexto de la aplicación.
     * @param imageUri La URI (content://) de la imagen a comprimir.
     * @param quality La calidad deseada (0-100). 80 es un buen balance.
     * @param maxSizeKb El tamaño máximo en kilobytes que debe tener la imagen.
     * @return ByteArray listo para subir a Firebase Storage, o null si falla.
     */
    fun compressImage(context: Context, imageUri: Uri, quality: Int = 80, maxSizeKb: Int = 500): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            var currentQuality = quality
            var outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)

            // Bucle para reducir la calidad hasta que la imagen sea lo suficientemente pequeña
            while (outputStream.size() / 1024 > maxSizeKb && currentQuality > 10) {
                Log.d("ImageCompressor", "Comprimiendo... Tamaño actual: ${outputStream.size() / 1024}KB, Calidad: $currentQuality")
                outputStream.reset() // Limpia el stream
                currentQuality -= 10 // Reduce la calidad
                bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, outputStream)
            }

            Log.d("ImageCompressor", "Compresión finalizada. Tamaño: ${outputStream.size() / 1024}KB, Calidad: $currentQuality")

            val byteArray = outputStream.toByteArray()
            outputStream.close()
            bitmap.recycle() // Libera la memoria del bitmap
            byteArray

        } catch (e: Exception) {
            Log.e("ImageCompressor", "Error al comprimir imagen", e)
            null
        }
    }
}