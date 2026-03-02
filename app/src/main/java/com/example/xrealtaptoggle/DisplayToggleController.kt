package com.example.xrealtaptoggle

import android.content.Context
import android.provider.Settings

object DisplayToggleController {
    private const val SHOW_TOUCHES_KEY = "show_touches"

    fun readShowTaps(context: Context): Boolean = try {
        Settings.System.getInt(context.contentResolver, SHOW_TOUCHES_KEY, 0) == 1
    } catch (_: Exception) {
        false
    }

    fun setShowTaps(context: Context, enabled: Boolean): Boolean = try {
        Settings.System.putInt(context.contentResolver, SHOW_TOUCHES_KEY, if (enabled) 1 else 0)
    } catch (_: Exception) {
        false
    }
}
