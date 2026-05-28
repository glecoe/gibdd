package com.gibdd.officer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gibdd.officer.data.AppPreferences
import com.gibdd.officer.data.OfficerRepository
import com.gibdd.officer.network.IncidentOut
import com.gibdd.officer.network.UserOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionState(
    val loggedIn: Boolean = false,
    val role: String = "",
    val name: String = "",
    val userId: Int = -1,
    val notificationsEnabled: Boolean = true,
)

class OfficerViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPreferences(app)
    private val repo = OfficerRepository(app, prefs)

    private val _session = MutableStateFlow(SessionState())
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    // Патруль
    private val _onPatrol = MutableStateFlow(false)
    val onPatrol: StateFlow<Boolean> = _onPatrol.asStateFlow()

    // Инциденты
    private val _incidents = MutableStateFlow<List<IncidentOut>>(emptyList())
    val incidents: StateFlow<List<IncidentOut>> = _incidents.asStateFlow()

    // Пользователи (админка)
    private val _users = MutableStateFlow<List<UserOut>>(emptyList())
    val users: StateFlow<List<UserOut>> = _users.asStateFlow()

    private val _serverUrl = MutableStateFlow(AppPreferences.DEFAULT_BASE_URL)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    init {
        viewModelScope.launch {
            _serverUrl.value = prefs.getBaseUrl()
            if (repo.isLoggedIn()) {
                _session.value = SessionState(
                    loggedIn = true,
                    role = repo.currentRole() ?: "",
                    name = repo.currentName() ?: "",
                    userId = repo.currentUserId() ?: -1,
                    notificationsEnabled = repo.notificationsEnabled(),
                )
                refreshPatrolStatus()
            }
        }
    }

    fun saveServerUrl(url: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            prefs.saveBaseUrl(url)
            _serverUrl.value = prefs.getBaseUrl()
            onDone()
        }
    }

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _loginError.value = null
            try {
                val user = repo.login(phone, password)
                _session.value = SessionState(
                    loggedIn = true,
                    role = user.role,
                    name = user.fullName ?: "",
                    userId = user.id,
                    notificationsEnabled = user.notificationsEnabled,
                )
                refreshPatrolStatus()
                // Регистрируем FCM-токен (если Firebase настроен)
                registerFcmTokenSafely()
            } catch (e: Exception) {
                _loginError.value = parseError(e)
            } finally {
                _loading.value = false
            }
        }
    }

    private fun registerFcmTokenSafely() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    viewModelScope.launch { repo.registerFcmToken(token) }
                }
        } catch (_: Throwable) {
            // Firebase не настроен — игнорируем, работаем через опрос
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            _session.value = SessionState()
            _onPatrol.value = false
            _incidents.value = emptyList()
            _users.value = emptyList()
        }
    }

    fun refreshPatrolStatus() {
        viewModelScope.launch {
            try {
                _onPatrol.value = repo.patrolStatus().onPatrol
            } catch (_: Exception) {
            }
        }
    }

    fun togglePatrol() {
        viewModelScope.launch {
            try {
                if (_onPatrol.value) {
                    repo.stopPatrol()
                    _onPatrol.value = false
                } else {
                    repo.startPatrol()
                    _onPatrol.value = true
                    loadIncidents(onlyNew = true)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun loadIncidents(onlyNew: Boolean) {
        viewModelScope.launch {
            try {
                _incidents.value = repo.incidents(onlyNew)
            } catch (_: Exception) {
            }
        }
    }

    /** Возвращает инцидент по id из текущего кэша. */
    fun incidentById(id: Int): IncidentOut? = _incidents.value.find { it.id == id }

    fun acceptIncident(id: Int) {
        viewModelScope.launch {
            try {
                repo.acceptIncident(id)
                loadIncidents(onlyNew = false)
            } catch (_: Exception) {
            }
        }
    }

    fun closeIncident(id: Int) {
        viewModelScope.launch {
            try {
                repo.closeIncident(id)
                loadIncidents(onlyNew = false)
            } catch (_: Exception) {
            }
        }
    }

    // --- Админка ---
    fun loadUsers() {
        viewModelScope.launch {
            try {
                _users.value = repo.users()
            } catch (_: Exception) {
            }
        }
    }

    fun createUser(phone: String, password: String, name: String, role: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                repo.createUser(phone, password, name.ifBlank { null }, role)
                loadUsers()
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, parseError(e))
            }
        }
    }

    fun changeRole(id: Int, role: String) {
        viewModelScope.launch {
            try {
                repo.updateRole(id, role)
                loadUsers()
            } catch (_: Exception) {
            }
        }
    }

    fun deactivate(id: Int) {
        viewModelScope.launch {
            try {
                repo.deactivateUser(id)
                loadUsers()
            } catch (_: Exception) {
            }
        }
    }

    fun toggleMyNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val updated = repo.toggleNotifications(enabled)
                _session.value = _session.value.copy(notificationsEnabled = updated.notificationsEnabled)
            } catch (_: Exception) {
            }
        }
    }

    private fun parseError(e: Exception): String {
        val msg = e.message ?: "Ошибка"
        return when {
            msg.contains("401") -> "Неверный телефон или пароль"
            msg.contains("403") -> "Недостаточно прав"
            else -> "Ошибка соединения: $msg"
        }
    }
}
