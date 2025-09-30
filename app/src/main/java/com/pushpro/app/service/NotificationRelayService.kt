package com.pushpro.app.service

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.pushpro.app.net.Sender
import com.pushpro.app.util.LogUtil
import java.util.Collections
import java.util.LinkedHashMap

class NotificationRelayService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            if (sbn == null) return
            val n = sbn.notification ?: return

            val pkg = sbn.packageName ?: ""
            if (pkg == packageName) return

            // 1) Group Summary ignorieren
            if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

            // 2) Ongoing/Foreground-Service ignorieren
            if ((n.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
                (n.flags and 0x00000040) != 0 // FLAG_FOREGROUND_SERVICE ist nicht immer öffentlich
            ) return

            // 3) Channel-Importance prüfen: nur sichtbare Notifications
            try {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = n.channelId?.let { nm.getNotificationChannel(it) }
                val imp = channel?.importance ?: NotificationManager.IMPORTANCE_DEFAULT
                if (imp < NotificationManager.IMPORTANCE_DEFAULT) return
            } catch (_: Throwable) {
                // falls kein Channel verfügbar → Default nehmen
            }

            // 4) Titel/Text robust extrahieren
            val (title, text) = extractTitleText(n.extras)

            // 5) Debounce
            val key = buildDedupKey(sbn, pkg, title, text)
            if (isDuplicate(key)) return

            LogUtil.append(this, "Got push: pkg=$pkg; title=$title")
            Sender.forward(this, title, text, pkg)
        } catch (e: Throwable) {
            LogUtil.append(this, "Relay error: ${e.message}")
        }
    }

    override fun onListenerConnected() {
        LogUtil.append(this, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        ensureBound(this)
        LogUtil.append(this, "Notification listener disconnected – rebind issued")
    }

    // --- Hilfen ---

    private fun extractTitleText(extras: Bundle?): Pair<String, String> {
        if (extras == null) return "" to ""
        val title = (extras.getCharSequence(Notification.EXTRA_TITLE) ?: "").toString()

        val big = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: "").toString()
        if (big.isNotBlank()) return title to big

        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (lines != null && lines.isNotEmpty()) {
            val joined = lines.joinToString("\n") { (it ?: "").toString() }.trim()
            if (joined.isNotBlank()) return title to joined
        }

        val text = (extras.getCharSequence(Notification.EXTRA_TEXT) ?: "").toString()
        if (text.isNotBlank()) return title to text

        val sub = (extras.getCharSequence(Notification.EXTRA_SUB_TEXT) ?: "").toString()
        return title to sub
    }

    private fun buildDedupKey(
        sbn: StatusBarNotification,
        pkg: String,
        title: String,
        text: String
    ): String {
        val shortText = if (text.length > 64) text.substring(0, 64) else text
        return "${sbn.key}|$pkg|$title|$shortText"
    }

    companion object {
        private const val WINDOW_MS = 2500L
        private val recent = Collections.synchronizedMap(
            object : LinkedHashMap<String, Long>(256, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                    return this.size > 256
                }
            }
        )

        private fun isDuplicate(key: String): Boolean {
            val now = System.currentTimeMillis()
            synchronized(recent) {
                val last = recent[key]
                val it = recent.entries.iterator()
                while (it.hasNext()) {
                    val e = it.next()
                    if (now - e.value > WINDOW_MS * 4) it.remove()
                }
                if (last != null && now - last < WINDOW_MS) return true
                recent[key] = now
            }
            return false
        }

        fun ensureBound(ctx: Context) {
            try {
                val cn = ComponentName(ctx, NotificationRelayService::class.java)
                val pm = ctx.packageManager
                pm.setComponentEnabledSetting(
                    cn,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    cn,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (_: Throwable) {
            }
        }
    }
}
