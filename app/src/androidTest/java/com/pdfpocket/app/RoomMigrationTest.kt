package com.pdfpocket.app

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdfpocket.data.MIGRATION_1_2
import com.pdfpocket.data.PocketDatabase
import com.pdfpocket.data.RecentFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {
    private val dbName = "migration-test.db"

    @Test
    fun migratesVersion1To2AndKeepsRecentFiles() = runBlocking {
        val migrationHelper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            PocketDatabase::class.java,
        )
        migrationHelper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO recent_files (displayName, uri, mimeType, pageCount, createdAt) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any>("keep.pdf", "content://x", "application/pdf", 3, 1L),
            )
            db.execSQL("INSERT INTO qr_history (text, format, createdAt) VALUES (?, ?, ?)", arrayOf<Any>("OLD", "TEXT", 1L))
        }
        migrationHelper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2).close()
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext<android.content.Context>(),
            PocketDatabase::class.java,
            dbName,
        ).addMigrations(MIGRATION_1_2).build()
        val rows = db.dao().recentFiles().first()
        assertEquals(1, rows.size)
        assertEquals("keep.pdf", rows.first().displayName)
        db.close()
    }
}
