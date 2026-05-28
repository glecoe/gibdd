package com.gibdd.officer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gibdd.officer.network.IncidentOut
import com.gibdd.officer.ui.OfficerViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentsScreen(
    vm: OfficerViewModel,
    onOpenAdmin: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMap: (lat: Double, lon: Double, incidentId: Int, description: String?) -> Unit,
    onOpenMedia: (incidentId: Int, startIndex: Int) -> Unit,
    onLogout: () -> Unit,
) {
    val session by vm.session.collectAsState()
    val onPatrol by vm.onPatrol.collectAsState()
    val incidents by vm.incidents.collectAsState()

    val isAdminOrChief = session.role == "admin" || session.role == "chief"

    // Первичная загрузка
    LaunchedEffect(Unit) {
        vm.refreshPatrolStatus()
        vm.loadIncidents(onlyNew = false)
    }

    // Опрос сервера, пока на патруле (fallback к пушам)
    LaunchedEffect(onPatrol) {
        while (onPatrol) {
            vm.loadIncidents(onlyNew = false)
            delay(15_000) // каждые 15 секунд
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Инциденты")
                        Text(
                            roleLabel(session.role) + (if (session.name.isNotBlank()) " · ${session.name}" else ""),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                actions = {
                    if (isAdminOrChief) {
                        IconButton(onClick = onOpenAdmin) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = "Админ")
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Выход")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // --- Кнопка патруля ---
            PatrolButton(onPatrol = onPatrol, onClick = { vm.togglePatrol() })

            if (!onPatrol) {
                Text(
                    "Вы не на патруле. Уведомления о новых инцидентах не приходят.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            if (incidents.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Инцидентов нет", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(incidents, key = { it.id }) { item ->
                        IncidentCard(
                            item = item,
                            onAccept = { vm.acceptIncident(item.id) },
                            onClose = { vm.closeIncident(item.id) },
                            onOpenMap = { lat, lon ->
                                onOpenMap(lat, lon, item.id, item.description)
                            },
                            onOpenMedia = { index -> onOpenMedia(item.id, index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PatrolButton(onPatrol: Boolean, onClick: () -> Unit) {
    val color = if (onPatrol) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
    ) {
        Icon(
            if (onPatrol) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (onPatrol) "Закончить патруль" else "Начать патруль",
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun IncidentCard(
    item: IncidentOut,
    onAccept: () -> Unit,
    onClose: () -> Unit,
    onOpenMap: (lat: Double, lon: Double) -> Unit,
    onOpenMedia: (startIndex: Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Инцидент #${item.id}", fontWeight = FontWeight.Bold)
                StatusBadge(item.status)
            }

            if (!item.description.isNullOrBlank()) {
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
            }

            // Превью медиа: тап открывает фуллскрин-вьювер с этого индекса
            if (item.media.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(item.media) { index, media ->
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenMedia(index) },
                        ) {
                            AsyncImage(
                                model = media.url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            // Значок play поверх видео-превью
                            if (media.mediaType == "video") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Default.PlayCircle,
                                        contentDescription = "Видео",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (item.latitude != null && item.longitude != null) {
                // Кликабельная строка — открывает полноэкранную карту
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenMap(item.latitude, item.longitude) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "%.5f, %.5f · Показать на карте".format(item.latitude, item.longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                item.createdAt.take(19).replace("T", " "),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
            )

            // Действия в зависимости от статуса
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (item.status) {
                    "new" -> {
                        Button(onClick = onAccept) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Принять")
                        }
                    }
                    "accepted" -> {
                        OutlinedButton(onClick = onClose) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Закрыть")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (text, color) = when (status) {
        "new" -> "Новый" to Color(0xFFFF9800)
        "accepted" -> "В работе" to Color(0xFF2196F3)
        "closed" -> "Закрыт" to Color(0xFF4CAF50)
        else -> status to Color.Gray
    }
    Surface(color = color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun roleLabel(role: String): String = when (role) {
    "inspector" -> "Инспектор"
    "admin" -> "Администратор"
    "chief" -> "Начальник"
    else -> role
}
