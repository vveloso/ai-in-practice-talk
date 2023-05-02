package com.book.example.facerecognition.processing

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer

class FaceNetEmbeddingsExtractor(context: Context): AutoCloseable {

    private val interpreter: Interpreter
    private val modelFile: ByteBuffer

    private val inputTensor: Tensor
    private val imageProcessor: ImageProcessor
    private val outputBuffer: TensorBuffer

    init {
        modelFile = FileUtil.loadMappedFile(
            context, "mobilefacenet.tflite")
        interpreter = Interpreter(modelFile)

        inputTensor = interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape() // {1, width, height, channels}
        val imageWidth = inputShape[1]
        val imageHeight = inputShape[2]
        imageProcessor = ImageProcessor.Builder()
            .add(
                ResizeOp(imageWidth, imageHeight,
                    ResizeOp.ResizeMethod.BILINEAR)
            )
            .add(
                NormalizeOp(127.5f , 128f)
            )
            .build()

        outputBuffer =
            interpreter.getOutputTensor(0).let {
                TensorBuffer.createFixedSize(
                    it.shape(), it.dataType())
            }
    }

    override fun close() {
        interpreter.close()
    }

    fun embeddings(bitmap: Bitmap): FloatArray {
        val ticks = System.currentTimeMillis()
        interpreter.run(
            loadImage(bitmap),
            outputBuffer.buffer.rewind()
        )
        Log.i(TAG, "Extraction time: ${System.currentTimeMillis() - ticks}ms")
        return outputBuffer.floatArray
    }

    private fun loadImage(bitmap: Bitmap)
            : ByteBuffer {
        val tensorImage = TensorImage(inputTensor.dataType())
        tensorImage.load(bitmap)
        return imageProcessor
            .process(tensorImage)
            .buffer
    }

    companion object {
        const val TAG = "FaceNet"
    }
}
