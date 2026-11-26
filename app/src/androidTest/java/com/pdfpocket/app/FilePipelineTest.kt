package com.pdfpocket.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdfpocket.data.FileGateway
import com.pdfpocket.data.PocketDatabase
import com.pdfpocket.data.RecentFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilePipelineTest {

    @Test
    fun noBroadStoragePermissionIsRequested() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val permissions = FileGateway.requestedPermissions(context).toList()
        assertFalse(permissions.contains("android.permission.READ_EXTERNAL_STORAGE"))
        assertFalse(permissions.contains("android.permission.WRITE_EXTERNAL_STORAGE"))
    }

    @Test
    fun workingDirectoryIsCreatedAndRoomRoundTripsRecentFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(FileGateway.workingDir(context).isDirectory)

        val db = Room.inMemoryDatabaseBuilder(context, PocketDatabase::class.java).build()
        val file = RecentFile(displayName = "a.pdf", uri = "content://x", mimeType = "application/pdf", pageCount = 2)
        db.dao().insertRecent(file)

        assertEquals(1, db.dao().recentFiles().first().size)
        assertEquals(2, db.dao().recentFiles().first().first().pageCount)
        db.close()
    }
}
