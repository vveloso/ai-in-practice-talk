package com.book.example.facerecognition.processing

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.graphics.toRect
import androidx.core.graphics.toRectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

data class FaceEmbeddings(
    val face: Bitmap,
    val embeddings: FloatArray
)

class RealtimeFaceEmbeddingsExtractor(
        context: Context,
        private val receiver: (List<FaceEmbeddings>) -> Unit)
    : ImageAnalysis.Analyzer, AutoCloseable {

    private val extractor: FaceNetEmbeddingsExtractor =
        FaceNetEmbeddingsExtractor(context)
    private val detector: FaceDetector =
        FaceDetectorBuilder.build()
    private val conversion: ImageProxyYuvConversion =
        ImageProxyYuvConversion(context)

    private val closureLock: Lock = ReentrantLock()
    @Volatile private var closed: Boolean = false

    override fun analyze(proxy: ImageProxy) {
        try {
            closureLock.lock()
            if (!closed) {
                process(proxy)
            }
        } finally {
            closureLock.unlock()
        }
    }

    override fun close() {
        try {
            closureLock.lock()
            closed = true
            extractor.close()
            detector.close()
        } finally {
            closureLock.unlock()
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
    private fun process(proxy: ImageProxy) {
        val originalImage = proxy.image
        if (originalImage == null) {
            proxy.close()
            return
        }
        detector.process(InputImage.fromMediaImage(
                originalImage, proxy.imageInfo.rotationDegrees))
            .addOnSuccessListener {
                if (it.isNotEmpty()) {
                    facesDetected(proxy, it)
                } else {
                    receiver(emptyList())
                }
            }
            .addOnFailureListener {
                receiver(emptyList())
            }
            .addOnCompleteListener {
                proxy.close()
            }
    }

    private fun facesDetected(proxy: ImageProxy, faces: List<Face>) {
        if (closed) {
            return
        }
        val imageBounds = Rect(0, 0, proxy.width, proxy.height)
        val imageBitmap = conversion.toBitmap(proxy, imageBounds)
        receiver(
            faces.filter {
                it.boundingBox.setIntersect(imageBounds, it.boundingBox)
            }.map {
                val faceBitmap = Bitmap.createBitmap(imageBitmap,
                    it.boundingBox.left, it.boundingBox.top,
                    it.boundingBox.width(), it.boundingBox.height())
                FaceEmbeddings(
                    faceBitmap,
                    extractor.embeddings(faceBitmap)
                )
            }
        )
    }

}
