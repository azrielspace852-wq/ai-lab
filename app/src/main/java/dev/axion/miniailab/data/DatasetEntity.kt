package dev.axion.miniailab.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "datasets")
data class DatasetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String? = null,
    val isFolder: Boolean = false,
    val content: String = "",
    val tokenizeContent: Boolean = true,
    val tokenizeCaptions: Boolean = true,
    val maskJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
