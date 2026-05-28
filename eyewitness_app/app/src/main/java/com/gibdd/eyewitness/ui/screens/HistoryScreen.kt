package com.gibdd.eyewitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gibdd.eyewitness.network.IncidentOut
import com.gibdd.eyewitness.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: MainViewModel, onBack: () -> Unit) {
    val incidents by vm.incidents.collectAsState()
    val loading by vm.loadingList.collectAsState()

    LaunchedEffect(Unit) { vm.loadMyIncidents() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои сообщения") },
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
        when {
            loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            incidents.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Вы пока ничего не отправляли", color = Color.Gray)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(incidents, key = { it.id }) { item ->
                        IncidentCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentCard(item: IncidentOut) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("#${item.id}", fontWeight = FontWeight.Bold)
                StatusBadge(item.status)
            }

            if (!item.description.isNullOrBlank()) {
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
            }

            if (item.latitude != null && item.longitude != null) {
                Text(
                    "📍 %.4f, %.4f".format(item.latitude, item.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }

            if (item.media.isNotEmpty()) {
                Text(
                    "📎 Файлов: ${item.media.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }

            Text(
                item.createdAt.take(19).replace("T", " "),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (text, color) = when (status) {
        "new" -> "Новый" to Color(0xFFFF9800)
        "accepted" -> "Принят" to Color(0xFF2196F3)
        "closed" -> "Обработан" to Color(0xFF4CAF50)
        else -> status to Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
