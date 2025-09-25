package pro.pushpro.app.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtil {
    private const val PREF = "pushpro_prefs"
    private const val KEY = "logs"

    fun append(ctx: Context, message: String) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val line = "[$ts] $message"
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val old = p.getString(KEY, "") ?: ""
        p.edit().putString(KEY, if (old.isEmpty()) line + "\n" else old + line + "\n").apply()
    }
}