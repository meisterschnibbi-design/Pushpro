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

            // Blacklist
            if (isBlockedPackage(pkg)) {
                LogUtil.append(this, "Blocked by blacklist: $pkg")
                return
            }

            // *** NEU: Stabile De-Dup-Prüfung (pkg|id|tag, postTime aufsteigend) ***
            val stableKey = makeStableKey(sbn, pkg)
            if (isReplayOrDuplicate(stableKey, sbn.postTime)) {
                LogUtil.append(this, "Suppressed duplicate/replay: $pkg")
                return
            }
            // *********************************************************************

            // 1) Group Summary ignorieren
            if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

            // 2) Ongoing/Foreground-Service ignorieren
            if ((n.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
                (n.flags and 0x00000040) != 0 // FLAG_FOREGROUND_SERVICE (nicht immer öffentlich)
            ) return

            // 3) Channel-Importance prüfen: nur sichtbare Notifications
            try {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = n.channelId?.let { nm.getNotificationChannel(it) }
                val imp = channel?.importance ?: NotificationManager.IMPORTANCE_DEFAULT
                if (imp < NotificationManager.IMPORTANCE_DEFAULT) return
            } catch (_: Throwable) {
                // kein Channel → Default
            }

            // 4) Titel/Text robust extrahieren
            val (title, text) = extractTitleText(n.extras)

            // 5) (bestehendes) kurzes Debounce-Fenster
            val key = buildDedupKey(sbn, pkg, title, text)
            if (isDuplicate(key)) return

            // Stable-Key als "gesendet" markieren (mit dieser postTime)
            markSent(stableKey, sbn.postTime)

            LogUtil.append(this, "Got push: pkg=$pkg; title=$title")
            Sender.forward(this, title, text, pkg)
        } catch (e: Throwable) {
            LogUtil.append(this, "Relay error: ${e.message}")
        }
    }

    override fun onListenerConnected() {
        connectedAt = System.currentTimeMillis()   // (bleibt bestehen; hat keine Filterwirkung mehr)
        LogUtil.append(this, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        ensureBound(this) // nur hier bei echtem Disconnect neu binden
        LogUtil.append(this, "Notification listener disconnected – rebind issued")
    }

    // --- Hilfen ---

    private fun normalizePkg(pkg: String): String {
        // Fix: Backslashes im Regex doppelt escapen
        return pkg.replace(Regex("\\.clone(\\d+)\\.clone\\1$"), ".clone$1")
    }

    private fun isBlockedPackage(rawPkg: String): Boolean {
        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        val blocked = prefs.getStringSet("blacklist_set", emptySet()) ?: emptySet()
        if (blocked.isEmpty()) return false
        val norm = normalizePkg(rawPkg)
        return blocked.any { rule ->
            when {
                rule == rawPkg || rule == norm -> true
                rule.endsWith(".*") -> {
                    val pref = rule.removeSuffix(".*")
                    rawPkg.startsWith(pref) || norm.startsWith(pref)
                }
                else -> false
            }
        }
    }

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

    // --- NEU: stabile De-Dup-Hilfen ---
    private fun makeStableKey(sbn: StatusBarNotification, pkg: String): String {
        val tag = sbn.tag ?: ""
        return "$pkg|${sbn.id}|$tag"
    }

    private fun isReplayOrDuplicate(stableKey: String, postTime: Long): Boolean {
        synchronized(lastPostTimes) {
            val last = lastPostTimes[stableKey]
            return last != null && postTime <= last
        }
    }

    private fun markSent(stableKey: String, postTime: Long) {
        synchronized(lastPostTimes) {
            lastPostTimes[stableKey] = postTime
            // Housekeeping: Größe begrenzen
            if (lastPostTimes.size > 2048) {
                // einfache Ausdünnung (kein LRU nötig hier)
                val it = lastPostTimes.entries.iterator()
                repeat(256) { if (it.hasNext()) it.next(); if (it.hasNext()) it.remove() }
            }
        }
    }
    // -------------------------------

    companion object {
        private const val WINDOW_MS = 2500L
        @Volatile private var connectedAt: Long = 0L  // bleibt, wird aber nicht mehr zum Filtern benutzt

        private val recent = Collections.synchronizedMap(
            object : LinkedHashMap<String, Long>(256, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
                    return this.size > 256
                }
            }
        )

        // NEU: merkt letzte weitergeleitete postTime pro (pkg|id|tag)
        private val lastPostTimes = HashMap<String, Long>(512)

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
