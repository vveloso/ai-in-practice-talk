package com.book.example.photocaption.processing

import android.content.Context
import org.tensorflow.lite.support.common.FileUtil.loadSingleColumnTextFile
import java.util.*
import kotlin.collections.HashMap

/*
 * This implementation is based on the im2txt CaptionGenerator.
 */

class Vocabulary(context: Context) {

    val idToWord: Map<Int, String>
    val startId : Int
    val endId : Int

    init {
        val words = loadSingleColumnTextFile(context, "word_counts.txt", Charsets.UTF_8)
            .map { it.split(" ")[0] }

        check(words.containsAll(listOf(START_WORD, END_WORD))) { "Incomplete vocabulary" }

        val wordToId = HashMap<String, Int>(words.size)
        idToWord = HashMap(words.size)

        var index = 0
        for (word in words) {
            wordToId[word] = index
            idToWord[index] = word
            index++
        }

        if (!wordToId.containsKey(UNK_WORD)) {
            wordToId[UNK_WORD] = index
            idToWord[index] = UNK_WORD
        }

        startId = wordToId[START_WORD]!!
        endId = wordToId[END_WORD]!!
    }

    companion object {
        private const val START_WORD = "<S>"
        private const val END_WORD = "</S>"
        private const val UNK_WORD = "<UNK>"
    }
}

data class Caption(val sentence: List<Long>, val state: FloatArray, val logprob: Double)
    : Comparable<Caption> {

    override fun compareTo(other: Caption): Int =
        when {
            this.logprob == other.logprob -> 0
            this.logprob < other.logprob  -> -1
            else -> 1
        }

}

internal class TopN<T: Comparable<T>>(private val n: Int) {

    private val queue = PriorityQueue<T>(n)

    fun push(element: T) {
        queue.offer(element)
        if (queue.size == n) {
            queue.poll()
        }
    }

    fun extract(sorted: Boolean = false) =
        queue.toList()
            .let { if (sorted) it.sorted() else it }
            .also { queue.clear() }

    fun isEmpty() =
        queue.size == 0
}
