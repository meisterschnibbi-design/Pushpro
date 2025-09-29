package com.pushpro.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.pushpro.app.service.ForegroundKeeperService
import com.pushpro.app.service.NotificationRelayService

class KeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ForegroundKeeperService.start(context)
        NotificationRelayService.ensureBound(context)
        schedule(context)
    }

    companion object {
        fun schedule(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(ctx, KeepAliveReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                ctx, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerMs = SystemClock.elapsedRealtime() + 15 * 60 * 1000L
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerMs, pi)
        }
    }
}
