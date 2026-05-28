package com.gibdd.eyewitness.network

import com.gibdd.eyewitness.data.AppPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Создаёт ApiService под текущий базовый URL.
 * Токен берётся из хранилища на каждый запрос, поэтому свежий токен
 * подхватывается без пересоздания клиента.
 */
object ApiFactory {

    fun create(baseUrl: String, prefs: AppPreferences): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                // Токен читаем синхронно — интерцептор и так работает в фоновом потоке OkHttp
                val token = runCatchingBlocking { prefs.getToken() }
                if (!token.isNullOrEmpty()) {
                    builder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(builder.build())
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS) // загрузка видео может быть долгой
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

/** Мостик для вызова suspend-функции из синхронного интерцептора OkHttp. */
private fun <T> runCatchingBlocking(block: suspend () -> T): T? =
    try {
        kotlinx.coroutines.runBlocking { block() }
    } catch (_: Exception) {
        null
    }
