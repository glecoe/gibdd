package com.gibdd.officer.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // --- Авторизация ---
    @POST("auth/login-json")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @GET("auth/me")
    suspend fun me(): UserOut

    @POST("auth/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): UserOut

    // --- Патруль ---
    @POST("patrol/start")
    suspend fun startPatrol(): PatrolOut

    @POST("patrol/stop")
    suspend fun stopPatrol(): PatrolOut

    @GET("patrol/status")
    suspend fun patrolStatus(): PatrolStatus

    // --- Инциденты ---
    @GET("incidents")
    suspend fun listIncidents(@Query("only_new") onlyNew: Boolean = false): List<IncidentOut>

    @POST("incidents/{id}/accept")
    suspend fun acceptIncident(@Path("id") id: Int): IncidentOut

    @POST("incidents/{id}/close")
    suspend fun closeIncident(@Path("id") id: Int): IncidentOut

    // --- Администрирование ---
    @GET("admin/users")
    suspend fun listUsers(): List<UserOut>

    @POST("admin/users")
    suspend fun createUser(@Body request: UserCreateRequest): UserOut

    @PATCH("admin/users/{id}/role")
    suspend fun updateRole(@Path("id") id: Int, @Body request: RoleUpdateRequest): UserOut

    @DELETE("admin/users/{id}")
    suspend fun deactivateUser(@Path("id") id: Int): UserOut

    @PATCH("admin/notifications")
    suspend fun toggleNotifications(@Body request: NotificationsUpdateRequest): UserOut
}
