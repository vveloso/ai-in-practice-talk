package com.book.example.facerecognition.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Identity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val name: String,
    val embeddings: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Identity

        if (id != other.id) return false
        if (name != other.name) return false
        if (!embeddings.contentEquals(other.embeddings)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + embeddings.contentHashCode()
        return result
    }
}