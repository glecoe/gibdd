package com.gibdd.eyewitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gibdd.eyewitness.ui.MainViewModel
import com.gibdd.eyewitness.ui.screens.HistoryScreen
import com.gibdd.eyewitness.ui.screens.ReportScreen
import com.gibdd.eyewitness.ui.screens.SettingsScreen
import com.gibdd.eyewitness.ui.theme.GibddEyewitnessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GibddEyewitnessTheme {
                val navController = rememberNavController()
                val vm: MainViewModel = viewModel()

                NavHost(navController = navController, startDestination = "report") {
                    composable("report") {
                        ReportScreen(
                            vm = vm,
                            onNavigateToHistory = { navController.navigate("history") },
                            onNavigateToSettings = { navController.navigate("settings") },
                        )
                    }
                    composable("history") {
                        HistoryScreen(vm = vm, onBack = { navController.popBackStack() })
                    }
                    composable("settings") {
                        SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
