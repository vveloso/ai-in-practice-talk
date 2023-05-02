package com.book.example.facerecognition

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.core.os.HandlerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.book.example.facerecognition.databinding.FragmentAddNewFaceBinding
import com.book.example.facerecognition.model.IdentityViewModel
import com.book.example.facerecognition.processing.FaceEmbeddings
import com.book.example.facerecognition.processing.RealtimeFaceEmbeddingsExtractor
import com.book.example.facerecognition.processing.configureCamera
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AddNewFaceFragment : Fragment() {

    private lateinit var binding: FragmentAddNewFaceBinding

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageAnalyser: RealtimeFaceEmbeddingsExtractor

    private val viewModel: IdentityViewModel by activityViewModels()

    private val currentIdentityData = IdentityData()

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
        binding = FragmentAddNewFaceBinding.inflate(
            inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        imageAnalyser = RealtimeFaceEmbeddingsExtractor(requireContext(),
            this::onFacesDetected)

        binding.btnAddThisIdentity.isEnabled = false
        binding.btnAddThisIdentity.setOnClickListener {
            onAddIdentityClicked()
        }

        binding.editPersonName.text.clear()
        binding.editPersonName.addTextChangedListener(PersonNameWatcher())

        binding.previewView.post {
            configureCamera(this,
                cameraExecutor, binding.previewView,
                imageAnalyser, CameraSelector.LENS_FACING_FRONT)
        }
    }

    override fun onStop() {
        super.onStop()
        imageAnalyser.close()
    }

    private fun onAddIdentityClicked() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.addIdentity(
                currentIdentityData.name,
                currentIdentityData.embeddings
            )

            Toast.makeText(requireContext(),
                resources.getString(R.string.identity_added,
                    currentIdentityData.name),
                Toast.LENGTH_SHORT).show()

            requireView().findNavController().navigate(
                AddNewFaceFragmentDirections
                    .actionAddNewFaceFragmentToFaceRecognitionFragment()
            )
        }
    }

    private fun onFacesDetected(faces: List<FaceEmbeddings>) {
        lifecycleScope.launch(Dispatchers.Main) {
            currentIdentityData.embeddings = if (faces.isEmpty()) {
                    EMPTY_EMBEDDINGS
                } else {
                    faces.first().embeddings
                }
            binding.btnAddThisIdentity.isEnabled =
                currentIdentityData.isComplete
        }
    }

    private inner class PersonNameWatcher: TextWatcher {
        override fun beforeTextChanged(
            s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(
            s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            currentIdentityData.name = s.toString()
            binding.btnAddThisIdentity.isEnabled =
                currentIdentityData.isComplete
        }

    }

    private data class IdentityData(
            var name: String = "",
            var embeddings: FloatArray = EMPTY_EMBEDDINGS) {
        val isComplete: Boolean
        get() = name.isNotBlank() && embeddings.isNotEmpty()
    }

    companion object {

        private val EMPTY_EMBEDDINGS = floatArrayOf()

    }

}