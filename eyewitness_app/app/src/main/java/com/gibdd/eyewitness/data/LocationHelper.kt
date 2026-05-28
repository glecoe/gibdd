package com.gibdd.eyewitness.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

/** Однократное получение текущих координат. Разрешение должно быть выдано заранее. */
object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        return try {
            val location = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()
            location?.let { it.latitude to it.longitude }
        } catch (_: Exception) {
            null
        }
    }
}
