package com.pdfpocket.pdf

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfpocket.data.Analytics
import com.pdfpocket.data.FileGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.ByteArrayInputStream
import java.io.InputStream

data class PdfFileInfo(
    val name: String,
    val pageCount: Int,
    val locked: Boolean,
)

data class PdfUiState(
    val files: List<PdfFileInfo> = emptyList(),
    val fileNames: List<String> = emptyList(),
    val pageCount: Int = 0,
    val status: String = "No PDFs selected",
    val outputPath: String? = null,
    val outputName: String = "pdf-pocket-output.pdf",
    val needsPassword: Boolean = false,
    val fileLoaded: Boolean = false,
    val selectedPages: Set<Int> = emptySet(),
    val viewPath: String? = null,
    val imagePaths: List<String> = emptyList(),
)

class PdfViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PdfRepository(app)
    private val _state = MutableStateFlow(PdfUiState())
    val state: StateFlow<PdfUiState> = _state
    private val uris = mutableListOf<Uri>()
    private val sampleFiles = mutableListOf<File>()
    private var sessionId = "default"

    fun setSession(id: String) {
        sessionId = id
        PdfSessionStore.clear(getApplication<Application>().cacheDir, id)
    }

    fun dispose() {
        PdfSessionStore.clear(getApplication<Application>().cacheDir, sessionId)
        _state.value = PdfUiState()
    }

    private fun storeOutput(bytes: ByteArray, name: String = _state.value.outputName): String =
        PdfSessionStore.write(getApplication<Application>().cacheDir, sessionId, name, bytes)

    private fun storeView(bytes: ByteArray): String =
        PdfSessionStore.write(getApplication<Application>().cacheDir, sessionId, "viewer.pdf", bytes)

    private fun readOutput(): ByteArray? = PdfSessionStore.read(_state.value.outputPath)
    private fun readView(): ByteArray? = PdfSessionStore.read(_state.value.viewPath)

    fun setSelection(indices: Set<Int>) {
        _state.value = _state.value.copy(selectedPages = indices)
    }

    fun invertSelection() {
        val current = _state.value.selectedPages
        _state.value = _state.value.copy(
            selectedPages = (0 until _state.value.pageCount).toSet() - current,
        )
    }

    fun removeFile(index: Int) {
        if (index !in uris.indices) return
        uris.removeAt(index)
        sampleFiles.removeAt(index.coerceAtMost(sampleFiles.lastIndex))
        val files = _state.value.files.toMutableList()
        if (index in files.indices) files.removeAt(index)
        _state.value = _state.value.copy(
            files = files,
            fileNames = files.map { it.name },
            pageCount = files.sumOf { it.pageCount },
            status = "Removed file",
        )
    }

    fun moveFile(from: Int, to: Int) {
        if (from !in uris.indices || to !in uris.indices || from == to) return
        val moved = uris.removeAt(from)
        uris.add(to, moved)
        val files = _state.value.files.toMutableList()
        val file = files.removeAt(from)
        files.add(to, file)
        _state.value = _state.value.copy(
            files = files,
            fileNames = files.map { it.name },
            status = "Reordered files",
        )
    }

    fun unlockFileAt(index: Int, password: String) {
        viewModelScope.launch {
            val uri = uris.getOrNull(index) ?: return@launch
            val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@launch
            val plain = runCatching { repo.decrypt(bytes, password) }.getOrNull()
            if (plain == null) {
                _state.value = _state.value.copy(status = "Wrong password for ${_state.value.files[index].name}")
                return@launch
            }
            val path = storeOutput(plain, "unlocked-${index}.pdf")
            uris[index] = android.net.Uri.fromFile(java.io.File(path))
            val files = _state.value.files.toMutableList()
            files[index] = files[index].copy(locked = false, pageCount = runCatching { repo.pageCount(java.io.ByteArrayInputStream(plain)) }.getOrDefault(files[index].pageCount))
            _state.value = _state.value.copy(
                files = files,
                fileNames = files.map { it.name },
                pageCount = files.sumOf { it.pageCount },
                status = "Unlocked ${files[index].name}",
            )
        }
    }

    fun addFiles(newUris: List<Uri>, append: Boolean = false) {
        viewModelScope.launch {
            if (!append) {
                uris.clear()
                sampleFiles.clear()
            }
            val files = if (append) _state.value.files.toMutableList() else mutableListOf()
            var firstPlain: ByteArray? = null
            newUris.forEach { uri ->
                val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val encrypted = bytes?.let { repo.isEncrypted(it) } ?: false
                val count = if (encrypted) {
                    0
                } else {
                    bytes?.let { runCatching { repo.pageCount(java.io.ByteArrayInputStream(it)) }.getOrDefault(0) } ?: 0
                }
                uris.add(uri)
                files.add(PdfFileInfo(displayName(uri), count, encrypted))
                if (!encrypted && firstPlain == null) firstPlain = bytes
            }
            _state.value = PdfUiState(
                files = files,
                fileNames = files.map { it.name },
                pageCount = files.firstOrNull()?.pageCount ?: 0,
                status = if (files.any { it.locked }) {
                    "Some files are password protected — enter their passwords"
                } else {
                    "${files.size} PDF(s) selected"
                },
                needsPassword = files.any { it.locked },
                fileLoaded = true,
                viewPath = firstPlain?.let { storeView(it) },
            )
            Analytics.log(Analytics.TOOL_USED, "pdf_add")
        }
    }

    fun useSampleFiles() {
        viewModelScope.launch {
            val dir = FileGateway.workingDir(getApplication())
            val files = listOf("sample-a.pdf", "sample-b.pdf").mapIndexed { index, name ->
                val f = File(dir, name)
                val doc = com.tom_roush.pdfbox.pdmodel.PDDocument()
                repeat(if (index == 0) 2 else 3) { pageIndex ->
                    val page = com.tom_roush.pdfbox.pdmodel.PDPage()
                    doc.addPage(page)
                    val content = com.tom_roush.pdfbox.pdmodel.PDPageContentStream(
                        doc, page, true, true,
                    )
                    content.beginText()
                    content.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 28f)
                    content.newLineAtOffset(60f, 700f)
                    content.showText("PDF Pocket sample ${index + 1} - page ${pageIndex + 1}")
                    content.endText()
                    content.close()
                }
                doc.save(f)
                doc.close()
                f
            }
            sampleFiles.clear()
            sampleFiles.addAll(files)
            val bytes = repo.mergeFiles(files)
            _state.value = PdfUiState(
                fileNames = files.map { it.name },
                fileLoaded = true,
                pageCount = 5,
                status = "2 sample PDFs merged into 5 pages — tap Save result",
                outputPath = storeOutput(bytes, "sample-merged.pdf"),
            )
            Analytics.log(Analytics.EXPORT_SUCCESS, "pdf_sample_merge")
        }
    }

    fun merge() {
        viewModelScope.launch {
            val streams = if (uris.isNotEmpty()) {
                uris.map { getApplication<Application>().contentResolver.openInputStream(it)!! }
            } else {
                sampleFiles.map { it.inputStream() }
            }
            if (streams.isEmpty()) {
                _state.value = _state.value.copy(status = "Pick PDF files first")
                return@launch
            }
            if (_state.value.files.any { it.locked }) {
                _state.value = _state.value.copy(status = "Unlock every file before merging")
                return@launch
            }
            val resultFiles = streams.size
            val bytes = runCatching { repo.merge(streams) }
            bytes.onSuccess { result ->
                _state.value = _state.value.copy(outputPath = storeOutput(result, "merged.pdf"), status = "Merged $resultFiles PDF(s)")
                Analytics.log(Analytics.EXPORT_SUCCESS, "pdf_merge")
            }.onFailure { _state.value = _state.value.copy(status = "Merge failed: ${it.message}") }
        }
    }

    fun extract(ranges: String) {
        viewModelScope.launch {
            val stream = if (uris.isNotEmpty()) {
                getApplication<Application>().contentResolver.openInputStream(uris.first())!!
            } else {
                sampleFiles.firstOrNull()?.inputStream() ?: run {
                    _state.value = _state.value.copy(status = "No PDF selected")
                    return@launch
                }
            }
            val bytes = runCatching { repo.extractPages(stream, ranges) }
            bytes.onSuccess { result ->
                _state.value = _state.value.copy(outputPath = storeOutput(result, "extracted.pdf"), status = "Extracted pages $ranges")
                Analytics.log(Analytics.EXPORT_SUCCESS, "pdf_extract")
            }.onFailure { _state.value = _state.value.copy(status = "Extract failed: ${it.message}") }
        }
    }

    private fun openSource(): InputStream? =
        if (uris.isNotEmpty()) {
            getApplication<Application>().contentResolver.openInputStream(uris.first())
        } else {
            sampleFiles.firstOrNull()?.inputStream()
        }

    private fun applyResult(bytes: Result<ByteArray>, label: String, tool: String) {
        bytes.onSuccess { result ->
            _state.value = _state.value.copy(outputPath = storeOutput(result), status = label)
            Analytics.log(Analytics.EXPORT_SUCCESS, tool)
        }.onFailure { _state.value = _state.value.copy(status = "$label failed: ${it.message}") }
    }

    fun rotate(degrees: Int) {
        viewModelScope.launch {
            val source = openSource()
            if (source == null) {
                _state.value = _state.value.copy(status = "Select a PDF first")
                return@launch
            }
            applyResult(runCatching { repo.rotate(source, degrees) }, "Rotated pages $degrees degrees", "pdf_rotate")
        }
    }

    fun reverse() {
        viewModelScope.launch {
            val source = openSource()
            if (source == null) {
                _state.value = _state.value.copy(status = "Select a PDF first")
                return@launch
            }
            applyResult(runCatching { repo.reverse(source) }, "Reversed page order", "pdf_reverse")
        }
    }

    fun movePage(spec: String) {
        val parts = spec.split('>', ',', '-').map { it.trim().toIntOrNull() }
        if (parts.size != 2 || parts.any { it == null }) {
            _state.value = _state.value.copy(status = "Use format from>to, e.g. 1>3")
            return
        }
        viewModelScope.launch {
            val source = openSource()
            if (source == null) {
                _state.value = _state.value.copy(status = "Select a PDF first")
                return@launch
            }
            applyResult(
                runCatching { repo.movePage(source, parts[0]!! - 1, parts[1]!! - 1) },
                "Moved page ${parts[0]} to position ${parts[1]}",
                "pdf_move",
            )
        }
    }

    fun compress(quality: Int) {
        viewModelScope.launch {
            val source = openSource()
            if (source == null) {
                _state.value = _state.value.copy(status = "Select a PDF first")
                return@launch
            }
            applyResult(runCatching { repo.compress(source, quality) }, "Compressed pages at JPEG $quality", "pdf_compress")
        }
    }

    fun addTextStamp(page: Int, text: String, x: Float, y: Float, size: Float) {
        viewModelScope.launch {
            val source = openSource()
            if (source == null) {
                _state.value = _state.value.copy(status = "Select a PDF first")
                return@launch
            }
            applyResult(
                runCatching { repo.addText(source, page - 1, text, x, y, size) },
                "Added text stamp '$text' on page $page",
                "pdf_add_text",
            )
        }
    }

    fun addImageStamp(uri: Uri, page: Int) {
        viewModelScope.launch {
            val source = openSource()
            val image = getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (source == null || image == null) {
                _state.value = _state.value.copy(status = "Select a PDF and an image first")
                return@launch
            }
            applyResult(
                runCatching { repo.addImage(source, page - 1, image, 40f, 40f, 140f, 45f) },
                "Added signature image on page $page",
                "pdf_add_image",
            )
        }
    }

    private fun sourceBytes(): ByteArray? =
        if (uris.isNotEmpty()) {
            getApplication<Application>().contentResolver.openInputStream(uris.first())?.use { it.readBytes() }
        } else {
            sampleFiles.firstOrNull()?.readBytes()
        }

    fun openViewer() {
        viewModelScope.launch {
            val bytes = readOutput() ?: sourceBytes()
            if (bytes == null) {
                _state.value = _state.value.copy(status = "Pick a PDF first")
                return@launch
            }
            if (repo.isEncrypted(bytes)) {
                _state.value = _state.value.copy(needsPassword = true, status = "Enter password to view")
                return@launch
            }
            _state.value = _state.value.copy(viewPath = storeView(bytes), status = "Viewer ready")
        }
    }

    fun unlock(password: String) {
        viewModelScope.launch {
            val bytes = sourceBytes()
            if (bytes == null) {
                _state.value = _state.value.copy(status = "Pick a PDF first")
                return@launch
            }
            val result = runCatching { repo.decrypt(bytes, password) }
            result.onSuccess { plain ->
                _state.value = _state.value.copy(
                    outputPath = storeOutput(plain, "unlocked.pdf"),
                    viewPath = storeView(plain),
                    needsPassword = false,
                    status = "Unlocked — password removed",
                    fileLoaded = true,
                    )
                Analytics.log(Analytics.EXPORT_SUCCESS, "pdf_unlock")
            }.onFailure { _state.value = _state.value.copy(status = "Wrong password or unsupported encryption") }
        }
    }

    fun unlockAndOpen(password: String) {
        viewModelScope.launch {
            val bytes = sourceBytes()
            if (bytes == null) {
                _state.value = _state.value.copy(status = "Pick a PDF first")
                return@launch
            }
            val result = runCatching { repo.decrypt(bytes, password) }
            result.onSuccess { plain ->
                val count = runCatching { repo.pageCount(ByteArrayInputStream(plain)) }.getOrDefault(0)
                _state.value = _state.value.copy(
                    outputPath = storeOutput(plain, "unlocked.pdf"),
                    viewPath = storeView(plain),
                    needsPassword = false,
                    pageCount = count,
                    status = "Unlocked — viewer ready",
                )
            }.onFailure { error ->
                android.util.Log.e("PDFPocketUnlock", "unlock failed", error)
                _state.value = _state.value.copy(status = "Wrong password or unsupported encryption: ${error.message}")
            }
        }
    }

    fun protect(password: String) {
        viewModelScope.launch {
            val bytes = sourceBytes()
            if (bytes == null) {
                _state.value = _state.value.copy(status = "Pick a PDF first")
                return@launch
            }
            val result = runCatching { repo.protect(bytes, password) }
            result.onSuccess { locked ->
                _state.value = _state.value.copy(outputPath = storeOutput(locked, "protected.pdf"), status = "Password protection added")
                Analytics.log(Analytics.EXPORT_SUCCESS, "pdf_protect")
            }.onFailure { _state.value = _state.value.copy(status = "Protect failed: ${it.message}") }
        }
    }

    fun deletePages(spec: String) {
        val indices = spec.split(',', ' ').mapNotNull { it.trim().toIntOrNull()?.minus(1) }
        if (indices.isEmpty()) {
            _state.value = _state.value.copy(status = "Enter page numbers, e.g. 2,4")
            return
        }
        viewModelScope.launch {
            val bytes = sourceBytes() ?: return@launch
            val result = runCatching { repo.deletePages(bytes, indices) }
            result.onSuccess { out ->
                _state.value = _state.value.copy(outputPath = storeOutput(out, "pages-removed.pdf"), status = "Deleted ${indices.size} page(s)")
                Analytics.log(Analytics.EXPORT_SUCCESS, "pdf_delete_pages")
            }.onFailure { _state.value = _state.value.copy(status = "Delete failed: ${it.message}") }
        }
    }

    fun crop(margin: Float) {
        viewModelScope.launch {
            val bytes = sourceBytes() ?: return@launch
            val result = runCatching { repo.cropPages(bytes, margin) }
            result.onSuccess { out ->
                _state.value = _state.value.copy(outputPath = storeOutput(out, "cropped.pdf"), status = "Cropped ${margin.toInt()} pt margins")
                Analytics.log(Analytics.EXPORT_SUCCESS, "pdf_crop")
            }.onFailure { _state.value = _state.value.copy(status = "Crop failed: ${it.message}") }
        }
    }

    fun numberPages() {
        viewModelScope.launch {
            val bytes = sourceBytes() ?: return@launch
            val result = runCatching { repo.addPageNumbers(bytes) }
            result.onSuccess { out ->
                _state.value = _state.value.copy(outputPath = storeOutput(out, "numbered.pdf"), status = "Page numbers added")
                Analytics.log(Analytics.EXPORT_SUCCESS, "pdf_numbers")
            }.onFailure { _state.value = _state.value.copy(status = "Numbering failed: ${it.message}") }
        }
    }

    fun watermark(text: String) {
        viewModelScope.launch {
            val bytes = sourceBytes() ?: return@launch
            val result = runCatching { repo.textWatermark(bytes, text) }
            result.onSuccess { out ->
                _state.value = _state.value.copy(outputPath = storeOutput(out, "watermarked.pdf"), status = "Watermark '$text' added")
                Analytics.log(Analytics.EXPORT_SUCCESS, "pdf_watermark")
            }.onFailure { _state.value = _state.value.copy(status = "Watermark failed: ${it.message}") }
        }
    }

    fun exportImages(quality: Int) {
        viewModelScope.launch {
            val bytes = sourceBytes() ?: return@launch
            val result = runCatching { repo.pagesToImages(bytes, quality) }
            result.onSuccess { list ->
                _state.value = _state.value.copy(
                    imagePaths = list.mapIndexed { i, image -> storeOutput(image, "page-${i + 1}.jpg") },
                    outputPath = list.firstOrNull()?.let { storeOutput(it, "page-1.jpg") },
                    outputName = "pdf-pocket-page1.jpg",
                    status = "Exported ${list.size} page(s) as JPEG — saving page 1",
                )
                Analytics.log(Analytics.EXPORT_SUCCESS, "pdf_to_images")
            }.onFailure { _state.value = _state.value.copy(status = "Image export failed: ${it.message}") }
        }
    }

    fun useImages(newUris: List<android.net.Uri>) {
        viewModelScope.launch {
            val images = newUris.mapNotNull { uri ->
                getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (images.isEmpty()) return@launch
            val result = runCatching { repo.imagesToPdf(images) }
            result.onSuccess { out ->
                _state.value = _state.value.copy(
                    outputPath = storeOutput(out, "images-to-pdf.pdf"),
                    outputName = "images-to-pdf.pdf",
                    status = "Converted ${images.size} image(s) to PDF",
                )
                Analytics.log(Analytics.EXPORT_SUCCESS, "images_to_pdf")
            }.onFailure { _state.value = _state.value.copy(status = "Image to PDF failed: ${it.message}") }
        }
    }

    fun togglePage(index: Int) {
        val next = _state.value.selectedPages.toMutableSet()
        if (!next.add(index)) next.remove(index)
        _state.value = _state.value.copy(selectedPages = next)
    }

    fun selectAllPages() {
        _state.value = _state.value.copy(selectedPages = (0 until _state.value.pageCount).toSet())
    }

    fun clearPageSelection() {
        _state.value = _state.value.copy(selectedPages = emptySet())
    }

    fun unlockForTool(password: String) {
        viewModelScope.launch {
            val bytes = sourceBytes()
            if (bytes == null) return@launch
            runCatching { repo.decrypt(bytes, password) }.onSuccess { plain ->
                _state.value = _state.value.copy(
                    viewPath = storeView(plain),
                    outputPath = storeOutput(plain, "unlocked.pdf"),
                    needsPassword = false,
                    pageCount = runCatching { repo.pageCount(ByteArrayInputStream(plain)) }.getOrDefault(0),
                    status = "Unlocked for editing",
                )
            }.onFailure { _state.value = _state.value.copy(status = "Wrong password") }
        }
    }

    private fun selectedIndices(): List<Int> =
        _state.value.selectedPages.toList().sorted()

    fun applySelected(tool: String, extra: String = "") {
        viewModelScope.launch {
            val bytes = readView() ?: sourceBytes() ?: return@launch
            val indices = selectedIndices()
            val result = runCatching {
                when (tool) {
                    "extract" -> repo.extractIndices(bytes, indices)
                    "delete" -> repo.deletePages(bytes, indices)
                    "rotate" -> repo.rotateIndices(bytes, indices, 90)
                    "crop" -> repo.cropIndices(bytes, indices, extra.toFloatOrNull() ?: 24f)
                    "number" -> repo.numberIndices(bytes, indices)
                    "watermark" -> repo.watermarkIndices(bytes, indices, extra)
                    else -> bytes
                }
            }
            result.onSuccess { out ->
                _state.value = _state.value.copy(
                    outputPath = storeOutput(out, "$tool.pdf"),
                    status = "$tool applied to ${if (indices.isEmpty()) "all pages" else indices.size.toString() + " page(s)"}",
                )
            }.onFailure { _state.value = _state.value.copy(status = "$tool failed: ${it.message}") }
        }
    }

    fun exportSelectedImages(quality: Int) {
        viewModelScope.launch {
            val bytes = readView() ?: sourceBytes() ?: return@launch
            val result = runCatching { repo.imagesForIndices(bytes, selectedIndices(), quality) }
            result.onSuccess { list ->
                _state.value = _state.value.copy(
                    imagePaths = list.mapIndexed { i, image -> storeOutput(image, "page-${i + 1}.jpg") },
                    outputPath = list.firstOrNull()?.let { storeOutput(it, "page-1.jpg") },
                    outputName = "pdf-pocket-page1.jpg",
                    status = "Exported ${list.size} page(s) as JPEG; saving page 1",
                )
            }.onFailure { _state.value = _state.value.copy(status = "Image export failed: ${it.message}") }
        }
    }

    fun save(context: Context, target: Uri) {
        val bytes = readOutput() ?: return
        val ok = FileGateway.writeToUri(context, bytes, target)
        _state.value = _state.value.copy(
            status = if (ok) "Saved PDF to chosen location" else "Save failed",
        )
    }

    fun consumeOutput() {
        _state.value = _state.value.copy(outputPath = null)
    }

    private fun displayName(uri: Uri): String {
        getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
        }
        return uri.lastPathSegment ?: "document.pdf"
    }
}
