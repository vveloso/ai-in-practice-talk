package com.book.example.facerecognition.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.book.example.facerecognition.database.ApplicationDatabase
import com.book.example.facerecognition.database.Identity
import com.book.example.facerecognition.processing.euclidianDistance
import com.google.common.base.Preconditions.checkArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IdentityViewModel(application: Application)
    : AndroidViewModel(application)  {

    private val database = ApplicationDatabase.getDatabase(
        getApplication<Application>().applicationContext)

    private val coroutineContext =
        viewModelScope.coroutineContext + Dispatchers.IO

    suspend fun addIdentity(name: String, embeddings: FloatArray) {
        checkArgument(name.isNotBlank())
        checkArgument(embeddings.isNotEmpty())
        withContext(coroutineContext) {
            database.identityDao()
                .insert(Identity(0, name, embeddings))
        }
    }

    suspend fun recogniseOrNull(embeddings: FloatArray)
    : IdentityDistance? =
        withContext(coroutineContext) {
            calculateDistancesFrom(embeddings)
                .filter { it.distance < IDENTIFICATION_THRESHOLD }
                .minByOrNull { it.distance }
        }

    private fun calculateDistancesFrom(embeddings: FloatArray)
    : List<IdentityDistance> =
        database.identityDao().findAll()
            .map {
                IdentityDistance(
                    it.id, it.name,
                    euclidianDistance(it.embeddings, embeddings)
                )
            }

    override fun onCleared() {
        super.onCleared()
        database.close()
    }

    companion object {
        private const val IDENTIFICATION_THRESHOLD = 1.20f
    }
}

data class IdentityDistance(
    val id: Int, val name: String, val distance: Float)
