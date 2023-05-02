package com.book.example.facerecognition.processing

import android.annotation.SuppressLint
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

fun configureCamera(
    ownerFragment: Fragment,
    cameraExecutor: Executor,
    previewView: PreviewView,
    imageAnalyser: ImageAnalysis.Analyzer,
    lensFacing: Int,
    onConfigurationDone: () -> Unit = { }
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(ownerFragment.requireContext())
    cameraProviderFuture.addListener({
        configureCameraUseCase(ownerFragment, cameraExecutor, cameraProviderFuture.get(),
            previewView, imageAnalyser, lensFacing)
        onConfigurationDone()
    }, ContextCompat.getMainExecutor(ownerFragment.requireContext()))
}

@SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
private fun configureCameraUseCase(
    owner: LifecycleOwner,
    cameraExecutor: Executor,
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    imageAnalyser: ImageAnalysis.Analyzer,
    lensFacing: Int
) {
    val preview = Preview.Builder()
        .build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }

    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setTargetResolution(Size(480, 360))
        .build().also {
            it.setAnalyzer(
                cameraExecutor,
                imageAnalyser
            )
        }

    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    val useCaseGroup = UseCaseGroup.Builder()
        .addUseCase(preview)
        .addUseCase(imageAnalysis)
    previewView.viewPort?.let {
        useCaseGroup.setViewPort(it)
    }

    cameraProvider.unbindAll()

    cameraProvider.bindToLifecycle(owner, cameraSelector,
        useCaseGroup.build())
}
