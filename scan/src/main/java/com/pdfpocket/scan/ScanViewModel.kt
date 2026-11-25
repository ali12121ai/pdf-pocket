package com.pdfpocket.scan

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdfpocket.data.Analytics
import com.pdfpocket.data.FileGateway
import com.pdfpocket.pdf.PdfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ScanUiState(
    val pageCount: Int = 0,
    val status: String = "Camera not started",
    val output: ByteArray? = null,
    val ocrText: String = "",
    val autoCrop: Boolean = true,
    val margins: IntArray = intArrayOf(0, 0, 0, 0),
    val reviewIndex: Int? = null,
    val cameraReady: Boolean = false,
    val pagesOpen: Boolean = false,
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {
    private val pdfRepo = PdfRepository(app)
    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state
    private val pages = mutableListOf<ByteArray>()
    private val originals = mutableListOf<ByteArray>()

    fun onCameraReady() {
        _state.value = _state.value.copy(status = "Camera ready - point at the document", cameraReady = true)
    }

    fun onCameraError(message: String) {
        _state.value = _state.value.copy(status = message, cameraReady = false)
    }

    fun addPage(jpeg: ByteArray) {
        originals.add(jpeg)
        val margins = if (_state.value.autoCrop) ScanCropper.autoMargins(jpeg) else intArrayOf(0, 0, 0, 0)
        val cropped = if (_state.value.autoCrop) ScanCropper.crop(jpeg, margins) else jpeg
        pages.add(cropped)
        _state.value = _state.value.copy(
            pageCount = pages.size,
            status = "Captured page " + pages.size + " - review crop",
            output = null,
            margins = margins,
            reviewIndex = pages.lastIndex,
        )
        Analytics.log(Analytics.TOOL_USED, "scan_capture")
    }

    fun setAutoCrop(enabled: Boolean) {
        val next = mutableListOf<ByteArray>()
        originals.forEach { original ->
            next.add(if (enabled) ScanCropper.crop(original, _state.value.margins) else original)
        }
        pages.clear()
        pages.addAll(next)
        _state.value = _state.value.copy(
            autoCrop = enabled,
            pageCount = pages.size,
            status = if (enabled) "Auto-crop on" else "Auto-crop off - full frame",
            output = null,
        )
    }

    fun adjustMargin(position: Int, value: Int) {
        val index = _state.value.reviewIndex ?: return
        val updated = _state.value.margins.copyOf()
        if (position !in updated.indices) return
        updated[position] = value
        val source = originals.getOrNull(index) ?: return
        pages[index] = if (_state.value.autoCrop) ScanCropper.crop(source, updated) else source
        _state.value = _state.value.copy(margins = updated, pageCount = pages.size, output = null)
    }

    fun finishReview() {
        _state.value = _state.value.copy(reviewIndex = null, pagesOpen = true, status = "Pages ready - export or recognise text")
    }

    fun openPages() {
        if (pages.isEmpty()) return
        _state.value = _state.value.copy(reviewIndex = null, pagesOpen = true, status = "Pages ready")
    }

    fun closePages() {
        _state.value = _state.value.copy(pagesOpen = false, status = "Camera ready")
    }

    fun discard() {
        pages.clear(); originals.clear()
        _state.value = ScanUiState(status = "Camera ready", cameraReady = true)
    }

    fun retake() {
        if (pages.isEmpty()) return
        pages.removeAt(pages.lastIndex)
        if (originals.isNotEmpty()) originals.removeAt(originals.lastIndex)
        _state.value = _state.value.copy(
            pageCount = pages.size,
            reviewIndex = pages.lastIndex.takeIf { it >= 0 },
            status = if (pages.isEmpty()) "Camera ready - point at the document" else "Review previous page",
            output = null,
        )
    }

    fun removeAt(index: Int) {
        if (index in pages.indices) {
            pages.removeAt(index)
            if (index in originals.indices) originals.removeAt(index)
            _state.value = _state.value.copy(
                pageCount = pages.size,
                status = "Removed page ${index + 1}; ${pages.size} remaining",
                output = null,
            )
        }
    }

    fun removeLast() {
        if (pages.isEmpty()) return
        pages.removeAt(pages.lastIndex)
        if (originals.isNotEmpty()) originals.removeAt(originals.lastIndex)
        _state.value = _state.value.copy(
            pageCount = pages.size,
            status = "Removed last page; ${pages.size} remaining",
            output = null,
        )
    }

    fun exportPdf() {
        if (pages.isEmpty()) {
            _state.value = _state.value.copy(status = "Capture at least one page")
            return
        }
        viewModelScope.launch {
            val bytes = runCatching { pdfRepo.imagesToPdf(pages.toList()) }
            bytes.onSuccess {
                _state.value = _state.value.copy(output = it, pagesOpen = false, status = "Exported ${pages.size} page(s) to PDF")
                Analytics.log(Analytics.EXPORT_SUCCESS, "scan_pdf")
            }.onFailure { _state.value = _state.value.copy(status = "Export failed: ${it.message}") }
        }
    }

    fun save(context: Context, target: Uri) {
        val bytes = _state.value.output ?: return
        val ok = FileGateway.writeToUri(context, bytes, target)
        _state.value = _state.value.copy(status = if (ok) "Saved scan PDF" else "Save failed")
    }

    fun lastPage(): ByteArray? = pages.lastOrNull()

    fun pageAt(index: Int): ByteArray? = pages.getOrNull(index)

    fun setOcrText(text: String) {
        _state.value = _state.value.copy(ocrText = text, status = "Text recognised")
    }
}
