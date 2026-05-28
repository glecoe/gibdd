package com.gibdd.eyewitness.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "eyewitness_prefs")

/**
 * Хранилище настроек приложения.
 * Держит идентификатор устройства (генерится один раз), JWT-токен и адрес сервера.
 */
class AppPreferences(private val context: Context) {

    companion object {
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val TOKEN = stringPreferencesKey("token")
        private val BASE_URL = stringPreferencesKey("base_url")

        // Для эмулятора Android хост-машина доступна как 10.0.2.2.
        // На реальном телефоне поменяй на IP компьютера в локальной сети.
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN] }
    val baseUrlFlow: Flow<String> = context.dataStore.data.map { it[BASE_URL] ?: DEFAULT_BASE_URL }

    /** Возвращает device_id, создавая и сохраняя его при первом обращении. */
    suspend fun getOrCreateDeviceId(): String {
        val existing = context.dataStore.data.first()[DEVICE_ID]
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { it[DEVICE_ID] = newId }
        return newId
    }

    suspend fun getToken(): String? = context.dataStore.data.first()[TOKEN]

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[TOKEN] = token }
    }

    suspend fun getBaseUrl(): String =
        context.dataStore.data.first()[BASE_URL] ?: DEFAULT_BASE_URL

    suspend fun saveBaseUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        context.dataStore.edit { it[BASE_URL] = normalized }
    }
}
