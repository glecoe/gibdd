package com.gibdd.officer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.gibdd.officer.network.UserOut
import com.gibdd.officer.ui.OfficerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(vm: OfficerViewModel, onBack: () -> Unit) {
    val session by vm.session.collectAsState()
    val users by vm.users.collectAsState()
    val context = LocalContext.current

    val isChief = session.role == "chief"
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadUsers() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Управление") },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить сотрудника")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // Тумблер моих уведомлений
            ListItem(
                headlineContent = { Text("Уведомления от очевидцев") },
                supportingContent = { Text("Включает/выключает пуши только для вас") },
                trailingContent = {
                    Switch(
                        checked = session.notificationsEnabled,
                        onCheckedChange = { vm.toggleMyNotifications(it) },
                    )
                },
            )
            HorizontalDivider()

            Text(
                "Сотрудники и пользователи",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(16.dp),
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(users, key = { it.id }) { user ->
                    UserRow(
                        user = user,
                        isChief = isChief,
                        currentUserId = session.userId,
                        onChangeRole = { role -> vm.changeRole(user.id, role) },
                        onDeactivate = { vm.deactivate(user.id) },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateUserDialog(
            isChief = isChief,
            onDismiss = { showCreateDialog = false },
            onCreate = { phone, pass, name, role ->
                vm.createUser(phone, pass, name, role) { ok, err ->
                    if (ok) {
                        Toast.makeText(context, "Сотрудник создан", Toast.LENGTH_SHORT).show()
                        showCreateDialog = false
                    } else {
                        Toast.makeText(context, err ?: "Ошибка", Toast.LENGTH_LONG).show()
                    }
                }
            },
        )
    }
}

@Composable
private fun UserRow(
    user: UserOut,
    isChief: Boolean,
    currentUserId: Int,
    onChangeRole: (String) -> Unit,
    onDeactivate: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isSelf = user.id == currentUserId

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    user.fullName?.ifBlank { null } ?: user.phone ?: "Без имени",
                    fontWeight = FontWeight.SemiBold,
                    color = if (user.isActive) Color.Unspecified else Color.Gray,
                )
                Text(
                    roleLabelAdmin(user.role) + (if (!user.isActive) " · отключён" else ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
                if (user.phone != null && user.fullName?.isNotBlank() == true) {
                    Text(user.phone, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            if (!isSelf && user.isActive) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Действия")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        // Назначение базовых ролей — доступно admin и chief
                        DropdownMenuItem(
                            text = { Text("Сделать инспектором") },
                            onClick = { onChangeRole("inspector"); menuOpen = false },
                        )
                        DropdownMenuItem(
                            text = { Text("Сделать очевидцем") },
                            onClick = { onChangeRole("eyewitness"); menuOpen = false },
                        )
                        // Админ/начальник — только chief
                        if (isChief) {
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Назначить администратором") },
                                onClick = { onChangeRole("admin"); menuOpen = false },
                            )
                            DropdownMenuItem(
                                text = { Text("Назначить начальником") },
                                onClick = { onChangeRole("chief"); menuOpen = false },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Отключить", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(Icons.Default.Block, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = { onDeactivate(); menuOpen = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(
    isChief: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String) -> Unit,
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("inspector") }
    var roleMenuOpen by remember { mutableStateOf(false) }

    // Админ может создавать только инспекторов; начальник — любые роли
    val roleOptions = if (isChief)
        listOf("inspector" to "Инспектор", "admin" to "Администратор", "chief" to "Начальник")
    else
        listOf("inspector" to "Инспектор")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый сотрудник") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Телефон") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Пароль") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("ФИО (необязательно)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = roleMenuOpen,
                    onExpandedChange = { roleMenuOpen = it },
                ) {
                    OutlinedTextField(
                        value = roleOptions.first { it.first == role }.second,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Роль") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleMenuOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = roleMenuOpen,
                        onDismissRequest = { roleMenuOpen = false },
                    ) {
                        roleOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { role = value; roleMenuOpen = false },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(phone.trim(), password, name.trim(), role) },
                enabled = phone.isNotBlank() && password.isNotBlank(),
            ) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

private fun roleLabelAdmin(role: String): String = when (role) {
    "eyewitness" -> "Очевидец"
    "inspector" -> "Инспектор"
    "admin" -> "Администратор"
    "chief" -> "Начальник"
    else -> role
}
