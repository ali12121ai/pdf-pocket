package com.pdfpocket.app

import com.pdfpocket.pdf.PdfRepository
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfSecurityTest {
    private fun pdf(pages: Int): ByteArray = PDDocument().use { doc ->
        repeat(pages) { doc.addPage(PDPage()) }
        ByteArrayOutputStream().use { out -> doc.save(out); out.toByteArray() }
    }

    @Test
    fun deleteAndCropKeepValidPdfs() = runBlocking {
        val repo = PdfRepository(null)
        val plain = pdf(3)
        PDDocument.load(repo.deletePages(plain, listOf(1))).use { assertEquals(2, it.numberOfPages) }
        val originalWidth = PDDocument.load(ByteArrayInputStream(plain)).use { it.getPage(0).cropBox.width }
        PDDocument.load(repo.cropPages(plain, 20f)).use {
            assertTrue(it.getPage(0).cropBox.width < originalWidth)
        }
    }
}
