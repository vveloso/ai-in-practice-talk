package com.book.example.animecamera.processing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

suspend fun saveImageToGallery(context: Context, bitmap: Bitmap)
    : Uri? = withContext(Dispatchers.IO) {

    val name = "anime-" + SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(System.currentTimeMillis());

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val imageUri = resolver
        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
    imageUri?.let {
        resolver.openOutputStream(it)
    }?.use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        contentValues.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        }
        resolver.update(imageUri, contentValues, null, null)
    }

    return@withContext imageUri
}
