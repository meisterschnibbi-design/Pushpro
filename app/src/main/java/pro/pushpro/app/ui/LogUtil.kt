package com.pushpro.app.util

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtil {
    private const val PREFS_NAME = "pushpro_prefs"
    private const val KEY_LOGS = "logs"

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun append(context: Context, message: String) {
        val ts = sdf.format(Date())
        val line = "[$ts] $message"

        // Android Logcat
        Log.d("PushPro", line)

        // Save to SharedPreferences (persistent log storage)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_LOGS, "") ?: ""
        val updated = if (current.isEmpty()) line else current + "\n" + line
        prefs.edit().putString(KEY_LOGS, updated).apply()
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_LOGS).apply()
    }

    fun getAll(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LOGS, "") ?: ""
    }
}
