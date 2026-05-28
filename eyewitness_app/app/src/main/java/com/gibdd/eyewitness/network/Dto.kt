package com.gibdd.eyewitness.network

import com.google.gson.annotations.SerializedName

/** Запрос регистрации/входа очевидца по идентификатору устройства. */
data class EyewitnessRegisterRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("full_name") val fullName: String? = null,
)

data class UserOut(
    val id: Int,
    val phone: String?,
    @SerializedName("full_name") val fullName: String?,
    val role: String,
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String,
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: UserOut,
)

data class MediaOut(
    val id: Int,
    val filename: String,
    @SerializedName("media_type") val mediaType: String?,
    val url: String,
)

data class IncidentOut(
    val id: Int,
    @SerializedName("author_id") val authorId: Int,
    val description: String?,
    val latitude: Double?,
    val longitude: Double?,
    val status: String,
    @SerializedName("accepted_by_id") val acceptedById: Int?,
    @SerializedName("created_at") val createdAt: String,
    val media: List<MediaOut> = emptyList(),
)
