package com.example.xrealtaptoggle

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class XrealMonitorService : Service(), DisplayManager.DisplayListener {
    private lateinit var displayManager: DisplayManager
    private val handler = Handler(Looper.getMainLooper())
    private var shuttingDown = false

    override fun onCreate() {
        super.onCreate()
        displayManager = getSystemService(DisplayManager::class.java)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Waiting for XREAL display"))
        displayManager.registerDisplayListener(this, handler)
        syncState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        shuttingDown = true
        displayManager.unregisterDisplayListener(this)
        maybeRestoreSetting()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDisplayAdded(displayId: Int) {
        syncState()
    }

    override fun onDisplayRemoved(displayId: Int) {
        syncState()
    }

    override fun onDisplayChanged(displayId: Int) {
        syncState()
    }

    private fun syncState() {
        try {
            val externalDisplays = try {
                displayManager.displays.filter { it.displayId != android.view.Display.DEFAULT_DISPLAY }
            } catch (_: Exception) {
                emptyList()
            }
            val hasExternalDisplay = externalDisplays.isNotEmpty()
            val displaySummary = externalDisplays.joinToString { "#${it.displayId}" }.ifBlank { "unknown" }

            if (!ShizukuController.isAvailable()) {
                updateNotification("Start Shizuku, then grant permission to this app")
                return
            }

            if (!ShizukuController.isPermissionGranted()) {
                updateNotification("Grant Shizuku permission to this app")
                return
            }

            if (hasExternalDisplay) {
                if (!AppPreferences.hasRestoreValue(this)) {
                    ShizukuController.getShowTaps(this) { current ->
                        if (current != null && !AppPreferences.hasRestoreValue(this)) {
                            AppPreferences.saveRestoreValue(this, current)
                        }
                        applyShowTaps(
                            enabled = true,
                            successMessage = "Display connected: $displaySummary. Show taps enabled",
                            failureMessage = "Display connected: $displaySummary, but Shizuku could not write show_touches",
                        )
                    }
                    return
                }
                applyShowTaps(
                    enabled = true,
                    successMessage = "Display connected: $displaySummary. Show taps enabled",
                    failureMessage = "Display connected: $displaySummary, but Shizuku could not write show_touches",
                )
            } else {
                maybeRestoreSetting()
                updateNotification("No external display: Show taps restored")
            }
        } catch (e: Exception) {
            updateNotification("Monitor error: ${e.javaClass.simpleName}")
        }
    }

    private fun maybeRestoreSetting() {
        if (shuttingDown && !ShizukuController.isPermissionGranted()) {
            return
        }
        if (AppPreferences.hasRestoreValue(this)) {
            val restoreValue = AppPreferences.getRestoreValue(this)
            ShizukuController.setShowTaps(this, restoreValue) { changed ->
                if (changed) {
                    AppPreferences.clearRestoreValue(this)
                }
            }
        }
    }

    private fun applyShowTaps(
        enabled: Boolean,
        successMessage: String,
        failureMessage: String,
    ) {
        ShizukuController.setShowTaps(this, enabled) { changed ->
            if (changed) {
                updateNotification(successMessage)
            } else {
                updateNotification(failureMessage)
            }
        }
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "xreal_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "com.example.xrealtaptoggle.START"
        private const val ACTION_STOP = "com.example.xrealtaptoggle.STOP"

        fun createStartIntent(context: Context) =
            Intent(context, XrealMonitorService::class.java).setAction(ACTION_START)

        fun createStopIntent(context: Context) =
            Intent(context, XrealMonitorService::class.java).setAction(ACTION_STOP)
    }
}
