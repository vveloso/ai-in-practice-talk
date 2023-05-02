package com.book.example.animecamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.experimental.UseExperimental
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
import androidx.camera.lifecycle.ExperimentalUseCaseGroupLifecycle
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.book.example.animecamera.processing.AnimeGanModel
import com.book.example.animecamera.processing.AnimeTransformation
import com.book.example.animecamera.processing.saveImageToGallery
import kotlinx.android.synthetic.main.fragment_camera_preview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalUseCaseGroup
@ExperimentalUseCaseGroupLifecycle
class CameraPreviewFragment : Fragment() {

    private lateinit var imageCapture: ImageCapture
    private lateinit var transformation: AnimeTransformation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_camera_preview, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transformation = AnimeTransformation(requireContext())

        if (cameraPermissionsGranted()) {
            configureCamera()
        } else {
            requestPermissions(
                CAMERA_PERMISSIONS_REQUESTED,
                PERMISSION_REQUEST_CODE)
        }

        btnTakePicture.setOnClickListener {
            takePicture()
        }
    }

    override fun onResume() {
        super.onResume()

        progressBar.visibility = View.INVISIBLE
        btnTakePicture.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        transformation.close()
    }

    private fun takePicture() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        progressBar.visibility = View.VISIBLE
                        btnTakePicture.visibility = View.INVISIBLE
                        saveImageToGallery(
                            requireContext(),
                            transformation.transform(image)
                        )
                        ?.also {
                            val action = CameraPreviewFragmentDirections
                                .actionCameraPreviewFragmentToPictureFragment(
                                    it.toString()
                                )
                            requireView().findNavController().navigate(action)
                        }
                        progressBar.visibility = View.INVISIBLE
                        btnTakePicture.visibility = View.VISIBLE

                        image.close()
                    }
                }
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Failed to capture the picture.", exception)
                    Toast.makeText(context, R.string.picture_not_captured,
                        Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun configureCamera() {
        val cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            configureCameraUseCase(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(context))
    }

    private fun configureCameraUseCase(
            cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(requireActivity().
                windowManager.defaultDisplay.rotation)
            .setTargetResolution(Size(
                AnimeGanModel.IMAGE_WIDTH,
                AnimeGanModel.IMAGE_HEIGHT))
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageCapture)
        previewView.viewPort?.let {
            useCaseGroup.setViewPort(it)
        }

        cameraProvider.unbindAll()

        cameraProvider.bindToLifecycle(this, cameraSelector,
            useCaseGroup.build())
    }

    private fun cameraPermissionsGranted() = CAMERA_PERMISSIONS_REQUESTED.all {
            ContextCompat.checkSelfPermission(
                requireActivity().baseContext, it) == PackageManager.PERMISSION_GRANTED
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
                Toast.makeText(context, R.string.permissions_not_granted,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private companion object {
        private val CAMERA_PERMISSIONS_REQUESTED = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private const val PERMISSION_REQUEST_CODE = 100
        private const val TAG = "[Camera]"
    }
}