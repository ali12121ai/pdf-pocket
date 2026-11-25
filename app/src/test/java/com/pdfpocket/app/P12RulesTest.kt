package com.pdfpocket.app

import com.pdfpocket.pdf.PdfRepository
import com.pdfpocket.pdf.PdfSelectionRules
import com.pdfpocket.pdf.PdfSessionStore
import com.pdfpocket.pdf.ToolKey
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class P12RulesTest {
    /** Distinct page sizes make it possible to identify which source page came out. */
    private fun sizedPdf(sizes: List<PDRectangle>): ByteArray = PDDocument().use { doc ->
        sizes.forEach { doc.addPage(PDPage(it)) }
        ByteArrayOutputStream().use { out -> doc.save(out); out.toByteArray() }
    }

    private fun widths(bytes: ByteArray): List<Float> = PDDocument.load(bytes).use { doc ->
        (0 until doc.numberOfPages).map { doc.getPage(it).cropBox.width }
    }

    @Test
    fun v1MergeSumsPagesFromIndependentFixtures() = runBlocking {
        val repo = PdfRepository(null)
        val three = sizedPdf(List(3) { PDRectangle(100f + it * 10f, 200f) })
        val five = sizedPdf(List(5) { PDRectangle(300f + it * 10f, 400f) })
        val merged = repo.merge(listOf(ByteArrayInputStream(three), ByteArrayInputStream(five)))
        assertEquals(8, widths(merged).size)
        assertEquals(listOf(100f, 110f, 120f, 300f, 310f, 320f, 330f, 340f), widths(merged))
    }

    @Test
    fun v2ExtractUsesOriginalPageIndicesAndSizes() = runBlocking {
        val repo = PdfRepository(null)
        val source = sizedPdf(List(5) { PDRectangle(100f + it * 25f, 200f) })
        val out = repo.extractIndices(source, listOf(1, 3))
        assertEquals(2, widths(out).size)
        assertEquals(listOf(125f, 175f), widths(out))
    }

    @Test
    fun v3DestructiveDefaultsAreSafe() {
        assertTrue(PdfSelectionRules.defaultSelection(ToolKey.DELETE, 5).isEmpty())
        assertTrue(PdfSelectionRules.defaultSelection(ToolKey.EXTRACT, 5).isEmpty())
        assertEquals((0 until 5).toSet(), PdfSelectionRules.defaultSelection(ToolKey.CROP, 5))
        assertFalse(PdfSelectionRules.canApply(ToolKey.DELETE, setOf(0, 1, 2, 3, 4), 5))
        assertFalse(PdfSelectionRules.canApply(ToolKey.DELETE, emptySet(), 5))
        assertTrue(PdfSelectionRules.canApply(ToolKey.DELETE, setOf(1), 5))
        assertEquals(setOf(1, 2, 3, 7), PdfSelectionRules.parsePages("2-4, 8", 8))
    }

    @Test
    fun v4PerPageOperationsOnlyTouchSelectedPages() = runBlocking {
        val repo = PdfRepository(null)
        val source = sizedPdf(List(4) { PDRectangle(200f, 300f) })
        val rotated = repo.rotateIndices(source, listOf(1), 90)
        PDDocument.load(rotated).use { doc ->
            assertEquals(0, doc.getPage(0).rotation)
            assertEquals(90, doc.getPage(1).rotation)
            assertEquals(0, doc.getPage(2).rotation)
        }
        val cropped = repo.cropIndices(source, listOf(2), 10f)
        PDDocument.load(cropped).use { doc ->
            assertEquals(200f, doc.getPage(0).cropBox.width, 0.01f)
            assertEquals(200f, doc.getPage(1).cropBox.width, 0.01f)
            assertTrue(doc.getPage(2).cropBox.width < 200f)
        }
        val remaining = repo.deletePages(source, listOf(0, 3))
        assertEquals(listOf(200f, 200f), widths(remaining))
    }

    @Test
    fun p12SessionsAreIsolatedAndDisposable() {
        val dir = File(System.getProperty("java.io.tmpdir"), "pdfpocket-session-test-${System.nanoTime()}")
        val a = PdfSessionStore.write(dir, "session-A", "a.pdf", "AAA".toByteArray())
        val b = PdfSessionStore.write(dir, "session-B", "b.pdf", "BBB".toByteArray())
        assertEquals("AAA", String(PdfSessionStore.read(a)!!))
        assertEquals("BBB", String(PdfSessionStore.read(b)!!))
        PdfSessionStore.clear(dir, "session-A")
        assertEquals(null, PdfSessionStore.read(a))
        assertEquals("BBB", String(PdfSessionStore.read(b)!!))
        dir.deleteRecursively()
    }
}
