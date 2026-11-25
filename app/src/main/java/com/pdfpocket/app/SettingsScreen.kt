package com.pdfpocket.app

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pdfpocket.ads.AdController
import com.pdfpocket.coreui.InfoCard
import com.pdfpocket.coreui.PrimaryButton
import com.pdfpocket.coreui.SecondaryButton
import com.pdfpocket.coreui.SectionTitle
import com.pdfpocket.coreui.TopBar
import com.pdfpocket.coreui.ThemePreference
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? Activity
    val removed = activity?.let { AdController.isAdsRemoved(it) } ?: false

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TopBar("Settings", "Support the app your way")
        InfoCard(
            title = "Free by design",
            body = "No account, no upload, no watermark, no daily limit. All processing happens on your phone.",
            icon = Icons.Default.Info,
        )
        Spacer(Modifier.height(16.dp))
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle("Advertising")
                Text(
                    if (removed) "Ads removed — thank you for supporting the app." else "Ads keep PDF Pocket free for everyone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    text = if (removed) "Ads already removed" else "Remove ads (one-time)",
                    icon = Icons.Default.FavoriteBorder,
                    enabled = !removed && activity != null,
                ) { activity?.let { AdController.launchRemoveAds(it) } }
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Try a rewarded ad", enabled = activity != null) {
                    activity?.let { ad -> AdController.showRewarded(ad) { Toast.makeText(ad, "Reward granted", Toast.LENGTH_SHORT).show() } }
                }
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Preview an interstitial", enabled = activity != null) {
                    if (activity != null) {
                        AdController.onUserAction(); AdController.onUserAction()
                        AdController.maybeShowInterstitial(activity)
                    }
                }
                Spacer(Modifier.height(12.dp))
                AdController.Banner()
            }
        }
        Spacer(Modifier.height(16.dp))
        val mode by ThemePreference.state.collectAsState(initial = 0)
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle("Appearance")
                SecondaryButton("System theme${if (mode == 0) " (active)" else ""}") {
                    ThemePreference.set(context, 0)
                }
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Light${if (mode == 1) " (active)" else ""}") {
                    ThemePreference.set(context, 1)
                }
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Dark${if (mode == 2) " (active)" else ""}") {
                    ThemePreference.set(context, 2)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle("About")
                Text("PDF Pocket · version 0.1.0", style = MaterialTheme.typography.bodyMedium)
                Text("PDF tools, scanner, OCR and QR codes in one offline app.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
