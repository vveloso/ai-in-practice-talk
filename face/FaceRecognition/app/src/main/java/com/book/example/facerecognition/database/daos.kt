package com.book.example.facerecognition.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface IdentityDao {

    @Insert
    fun insert(identity: Identity)

    @Query("SELECT * FROM Identity")
    fun findAll(): List<Identity>

}
