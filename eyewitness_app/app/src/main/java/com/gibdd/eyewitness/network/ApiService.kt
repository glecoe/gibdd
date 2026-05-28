package com.gibdd.eyewitness.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    /** Анонимный вход очевидца по device_id. */
    @POST("auth/eyewitness")
    suspend fun registerEyewitness(
        @Body request: EyewitnessRegisterRequest
    ): TokenResponse

    /** Создание инцидента: описание, координаты и список медиафайлов. */
    @Multipart
    @POST("incidents")
    suspend fun createIncident(
        @Part("description") description: RequestBody?,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part files: List<MultipartBody.Part>,
    ): IncidentOut

    /** Список своих отправленных инцидентов. */
    @GET("incidents/my")
    suspend fun myIncidents(): List<IncidentOut>
}
