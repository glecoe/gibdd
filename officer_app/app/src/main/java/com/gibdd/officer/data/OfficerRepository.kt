package com.gibdd.officer.data

import android.content.Context
import com.gibdd.officer.network.ApiFactory
import com.gibdd.officer.network.ApiService
import com.gibdd.officer.network.FcmTokenRequest
import com.gibdd.officer.network.IncidentOut
import com.gibdd.officer.network.LoginRequest
import com.gibdd.officer.network.NotificationsUpdateRequest
import com.gibdd.officer.network.PatrolStatus
import com.gibdd.officer.network.RoleUpdateRequest
import com.gibdd.officer.network.UserCreateRequest
import com.gibdd.officer.network.UserOut

class OfficerRepository(
    private val context: Context,
    private val prefs: AppPreferences,
) {
    private var cachedBaseUrl: String? = null
    private var api: ApiService? = null

    private suspend fun api(): ApiService {
        val baseUrl = prefs.getBaseUrl()
        if (api == null || cachedBaseUrl != baseUrl) {
            api = ApiFactory.create(baseUrl, prefs)
            cachedBaseUrl = baseUrl
        }
        return api!!
    }

    suspend fun isLoggedIn(): Boolean = prefs.getToken() != null

    suspend fun login(phone: String, password: String): UserOut {
        val resp = api().login(LoginRequest(phone, password))
        prefs.saveSession(
            token = resp.accessToken,
            userId = resp.user.id,
            name = resp.user.fullName,
            role = resp.user.role,
            notifs = resp.user.notificationsEnabled,
        )
        return resp.user
    }

    suspend fun logout() = prefs.clear()

    suspend fun currentRole(): String? = prefs.getUserRole()
    suspend fun currentName(): String? = prefs.getUserName()
    suspend fun currentUserId(): Int? = prefs.getUserId()
    suspend fun notificationsEnabled(): Boolean = prefs.getNotifs()

    /** Регистрирует FCM-токен на сервере, если пользователь авторизован. */
    suspend fun registerFcmToken(token: String) {
        if (!isLoggedIn()) return
        try {
            api().updateFcmToken(FcmTokenRequest(token))
        } catch (_: Exception) {
        }
    }

    // --- Патруль ---
    suspend fun patrolStatus(): PatrolStatus = api().patrolStatus()
    suspend fun startPatrol() = api().startPatrol()
    suspend fun stopPatrol() = api().stopPatrol()

    // --- Инциденты ---
    suspend fun incidents(onlyNew: Boolean): List<IncidentOut> = api().listIncidents(onlyNew)
    suspend fun acceptIncident(id: Int): IncidentOut = api().acceptIncident(id)
    suspend fun closeIncident(id: Int): IncidentOut = api().closeIncident(id)

    // --- Администрирование ---
    suspend fun users(): List<UserOut> = api().listUsers()

    suspend fun createUser(phone: String, password: String, name: String?, role: String): UserOut =
        api().createUser(UserCreateRequest(phone, password, name, role))

    suspend fun updateRole(id: Int, role: String): UserOut =
        api().updateRole(id, RoleUpdateRequest(role))

    suspend fun deactivateUser(id: Int): UserOut = api().deactivateUser(id)

    suspend fun toggleNotifications(enabled: Boolean): UserOut {
        val updated = api().toggleNotifications(NotificationsUpdateRequest(enabled))
        prefs.updateRoleAndNotifs(updated.role, updated.notificationsEnabled)
        return updated
    }
}
