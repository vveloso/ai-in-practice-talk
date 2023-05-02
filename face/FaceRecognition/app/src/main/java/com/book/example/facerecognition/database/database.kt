package com.book.example.facerecognition.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ Identity::class ], version = 1)
@TypeConverters(FloatArrayConverters::class)
abstract class ApplicationDatabase: RoomDatabase() {

    abstract fun identityDao(): IdentityDao

    companion object {

        private const val DATABASE_NAME = "app.db"

        @Volatile private var instance: ApplicationDatabase? = null

        /**
         * Retrieves the current database instance for the application.
         * @param context A reference to any {@link Context} object in the application.
         *                It will be used once to obtain the global application context.
         */
        fun getDatabase(context: Context): ApplicationDatabase =
            instance ?: synchronized(this) {
                instance ?:
                Room.databaseBuilder(
                        context.applicationContext,
                        ApplicationDatabase::class.java,
                        DATABASE_NAME)
                    .build()
                    .also { instance = it }
            }

    }
}
