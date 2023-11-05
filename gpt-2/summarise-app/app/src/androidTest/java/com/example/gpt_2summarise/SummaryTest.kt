package com.example.gpt_2summarise

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gpt_2summarise.process.SummariseModel
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class SummaryTest {
    @Test
    fun createSummary() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val appContext = instrumentation.targetContext

        val summariser = SummariseModel(appContext)
        summariser.initialise()

        val result = summariser.getSummary(
            """
                All flights have been suspended in London's Luton Airport following the breakout of 
                a "significant" fire in the airport's Terminal 2 parking lot, the airport said in a 
                statement on Wednesday.
            """.trimIndent()
        )

        summariser.close()

        assert(result.isNotEmpty())
        assertNotEquals("no summary", result)
    }
}