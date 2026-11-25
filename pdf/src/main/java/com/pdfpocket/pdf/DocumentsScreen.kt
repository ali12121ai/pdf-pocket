package com.pdfpocket.pdf

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.pdfpocket.coreui.EmptyState
import com.pdfpocket.coreui.SectionTitle
import com.pdfpocket.coreui.TopBar
import com.pdfpocket.data.FolderStore
import com.pdfpocket.data.PocketDatabase
import com.pdfpocket.data.RecentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DevicePdf(val uri: Uri, val name: String, val size: Long, val source: String)

@Composable
fun DocumentsScreen(modifier: Modifier = Modifier, sessionId: String = "documents", onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: PdfViewModel = viewModel(key = sessionId)
    val ui by vm.state.collectAsState()
    val repo = remember { PdfRepository(context) }
    val db = remember { Room.databaseBuilder(context, PocketDatabase::class.java, "pocket.db").build() }
    val recents by db.dao().recentFiles().collectAsState(initial = emptyList())
    var documents by remember { mutableStateOf<List<DevicePdf>>(emptyList()) }
    var folders by remember { mutableStateOf(FolderStore.list(context)) }
    var query by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerBytes by remember { mutableStateOf<ByteArray?>(null) }
    var openAfterUnlock by remember { mutableStateOf(false) }
    val thumbs = remember { mutableStateMapOf<Uri, Bitmap>() }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) openDocument(vm, uri, db)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            FolderStore.add(context, uri)
            folders = FolderStore.list(context)
        }
    }

    LaunchedEffect(Unit) { vm.setSession(sessionId) }
    DisposableEffect(Unit) { onDispose { vm.dispose() } }

    LaunchedEffect(folders) {
        val list = mutableListOf<DevicePdf>()
        folders.forEach { tree ->
            runCatching {
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(
                    tree, DocumentsContract.getTreeDocumentId(tree),
                )
                context.contentResolver.query(
                    children,
                    arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE),
                    null, null, null,
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                    while (cursor.moveToNext()) {
                        if (cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)) != "application/pdf") continue
                        val id = cursor.getString(idIndex)
                        list.add(
                            DevicePdf(
                                uri = DocumentsContract.buildDocumentUriUsingTree(tree, id),
                                name = cursor.getString(nameIndex),
                                size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L,
                                source = "Folders",
                            ),
                        )
                    }
                }
            }
        }
        // MediaStore returns only files this app can see; the copy is honest about that.
        val media = MediaStore.Files.getContentUri("external")
        runCatching {
            context.contentResolver.query(
                media,
                arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.MIME_TYPE),
                "${MediaStore.Files.FileColumns.MIME_TYPE}=?", arrayOf("application/pdf"),
                "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val nameIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                while (cursor.moveToNext()) {
                    list.add(
                        DevicePdf(
                            uri = Uri.withAppendedPath(media, cursor.getString(idIndex)),
                            name = if (nameIndex >= 0) cursor.getString(nameIndex) else "document.pdf",
                            size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L,
                            source = "Device",
                        ),
                    )
                }
            }
        }
        documents = list.distinctBy { it.uri.toString() }
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

    if (showPassword) {
        AlertDialog(
            onDismissRequest = { showPassword = false; vm.dispose(); vm.setSession(sessionId) },
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
            dismissButton = {
                TextButton(onClick = { showPassword = false; vm.dispose(); vm.setSession(sessionId) }) { Text("Cancel") }
            },
        )
    }

    if (viewerOpen && viewerBytes != null) {
        PdfViewerScreen(
            bytes = viewerBytes!!,
            pageCount = ui.pageCount,
            onClose = { viewerOpen = false; viewerBytes = null; vm.dispose() },
            modifier = modifier,
        )
        return
    }

    val recentDocs = recents.map { DevicePdf(Uri.parse(it.uri), it.displayName, 0L, "Recents") }
    val visible = (recentDocs + documents).filter { it.name.contains(query, ignoreCase = true) }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        TopBar("Documents", "Recents, folders and device files") {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search PDFs") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { folderPicker.launch(null) }) { Text("Add folder") }
            TextButton(onClick = { filePicker.launch(arrayOf("application/pdf")) }) { Text("Browse files") }
        }
        SectionTitle("Files")
        if (visible.isEmpty()) {
            EmptyState(
                "No PDFs found",
                "The system only shows files it can read. Add a folder or browse files to open a PDF from any location.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visible) { doc -> DocumentRow(doc, thumbs, repo) { openDocument(vm, doc.uri, db) } }
            }
        }
    }
}

private fun openDocument(vm: PdfViewModel, uri: Uri, db: PocketDatabase) {
    vm.addFiles(listOf(uri))
    vm.openViewer()
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        db.dao().insertRecent(RecentFile(displayName = uri.lastPathSegment ?: "document.pdf", uri = uri.toString(), mimeType = "application/pdf", pageCount = 0))
    }
}

@Composable
private fun DocumentRow(
    doc: DevicePdf,
    thumbs: MutableMap<Uri, Bitmap>,
    repo: PdfRepository,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(doc.uri) {
        if (doc.uri !in thumbs) {
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(doc.uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            if (bytes != null && !repo.isEncrypted(bytes)) {
                runCatching { repo.renderPage(bytes, 0) }.onSuccess { thumbs[doc.uri] = it }
            }
        }
    }
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
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
                Text("${doc.source} · ${doc.size / 1024} KB", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
