package com.pdfpocket.pdf

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.ByteArrayInputStream
import java.io.File

enum class ToolKey(val label: String, val multi: Boolean = false, val images: Boolean = false) {
    MERGE("Merge", multi = true),
    EXTRACT("Extract pages"),
    DELETE("Delete pages"),
    ORGANISE("Rotate / reverse"),
    COMPRESS("Compress"),
    CROP("Crop margins"),
    NUMBER("Page numbers"),
    WATERMARK("Watermark"),
    TO_IMAGES("PDF to images"),
    UNLOCK("Unlock"),
    PROTECT("Protect"),
    IMAGES_TO_PDF("Images to PDF", images = true),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolSheet(tool: ToolKey, sessionId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val vm: PdfViewModel = viewModel(key = sessionId)
    val ui by vm.state.collectAsState()
    var password by remember { mutableStateOf("") }
    var preset by remember { mutableStateOf("Balanced") }
    var margin by remember { mutableStateOf("24") }
    var watermark by remember { mutableStateOf("CONFIDENTIAL") }
    var pagesSpec by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var lastOutput by remember { mutableStateOf<String?>(null) }
    var previewBytes by remember { mutableStateOf<ByteArray?>(null) }
    var previewPages by remember { mutableStateOf(0) }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.addFiles(uris, append = tool == ToolKey.MERGE && ui.fileLoaded)
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) vm.useImages(uris)
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) vm.save(context, uri)
    }
    val saveImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { uri ->
        if (uri != null) vm.save(context, uri)
    }

    LaunchedEffect(Unit) {
        vm.setSession(sessionId)
        if (tool.images) imagePicker.launch(arrayOf("image/*"))
        else pdfPicker.launch(arrayOf("application/pdf"))
    }
    DisposableEffect(Unit) { onDispose { vm.dispose() } }

    LaunchedEffect(ui.pageCount, tool) {
        if (ui.pageCount > 0) vm.setSelection(PdfSelectionRules.defaultSelection(tool, ui.pageCount))
    }
    LaunchedEffect(ui.outputPath) {
        if (ui.outputPath != null && ui.outputPath != lastOutput) {
            lastOutput = ui.outputPath
            showResult = true
        }
    }
    LaunchedEffect(showResult, ui.outputPath) {
        val path = ui.outputPath
        if (showResult && path != null) {
            val bytes = PdfSessionStore.read(path)
            previewBytes = bytes
            previewPages = bytes?.let { runCatching { PdfRepository(context).pageCount(ByteArrayInputStream(it)) }.getOrDefault(0) } ?: 0
        }
    }

    if (showResult && ui.outputPath != null) {
        ResultScreen(
            name = ui.outputName,
            sizeLabel = "${previewPages} page(s)",
            canPreview = previewBytes != null && previewPages > 0,
            onSave = {
                if (ui.outputName.endsWith(".jpg")) saveImageLauncher.launch(ui.outputName)
                else saveLauncher.launch(ui.outputName)
            },
            onShare = { ui.outputPath?.let { shareOutput(context, it, ui.outputName) } },
            onPreview = { showResult = false },
            onClose = { showResult = false; vm.dispose(); onDismiss() },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    if (previewBytes != null && !showResult) {
        PdfViewerScreen(
            bytes = previewBytes!!,
            pageCount = previewPages,
            onClose = { previewBytes = null; showResult = true },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tool.label) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(actionLabel(tool, ui), modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Button(
                        enabled = actionEnabled(tool, ui),
                        onClick = { runAction(vm, tool, ui, password, preset, margin, watermark) },
                    ) { Text("Run") }
                }
            }
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(ui.status, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            if (tool.images) {
                Text("Images are converted to one PDF in the order shown.")
                Spacer(Modifier.height(8.dp))
            }
            if (!ui.fileLoaded) {
                Text("Choose ${if (tool.multi) "one or more" else "a"} PDF file(s) to continue.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    if (tool.images) imagePicker.launch(arrayOf("image/*"))
                    else pdfPicker.launch(arrayOf("application/pdf"))
                }) { Text(if (tool.images) "Choose images" else "Choose PDF files") }
            } else {
                if (tool == ToolKey.MERGE) {
                    MergeList(vm, ui, pdfPicker, password) { password = it }
                } else if (tool in PdfSelectionRules.pageTools) {
                    SelectionControls(ui, pagesSpec, { pagesSpec = it }, vm)
                    PageGrid(vm)
                }
                when (tool) {
                    ToolKey.COMPRESS -> {
                        Text("Presets: Small / Balanced / Best. Compression is lossy and drops the text layer.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Small", "Balanced", "Best").forEach {
                                TextButton(onClick = { preset = it }) { Text(if (preset == it) "[$it]" else it) }
                            }
                        }
                    }
                    ToolKey.CROP -> OutlinedTextField(value = margin, onValueChange = { margin = it }, label = { Text("Margin in points") }, modifier = Modifier.fillMaxWidth())
                    ToolKey.WATERMARK -> OutlinedTextField(value = watermark, onValueChange = { watermark = it }, label = { Text("Watermark text") }, modifier = Modifier.fillMaxWidth())
                    ToolKey.UNLOCK, ToolKey.PROTECT -> OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (tool == ToolKey.UNLOCK) "Current password" else "New password") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    else -> Unit
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun actionLabel(tool: ToolKey, ui: PdfUiState): String = when (tool) {
    ToolKey.MERGE -> "Merge ${ui.files.size} files · ${ui.files.sumOf { it.pageCount }} pages"
    ToolKey.EXTRACT -> "Extract ${ui.selectedPages.size} page(s)"
    ToolKey.DELETE -> "Delete ${ui.selectedPages.size} page(s)"
    ToolKey.ORGANISE -> if (ui.selectedPages.isEmpty()) "Reverse all pages" else "Rotate ${ui.selectedPages.size} page(s)"
    ToolKey.CROP -> "Crop ${ui.selectedPages.size} page(s)"
    ToolKey.NUMBER -> "Number ${ui.selectedPages.size} page(s)"
    ToolKey.WATERMARK -> "Watermark ${ui.selectedPages.size} page(s)"
    ToolKey.TO_IMAGES -> "Export ${ui.selectedPages.size} image(s)"
    ToolKey.COMPRESS -> "Compress whole document"
    ToolKey.UNLOCK -> "Remove password"
    ToolKey.PROTECT -> "Add password"
    ToolKey.IMAGES_TO_PDF -> "Create PDF"
}

private fun actionEnabled(tool: ToolKey, ui: PdfUiState): Boolean = when (tool) {
    ToolKey.MERGE -> ui.files.size >= 2 && ui.files.none { it.locked }
    ToolKey.IMAGES_TO_PDF -> ui.fileLoaded
    else -> PdfSelectionRules.canApply(tool, ui.selectedPages, ui.pageCount)
}

private fun runAction(
    vm: PdfViewModel, tool: ToolKey, ui: PdfUiState,
    password: String, preset: String, margin: String, watermark: String,
) {
    when (tool) {
        ToolKey.MERGE -> vm.merge()
        ToolKey.EXTRACT -> vm.applySelected("extract")
        ToolKey.DELETE -> vm.applySelected("delete")
        ToolKey.ORGANISE -> if (ui.selectedPages.isEmpty()) vm.reverse() else vm.applySelected("rotate")
        ToolKey.CROP -> vm.applySelected("crop", margin)
        ToolKey.NUMBER -> vm.applySelected("number")
        ToolKey.WATERMARK -> vm.applySelected("watermark", watermark)
        ToolKey.TO_IMAGES -> vm.exportSelectedImages(qualityFor(preset))
        ToolKey.COMPRESS -> vm.compress(qualityFor(preset))
        ToolKey.UNLOCK -> vm.unlock(password)
        ToolKey.PROTECT -> vm.protect(password)
        ToolKey.IMAGES_TO_PDF -> Unit
    }
}

private fun qualityFor(preset: String): Int = when (preset) { "Small" -> 50; "Best" -> 90; else -> 70 }

@Composable
private fun SelectionControls(ui: PdfUiState, pagesSpec: String, onPagesSpec: (String) -> Unit, vm: PdfViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { vm.setSelection((0 until ui.pageCount).toSet()) }) { Text("All") }
        TextButton(onClick = { vm.setSelection(emptySet()) }) { Text("None") }
        TextButton(onClick = { vm.invertSelection() }) { Text("Invert") }
    }
    OutlinedTextField(value = pagesSpec, onValueChange = onPagesSpec, label = { Text("Pages (e.g. 2-5, 8)") }, modifier = Modifier.fillMaxWidth())
    TextButton(onClick = { vm.setSelection(PdfSelectionRules.parsePages(pagesSpec, ui.pageCount)) }) { Text("Apply page range") }
    Text("${ui.selectedPages.size} of ${ui.pageCount} pages selected")
}

@Composable
private fun MergeList(
    vm: PdfViewModel,
    ui: PdfUiState,
    picker: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    password: String,
    onPassword: (String) -> Unit,
) {
    ui.files.forEachIndexed { index, file ->
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${index + 1}. ${file.name}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = { vm.moveFile(index, (index - 1).coerceAtLeast(0)) }) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                }
                IconButton(onClick = { vm.moveFile(index, (index + 1).coerceAtMost(ui.files.lastIndex)) }) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                }
                IconButton(onClick = { vm.removeFile(index) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove file")
                }
            }
            Text("${file.pageCount} page(s)${if (file.locked) " · locked" else ""}", style = MaterialTheme.typography.bodySmall)
            if (file.locked) {
                OutlinedTextField(value = password, onValueChange = onPassword, label = { Text("Password for this file") }, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { vm.unlockFileAt(index, password) }) { Text("Unlock") }
            }
        }
    }
    TextButton(onClick = { picker.launch(arrayOf("application/pdf")) }) { Text("Add more files") }
}

@Composable
private fun PageGrid(vm: PdfViewModel) {
    val context = LocalContext.current
    val ui by vm.state.collectAsState()
    val repo = remember { PdfRepository(context) }
    val bitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val bytes = PdfSessionStore.read(ui.viewPath)
    LaunchedEffect(ui.pageCount, ui.viewPath) {
        bytes?.let { source ->
            (0 until ui.pageCount).forEach { index ->
                if (index !in bitmaps) {
                    runCatching { repo.renderPage(source, index) }.onSuccess { bitmaps[index] = it }
                }
            }
        }
    }
    if (bytes == null || ui.pageCount == 0) {
        Text("Open a PDF first")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(420.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ui.pageCount) { index ->
            Box(Modifier.size(90.dp).clickable { vm.togglePage(index) }) {
                val bitmap = bitmaps[index]
                if (bitmap != null) {
                    Image(bitmap.asImageBitmap(), contentDescription = "Page ${index + 1}", modifier = Modifier.fillMaxSize())
                } else {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                Checkbox(
                    checked = index in ui.selectedPages,
                    onCheckedChange = { vm.togglePage(index) },
                    modifier = Modifier.align(Alignment.TopEnd),
                )
                Text("${index + 1}", modifier = Modifier.align(Alignment.BottomStart))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultScreen(
    name: String,
    sizeLabel: String,
    canPreview: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onPreview: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Result") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(name, style = MaterialTheme.typography.headlineSmall)
            Text(sizeLabel)
            Button(onClick = onSave, Modifier.fillMaxWidth()) { Text("Save") }
            Button(onClick = onShare, Modifier.fillMaxWidth()) { Text("Share") }
            Button(onClick = onPreview, Modifier.fillMaxWidth(), enabled = canPreview) { Text("Preview") }
            TextButton(onClick = onClose) { Text("Done") }
        }
    }
}

private fun shareOutput(context: android.content.Context, path: String, name: String) {
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (name.endsWith(".jpg")) "image/jpeg" else "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF Pocket result"))
}
