
package pro.pushpro.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONObject

class NotificationRelayService : NotificationListenerService() {
  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
    if (prefs.getBoolean("logs_enabled", true)) {
      val prev = prefs.getString("logs", "") ?: ""
      prefs.edit().putString("logs", prev + "Posted: " + (sbn?.packageName ?: "unknown") + "\n").apply()
    }
    // Example queue usage (disabled by default; you would add real failures):
    // if (prefs.getBoolean("queue_enabled", true)) {
    //   val obj = JSONObject().put("type","webhook").put("url","https://example.com").put("body","{}")
    //   val prevQ = prefs.getString("queue","") ?: ""
    //   prefs.edit().putString("queue", prevQ + obj.toString() + "\n").apply()
    // }
  }
  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
    if (prefs.getBoolean("logs_enabled", true)) {
      val prev = prefs.getString("logs", "") ?: ""
      prefs.edit().putString("logs", prev + "Removed: " + (sbn?.packageName ?: "unknown") + "\n").apply()
    }
  }
}
