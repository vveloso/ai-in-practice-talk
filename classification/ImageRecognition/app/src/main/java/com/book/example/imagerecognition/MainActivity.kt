package com.book.example.imagerecognition

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import com.book.example.tflite.task.TFLiteModelClassifier
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageAnalyser: ImageAnalyser

    private val mainThreadHandler: Handler =
        HandlerCompat.createAsync(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraExecutor = Executors.newSingleThreadExecutor()
        imageAnalyser = cameraExecutor.submit(Callable {
                ImageAnalyser(
                    this,
                    TFLiteModelClassifier(this)) { result ->
                    mainThreadHandler.post {
                        onPredictionResult(result)
                    }
                }
            })
            .get()

        previewView.post {
            if (cameraPermissionsGranted()) {
                configureCamera()
            } else {
                requestPermissions(
                    CAMERA_PERMISSIONS_REQUESTED,
                    PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        imageAnalyser.close()
    }

    private fun configureCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            configureCameraUseCase(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun configureCameraUseCase(
        cameraProvider: ProcessCameraProvider
    ) {
        val preview = Preview.Builder()
            .build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().apply {
                setAnalyzer(
                    cameraExecutor,
                    imageAnalyser
                )
            }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageAnalysis)
        previewView.viewPort?.let {
            useCaseGroup.setViewPort(it)
        }

        cameraProvider.unbindAll()

        cameraProvider.bindToLifecycle(this, cameraSelector,
            useCaseGroup.build())
    }

    private fun onPredictionResult(result: List<Pair<String, Float>>) {
        txtClassificationResult.text = result
            .subList(0, 2)
            .map { "%s (%.0f%%)".format(it.first, it.second) }
            .joinToString(", ")
    }

    private fun cameraPermissionsGranted() = CAMERA_PERMISSIONS_REQUESTED.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (cameraPermissionsGranted()) {
                configureCamera()
            } else {
                Toast.makeText(this, R.string.permissions_not_granted,
                    Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private companion object {
        private val CAMERA_PERMISSIONS_REQUESTED = arrayOf(
            Manifest.permission.CAMERA)
        private const val PERMISSION_REQUEST_CODE = 100
        private const val TAG = "[ImageRecognition]"
    }

}