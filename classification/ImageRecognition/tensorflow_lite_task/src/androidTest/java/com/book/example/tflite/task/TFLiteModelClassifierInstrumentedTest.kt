package com.book.example.tflite.task

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class TFLiteModelClassifierInstrumentedTest {

    @Test
    fun classify() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appContext = instrumentation.targetContext

        val input = instrumentation.context.resources.assets.open("shirt.png")
        val bitmap = BitmapFactory.decodeStream(input)

        val classifier = TFLiteModelClassifier(appContext)
        val result = classifier.classify(bitmap)
        classifier.close()

        MatcherAssert.assertThat(result.size, Matchers.`is`(10))
        val topResult = result[0]
        MatcherAssert.assertThat(topResult.first, Matchers.`is`("T-shirt/top"))
        MatcherAssert.assertThat(topResult.second, Matchers.`is`(Matchers.greaterThan(0.50f)))
    }

}