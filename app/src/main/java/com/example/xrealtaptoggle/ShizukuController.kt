package com.example.xrealtaptoggle

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import rikka.shizuku.Shizuku

object ShizukuController {
    private const val REQUEST_CODE = 1001
    private const val SERVICE_TAG = "show-taps-user-service"
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    private var service: IShowTapsService? = null
    private var connection: ServiceConnection? = null
    private val pending = mutableListOf<(IShowTapsService?) -> Unit>()

    fun addPermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.addRequestPermissionResultListener(listener)
    }

    fun removePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.removeRequestPermissionResultListener(listener)
    }

    fun requestCode(): Int = REQUEST_CODE

    fun isAvailable(): Boolean = try {
        !Shizuku.isPreV11() && Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun isPermissionGranted(): Boolean = try {
        isAvailable() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    fun requestPermission(): Boolean {
        if (!isAvailable()) {
            return false
        }
        if (isPermissionGranted()) {
            return true
        }
        return if (Shizuku.shouldShowRequestPermissionRationale()) {
            false
        } else {
            Shizuku.requestPermission(REQUEST_CODE)
            false
        }
    }

    fun openShizukuOrDownload(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        val intent = launchIntent ?: Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://shizuku.rikka.app/download/"),
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getShowTaps(context: Context, callback: (Boolean?) -> Unit) {
        withService(context) { remote: IShowTapsService? ->
            if (remote == null) {
                callback(null)
                return@withService
            }

            try {
                val value = remote.getShowTaps()
                callback(
                    when (value) {
                        1 -> true
                        0 -> false
                        else -> null
                    },
                )
            } catch (_: Throwable) {
                service = null
                callback(null)
            }
        }
    }

    fun setShowTaps(context: Context, enabled: Boolean, callback: (Boolean) -> Unit) {
        withService(context) { remote: IShowTapsService? ->
            if (remote == null) {
                callback(false)
                return@withService
            }

            try {
                callback(remote.setShowTaps(enabled))
            } catch (_: Throwable) {
                service = null
                callback(false)
            }
        }
    }

    private fun withService(context: Context, callback: (IShowTapsService?) -> Unit) {
        val existing = service
        if (existing != null) {
            callback(existing)
            return
        }

        if (!isPermissionGranted()) {
            callback(null)
            return
        }

        synchronized(this) {
            val current = service
            if (current != null) {
                callback(current)
                return
            }

            pending += callback
            if (connection != null) {
                return
            }

            val userServiceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val remote = IShowTapsService.Stub.asInterface(binder)
                    val callbacks = synchronized(this@ShizukuController) {
                        service = remote
                        connection = this
                        pending.toList().also { pending.clear() }
                    }
                    callbacks.forEach { pendingCallback -> pendingCallback(remote) }
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    synchronized(this@ShizukuController) {
                        service = null
                        connection = null
                    }
                }
            }

            connection = userServiceConnection
            Shizuku.bindUserService(userServiceArgs(context), userServiceConnection)
        }
    }

    private fun userServiceArgs(context: Context): Shizuku.UserServiceArgs {
        val component = ComponentName(context.packageName, ShizukuShowTapsService::class.java.name)
        return Shizuku.UserServiceArgs(component)
            .processNameSuffix("show_taps")
            .daemon(false)
            .debuggable(false)
            .version(1)
            .tag(SERVICE_TAG)
    }
}
