package com.book.example.imagerecognition

import android.content.Context
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.book.example.common.Classifier
import java.io.Closeable
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

class ImageAnalyser(
        context: Context,
        private val classifier: Classifier,
        private val resultListener: (List<Pair<String,Float>>) -> Unit)
        : ImageAnalysis.Analyzer, Closeable {

    private val rs = RenderScript.create(context)
    private val script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    private val closureLock: Lock = ReentrantLock()
    private var closed: Boolean = false

    override fun analyze(imageProxy: ImageProxy) {
        try {
            closureLock.lock()
            if (!closed) {
                val result = classifier.classify(
                    imageProxy.toBitmap(rs, script)
                )
                resultListener(result)
            }
        } finally {
            closureLock.unlock()
            imageProxy.close()
        }
    }

    override fun close() {
        try {
            closureLock.lock()
            closed = true
            classifier.close()
        } finally {
            closureLock.unlock()
        }
    }

}
