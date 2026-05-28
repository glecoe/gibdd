package com.gibdd.officer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gibdd.officer.ui.OfficerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: OfficerViewModel, onBack: () -> Unit) {
    val savedUrl by vm.serverUrl.collectAsState()
    var urlInput by remember(savedUrl) { mutableStateOf(savedUrl) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Адрес сервера", style = MaterialTheme.typography.titleMedium)
            Text(
                "IP компьютера с сервером. Эмулятор: http://10.0.2.2:8000. " +
                        "Реальный телефон: http://<IP в Wi-Fi>:8000",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("URL сервера") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (urlInput.startsWith("http")) {
                        vm.saveServerUrl(urlInput.trim()) {
                            Toast.makeText(context, "Сохранено", Toast.LENGTH_SHORT).show()
                        }
                        onBack()
                    } else {
                        Toast.makeText(context, "URL должен начинаться с http", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Сохранить") }
        }
    }
}
