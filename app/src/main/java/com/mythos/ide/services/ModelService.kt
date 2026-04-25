package com.mythos.ide.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.mythos.ide.MainActivity
import com.mythos.ide.util.TermuxBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ModelService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private var isModelHealthy = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Starting model..."))
        acquireWakeLock()
        startHealthMonitor()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        serviceScope.cancel()
        updateNotification("Model stopped")
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MythosIDE::ModelService"
        ).apply {
            acquire(WAKE_LOCK_TIMEOUT)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun startHealthMonitor() {
        serviceScope.launch {
            while (isActive) {
                val running = TermuxBridge.isModelRunning()
                if (running != isModelHealthy) {
                    isModelHealthy = running
                    val message = if (running) {
                        "AI code completion running"
                    } else {
                        "Model not responding - waiting for server"
                    }
                    updateNotification(message)
                }
                delay(HEALTH_CHECK_INTERVAL)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the MYTHOS AI model running in the background"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ModelService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MYTHOS Model")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_media_pause, "Stop", pendingStop)
            .build()
    }

    private fun updateNotification(text: String) {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, createNotification(text))
        } catch (_: Exception) {
            // Ignore if we can't update (e.g., service already stopped)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "mythos_model_channel"
        private const val CHANNEL_NAME = "MYTHOS Model"
        private const val ACTION_STOP = "com.mythos.ide.STOP_MODEL"
        private const val HEALTH_CHECK_INTERVAL = 15_000L
        private const val WAKE_LOCK_TIMEOUT = 4 * 60 * 60 * 1000L // 4 hours

        fun stopService(context: Context) {
            val intent = Intent(context, ModelService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
