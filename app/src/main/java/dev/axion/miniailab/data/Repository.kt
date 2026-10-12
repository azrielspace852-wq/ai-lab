package dev.axion.miniailab.data

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DatasetRepository(private val dao: DatasetDao) {
    fun observe(): Flow<List<DatasetEntity>> = dao.observeAll()
    suspend fun save(item: DatasetEntity) = dao.upsert(item.copy(updatedAt = System.currentTimeMillis()))
    suspend fun create(name: String, folder: Boolean = false, parentId: String? = null) =
        dao.upsert(DatasetEntity(UUID.randomUUID().toString(), name.trim().ifBlank { if(folder) "Folder" else "dataset.txt" }, parentId, folder))
    suspend fun delete(id: String) = dao.delete(id)
    suspend fun get(id: String) = dao.get(id)
}
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "mini-ai-lab.db").build()
    @Provides @Singleton fun repository(db: AppDatabase) = DatasetRepository(db.datasetDao())
}
