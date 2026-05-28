package com.gibdd.eyewitness.data

import android.content.Context
import android.net.Uri
import com.gibdd.eyewitness.network.ApiFactory
import com.gibdd.eyewitness.network.ApiService
import com.gibdd.eyewitness.network.EyewitnessRegisterRequest
import com.gibdd.eyewitness.network.IncidentOut
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * Главный слой логики приложения очевидца.
 * Пересоздаёт ApiService при смене адреса сервера.
 */
class IncidentRepository(
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

    /** Гарантирует, что у нас есть валидный токен: входит по device_id при необходимости. */
    suspend fun ensureLoggedIn() {
        if (prefs.getToken() != null) return
        login()
    }

    suspend fun login() {
        val deviceId = prefs.getOrCreateDeviceId()
        val response = api().registerEyewitness(EyewitnessRegisterRequest(deviceId))
        prefs.saveToken(response.accessToken)
    }

    /** Отправляет инцидент. mediaUris — список фото/видео из камеры или галереи. */
    suspend fun sendIncident(
        description: String?,
        latitude: Double?,
        longitude: Double?,
        mediaUris: List<Uri>,
    ): IncidentOut {
        ensureLoggedIn()

        val descBody: RequestBody? =
            description?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
        val latBody: RequestBody? =
            latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val lonBody: RequestBody? =
            longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

        val parts = mediaUris.mapIndexedNotNull { index, uri ->
            uriToPart(uri, index)
        }

        return api().createIncident(descBody, latBody, lonBody, parts)
    }

    suspend fun myIncidents(): List<IncidentOut> {
        ensureLoggedIn()
        return api().myIncidents()
    }

    /** Копирует содержимое Uri во временный файл и оборачивает в multipart-часть. */
    private fun uriToPart(uri: Uri, index: Int): MultipartBody.Part? {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val ext = when {
            mime.startsWith("video") -> ".mp4"
            mime == "image/png" -> ".png"
            else -> ".jpg"
        }
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}_$index$ext")
        return try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            val body = tempFile.asRequestBody(mime.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("files", tempFile.name, body)
        } catch (_: Exception) {
            null
        }
    }
}
