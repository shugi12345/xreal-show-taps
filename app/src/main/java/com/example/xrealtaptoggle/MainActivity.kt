package com.example.xrealtaptoggle

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var statusView: TextView
    private lateinit var permissionView: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var settingsButton: Button
    private var pendingStartAfterPermission = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshUi()
        }

    private val shizukuPermissionListener =
        rikka.shizuku.Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != ShizukuController.requestCode()) {
                return@OnRequestPermissionResultListener
            }

            val granted = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted && pendingStartAfterPermission) {
                pendingStartAfterPermission = false
                startMonitoring()
            } else {
                pendingStartAfterPermission = false
                refreshUi()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ShizukuController.addPermissionListener(shizukuPermissionListener)

        statusView = findViewById(R.id.statusView)
        permissionView = findViewById(R.id.permissionView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        settingsButton = findViewById(R.id.settingsButton)

        startButton.setOnClickListener {
            requestNotificationPermissionIfNeeded()
            if (ShizukuController.isPermissionGranted()) {
                startMonitoring()
            } else {
                pendingStartAfterPermission = true
                if (!ShizukuController.requestPermission()) {
                    refreshUi()
                }
            }
        }

        stopButton.setOnClickListener {
            AppPreferences.setMonitoringEnabled(this, false)
            stopService(XrealMonitorService.createStopIntent(this))
            refreshUi()
        }

        settingsButton.setOnClickListener {
            ShizukuController.openShizukuOrDownload(this)
        }

        refreshUi()
    }

    override fun onDestroy() {
        ShizukuController.removePermissionListener(shizukuPermissionListener)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        try {
            val monitoringEnabled = AppPreferences.isMonitoringEnabled(this)
            val connectedDisplays = safeDisplaySummary()
            val shizukuState = when {
                !ShizukuController.isAvailable() -> getString(R.string.shizuku_not_running)
                ShizukuController.isPermissionGranted() -> getString(R.string.shizuku_ready)
                else -> getString(R.string.shizuku_permission_needed)
            }

            statusView.text = getString(
                R.string.status_template,
                if (monitoringEnabled) getString(R.string.enabled) else getString(R.string.disabled),
                shizukuState,
            ) + "\nExternal displays: $connectedDisplays"
            permissionView.text = getString(
                R.string.permission_template,
                shizukuState,
            )

            startButton.isEnabled = !monitoringEnabled
            stopButton.isEnabled = monitoringEnabled
        } catch (_: Exception) {
            statusView.text = "Status unavailable"
            permissionView.text = "Display check failed"
        }
    }

    private fun startMonitoring() {
        AppPreferences.setMonitoringEnabled(this, true)
        ContextCompat.startForegroundService(this, XrealMonitorService.createStartIntent(this))
        refreshUi()
    }

    private fun safeDisplaySummary(): String = try {
        val displayManager = getSystemService(android.hardware.display.DisplayManager::class.java)
        if (displayManager == null) {
            "Unknown"
        } else {
            displayManager.displays
                .filter { it.displayId != android.view.Display.DEFAULT_DISPLAY }
                .joinToString { "${it.displayId}" }
                .ifBlank { "None" }
        }
    } catch (_: Exception) {
        "Unknown"
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
