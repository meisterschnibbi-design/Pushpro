package com.pushpro.app.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import com.pushpro.app.net.Sender
import com.pushpro.app.util.LogUtil

class NotificationRelayService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            if (sbn == null) return
            val n = sbn.notification ?: return
            val extras = n.extras
            val title = (extras?.getCharSequence(Notification.EXTRA_TITLE) ?: "").toString()
            val text = (extras?.getCharSequence(Notification.EXTRA_TEXT) ?: "").toString()
            val pkg = sbn.packageName ?: ""

            if (pkg == packageName) return

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
        // Einige OEMs trennen willkürlich – Toggle erzwingt Rebind
        ensureBound(this)
        LogUtil.append(this, "Notification listener disconnected – rebind issued")
    }

    companion object {
        fun ensureBound(ctx: Context) {
            try {
                val cn = ComponentName(ctx, NotificationRelayService::class.java)
                val pm = ctx.packageManager
                pm.setComponentEnabledSetting(cn,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(cn,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP)
            } catch (_: Throwable) {}
        }
    }
}
