package dev.axion.miniailab.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DatasetEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() { abstract fun datasetDao(): DatasetDao }
