package com.book.example.tflite.interpreter

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.Image
import android.media.ImageReader
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.greaterThan
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConvertedModelClassifierInstrumentedTest {

    @Test
    fun classify() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appContext = instrumentation.targetContext

        val input = instrumentation.context.resources.assets.open("shirt.png")
        val bitmap = BitmapFactory.decodeStream(input)

        val classifier = ConvertedModelClassifier(appContext)
        val result = classifier.classify(bitmap)
        classifier.close()

        assertThat(result.size, `is`(10))
        val topResult = result[0]
        assertThat(topResult.first, `is`("T-shirt/top"))
        assertThat(topResult.second, `is`(greaterThan(0.90f)))
    }

}