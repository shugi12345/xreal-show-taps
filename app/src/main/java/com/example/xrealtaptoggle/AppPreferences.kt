package com.example.xrealtaptoggle

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "xreal_tap_toggle"
    private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    private const val KEY_RESTORE_VALUE = "restore_value"
    private const val KEY_HAS_RESTORE_VALUE = "has_restore_value"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isMonitoringEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MONITORING_ENABLED, false)

    fun setMonitoringEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
    }

    fun saveRestoreValue(context: Context, value: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_RESTORE_VALUE, value)
            .putBoolean(KEY_HAS_RESTORE_VALUE, true)
            .apply()
    }

    fun hasRestoreValue(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HAS_RESTORE_VALUE, false)

    fun getRestoreValue(context: Context): Boolean =
        prefs(context).getBoolean(KEY_RESTORE_VALUE, false)

    fun clearRestoreValue(context: Context) {
        prefs(context).edit()
            .remove(KEY_RESTORE_VALUE)
            .putBoolean(KEY_HAS_RESTORE_VALUE, false)
            .apply()
    }
}
