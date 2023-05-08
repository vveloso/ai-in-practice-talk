package com.book.example.common

import android.graphics.Bitmap
import java.io.Closeable

interface Classifier : Closeable {

    fun classify(bitmap: Bitmap): List<Pair<String, Float>>

}
