package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CueMarkerEntity
import com.example.data.model.TrackEntity

@Database(
    entities = [TrackEntity::class, CueMarkerEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CueMasterDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun cueDao(): CueDao

    companion object {
        @Volatile
        private var INSTANCE: CueMasterDatabase? = null

        fun getDatabase(context: Context): CueMasterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CueMasterDatabase::class.java,
                    "cuemaster_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
