package com.gibdd.officer.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gibdd.officer.MainActivity
import com.gibdd.officer.data.AppPreferences
import com.gibdd.officer.data.OfficerRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Принимает пуш-уведомления о новых инцидентах.
 * Работает только при настроенном Firebase (наличие google-services.json).
 */
class PushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Токен изменился — отправляем на сервер
        val prefs = AppPreferences(applicationContext)
        val repo = OfficerRepository(applicationContext, prefs)
        CoroutineScope(Dispatchers.IO).launch {
            repo.registerFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "Новый инцидент"
        val body = message.notification?.body ?: "Поступило сообщение от очевидца"
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "incidents"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Инциденты",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Уведомления о новых сообщениях очевидцев" }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
