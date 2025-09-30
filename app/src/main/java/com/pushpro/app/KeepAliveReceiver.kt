package com.pushpro.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pushpro.app.service.ForegroundKeeperService
import com.pushpro.app.util.LogUtil

class KeepAliveReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        try {
            // Nur den Foreground-Keeper anstupsen, damit die App wach bleibt
            val svc = Intent(context, ForegroundKeeperService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc)
            } else {
                context.startService(svc)
            }

            // WICHTIG: KEIN ensureBound() mehr hier
            LogUtil.append(context, "KeepAliveReceiver: keeper ping")
        } catch (e: Throwable) {
            LogUtil.append(context, "KeepAliveReceiver error: ${e.message}")
        }
    }
}
