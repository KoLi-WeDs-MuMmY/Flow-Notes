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

@Entity(tableName = "sketch_pages")
data class SketchPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val categoryTag: String, // e.g. "Drawing", "Handwriting", "Diagram"
    val paperTemplateName: String,
    val strokesPayload: String, // Compact serialized strokes
    val textNotesPayload: String, // Compact serialized text annotations
    val strokeCount: Int,
    val lastZoomFloat: Float = 1.0f,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

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

    @Query("DELETE FROM sketch_pages WHERE id = :pageId")
    suspend fun deletePageById(pageId: Long)
}

@Database(entities = [SketchPageEntity::class], version = 1, exportSchema = false)
abstract class StudioDatabase : RoomDatabase() {
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
