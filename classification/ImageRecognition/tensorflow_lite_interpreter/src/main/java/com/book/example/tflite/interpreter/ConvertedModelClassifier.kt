package com.book.example.tflite.interpreter

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.book.example.common.Classifier
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.TransformToGrayscaleOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer

class ConvertedModelClassifier(context: Context) : Classifier {

    private val interpreter: Interpreter
    private val modelFile: ByteBuffer

    private val inputTensor: Tensor

    private val outputBuffer: TensorBuffer
    private val outputProcessor: TensorProcessor = TensorProcessor.Builder()
        .add(NormalizeOp(0.0f, 1.0f))
        .build()

    private val labels: List<String> = FileUtil.loadLabels(
        context, "labels-fashion.txt")

    init {
        val compatibility = CompatibilityList()
        val options = Interpreter.Options().apply {
            if (compatibility.isDelegateSupportedOnThisDevice) {
                addDelegate(GpuDelegate(
                    compatibility.bestOptionsForThisDevice
                ))
            }
        }
        compatibility.close()

        modelFile = FileUtil.loadMappedFile(
            context, "converted-fashion.tflite")
        interpreter = Interpreter(modelFile, options)

        inputTensor = interpreter.getInputTensor(0)

        val outputTensor = interpreter.getOutputTensor(0)
        outputBuffer = TensorBuffer.createFixedSize(
            outputTensor.shape(), outputTensor.dataType())
    }

    override fun classify(bitmap: Bitmap): List<Pair<String, Float>> =
        classify(loadImage(bitmap))

    override fun close() {
        interpreter.close()
    }

    private fun classify(image: TensorImage): List<Pair<String, Float>> {
        interpreter.run(
            image.buffer,
            outputBuffer.buffer.rewind())

        return TensorLabel(labels, outputProcessor.process(outputBuffer))
            .mapWithFloatValue
            .map { (key, value) -> Pair(key, value * 100.0f) }
            .sortedByDescending { (_, value) -> value }
    }

    private fun loadImage(bitmap: Bitmap)
            : TensorImage {
        val inputShape = inputTensor.shape() // {1, width, height, channels}
        val imageHeight = inputShape[1]
        val imageWidth = inputShape[2]

        val tensorImage = TensorImage(inputTensor.dataType())
        tensorImage.load(bitmap)
        return ImageProcessor.Builder()
                .add(ResizeOp(imageWidth, imageHeight,
                    ResizeOp.ResizeMethod.BILINEAR))
                .add(TransformToGrayscaleOp())
                .build()
                .process(tensorImage)
    }

}