package com.book.example.photocaption.storage

import androidx.camera.core.ImageProxy

val ImageProxy.jpegData: ByteArray
get() {
    val jpegBuffer = planes[0].buffer
    val jpegSize = jpegBuffer.rewind().remaining()
    val data = ByteArray(jpegSize)
    jpegBuffer.get(data)
    return data
}
