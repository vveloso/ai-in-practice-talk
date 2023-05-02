package com.book.example.facerecognition

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.book.example.facerecognition.database.ApplicationDatabase
import com.book.example.facerecognition.database.Identity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.contains
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: ApplicationDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ApplicationDatabase::class.java)
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testIdentityDao() {

        val someEmbeddings = FloatArray(192) { Random.nextFloat() }

        val dao = db.identityDao()

        dao.insert(Identity(0, "some person", someEmbeddings))

        val identities = dao.findAll()
        assertThat(identities, contains(
            Identity(1, "some person", someEmbeddings)
        ))
    }
}