package com.pdfpocket.coreui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private fun iconFor(screen: AppScreen): ImageVector = when (screen) {
    AppScreen.TOOLS -> Icons.Default.Dashboard
    AppScreen.SCAN -> Icons.Default.CameraAlt
    AppScreen.HISTORY -> Icons.Default.History
    AppScreen.SETTINGS -> Icons.Default.Settings
}

@Composable
fun PdfBottomBar(current: AppScreen, onSelect: (AppScreen) -> Unit) {
    NavigationBar {
        AppScreen.entries.forEach { screen ->
            NavigationBarItem(
                selected = screen == current,
                onClick = { onSelect(screen) },
                icon = {
                    Icon(
                        iconFor(screen),
                        contentDescription = screen.label,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = { Text(screen.label) },
            )
        }
    }
}
