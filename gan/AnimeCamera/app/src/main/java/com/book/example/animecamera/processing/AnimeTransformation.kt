package com.book.example.animecamera.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Environment
import android.util.Size
import androidx.camera.core.ImageProxy
import com.book.example.animecamera.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AnimeTransformation(context: Context) : AutoCloseable {

    private val model = AnimeGanModel(context)

    override fun close() {
        model.close()
    }

    suspend fun transform(proxy: ImageProxy): Bitmap =
        withContext(Dispatchers.IO) {
            val bitmap = bitmapFromJpeg(proxy)
            return@withContext model.process(
                bitmap, proxy.imageInfo.rotationDegrees)
        }

    private fun bitmapFromJpeg(proxy: ImageProxy): Bitmap {
        val jpegBuffer = proxy.planes[0].buffer
        val jpegSize = jpegBuffer.remaining()
        val data = ByteArray(jpegSize)
        jpegBuffer.get(data)
        return BitmapFactory.decodeByteArray(data, 0, data.size)
    }

}