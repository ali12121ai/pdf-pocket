package com.pdfpocket.app

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.pdfpocket.coreui.EmptyState
import com.pdfpocket.coreui.SectionTitle
import com.pdfpocket.coreui.TopBar
import com.pdfpocket.data.PocketDatabase
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = remember { Room.databaseBuilder(context, PocketDatabase::class.java, "pocket.db").build() }
    val qr by db.dao().qrHistory().collectAsState(initial = emptyList())
    val files by db.dao().recentFiles().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TopBar("History", "Everything stays on this device")
        SectionTitle("Saved QR codes (legacy)")
        if (qr.isEmpty()) {
            EmptyState("No QR codes yet", "Generate a code in the QR tab and it will appear here.")
        } else {
            qr.forEach { item ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.padding(8.dp))
                        Text(item.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { scope.launch { db.dao().deleteQr(item.id) } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SectionTitle("Recent documents")
        if (files.isEmpty()) {
            EmptyState("No recent documents", "Exports and saved PDFs will be listed here.")
        } else {
            files.forEach { file ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.padding(8.dp))
                        Column {
                            Text(file.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text("${file.pageCount} pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
