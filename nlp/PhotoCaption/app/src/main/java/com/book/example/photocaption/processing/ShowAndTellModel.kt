package com.book.example.photocaption.processing

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
import java.nio.FloatBuffer


internal class ShowAndTellModel(context: Context): AutoCloseable {

    private val imageNet: Interpreter
    private val ltsm: Interpreter
    private val imageNetModelFile: ByteBuffer
    private val ltsmModelFile: ByteBuffer

    init {
        val options = Interpreter.Options().apply {
            setNumThreads(4)
            setUseNNAPI(true)
        }
        imageNetModelFile = FileUtil.loadMappedFile(
            context, "imagenet.tflite")
        imageNet = Interpreter(imageNetModelFile, options)
        ltsmModelFile = FileUtil.loadMappedFile(
            context, "ltsm.tflite")
        ltsm = Interpreter(ltsmModelFile, options)
    }

    override fun close() {
        imageNet.close()
        ltsm.close()
    }

    fun feedImage(bitmap: Bitmap): FloatArray {
        val ticks = System.currentTimeMillis()
        val outputBuffer = imageNet.allocateOutputBuffer("lstm/initial_state")
        imageNet.runForMultipleInputsOutputs(
            arrayOf(loadImage(bitmap)),
            mapOf(imageNet.getOutputIndex("lstm/initial_state") to outputBuffer)
        )
        Log.i(TAG, "Image feed time: ${System.currentTimeMillis() - ticks}ms")
        return outputBuffer.array()
    }

    fun inferenceStep(inputFeed: LongArray, stateFeed: Array<FloatArray>): InferenceResults {
        val ticks = System.currentTimeMillis()
        ltsm.resizeInput(ltsm.getInputIndex(LTSM_INPUT_FEED), intArrayOf(inputFeed.size))
        ltsm.resizeInput(ltsm.getInputIndex(LTSM_STATE_FEED), intArrayOf(stateFeed.size, stateFeed[0].size))
        val softmaxBuffer = ltsm.allocateOutputBuffer(inputFeed.size, LTSM_SOFTMAX_OUTPUT)
        val stateBuffer = ltsm.allocateOutputBuffer(inputFeed.size, LTSM_STATE_OUTPUT)
        ltsm.runForMultipleInputsOutputs(
            arrayOf(inputFeed, stateFeed),
            mapOf(
                ltsm.getOutputIndex(LTSM_SOFTMAX_OUTPUT) to softmaxBuffer,
                ltsm.getOutputIndex(LTSM_STATE_OUTPUT) to stateBuffer
            )
        )
        Log.i(TAG, "Inference step time: ${System.currentTimeMillis() - ticks}ms")
        return InferenceResults(softmaxBuffer, stateBuffer)
    }

    private fun buildProcessor() =
        ImageProcessor.Builder()
            .add(
                ResizeOp(IMAGE_WIDTH, IMAGE_HEIGHT,
                    ResizeOp.ResizeMethod.BILINEAR)
            )
            .add(
                NormalizeOp(127.5f , 127.5f)
            )
            .build()


    private fun loadImage(bitmap: Bitmap)
            : ByteBuffer {
        val tensorImage = TensorImage(
            imageNet.getInputIndex(IMAGENET_INPUT_TENSOR)
            .let { imageNet.getInputTensor(it).dataType() }
        )
        tensorImage.load(bitmap)
        return buildProcessor()
            .process(tensorImage)
            .buffer
    }

    companion object {
        const val IMAGE_WIDTH = 299
        const val IMAGE_HEIGHT = 299

        private const val IMAGENET_INPUT_TENSOR = "ExpandDims_3"

        private const val LTSM_INPUT_FEED = "input_feed"
        private const val LTSM_STATE_FEED = "lstm/state_feed"
        private const val LTSM_SOFTMAX_OUTPUT = "softmax"
        private const val LTSM_STATE_OUTPUT = "lstm/state"

        private const val TAG = "im2txt"
    }
}

data class InferenceResults(val softmax: Array<FloatArray>, val state: Array<FloatArray>)

private fun Interpreter.allocateOutputBuffer(batchCount: Int, name: String): Array<FloatArray> =
    getOutputTensor(getOutputIndex(name))
        .let { tensor -> Array(batchCount) { FloatArray(tensor.shape()[1]) { 0.0f } } }

private fun Interpreter.allocateOutputBuffer(name: String): FloatBuffer =
    getOutputTensor(getOutputIndex(name))
        .let { FloatBuffer.allocate(it.shape()[1]) }
