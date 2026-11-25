package com.pdfpocket.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdfpocket.data.FileGateway
import com.pdfpocket.pdf.PdfRepository
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfStampTest {
    @Test
    fun textStampIsExtractableFromSavedPdf() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PdfRepository(context)
        val source = File(FileGateway.workingDir(context), "stamp-source.pdf")
        val doc = PDDocument()
        doc.addPage(PDPage())
        doc.save(source)
        doc.close()

        val out = repo.addText(source.inputStream(), 0, "SIGNED-TEST", 50f, 700f, 14f)
        PDDocument.load(out).use { saved ->
            val text = PDFTextStripper().getText(saved)
            assertTrue("Extracted text was: $text", text.contains("SIGNED-TEST"))
        }
    }
}
