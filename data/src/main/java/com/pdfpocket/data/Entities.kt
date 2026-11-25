package com.pdfpocket.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val uri: String,
    val mimeType: String,
    val pageCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "qr_history")
data class QrHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val format: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface PocketDao {
    @Query("SELECT * FROM recent_files ORDER BY createdAt DESC")
    fun recentFiles(): Flow<List<RecentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(file: RecentFile)

    @Query("SELECT * FROM qr_history ORDER BY createdAt DESC")
    fun qrHistory(): Flow<List<QrHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQr(item: QrHistory)

    @Query("DELETE FROM qr_history WHERE id = :id")
    suspend fun deleteQr(id: Long)
}

@Database(entities = [RecentFile::class, QrHistory::class], version = 1, exportSchema = true)
abstract class PocketDatabase : RoomDatabase() {
    abstract fun dao(): PocketDao
}
