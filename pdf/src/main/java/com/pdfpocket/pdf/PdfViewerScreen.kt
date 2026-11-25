package com.pdfpocket.pdf

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pdfpocket.coreui.SecondaryButton
import com.pdfpocket.coreui.TopBar

@Composable
fun PdfViewerScreen(
    bytes: ByteArray,
    pageCount: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repo = remember { PdfRepository(context) }
    var page by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(bytes, page) {
        bitmap = runCatching { repo.renderPage(bytes, page) }.getOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        TopBar("PDF viewer", "Page ${page + 1} of $pageCount") {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Close viewer", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            val rendered = bitmap
            if (rendered != null) {
                Image(
                    rendered.asImageBitmap(),
                    contentDescription = "PDF page ${page + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(scaleX = zoom, scaleY = zoom),
                )
            } else {
                Text("Rendering page…", modifier = Modifier.padding(24.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (page > 0) page-- }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous page")
            }
            Text("${page + 1} / $pageCount", modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { if (page < pageCount - 1) page++ }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next page")
            }
            IconButton(onClick = { zoom = (zoom - 0.25f).coerceAtLeast(0.5f) }) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
            }
            Text("${(zoom * 100).toInt()}%")
            IconButton(onClick = { zoom = (zoom + 0.25f).coerceAtMost(3f) }) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
            }
        }
        Spacer(Modifier.height(8.dp))
        SecondaryButton("Close viewer", onClick = onClose)
    }
}
