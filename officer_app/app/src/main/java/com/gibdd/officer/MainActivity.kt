package com.gibdd.officer

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gibdd.officer.ui.OfficerViewModel
import com.gibdd.officer.ui.screens.AdminScreen
import com.gibdd.officer.ui.screens.IncidentsScreen
import com.gibdd.officer.ui.screens.LoginScreen
import com.gibdd.officer.ui.screens.MapScreen
import com.gibdd.officer.ui.screens.MediaViewerScreen
import com.gibdd.officer.ui.screens.SettingsScreen
import com.gibdd.officer.ui.theme.GibddOfficerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GibddOfficerTheme {
                val vm: OfficerViewModel = viewModel()
                val session by vm.session.collectAsState()
                val navController = rememberNavController()

                // Запрос разрешения на уведомления (Android 13+)
                val notifPermission = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { /* результат не критичен */ }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Стартовый экран зависит от того, авторизован ли пользователь
                val start = if (session.loggedIn) "incidents" else "login"

                NavHost(navController = navController, startDestination = start) {
                    composable("login") {
                        LoginScreen(
                            vm = vm,
                            onOpenSettings = { navController.navigate("settings") },
                        )
                        // После успешного входа переходим к ленте
                        LaunchedEffect(session.loggedIn) {
                            if (session.loggedIn) {
                                navController.navigate("incidents") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        }
                    }
                    composable("incidents") {
                        IncidentsScreen(
                            vm = vm,
                            onOpenAdmin = { navController.navigate("admin") },
                            onOpenSettings = { navController.navigate("settings") },
                            onOpenMap = { lat, lon, id, desc ->
                                val descArg = java.net.URLEncoder.encode(desc ?: "", "UTF-8")
                                navController.navigate("map/$lat/$lon/$id/$descArg")
                            },
                            onOpenMedia = { incidentId, startIndex ->
                                navController.navigate("media/$incidentId/$startIndex")
                            },
                            onLogout = {
                                vm.logout()
                                navController.navigate("login") {
                                    popUpTo("incidents") { inclusive = true }
                                }
                            },
                        )
                    }
                    composable(
                        route = "media/{incidentId}/{startIndex}",
                        arguments = listOf(
                            androidx.navigation.navArgument("incidentId") { type = androidx.navigation.NavType.IntType },
                            androidx.navigation.navArgument("startIndex") { type = androidx.navigation.NavType.IntType },
                        ),
                    ) { entry ->
                        val incidentId = entry.arguments?.getInt("incidentId") ?: 0
                        val startIndex = entry.arguments?.getInt("startIndex") ?: 0
                        val incident = vm.incidentById(incidentId)
                        MediaViewerScreen(
                            media = incident?.media ?: emptyList(),
                            startIndex = startIndex,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = "map/{lat}/{lon}/{id}/{desc}",
                        arguments = listOf(
                            androidx.navigation.navArgument("lat") { type = androidx.navigation.NavType.StringType },
                            androidx.navigation.navArgument("lon") { type = androidx.navigation.NavType.StringType },
                            androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.IntType },
                            androidx.navigation.navArgument("desc") { type = androidx.navigation.NavType.StringType },
                        ),
                    ) { entry ->
                        val lat = entry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
                        val lon = entry.arguments?.getString("lon")?.toDoubleOrNull() ?: 0.0
                        val id = entry.arguments?.getInt("id") ?: 0
                        val desc = entry.arguments?.getString("desc")
                            ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                            ?.ifBlank { null }
                        MapScreen(
                            lat = lat,
                            lon = lon,
                            incidentId = id,
                            description = desc,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable("admin") {
                        AdminScreen(vm = vm, onBack = { navController.popBackStack() })
                    }
                    composable("settings") {
                        SettingsScreen(vm = vm, onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
