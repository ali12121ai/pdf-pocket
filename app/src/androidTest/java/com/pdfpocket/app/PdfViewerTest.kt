package com.pdfpocket.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdfpocket.pdf.PdfRepository
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class PdfViewerTest {
    @Test
    fun rendersPageToBitmap() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val doc = PDDocument()
        doc.addPage(PDPage())
        val out = ByteArrayOutputStream()
        doc.save(out)
        doc.close()

        val repo = PdfRepository(context)
        val bitmap = repo.renderPage(out.toByteArray(), 0)
        assertTrue(bitmap.width > 100)
        assertTrue(bitmap.height > 100)
    }
}
