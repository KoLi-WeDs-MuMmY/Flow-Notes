package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sketch_folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentFolderId: Long? = null, // null = Root Workspace; supports unlimited nesting depth
    val name: String,
    val colorArgb: Long = 0xFFE85D04L,
    val iconName: String = "FOLDER",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "sketch_pages")
data class SketchPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long? = null, // null = Root Workspace; non-null = inside a nested FolderEntity
    val isFavorite: Boolean = false,
    val title: String,
    val categoryTag: String, // e.g. "Drawing", "Handwriting", "Blueprint"
    val paperTemplateName: String,
    val strokesPayload: String, // Compact serialized strokes
    val textNotesPayload: String, // Compact serialized text annotations
    val strokeCount: Int,
    val lastZoomFloat: Float = 1.0f,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface FolderDao {
    @Query("SELECT * FROM sketch_folders ORDER BY name ASC")
    fun observeAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM sketch_folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Long): FolderEntity?

    @Query("SELECT COUNT(*) FROM sketch_folders")
    suspend fun getFolderCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity): Long

    @Query("UPDATE sketch_folders SET parentFolderId = :newParentId WHERE parentFolderId = :oldParentId")
    suspend fun reparentChildFolders(oldParentId: Long, newParentId: Long?)

    @Query("DELETE FROM sketch_folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: Long)
}

@Dao
interface SketchPageDao {
    @Query("SELECT * FROM sketch_pages ORDER BY updatedAtMillis DESC")
    fun observeAllPages(): Flow<List<SketchPageEntity>>

    @Query("SELECT * FROM sketch_pages WHERE id = :pageId LIMIT 1")
    suspend fun getPageById(pageId: Long): SketchPageEntity?

    @Query("SELECT COUNT(*) FROM sketch_pages")
    suspend fun getPageCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPage(page: SketchPageEntity): Long

    @Query("UPDATE sketch_pages SET folderId = :targetFolderId, updatedAtMillis = :now WHERE id = :pageId")
    suspend fun movePageToFolder(pageId: Long, targetFolderId: Long?, now: Long = System.currentTimeMillis())

    @Query("UPDATE sketch_pages SET folderId = :newFolderId WHERE folderId = :oldFolderId")
    suspend fun movePagesFromFolder(oldFolderId: Long, newFolderId: Long?)

    @Query("UPDATE sketch_pages SET isFavorite = :isFavorite, updatedAtMillis = :now WHERE id = :pageId")
    suspend fun setPageFavorite(pageId: Long, isFavorite: Boolean, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM sketch_pages WHERE id = :pageId")
    suspend fun deletePageById(pageId: Long)
}

@Database(
    entities = [FolderEntity::class, SketchPageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun sketchPageDao(): SketchPageDao

    companion object {
        @Volatile
        private var INSTANCE: StudioDatabase? = null

        fun getInstance(context: Context): StudioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudioDatabase::class.java,
                    "canvasflow_studio.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
