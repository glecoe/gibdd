package com.gibdd.officer.network

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val phone: String,
    val password: String,
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

data class PatrolOut(
    val id: Int,
    @SerializedName("inspector_id") val inspectorId: Int,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("ended_at") val endedAt: String?,
)

data class PatrolStatus(
    @SerializedName("on_patrol") val onPatrol: Boolean,
    @SerializedName("patrol_id") val patrolId: Int?,
    @SerializedName("started_at") val startedAt: String?,
)

// --- Администрирование ---

data class UserCreateRequest(
    val phone: String,
    val password: String,
    @SerializedName("full_name") val fullName: String?,
    val role: String,
)

data class RoleUpdateRequest(val role: String)

data class NotificationsUpdateRequest(val enabled: Boolean)

data class FcmTokenRequest(@SerializedName("fcm_token") val fcmToken: String)
