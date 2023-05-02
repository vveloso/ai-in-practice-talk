package com.book.example.facerecognition.processing

import kotlin.math.pow
import kotlin.math.sqrt

fun euclidianDistance(a: FloatArray, b: FloatArray): Float {
    var sum = 0.0f
    for (i in a.indices) {
        sum += (a[i] - b[i]).pow(2)
    }
    return sqrt(sum)
}
