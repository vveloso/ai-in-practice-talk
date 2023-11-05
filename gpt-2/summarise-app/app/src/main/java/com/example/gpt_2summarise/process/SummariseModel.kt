package com.example.gpt_2summarise.process

import android.content.Context
import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

private const val SUMMARY_TOKEN = "TL;DR:"
private const val OUTPUT_SIZE = 1600

class SummariseModel(private val context: Context, private val dispatcher: CoroutineDispatcher = Dispatchers.IO): AutoCloseable {

    private lateinit var interpreter: Interpreter
    private var isInitialised : AtomicBoolean = AtomicBoolean(false)
    private val outputBuffer = ByteBuffer.allocateDirect(OUTPUT_SIZE)

    suspend fun initialise() = withContext(dispatcher) {
        if (isInitialised.get()) {
            return@withContext
        }
        val options = Interpreter.Options().apply {
            numThreads = Runtime.getRuntime().availableProcessors()
        }
        val model = FileUtil.loadMappedFile(context, "summarise.tflite")
        interpreter = Interpreter(model, options)
        isInitialised.set(true)
    }

    suspend fun getSummary(input: String) : String = withContext(dispatcher) {
        if (!isInitialised.get()) {
            return@withContext ""
        }
        val joinedInput = input.replace('\n', ' ')
        val output = summarise("$joinedInput $SUMMARY_TOKEN ")
        val sections = output.split(SUMMARY_TOKEN)
        if (sections.size < 2) "no summary" else sections[1]
    }

    override fun close() {
        if (isInitialised.getAndSet(false)) {
            interpreter.close()
        }
    }

    @WorkerThread
    private fun summarise(input: String) : String {
        outputBuffer.clear()

        interpreter.run(input, outputBuffer)

        outputBuffer.flip()
        val bytes = ByteArray(outputBuffer.remaining())
        outputBuffer.get(bytes)

        outputBuffer.clear()

        return String(bytes, Charsets.UTF_8)
    }

}