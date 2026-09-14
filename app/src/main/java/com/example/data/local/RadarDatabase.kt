package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.RadarEntity

@Database(entities = [RadarEntity::class], version = 1, exportSchema = false)
abstract class RadarDatabase : RoomDatabase() {

    abstract fun radarDao(): RadarDao

    companion object {
        @Volatile
        private var INSTANCE: RadarDatabase? = null

        fun getInstance(context: Context): RadarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RadarDatabase::class.java,
                    "turkey_radars.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
