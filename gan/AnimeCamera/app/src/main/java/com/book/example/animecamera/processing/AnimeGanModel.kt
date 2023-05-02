package com.book.example.animecamera.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.util.Size
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer


class AnimeGanModel(context: Context): AutoCloseable {

    private val inputDataType: DataType
    private val interpreter: Interpreter
    private val modelFile: ByteBuffer

    init {
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        modelFile = FileUtil.loadMappedFile(
            context, "AnimeGANv2_Hayao-64.tflite")
        interpreter = Interpreter(modelFile, options)
        interpreter.resizeInput(0,
            intArrayOf(1, IMAGE_WIDTH, IMAGE_HEIGHT, 3))

        inputDataType = interpreter.getInputTensor(0).dataType()
    }

    fun process(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val ticks = System.currentTimeMillis()
        val outputBuffer = allocateOutputBuffer()
        interpreter.run(
            loadImage(bitmap, rotationDegrees),
            outputBuffer.buffer.rewind()
        )
        Log.i(TAG, "Processing time: ${System.currentTimeMillis() - ticks}ms")
        return postprocess(outputBuffer.floatArray)
    }

    override fun close() {
        interpreter.close()
    }

    private fun postprocess(data: FloatArray): Bitmap {
        val pixelCount = (data.size / 3)
        val pixels = IntArray(pixelCount) // ARGB
        var floatPos = 0
        for (i in 0 until pixelCount) {
            pixels[i] = Color.rgb(
                ((data[floatPos++] + 1.0f) / 2.0f * 255.0f).toInt(),
                ((data[floatPos++] + 1.0f) / 2.0f * 255.0f).toInt(),
                ((data[floatPos++] + 1.0f) / 2.0f * 255.0f).toInt()
            )
        }
        return Bitmap.createBitmap(pixels, IMAGE_WIDTH, IMAGE_HEIGHT,
            Bitmap.Config.ARGB_8888)
    }

    private fun buildProcessor(rotationDegrees: Int) =
        ImageProcessor.Builder()
            .add(
                ResizeOp(IMAGE_WIDTH, IMAGE_HEIGHT,
                    ResizeOp.ResizeMethod.BILINEAR)
            )
            .add(
                Rot90Op(4 - rotationDegrees / 90)
            )
            .add(
                NormalizeOp(127.5f , 127.5f)
            )
            .build()


    private fun loadImage(bitmap: Bitmap, rotationDegrees: Int)
            : ByteBuffer {
        val tensorImage = TensorImage(inputDataType)
        tensorImage.load(bitmap)
        return buildProcessor(rotationDegrees)
            .process(tensorImage)
            .buffer
    }

    private fun allocateOutputBuffer(): TensorBuffer =
        interpreter.getOutputTensor(0).let {
            TensorBuffer.createFixedSize(
                intArrayOf(1, IMAGE_WIDTH, IMAGE_HEIGHT, 3),
                it.dataType())
        }

    companion object {
        const val IMAGE_WIDTH = 256
        const val IMAGE_HEIGHT = 256
        private const val TAG = "AnimeGANv2"
    }
}