package com.pdfpocket.app

import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfMergeTest {

    // Text rendering needs PDFBoxResourceLoader.init(context) on Android; the spike
    // intentionally uses blank pages to prove the merge pipeline in a JVM test.
    private fun makePdf(pages: Int): ByteArray = PDDocument().use { doc ->
        repeat(pages) { doc.addPage(PDPage()) }
        ByteArrayOutputStream().use { out ->
            doc.save(out)
            out.toByteArray()
        }
    }

    @Test
    fun mergesTwoDocumentsIntoFivePages() {
        val first = makePdf(2)
        val second = makePdf(3)

        val merger = PDFMergerUtility()
        merger.addSource(ByteArrayInputStream(first))
        merger.addSource(ByteArrayInputStream(second))
        val result = ByteArrayOutputStream()
        merger.destinationStream = result
        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())

        PDDocument.load(result.toByteArray()).use { merged ->
            assertEquals(5, merged.numberOfPages)
            assertTrue(merged.getPage(0).mediaBox.width > 0f)
        }
    }
}
