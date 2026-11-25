package com.pdfpocket.pdf

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pdfpocket.coreui.EmptyState
import com.pdfpocket.coreui.TopBar

data class DevicePdf(val uri: Uri, val name: String, val size: Long)

@Composable
fun DocumentsScreen(modifier: Modifier = Modifier, sessionId: String = "documents", onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: PdfViewModel = viewModel(key = sessionId)
    val ui by vm.state.collectAsState()
    val repo = remember { PdfRepository(context) }
    var documents by remember { mutableStateOf<List<DevicePdf>>(emptyList()) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerBytes by remember { mutableStateOf<ByteArray?>(null) }
    var openAfterUnlock by remember { mutableStateOf(false) }
    val thumbs = remember { mutableStateMapOf<Uri, Bitmap>() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.addFiles(listOf(uri))
            vm.openViewer()
        }
    }

    LaunchedEffect(Unit) {
        vm.setSession(sessionId)
        val list = mutableListOf<DevicePdf>()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
        )
        runCatching {
            context.contentResolver.query(
                uri, projection,
                "${MediaStore.Files.FileColumns.MIME_TYPE}=?", arrayOf("application/pdf"),
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                while (cursor.moveToNext()) {
                    list.add(
                        DevicePdf(
                            uri = Uri.withAppendedPath(uri, cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID))),
                            name = if (nameIndex >= 0) cursor.getString(nameIndex) else "document.pdf",
                            size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L,
                        ),
                    )
                }
            }
        }
        documents = list
    }

    LaunchedEffect(documents) {
        documents.forEach { doc ->
            if (doc.uri !in thumbs) {
                val bytes = runCatching {
                    context.contentResolver.openInputStream(doc.uri)?.use { it.readBytes() }
                }.getOrNull()
                if (bytes != null && !repo.isEncrypted(bytes)) {
                    runCatching { repo.renderPage(bytes, 0) }.onSuccess { thumbs[doc.uri] = it }
                }
            }
        }
    }

    LaunchedEffect(ui.needsPassword, ui.viewPath) {
        if (ui.needsPassword) {
            password = ""
            showPassword = true
        } else if (ui.viewPath != null && !showPassword) {
            viewerBytes = PdfSessionStore.read(ui.viewPath)
            if (viewerBytes != null) viewerOpen = true
        }
    }

    LaunchedEffect(ui.viewPath, openAfterUnlock) {
        if (openAfterUnlock && ui.viewPath != null) {
            viewerBytes = PdfSessionStore.read(ui.viewPath)
            if (viewerBytes != null) viewerOpen = true
            openAfterUnlock = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { vm.dispose() }
    }

    if (showPassword) {
        AlertDialog(
            onDismissRequest = { showPassword = false },
            title = { Text("This PDF is locked") },
            text = {
                Column {
                    Text("Enter the password to open this document.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showPassword = false
                    openAfterUnlock = true
                    vm.unlockAndOpen(password)
                }) { Text("Unlock and view") }
            },
            dismissButton = { TextButton(onClick = { showPassword = false }) { Text("Cancel") } },
        )
    }

    if (viewerOpen && viewerBytes != null) {
        PdfViewerScreen(
            bytes = viewerBytes!!,
            pageCount = ui.pageCount,
            onClose = {
                viewerOpen = false
                viewerBytes = null
                vm.dispose()
            },
            modifier = modifier,
        )
        return
    }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        TopBar("Documents", "PDFs on this device") {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
        }
        TextButton(onClick = { picker.launch(arrayOf("application/pdf")) }) { Text("Browse device storage") }
        if (documents.isEmpty()) {
            EmptyState("No PDFs found", "Use Browse device storage to pick a PDF from any folder.")
        } else {
            LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                items(documents) { doc ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.addFiles(listOf(doc.uri))
                                vm.openViewer()
                            },
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            val bitmap = thumbs[doc.uri]
                            if (bitmap != null) {
                                Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(56.dp))
                            } else {
                                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(doc.name, style = MaterialTheme.typography.titleSmall)
                                Text("${doc.size / 1024} KB", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
