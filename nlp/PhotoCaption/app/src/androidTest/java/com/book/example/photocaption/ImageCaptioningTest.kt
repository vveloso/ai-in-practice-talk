package com.book.example.photocaption

import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.book.example.photocaption.processing.Captioning
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageCaptioningTest {
    @Test
    fun caption() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appContext = instrumentation.targetContext

        val input = instrumentation.context.resources.assets.open("test-square.jpg")
        val bitmap = BitmapFactory.decodeStream(input)

        val analyser = Captioning(appContext)

        val caption = analyser.caption(bitmap)
        assertThat(caption, not(`is`(emptyList())))

        analyser.close()

        Log.i("TEST", caption.joinToString("\n"))
    }
}