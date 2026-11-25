package com.pdfpocket.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.File

class PdfRepository(private val context: Context?) {
    init {
        context?.let { PDFBoxResourceLoader.init(it.applicationContext) }
    }

    suspend fun pageCount(input: InputStream): Int = withContext(Dispatchers.IO) {
        input.use { PDDocument.load(it).use { doc -> doc.numberOfPages } }
    }

    suspend fun mergeFiles(files: List<File>): ByteArray = merge(files.map { it.inputStream() })

    suspend fun extractFile(file: File, ranges: String): ByteArray = extractPages(file.inputStream(), ranges)

    suspend fun merge(inputs: List<InputStream>): ByteArray = withContext(Dispatchers.IO) {
        val merger = PDFMergerUtility()
        val result = ByteArrayOutputStream()
        merger.destinationStream = result
        inputs.forEach { merger.addSource(it) }
        try {
            merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())
        } finally {
            inputs.forEach { it.close() }
        }
        result.toByteArray()
    }

    suspend fun rotate(input: InputStream, degrees: Int): ByteArray = withContext(Dispatchers.IO) {
        input.use { source ->
            PDDocument.load(source).use { doc ->
                repeat(doc.numberOfPages) { i ->
                    val page = doc.getPage(i)
                    page.rotation = (page.rotation + degrees) % 360
                }
                val out = ByteArrayOutputStream()
                doc.save(out)
                out.toByteArray()
            }
        }
    }

    suspend fun reverse(input: InputStream): ByteArray = withContext(Dispatchers.IO) {
        input.use { source ->
            PDDocument.load(source).use { doc ->
                val outDoc = PDDocument()
                for (i in doc.numberOfPages - 1 downTo 0) {
                    outDoc.importPage(doc.getPage(i))
                }
                val out = ByteArrayOutputStream()
                outDoc.save(out)
                outDoc.close()
                out.toByteArray()
            }
        }
    }

    suspend fun movePage(input: InputStream, from: Int, to: Int): ByteArray = withContext(Dispatchers.IO) {
        input.use { source ->
            PDDocument.load(source).use { doc ->
                val count = doc.numberOfPages
                require(from in 0 until count && to in 0 until count) { "Page number out of range" }
                val order = (0 until count).toMutableList()
                val moved = order.removeAt(from)
                order.add(to, moved)
                val outDoc = PDDocument()
                order.forEach { outDoc.importPage(doc.getPage(it)) }
                val out = ByteArrayOutputStream()
                outDoc.save(out)
                outDoc.close()
                out.toByteArray()
            }
        }
    }

    suspend fun extractIndices(bytes: ByteArray, indices: List<Int>): ByteArray = withContext(Dispatchers.IO) {
        PDDocument.load(ByteArrayInputStream(bytes)).use { doc ->
            val outDoc = PDDocument()
            indices.filter { it in 0 until doc.numberOfPages }.forEach { outDoc.importPage(doc.getPage(it)) }
            val result = ByteArrayOutputStream()
            outDoc.save(result)
            outDoc.close()
            result.toByteArray()
        }
    }

    suspend fun rotateIndices(bytes: ByteArray, indices: List<Int>, degrees: Int): ByteArray = withContext(Dispatchers.IO) {
        PDDocument.load(ByteArrayInputStream(bytes)).use { doc ->
            val selected = indices.filter { it in 0 until doc.numberOfPages }.toSet()
            repeat(doc.numberOfPages) { i ->
                if (selected.isEmpty() || i in selected) {
                    val page = doc.getPage(i)
                    page.rotation = (page.rotation + degrees) % 360
                }
            }
            val result = ByteArrayOutputStream()
            doc.save(result)
            result.toByteArray()
        }
    }

    suspend fun cropIndices(bytes: ByteArray, indices: List<Int>, margin: Float): ByteArray = withContext(Dispatchers.IO) {
        PDDocument.load(ByteArrayInputStream(bytes)).use { doc ->
            val selected = indices.filter { it in 0 until doc.numberOfPages }.toSet()
            repeat(doc.numberOfPages) { i ->
                if (selected.isEmpty() || i in selected) {
                    val page = doc.getPage(i)
                    val box = page.cropBox
                    page.cropBox = PDRectangle(
                        box.lowerLeftX + margin,
                        box.lowerLeftY + margin,
                        box.upperRightX - margin,
                        box.upperRightY - margin,
                    )
                }
            }
            val result = ByteArrayOutputStream()
            doc.save(result)
            result.toByteArray()
        }
    }

    suspend fun numberIndices(bytes: ByteArray, indices: List<Int>): ByteArray = withContext(Dispatchers.IO) {
        PDDocument.load(ByteArrayInputStream(bytes)).use { doc ->
            val selected = indices.filter { it in 0 until doc.numberOfPages }.toSet()
            repeat(doc.numberOfPages) { i ->
                if (selected.isEmpty() || i in selected) {
                    val page = doc.getPage(i)
                    val content = PDPageContentStream(doc, page, true, true)
                    content.beginText()
                    content.setFont(PDType1Font.HELVETICA, 11f)
                    content.newLineAtOffset(page.cropBox.width - 50f, 24f)
                    content.showText("${i + 1}")
                    content.endText()
                    content.close()
                }
            }
            val result = ByteArrayOutputStream()
            doc.save(result)
            result.toByteArray()
        }
    }

    suspend fun watermarkIndices(bytes: ByteArray, indices: List<Int>, text: String): ByteArray = withContext(Dispatchers.IO) {
        PDDocument.load(ByteArrayInputStream(bytes)).use { doc ->
            val selected = indices.filter { it in 0 until doc.numberOfPages }.toSet()
            repeat(doc.numberOfPages) { i ->
                if (selected.isEmpty() || i in selected) {
                    val page = doc.getPage(i)
                    val content = PDPageContentStream(doc, page, true, true)
                    content.beginText()
                    content.setFont(PDType1Font.HELVETICA_BOLD, 34f)
                    content.newLineAtOffset(page.cropBox.width / 6f, page.cropBox.height / 2f)
                    content.showText(text)
                    content.endText()
                    content.close()
                }
            }
            val result = ByteArrayOutputStream()
            doc.save(result)
            result.toByteArray()
        }
    }

    suspend fun imagesForIndices(bytes: ByteArray, indices: List<Int>, quality: Int): List<ByteArray> = withContext(Dispatchers.IO) {
        val source = File(context!!.cacheDir, "pages-source.pdf")
        source.writeBytes(bytes)
        val pfd = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val images = mutableListOf<ByteArray>()
        val scale = 150f / 72f
        try {
            val selected = indices.toSet()
            for (i in 0 until renderer.pageCount) {
                if (selected.isNotEmpty() && i !in selected) continue
                val pdfPage = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap((pdfPage.width * scale).toInt(), (pdfPage.height * scale).toInt(), Bitmap.Config.ARGB_8888)
                pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pdfPage.close()
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(20, 95), out)
                bitmap.recycle()
                images.add(out.toByteArray())
            }
        } finally {
            renderer.close(); pfd.close(); source.delete()
        }
        images
    }

    suspend fun isEncrypted(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            PDDocument.load(ByteArrayInputStream(bytes)).use { it.isEncrypted }
        } catch (e: InvalidPasswordException) {
            true
        }
    }

    suspend fun decrypt(bytes: ByteArray, password: String): ByteArray = withContext(Dispatchers.IO) {
        PDDocument.load(ByteArrayInputStream(bytes), password).use { doc ->
            val out = PDDocument()
            repeat(doc.numberOfPages) { out.importPage(doc.getPage(it)) }
            val result = ByteArrayOutputStream()
            out.save(result)
            out.close()
            result.toByteArray()
        }
    }

    suspend fun protect(bytes: ByteArray, password: String): ByteArray = withContext(Dispatchers.IO) {
        PDDocument.load(ByteArrayInputStream(bytes)).use { doc ->
            val permission = AccessPermission()
            permission.setCanPrint(true)
            permission.setCanExtractForAccessibility(true)
            doc.protect(StandardProtectionPolicy(password, password, permission))
            val result = ByteArrayOutputStream()
            doc.save(result)
            result.toByteArray()
        }
    }

    suspend fun deletePages(bytes: ByteArray, indices: List<Int>): ByteArray = withContext(Dispatchers.IO) {
        PDDocument.load(ByteArrayInputStream(bytes)).use { doc ->
            indices.sortedDescending().forEach { index ->
                if (index in 0 until doc.numberOfPages) doc.removePage(index)
            }
            val result = ByteArrayOutputStream()
            doc.save(result)
            result.toByteArray()
        }
    }

    suspend fun cropPages(bytes: ByteArray, margin: Float): ByteArray = withContext(Dispatchers.IO) {
        PDDocument.load(ByteArrayInputStream(bytes)).use { doc ->
            repeat(doc.numberOfPages) { index ->
                val page = doc.getPage(index)
                val box = page.cropBox
                page.cropBox = PDRectangle(
                    box.lowerLeftX + margin,
                    box.lowerLeftY + margin,
                    box.upperRightX - margin,
                    box.upperRightY - margin,
                )
            }
            val result = ByteArrayOutputStream()
            doc.save(result)
            result.toByteArray()
        }
    }

    suspend fun addPageNumbers(bytes: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        PDDocument.load(ByteArrayInputStream(bytes)).use { doc ->
            repeat(doc.numberOfPages) { index ->
                val page = doc.getPage(index)
                val width = page.cropBox.width
                val content = PDPageContentStream(doc, page, true, true)
                content.beginText()
                content.setFont(PDType1Font.HELVETICA, 11f)
                content.newLineAtOffset(width - 50f, 24f)
                content.showText("${index + 1}")
                content.endText()
                content.close()
            }
            val result = ByteArrayOutputStream()
            doc.save(result)
            result.toByteArray()
        }
    }

    suspend fun textWatermark(bytes: ByteArray, text: String): ByteArray = withContext(Dispatchers.IO) {
        PDDocument.load(ByteArrayInputStream(bytes)).use { doc ->
            repeat(doc.numberOfPages) { index ->
                val page = doc.getPage(index)
                val width = page.cropBox.width
                val height = page.cropBox.height
                val content = PDPageContentStream(doc, page, true, true)
                content.beginText()
                content.setFont(PDType1Font.HELVETICA_BOLD, 42f)
                content.newLineAtOffset(width / 6f, height / 2f)
                content.showText(text)
                content.endText()
                content.close()
            }
            val result = ByteArrayOutputStream()
            doc.save(result)
            result.toByteArray()
        }
    }

    suspend fun renderPage(bytes: ByteArray, index: Int): Bitmap = withContext(Dispatchers.IO) {
        val source = File(context!!.cacheDir, "viewer-page.pdf")
        source.writeBytes(bytes)
        val pfd = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        try {
            renderer.openPage(index).use { page ->
                val bitmap = Bitmap.createBitmap(
                    (page.width * 2f).toInt(),
                    (page.height * 2f).toInt(),
                    Bitmap.Config.ARGB_8888,
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return@withContext bitmap.copy(Bitmap.Config.ARGB_8888, true)
            }
        } finally {
            renderer.close()
            pfd.close()
            source.delete()
        }
    }

    suspend fun pagesToImages(bytes: ByteArray, quality: Int): List<ByteArray> = withContext(Dispatchers.IO) {
        val source = File(context!!.cacheDir, "viewer-source.pdf")
        source.writeBytes(bytes)
        val pfd = ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val images = mutableListOf<ByteArray>()
        val scale = 150f / 72f
        try {
            for (i in 0 until renderer.pageCount) {
                val pdfPage = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(
                    (pdfPage.width * scale).toInt(),
                    (pdfPage.height * scale).toInt(),
                    Bitmap.Config.ARGB_8888,
                )
                pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                pdfPage.close()
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(20, 95), out)
                bitmap.recycle()
                images.add(out.toByteArray())
            }
        } finally {
            renderer.close()
            pfd.close()
            source.delete()
        }
        images
    }

    suspend fun imagesToPdf(images: List<ByteArray>): ByteArray = withContext(Dispatchers.IO) {
        val doc = PDDocument()
        val a4 = PDRectangle.A4
        try {
            images.forEachIndexed { index, bytes ->
                val image = PDImageXObject.createFromByteArray(doc, bytes, "page$index")
                val page = PDPage(a4)
                val scale = minOf(a4.width / image.width, a4.height / image.height)
                val width = image.width * scale
                val height = image.height * scale
                val content = PDPageContentStream(doc, page)
                content.drawImage(image, (a4.width - width) / 2, (a4.height - height) / 2, width, height)
                content.close()
                doc.addPage(page)
            }
            val out = ByteArrayOutputStream()
            doc.save(out)
            out.toByteArray()
        } finally {
            doc.close()
        }
    }

    suspend fun addText(
        input: InputStream,
        pageIndex: Int,
        text: String,
        x: Float,
        y: Float,
        fontSize: Float,
    ): ByteArray = withContext(Dispatchers.IO) {
        input.use { source ->
            PDDocument.load(source).use { doc ->
                require(pageIndex in 0 until doc.numberOfPages) { "Page number out of range" }
                val content = PDPageContentStream(doc, doc.getPage(pageIndex), true, true)
                content.beginText()
                content.setFont(PDType1Font.HELVETICA, fontSize)
                content.newLineAtOffset(x, y)
                content.showText(text)
                content.endText()
                content.close()
                val out = ByteArrayOutputStream()
                doc.save(out)
                out.toByteArray()
            }
        }
    }

    suspend fun addImage(
        input: InputStream,
        pageIndex: Int,
        imageBytes: ByteArray,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
    ): ByteArray = withContext(Dispatchers.IO) {
        input.use { source ->
            PDDocument.load(source).use { doc ->
                require(pageIndex in 0 until doc.numberOfPages) { "Page number out of range" }
                val image = PDImageXObject.createFromByteArray(doc, imageBytes, "signature")
                val content = PDPageContentStream(doc, doc.getPage(pageIndex), true, true)
                content.drawImage(image, x, y, width, height)
                content.close()
                val out = ByteArrayOutputStream()
                doc.save(out)
                out.toByteArray()
            }
        }
    }

    /** Lossy JPEG re-render at 150 dpi; preserves physical page size and drops the text layer. */
    suspend fun compress(input: InputStream, quality: Int): ByteArray = withContext(Dispatchers.IO) {
        val bytes = input.use { it.readBytes() }
        val ctx = context ?: error("Context required for JPEG compression")
        val sourceFile = File(ctx.cacheDir, "compress-source.pdf")
        sourceFile.writeBytes(bytes)
        val pfd = ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val outDoc = PDDocument()
        val scale = 150f / 72f
        try {
            for (i in 0 until renderer.pageCount) {
                val pdfPage = renderer.openPage(i)
                val pointWidth = pdfPage.width.toFloat()
                val pointHeight = pdfPage.height.toFloat()
                val width = (pointWidth * scale).toInt()
                val height = (pointHeight * scale).toInt()
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                val jpeg = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(20, 95), jpeg)
                pdfPage.close()
                bitmap.recycle()

                val image = PDImageXObject.createFromByteArray(outDoc, jpeg.toByteArray(), "page$i")
                val page = PDPage(PDRectangle(pointWidth, pointHeight))
                val content = PDPageContentStream(outDoc, page)
                content.drawImage(image, 0f, 0f, page.cropBox.width, page.cropBox.height)
                content.close()
                outDoc.addPage(page)
            }
            val out = ByteArrayOutputStream()
            outDoc.save(out)
            out.toByteArray()
        } finally {
            outDoc.close()
            renderer.close()
            pfd.close()
            sourceFile.delete()
        }
    }

    suspend fun extractPages(input: InputStream, ranges: String): ByteArray = withContext(Dispatchers.IO) {
        val indices = parseRanges(ranges)
        input.use { source ->
            PDDocument.load(source).use { doc ->
                val outDoc = PDDocument()
                indices.forEach { index ->
                    if (index in 0 until doc.numberOfPages) outDoc.addPage(doc.getPage(index))
                }
                val result = ByteArrayOutputStream()
                outDoc.save(result)
                outDoc.close()
                result.toByteArray()
            }
        }
    }

    private fun parseRanges(text: String): List<Int> {
        val result = mutableListOf<Int>()
        text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
            if ('-' in part) {
                val (start, end) = part.split('-', limit = 2)
                val a = start.toIntOrNull() ?: return@forEach
                val b = end.toIntOrNull() ?: return@forEach
                if (a <= b) result.addAll(a..b)
            } else {
                part.toIntOrNull()?.let { result.add(it) }
            }
        }
        return result.map { it - 1 }.distinct()
    }
}
