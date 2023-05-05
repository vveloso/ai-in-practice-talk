package com.book.example.photocaption.processing

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageProxy
import com.book.example.photocaption.storage.jpegData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants.EXIF_TAG_USER_COMMENT
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants.*
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

suspend fun saveImageToGallery(context: Context, image: ImageProxy, description: String)
    : Uri? = withContext(Dispatchers.IO) {

    val name = "anime-" + SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(System.currentTimeMillis())

    val resolver = context.contentResolver
    val imageUri = resolver
        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                pendingContentValues(name))
    imageUri?.let {
        resolver.openOutputStream(it)
    }?.use { output ->
        saveWithComment(image, description, output)
        resolver.update(imageUri, completedContentValues(),
            null, null)
    }

    return@withContext imageUri
}

private fun saveWithComment(image: ImageProxy, comment: String, output: OutputStream) {
    val outputSet = TiffOutputSet()
    outputSet.orCreateExifDirectory.add(EXIF_TAG_USER_COMMENT, comment) // also see https://exiftool.org/TagNames/EXIF.html
    outputSet.rootDirectory.add(TIFF_TAG_ORIENTATION, orientation(image))
    ExifRewriter().updateExifMetadataLossless(
        image.jpegData,
        output,
        outputSet
    )
}

private fun orientation(image: ImageProxy) =
    when (image.imageInfo.rotationDegrees) {
          90 -> ORIENTATION_VALUE_ROTATE_90_CW
         180 -> ORIENTATION_VALUE_ROTATE_180
         270 -> ORIENTATION_VALUE_ROTATE_270_CW
        else -> ORIENTATION_VALUE_HORIZONTAL_NORMAL
    }.toShort()

private fun pendingContentValues(name: String) =
    ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

private fun completedContentValues() =
    ContentValues().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.IS_PENDING, 0)
        }
    }