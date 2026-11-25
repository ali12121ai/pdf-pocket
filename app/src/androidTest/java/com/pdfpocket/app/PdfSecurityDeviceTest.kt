package com.pdfpocket.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdfpocket.pdf.PdfRepository
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class PdfSecurityDeviceTest {
    @Test
    fun protectDecryptNumberAndWatermarkOnDevice() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PdfRepository(context)
        val doc = PDDocument()
        repeat(3) { doc.addPage(PDPage()) }
        val out = ByteArrayOutputStream()
        doc.save(out)
        doc.close()
        val plain = out.toByteArray()

        assertFalse(repo.isEncrypted(plain))
        val locked = repo.protect(plain, "secret123")
        assertTrue(repo.isEncrypted(locked))
        assertTrue(runCatching { repo.decrypt(locked, "wrong") }.isFailure)
        val unlocked = repo.decrypt(locked, "secret123")
        assertFalse(repo.isEncrypted(unlocked))
        PDDocument.load(unlocked).use { assertEquals(3, it.numberOfPages) }
        PDDocument.load(repo.addPageNumbers(plain)).use { assertEquals(3, it.numberOfPages) }
        PDDocument.load(repo.textWatermark(plain, "TEST")).use { assertEquals(3, it.numberOfPages) }
    }
}
