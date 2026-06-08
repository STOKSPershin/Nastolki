package com.tbgames.app.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

object ImageHelper {
    
    fun getCompressedAvatarBytes(context: Context, uri: Uri, maxSize: Int = 400): ByteArray? {
        try {
            var bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            if (bitmap == null) return null

            // Handle rotation from EXIF
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                inputStream.close()
            }

            // Crop to square
            val minEdge = minOf(bitmap.width, bitmap.height)
            val dx = (bitmap.width - minEdge) / 2
            val dy = (bitmap.height - minEdge) / 2
            val squareBitmap = Bitmap.createBitmap(bitmap, dx, dy, minEdge, minEdge)

            // Scale down if needed
            val scaledBitmap = if (minEdge > maxSize) {
                Bitmap.createScaledBitmap(squareBitmap, maxSize, maxSize, true)
            } else {
                squareBitmap
            }

            // Compress to JPEG
            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val byteArray = stream.toByteArray()
            
            // Clean up
            if (bitmap != scaledBitmap) bitmap.recycle()
            if (squareBitmap != scaledBitmap) squareBitmap.recycle()
            scaledBitmap.recycle()

            return byteArray
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
