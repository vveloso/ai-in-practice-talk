package com.book.example.facerecognition.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.renderscript.*
import androidx.camera.core.ImageProxy

class ImageProxyYuvConversion(context: Context) {

    private val rs = RenderScript.create(context)
    private val script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    fun toBitmap(
        imageProxy: ImageProxy,
        bounds: Rect
    ): Bitmap {
        val yuvBytes = imageProxy.toYuvByteArray()

        val yuvType = Type.Builder(rs, Element.U8(rs))
            .setX(yuvBytes.size)
            .create()
        val input = Allocation.createTyped(
            rs, yuvType, Allocation.USAGE_SCRIPT
        )

        val bitmap = Bitmap.createBitmap(
            imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
        )
        val output = Allocation.createFromBitmap(rs, bitmap)

        input.copyFrom(yuvBytes)
        script.setInput(input)
        script.forEach(output)

        output.copyTo(bitmap)

        input.destroy()
        output.destroy()

        val matrix = Matrix()
        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

        return Bitmap.createBitmap(
            bitmap, bounds.left, bounds.top,
            bounds.width(), bounds.height(),
            matrix, true
        )
    }

}

// Because only RGB images are supported in TensorFlow Lite's ResizeOp, but not YUV_420_888.
private fun ImageProxy.toYuvByteArray(): ByteArray {
    require(format == ImageFormat.YUV_420_888)
    { "Invalid image format" }

    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    return nv21
}
