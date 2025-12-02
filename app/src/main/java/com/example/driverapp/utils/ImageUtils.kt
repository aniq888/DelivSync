package com.example.driverapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {
    private const val MAX_IMAGE_SIZE = 800 // Max width/height in pixels
    private const val COMPRESSION_QUALITY = 80 // JPEG quality (0-100)
    private const val MAX_BASE64_SIZE = 900000 // ~900KB to stay under Firestore's 1MB limit

    /**
     * Compress and convert image to base64 string
     * @param context Application context
     * @param imageUri URI of the image to process
     * @return Base64 encoded string or null if failed
     */
    suspend fun imageToBase64(context: Context, imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            if (inputStream == null) {
                Log.e("ImageUtils", "Could not open input stream")
                return@withContext null
            }

            // Decode image
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Calculate sample size to reduce memory usage
            options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE)
            options.inJustDecodeBounds = false

            // Decode with sample size
            val inputStream2 = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, options)
            inputStream2?.close()

            if (bitmap == null) {
                Log.e("ImageUtils", "Could not decode bitmap")
                return@withContext null
            }

            // Compress bitmap (returns new bitmap if scaled, or original if not)
            val compressedBitmap = compressBitmap(bitmap)
            val isNewBitmap = compressedBitmap != bitmap
            
            // Convert to base64 BEFORE recycling any bitmaps
            val base64 = bitmapToBase64(compressedBitmap)
            
            // Check if we need more aggressive compression
            if (base64.length > MAX_BASE64_SIZE) {
                Log.w("ImageUtils", "Image still too large after compression, trying more aggressive compression")
                
                // Recycle bitmaps before creating new ones
                if (isNewBitmap) {
                    compressedBitmap.recycle()
                }
                bitmap.recycle()
                
                // Try more aggressive compression with fresh bitmap
                val inputStream3 = context.contentResolver.openInputStream(imageUri)
                val bitmap2 = BitmapFactory.decodeStream(inputStream3, null, options)
                inputStream3?.close()
                
                if (bitmap2 != null) {
                    val moreCompressed = compressBitmapAggressively(bitmap2)
                    bitmap2.recycle()
                    val base64Final = bitmapToBase64(moreCompressed)
                    moreCompressed.recycle()
                    return@withContext base64Final
                }
                
                // If aggressive compression failed, return the original base64
                return@withContext base64
            }
            
            // Recycle bitmaps after successful conversion
            if (isNewBitmap) {
                compressedBitmap.recycle()
            }
            bitmap.recycle()

            base64
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error converting image to base64", e)
            null
        }
    }

    /**
     * Convert base64 string back to bitmap
     */
    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error converting base64 to bitmap", e)
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        var width = bitmap.width
        var height = bitmap.height

        // Scale down if too large
        if (width > MAX_IMAGE_SIZE || height > MAX_IMAGE_SIZE) {
            val scale = minOf(
                MAX_IMAGE_SIZE.toFloat() / width,
                MAX_IMAGE_SIZE.toFloat() / height
            )
            width = (width * scale).toInt()
            height = (height * scale).toInt()
        }

        return if (width != bitmap.width || height != bitmap.height) {
            // Create new scaled bitmap
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            // Return original bitmap (no scaling needed)
            bitmap
        }
    }

    private fun compressBitmapAggressively(bitmap: Bitmap): Bitmap {
        val targetSize = 400 // Smaller target for aggressive compression
        var width = bitmap.width
        var height = bitmap.height

        if (width > targetSize || height > targetSize) {
            val scale = minOf(
                targetSize.toFloat() / width,
                targetSize.toFloat() / height
            )
            width = (width * scale).toInt()
            height = (height * scale).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
        val byteArray = outputStream.toByteArray()
        outputStream.close()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}

