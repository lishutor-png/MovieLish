package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.CloudAccountDao
import com.example.data.local.dao.MediaDao
import com.example.data.local.dao.WatchPositionDao
import com.example.data.local.entity.CloudAccountEntity
import com.example.data.local.entity.MediaItemEntity
import com.example.data.local.entity.WatchPositionEntity

@Database(
    entities = [
        MediaItemEntity::class,
        WatchPositionEntity::class,
        CloudAccountEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class MoviLishDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun watchPositionDao(): WatchPositionDao
    abstract fun cloudAccountDao(): CloudAccountDao

    companion object {
        @Volatile
        private var INSTANCE: MoviLishDatabase? = null

        fun getInstance(context: Context): MoviLishDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoviLishDatabase::class.java,
                    "movilish_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
