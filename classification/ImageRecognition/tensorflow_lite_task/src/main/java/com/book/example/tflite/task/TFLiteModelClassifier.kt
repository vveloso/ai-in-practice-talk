package com.book.example.tflite.task

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.book.example.common.Classifier
import com.book.example.tflite.task.ml.FashionMnistModel
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.model.Model

class TFLiteModelClassifier(context: Context) : Classifier {

    private val model: FashionMnistModel

    init {
        val options =
            Model.Options.Builder().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    setDevice(Model.Device.NNAPI)
                }
            }
            .build()
        model = FashionMnistModel.newInstance(context, options)
    }

    override fun close() {
        model.close()
    }

    override fun classify(bitmap: Bitmap): List<Pair<String, Float>> =
        model.process(TensorImage.fromBitmap(bitmap))
            .probabilityAsCategoryList
            .sortedByDescending { category -> category.score }
            .map { category ->
                Pair(category.label, category.score * 100.0f) }

}