package com.glucodes.swarmdoc.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ImageUtils {
    /**
     * Saves a Bitmap to the device's public pictures directory using MediaStore for modern Android versions,
     * and direct file creation for older versions.
     */
    suspend fun saveBitmapToDevice(context: Context, bitmap: Bitmap, fileName: String): String? {
        return withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SwarmDoc")
                }
            }

            var uri: Uri? = null
            var success = false

            try {
                uri = resolver.insert(imageCollection, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                        success = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            }
            
            if (success) uri?.toString() else null
        }
    }
}
