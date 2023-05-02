package com.book.example.facerecognition

import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.book.example.facerecognition.processing.FaceDetectorBuilder
import com.book.example.facerecognition.processing.FaceNetEmbeddingsExtractor
import com.book.example.facerecognition.processing.euclidianDistance
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.Test

class FaceNetEmbeddingsExtractorTest {

    @Test
    fun embeddings() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appContext = instrumentation.targetContext

        val bitmap = extractFace(instrumentation, "identity_1_a.jpg")

        val embeddings = FaceNetEmbeddingsExtractor(appContext)
                            .embeddings(bitmap)

        assertThat(embeddings, `is`(notNullValue()))
        assertThat(embeddings.size, `is`(greaterThan(0)))

        Log.i(TAG, embeddings.joinToString(",") { "${it}f" })
    }

    @Test
    fun distances() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appContext = instrumentation.targetContext

        val id1 = extractFace(instrumentation, "identity_1_a.jpg")
        val id2 = extractFace(instrumentation, "identity_1_b.jpg")
        val other = extractFace(instrumentation, "identity_2.jpg")

        val extractor = FaceNetEmbeddingsExtractor(appContext)

        val embeddingsId1 = extractor.embeddings(id1)
        val embeddingsId2 = extractor.embeddings(id2)
        val embeddingsOther = extractor.embeddings(other)

        val distanceId = euclidianDistance(embeddingsId1, embeddingsId2)
        val distanceOther = euclidianDistance(embeddingsId1, embeddingsOther)

        Log.i(TAG, "ID A - ID B = $distanceId, ID 1 - Other = $distanceOther")

        assertThat(distanceId, `is`(lessThan(1.2f)))
        assertThat(distanceOther, `is`(greaterThan(1.2f)))
    }

    private val detector: FaceDetector = FaceDetectorBuilder.build()

    private fun extractFace(instrumentation: Instrumentation, imageFile: String): Bitmap =
        instrumentation.context.resources.assets.open(imageFile).let { stream ->
            val decoded = BitmapFactory.decodeStream(stream)
            stream.close()
            decoded
        }.let { bitmap ->
            Tasks.await(detector.process(InputImage.fromBitmap(bitmap, 0)))
                .first().let {
                    val bounds = it.boundingBox
                    Bitmap.createBitmap(
                        bitmap, bounds.left, bounds.top,
                        bounds.width(), bounds.height())
                }
        }

    companion object {
        const val TAG = "Embeddings"
    }
}