package pro.pushpro.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import pro.pushpro.app.util.LogUtil

class NotificationRelayService : NotificationListenerService() {

    private fun patternsFrom(pref: String): List<Regex> {
        val raw = getSharedPreferences("pushpro_prefs", MODE_PRIVATE).getString(pref, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { token ->
            val t = token.trim()
            if (t.isEmpty()) null else {
                val escaped = Regex.escape(t).replace("\*", ".*")
                try { Regex("^$escaped$") } catch (_: Exception) { null }
            }
        }
    }

    private fun whitelisted(pkg: String): Boolean {
        val list = patternsFrom("whitelist")
        if (list.isEmpty()) return true
        return list.any { it.containsMatchIn(pkg) }
    }

    private fun containsFilterOk(channel: String, title: String, text: String): Boolean {
        val key = "contains_$channel"
        val needle = (getSharedPreferences("pushpro_prefs", MODE_PRIVATE).getString(key, "") ?: "").trim()
        if (needle.isEmpty()) return true
        val hay = (title + " " + text).lowercase()
        return hay.contains(needle.lowercase())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return
        val pkg = n.packageName ?: return
        val extras = n.notification?.extras
        val title = extras?.getCharSequence("android.title")?.toString() ?: ""
        val text  = extras?.getCharSequence("android.text") ?.toString() ?: ""

        if (!whitelisted(pkg)) {
            LogUtil.append(this, "Relay skip (whitelist): " + pkg)
            return
        }

        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        val globalOn = prefs.getBoolean("global_on", false)
        if (!globalOn) {
            LogUtil.append(this, "Relay blocked (global OFF)")
            return
        }

        val emailConfigured = !prefs.getString("email_host","")!!.isBlank()
                && !prefs.getString("email_user","")!!.isBlank()
                && !prefs.getString("email_pass","")!!.isBlank()
                && !prefs.getString("email_recipient","")!!.isBlank()
        val tgConfigured = !prefs.getString("tg_token","")!!.isBlank()
                && !prefs.getString("tg_chat","")!!.isBlank()
        val whConfigured = !prefs.getString("wh_url","")!!.isBlank()

        val sent = mutableListOf<String>()

        if (emailConfigured && containsFilterOk("email", title, text)) sent.add("email")
        if (tgConfigured    && containsFilterOk("telegram", title, text)) sent.add("telegram")
        if (whConfigured    && containsFilterOk("webhook", title, text)) sent.add("webhook")

        if (sent.isEmpty()) LogUtil.append(this, "Relay no-op: " + pkg)
        else LogUtil.append(this, "Relay OK (" + sent.joinToString("/") + "): " + pkg)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // kein Log-Spam
    }
}