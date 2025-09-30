package com.pushpro.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pushpro.app.service.ForegroundKeeperService
import com.pushpro.app.util.LogUtil

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        try {
            // Foreground-Keeper nach Boot starten (damit es immer läuft)
            val svc = Intent(context, ForegroundKeeperService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc)
            } else {
                context.startService(svc)
            }

            // WICHTIG: KEIN ensureBound() mehr hier
            LogUtil.append(context, "BootCompletedReceiver: keeper started")
        } catch (e: Throwable) {
            LogUtil.append(context, "BootCompletedReceiver error: ${e.message}")
        }
    }
}
