package dev.axion.miniailab.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DatasetDao {
    @Query("SELECT * FROM datasets ORDER BY isFolder DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<DatasetEntity>>
    @Query("SELECT * FROM datasets WHERE id = :id LIMIT 1")
    suspend fun get(id: String): DatasetEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DatasetEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<DatasetEntity>)
    @Query("DELETE FROM datasets WHERE id = :id")
    suspend fun delete(id: String)
    @Query("DELETE FROM datasets")
    suspend fun deleteAll()
}
