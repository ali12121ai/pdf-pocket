package com.pdfpocket.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdfpocket.coreui.SecondaryButton

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

private val PAGE_TOOLS = setOf(
    ToolKey.EXTRACT, ToolKey.DELETE, ToolKey.ORGANISE, ToolKey.COMPRESS,
    ToolKey.CROP, ToolKey.NUMBER, ToolKey.WATERMARK, ToolKey.TO_IMAGES,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolSheet(tool: ToolKey, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val vm: PdfViewModel = viewModel(key = "tool-${tool.name}")
    val ui by vm.state.collectAsState()
    var password by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf("70") }
    var margin by remember { mutableStateOf("24") }
    var watermark by remember { mutableStateOf("CONFIDENTIAL") }

    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> if (uris.isNotEmpty()) vm.addFiles(uris) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> if (uris.isNotEmpty()) vm.useImages(uris) }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> if (uri != null) vm.save(context, uri) }
    val saveImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg"),
    ) { uri -> if (uri != null) vm.save(context, uri) }

    LaunchedEffect(Unit) {
        if (tool.images) imagePicker.launch(arrayOf("image/*"))
        else pdfPicker.launch(arrayOf("application/pdf"))
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(tool.label, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            when {
                tool.images -> {
                    if (ui.output == null) {
                        Button(onClick = { imagePicker.launch(arrayOf("image/*")) }) { Text("Choose images") }
                    } else {
                        Text(ui.status)
                        SaveOutput(ui.outputName, ui.output != null, saveLauncher, saveImageLauncher)
                    }
                }
                !ui.fileLoaded -> {
                    Text("Choose ${if (tool.multi) "one or more" else "a"} PDF file(s) to continue.")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) { Text("Choose PDF files") }
                    SecondaryButton("Try a sample PDF") { vm.useSampleFiles() }
                }
                ui.needsPassword -> {
                    Text("This PDF is locked. Enter its password to continue.")
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.unlockForTool(password) }) { Text("Unlock for this tool") }
                }
                else -> {
                    Text(ui.fileNames.joinToString("\n"), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) { Text("Change files") }

                    if (tool in PAGE_TOOLS) {
                        PageGrid(vm)
                        Text(
                            if (ui.selectedPages.isEmpty()) "All pages will be used" else "${ui.selectedPages.size} page(s) selected",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.selectAllPages() }) { Text("All pages") }
                            Button(onClick = { vm.clearPageSelection() }) { Text("Clear") }
                        }
                    }

                    when (tool) {
                        ToolKey.MERGE -> Button(onClick = { vm.merge() }) { Text("Merge ${ui.fileNames.size} PDF(s)") }
                        ToolKey.EXTRACT -> Button(onClick = { vm.applySelected("extract") }) { Text("Extract selected pages") }
                        ToolKey.DELETE -> Button(onClick = { vm.applySelected("delete") }) { Text("Delete selected pages") }
                        ToolKey.ORGANISE -> {
                            Button(onClick = { vm.applySelected("rotate") }) { Text("Rotate selected +90") }
                            Button(onClick = { vm.reverse() }) { Text("Reverse page order") }
                        }
                        ToolKey.COMPRESS -> {
                            OutlinedTextField(value = quality, onValueChange = { quality = it }, label = { Text("JPEG quality") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { vm.compress(quality.toIntOrNull() ?: 70) }) { Text("Compress") }
                        }
                        ToolKey.CROP -> {
                            OutlinedTextField(value = margin, onValueChange = { margin = it }, label = { Text("Margin in points") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { vm.applySelected("crop", margin) }) { Text("Crop selected pages") }
                        }
                        ToolKey.NUMBER -> Button(onClick = { vm.applySelected("number") }) { Text("Add page numbers") }
                        ToolKey.WATERMARK -> {
                            OutlinedTextField(value = watermark, onValueChange = { watermark = it }, label = { Text("Watermark text") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { vm.applySelected("watermark", watermark) }) { Text("Add watermark") }
                        }
                        ToolKey.TO_IMAGES -> Button(onClick = { vm.exportSelectedImages(quality.toIntOrNull() ?: 70) }) { Text("Export selected as JPEG") }
                        ToolKey.UNLOCK -> {
                            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Current password") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { vm.unlock(password) }) { Text("Remove password") }
                        }
                        ToolKey.PROTECT -> {
                            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("New password") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { vm.protect(password) }) { Text("Add password") }
                        }
                        ToolKey.IMAGES_TO_PDF -> Unit
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(ui.status, style = MaterialTheme.typography.bodyMedium)
                    SaveOutput(ui.outputName, ui.output != null, saveLauncher, saveImageLauncher)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SaveOutput(
    name: String,
    enabled: Boolean,
    pdfLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    imageLauncher: androidx.activity.result.ActivityResultLauncher<String>,
) {
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            if (name.endsWith(".jpg")) imageLauncher.launch(name) else pdfLauncher.launch(name)
        },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Save result") }
}

@Composable
private fun PageGrid(vm: PdfViewModel) {
    val context = LocalContext.current
    val ui by vm.state.collectAsState()
    val repo = remember { PdfRepository(context) }
    val bitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val bytes = ui.viewBytes

    LaunchedEffect(ui.pageCount, bytes) {
        if (ui.selectedPages.isEmpty() && ui.pageCount > 0) vm.selectAllPages()
        bytes?.let { source ->
            (0 until ui.pageCount).forEach { index ->
                if (index !in bitmaps) {
                    runCatching { repo.renderPage(source, index) }.onSuccess { bitmaps[index] = it }
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    if (bytes == null) {
        CircularProgressIndicator()
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ui.pageCount) { index ->
                Box(
                    Modifier
                        .size(90.dp)
                        .clickable { vm.togglePage(index) },
                ) {
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
}
