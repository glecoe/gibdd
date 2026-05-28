package com.gibdd.eyewitness.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gibdd.eyewitness.data.LocationHelper
import com.gibdd.eyewitness.ui.MainViewModel
import com.gibdd.eyewitness.ui.ReportUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    vm: MainViewModel,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val state by vm.report.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Лаунчеры ---

    // Галерея (мульти-выбор)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(9),
    ) { uris: List<Uri> -> vm.addMedia(uris) }

    // Камера (фото)
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) cameraUri?.let { vm.addMedia(listOf(it)) }
    }

    // Разрешение на камеру
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val uri = createTempImageUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Нет доступа к камере", Toast.LENGTH_SHORT).show()
        }
    }

    // Разрешение на локацию
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                val loc = LocationHelper.getCurrentLocation(context)
                if (loc != null) {
                    vm.setLocation(loc.first, loc.second)
                } else {
                    Toast.makeText(context, "Не удалось определить координаты", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Нет доступа к геолокации", Toast.LENGTH_SHORT).show()
        }
    }

    // Снекбар при успехе
    LaunchedEffect(state.sentOk) {
        if (state.sentOk) {
            Toast.makeText(context, "Сообщение отправлено!", Toast.LENGTH_SHORT).show()
            vm.consumeSentOk()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сообщить о нарушении") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "История")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // --- Описание ---
            OutlinedTextField(
                value = state.description,
                onValueChange = vm::onDescriptionChange,
                label = { Text("Описание (марка, цвет, номер, что происходит)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
            )

            // --- Медиа ---
            Text("Фото / видео", style = MaterialTheme.typography.titleSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Галерея")
                }

                OutlinedButton(onClick = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Камера")
                }
            }

            if (state.mediaUris.isNotEmpty()) {
                MediaPreviewRow(uris = state.mediaUris, onRemove = vm::removeMedia)
            }

            // --- Геолокация ---
            Text("Геолокация", style = MaterialTheme.typography.titleSmall)

            if (state.latitude != null && state.longitude != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "%.5f, %.5f".format(state.latitude, state.longitude),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { vm.clearLocation() }) {
                        Icon(Icons.Default.Close, contentDescription = "Убрать", modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                OutlinedButton(onClick = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                }) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Определить местоположение")
                }
            }

            // --- Ошибка ---
            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.weight(1f))

            // --- Кнопка отправки ---
            Button(
                onClick = { vm.submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.sending,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                if (state.sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Отправка…")
                } else {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Отправить", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MediaPreviewRow(uris: List<Uri>, onRemove: (Uri) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(uris) { uri ->
            Box(modifier = Modifier.size(90.dp)) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Прикреплённый файл",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = "Удалить",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.7f))
                        .clickable { onRemove(uri) },
                    tint = Color.Red,
                )
            }
        }
    }
}

/** Создаёт временный Uri для съёмки фото камерой через FileProvider. */
private fun createTempImageUri(context: android.content.Context): Uri {
    val dir = java.io.File(context.cacheDir, "images").also { it.mkdirs() }
    val file = java.io.File.createTempFile("photo_", ".jpg", dir)
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}
