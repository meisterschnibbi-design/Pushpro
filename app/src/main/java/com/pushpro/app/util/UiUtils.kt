package com.pushpro.app.util

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Minimal safe helpers used across screens.
 * These are intentionally defensive no-ops on old APIs to avoid runtime crashes.
 */

fun setSystemBars(activity: Activity, statusColor: Int, navColor: Int = statusColor) {
    val window = activity.window
    setSystemBars(window, statusColor, navColor)
}

fun setSystemBars(window: Window, statusColor: Int, navColor: Int = statusColor) {
    try {
        window.statusBarColor = statusColor
        window.navigationBarColor = navColor
        if (Build.VERSION.SDK_INT >= 30) {
            val controller = window.insetsController
            controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    } catch (_: Throwable) { /* no-op */ }
}

fun View.applyStatusBarInset() {
    try {
        ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updatePadding(top = sys.top + v.paddingTop)
            WindowInsetsCompat.CONSUMED
        }
        requestApplyInsets()
    } catch (_: Throwable) { /* no-op */ }
}
