package com.pdfpocket.app

import com.pdfpocket.pdf.PdfRepository
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfPageOpsTest {
    private fun pdf(pages: Int): ByteArray = PDDocument().use { doc ->
        repeat(pages) { doc.addPage(PDPage()) }
        ByteArrayOutputStream().use { out -> doc.save(out); out.toByteArray() }
    }

    @Test
    fun rotateSetsRotationOnEveryPage() = runBlocking {
        val repo = PdfRepository(null)
        val out = repo.rotate(ByteArrayInputStream(pdf(3)), 90)
        PDDocument.load(out).use { doc ->
            assertEquals(3, doc.numberOfPages)
            assertEquals(90, doc.getPage(0).rotation)
            assertEquals(90, doc.getPage(2).rotation)
        }
    }

    @Test
    fun reverseKeepsExactPageCount() = runBlocking {
        val repo = PdfRepository(null)
        val out = repo.reverse(ByteArrayInputStream(pdf(3)))
        PDDocument.load(out).use { doc -> assertEquals(3, doc.numberOfPages) }
    }

    @Test
    fun movePageKeepsExactPageCount() = runBlocking {
        val repo = PdfRepository(null)
        val out = repo.movePage(ByteArrayInputStream(pdf(4)), 0, 2)
        PDDocument.load(out).use { doc -> assertEquals(4, doc.numberOfPages) }
    }
}
