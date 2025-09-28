
package com.pushpro.app.service

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
}
