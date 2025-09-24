package pro.pushpro.app.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtil {
    private const val PREF = "pushpro_prefs"
    private const val KEY = "logs"
    private const val MAX = 240

    private fun sanitize(msg: String): String {
        var s = msg.replace("\n", " ").replace("\r", " ").replace("\t", " ")
        s = Regex("""\s+""").replace(s, " ").trim()
        // Kein JSON/Mehrzeiler-Spam
        s = Regex("""[{}\[\]]""").replace(s, "")
        if (s.length > MAX) s = s.substring(0, MAX)
        return s
    }

    fun append(ctx: Context, message: String) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val line = "[$ts] ${sanitize(message)}"
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val old = p.getString(KEY, "") ?: ""
        p.edit().putString(KEY, if (old.isEmpty()) "$line\n" else old + line + "\n").apply()
    }
}
