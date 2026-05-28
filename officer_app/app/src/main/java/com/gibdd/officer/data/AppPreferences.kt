package com.gibdd.officer.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "officer_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val TOKEN = stringPreferencesKey("token")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val USER_ID = intPreferencesKey("user_id")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_ROLE = stringPreferencesKey("user_role")
        private val NOTIFS = booleanPreferencesKey("notifs")

        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"
    }

    suspend fun getToken(): String? = context.dataStore.data.first()[TOKEN]

    suspend fun saveSession(token: String, userId: Int, name: String?, role: String, notifs: Boolean) {
        context.dataStore.edit {
            it[TOKEN] = token
            it[USER_ID] = userId
            it[USER_NAME] = name ?: ""
            it[USER_ROLE] = role
            it[NOTIFS] = notifs
        }
    }

    suspend fun updateRoleAndNotifs(role: String, notifs: Boolean) {
        context.dataStore.edit {
            it[USER_ROLE] = role
            it[NOTIFS] = notifs
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(TOKEN)
            it.remove(USER_ID)
            it.remove(USER_NAME)
            it.remove(USER_ROLE)
            it.remove(NOTIFS)
        }
    }

    suspend fun getUserId(): Int? = context.dataStore.data.first()[USER_ID]
    suspend fun getUserName(): String? = context.dataStore.data.first()[USER_NAME]
    suspend fun getUserRole(): String? = context.dataStore.data.first()[USER_ROLE]
    suspend fun getNotifs(): Boolean = context.dataStore.data.first()[NOTIFS] ?: true

    suspend fun getBaseUrl(): String =
        context.dataStore.data.first()[BASE_URL] ?: DEFAULT_BASE_URL

    suspend fun saveBaseUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        context.dataStore.edit { it[BASE_URL] = normalized }
    }
}
