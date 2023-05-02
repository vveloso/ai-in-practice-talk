package com.book.example.photocaption.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.book.example.photocaption.storage.jpegData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.ln

/**
 * This implementation is based on the im2txt CaptionGenerator.
 */
class Captioning(context: Context) : AutoCloseable {

    private val model = ShowAndTellModel(context)

    private val vocabulary = Vocabulary(context)

    override fun close() {
        model.close()
    }

    suspend fun caption(proxy: ImageProxy): List<String> =
        withContext(Dispatchers.IO) {
            return@withContext caption(
                bitmapFromJpeg(proxy)
            )
        }

    internal fun caption(image: Bitmap): List<String> {
        val partialCaptions = TopN<Caption>(BEAM_SIZE)
        val completeCaptions = TopN<Caption>(BEAM_SIZE)

        fun captions(useCompleteCaptions: () -> Boolean) =
            if (useCompleteCaptions())
                completeCaptions
            else
                partialCaptions

        fun inference(partialList: List<Caption>): InferenceResults {
            val inputFeed = partialList.map { it.sentence.last() }.toLongArray()
            val stateFeed = partialList.map { it.state }.toTypedArray()
            return model.inferenceStep(inputFeed, stateFeed)
        }

        val initialState = model.feedImage(image)

        partialCaptions.push(Caption(
            listOf(vocabulary.startId.toLong()), initialState, 0.0
        ))

        for (l in 0 until MAX_CAPTION_LENGTH) {
            val partialList = partialCaptions.extract()
            val inferred = inference(partialList)
            for ((partialIndex, partialCaption) in partialList.withIndex()) {
                val wordProbabilities = findTopBeamValues(
                    inferred.softmax[partialIndex])
                for (wordProbability in wordProbabilities) {
                    if (wordProbability.value < 1e-12) continue // avoid ln(0)
                    val caption = Caption(
                        partialCaption.sentence + wordProbability.index.toLong(),
                        inferred.state[partialIndex],
                        partialCaption.logprob + ln(wordProbability.value.toDouble())
                    )
                    captions { wordProbability.index == vocabulary.endId }
                        .push(caption)
                }
            }
            if (partialCaptions.isEmpty()) break
        }

        return captionsToStrings(
            captions { !completeCaptions.isEmpty() }
        )
    }

    private fun captionsToStrings(captions: TopN<Caption>) =
        captions.extract(sorted = true)
            .map { caption ->
                caption.sentence
                    .map { it.toInt() }
                    .filter { it != vocabulary.startId && it != vocabulary.endId }
                    .map { vocabulary.idToWord[it] }
                    .joinToString(" ")
            }

    private fun findTopBeamValues(array: FloatArray): Array<IndexedValue<Float>> {
        val topValues = Array(BEAM_SIZE) { popMax(array) }
        topValues.sortByDescending { it.value }
        return topValues
    }

    private fun popMax(array: FloatArray): IndexedValue<Float> {
        val index = maxIndex(array)
        val value = IndexedValue(index, array[index])
        array[index] = Float.MIN_VALUE // "pop" the value so it's no longer the maximum
        return value
    }

    private fun maxIndex(array: FloatArray): Int {
        var max = Float.MIN_VALUE
        var index = 0
        for (i in array.indices) {
            if (array[i] > max) {
                max = array[i]
                index = i
            }
        }
        return index
    }

    private fun bitmapFromJpeg(proxy: ImageProxy): Bitmap {
        val bounds = proxy.cropRect
        val data = proxy.jpegData
        val matrix = Matrix()
        matrix.postRotate(proxy.imageInfo.rotationDegrees.toFloat())
        val original = BitmapFactory.decodeByteArray(data, 0, data.size)
        return Bitmap.createBitmap(
            original, bounds.left, bounds.top,
            bounds.width(), bounds.height(),
            matrix, true
        )
    }

    companion object {
        private const val BEAM_SIZE = 3
        private const val MAX_CAPTION_LENGTH = 20
    }
}