package com.pdfpocket.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pdfpocket.ads.AdController
import com.pdfpocket.coreui.AppScreen
import com.pdfpocket.coreui.PdfBottomBar
import com.pdfpocket.coreui.PocketTheme
import com.pdfpocket.coreui.ThemePreference
import com.pdfpocket.data.Analytics
import com.pdfpocket.pdf.PdfToolScreen
import com.pdfpocket.scan.ScanScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFFF4F7F7.toInt()
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        AdController.initialize(this)
        ThemePreference.load(this)
        setContent {
            val themeMode by ThemePreference.state.collectAsState()
            PocketTheme(darkOverride = when (themeMode) { 1 -> false; 2 -> true; else -> null }) {
                val navController: NavHostController = rememberNavController()
                val entry by navController.currentBackStackEntryAsState()
                val route = entry?.destination?.route ?: AppScreen.TOOLS.route
                val current = AppScreen.entries.first { it.route == route }

                Scaffold(
                    bottomBar = {
                        if (current != AppScreen.SCAN) {
                            PdfBottomBar(current = current) { screen ->
                                AdController.onUserAction()
                                Analytics.log(Analytics.TOOL_USED, screen.route)
                                navController.navigate(screen.route) {
                                    popUpTo(AppScreen.TOOLS.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppScreen.TOOLS.route,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(AppScreen.TOOLS.route) {
                            PdfToolScreen(Modifier.fillMaxSize()) {
                                navController.navigate(AppScreen.SETTINGS.route)
                            }
                        }
                        composable(AppScreen.SCAN.route) {
                            ScanScreen(Modifier.fillMaxSize()) { navController.navigate(AppScreen.TOOLS.route) }
                        }
                        composable(AppScreen.HISTORY.route) { HistoryScreen(Modifier.fillMaxSize()) }
                        composable(AppScreen.SETTINGS.route) { SettingsScreen(Modifier.fillMaxSize()) }
                    }
                }
            }
        }
    }
}
