package com.book.example.facerecognition

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.book.example.facerecognition.databinding.FragmentFaceRecognitionBinding
import com.book.example.facerecognition.model.IdentityViewModel
import com.book.example.facerecognition.processing.FaceEmbeddings
import com.book.example.facerecognition.processing.RealtimeFaceEmbeddingsExtractor
import com.book.example.facerecognition.processing.configureCamera
import com.book.example.facerecognition.views.IdentitiesListAdapter
import com.book.example.facerecognition.views.Identity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FaceRecognitionFragment : Fragment() {

    private lateinit var binding: FragmentFaceRecognitionBinding

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageAnalyser: RealtimeFaceEmbeddingsExtractor
    private lateinit var identitiesListAdapter: IdentitiesListAdapter

    private val viewModel: IdentityViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        identitiesListAdapter = IdentitiesListAdapter(requireContext())
        binding = FragmentFaceRecognitionBinding.inflate(
            inflater, container, false)
        binding.listIdentities.adapter = identitiesListAdapter
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        imageAnalyser = RealtimeFaceEmbeddingsExtractor(requireContext(),
            this::onFacesDetected)

        binding.previewView.post {
            if (cameraPermissionsGranted()) {
                setUpCamera()
            } else {
                requestPermissions(
                    CAMERA_PERMISSIONS_REQUESTED,
                    PERMISSION_REQUEST_CODE
                )
            }
        }

    }

    override fun onStop() {
        super.onStop()
        imageAnalyser.close()
    }

    private fun onFacesDetected(faces: List<FaceEmbeddings>) {
        lifecycleScope.launch(Dispatchers.Main) {
            val info = faces.map {
                Identity(
                    it.face,
                    viewModel.recogniseOrNull(it.embeddings)
                )
            }
            identitiesListAdapter.identities = info
        }
    }

    private fun onAddIdentityButtonClicked(view: View) {
        requireView().findNavController().navigate(
            FaceRecognitionFragmentDirections
                .actionFaceRecognitionFragmentToAddNewFaceFragment()
        )
    }

    private fun setUpCamera() {
        configureCamera(this, cameraExecutor,
                binding.previewView, imageAnalyser,
            CameraSelector.LENS_FACING_BACK) {
            binding.btnAddIdentity.setOnClickListener(
                this::onAddIdentityButtonClicked)
        }
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
                setUpCamera()
            } else {
                Toast.makeText(requireContext(), R.string.permissions_not_granted,
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
    }

}