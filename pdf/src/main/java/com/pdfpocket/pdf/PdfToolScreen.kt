package com.pdfpocket.pdf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdfpocket.coreui.HeroCard
import com.pdfpocket.coreui.SectionTitle
import com.pdfpocket.coreui.ToolTile
import com.pdfpocket.coreui.TopBar
import com.pdfpocket.coreui.PocketTheme

@Composable
fun PdfToolScreen(modifier: Modifier = Modifier, onSettings: () -> Unit = {}) {
    var activeTool by remember { mutableStateOf<ToolKey?>(null) }
    var documentsOpen by remember { mutableStateOf(false) }

    if (documentsOpen) {
        DocumentsScreen(modifier = modifier, onBack = { documentsOpen = false })
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TopBar("PDF Pocket", "Every tool works on its own files")
        HeroCard(
            title = "Your PDF workshop",
            subtitle = "View, unlock, protect and edit PDFs offline — no account, no upload.",
            chips = listOf("No account", "No upload", "No limits"),
        )
        Spacer(Modifier.height(20.dp))
        SectionTitle("Documents")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile(Icons.Default.Description, "View PDF", "Device files", { documentsOpen = true }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        SectionTitle("PDF tools")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile(Icons.Default.CallMerge, "Merge", "Join PDFs", { activeTool = ToolKey.MERGE }, Modifier.weight(1f))
            ToolTile(Icons.Default.ContentCut, "Extract", "Pick pages", { activeTool = ToolKey.EXTRACT }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile(Icons.Default.SwapVert, "Organise", "Rotate · reverse", { activeTool = ToolKey.ORGANISE }, Modifier.weight(1f))
            ToolTile(Icons.Default.ContentCopy, "Compress", "Smaller files", { activeTool = ToolKey.COMPRESS }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile(Icons.Default.LockOpen, "Unlock", "Remove password", { activeTool = ToolKey.UNLOCK }, Modifier.weight(1f))
            ToolTile(Icons.Default.Lock, "Protect", "Add password", { activeTool = ToolKey.PROTECT }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        SectionTitle("More tools")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile(Icons.Default.ContentCut, "Delete pages", "Pick pages", { activeTool = ToolKey.DELETE }, Modifier.weight(1f))
            ToolTile(Icons.Default.Crop, "Crop", "Pick pages", { activeTool = ToolKey.CROP }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile(Icons.Default.Numbers, "Page numbers", "Pick pages", { activeTool = ToolKey.NUMBER }, Modifier.weight(1f))
            ToolTile(Icons.Default.TextFields, "Watermark", "Pick pages", { activeTool = ToolKey.WATERMARK }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile(Icons.Default.Image, "To images", "Pick pages", { activeTool = ToolKey.TO_IMAGES }, Modifier.weight(1f))
            ToolTile(Icons.Default.Image, "Images to PDF", "JPG/PNG → PDF", { activeTool = ToolKey.IMAGES_TO_PDF }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }

    activeTool?.let { tool ->
        ToolSheet(tool = tool, onDismiss = { activeTool = null })
    }
}
