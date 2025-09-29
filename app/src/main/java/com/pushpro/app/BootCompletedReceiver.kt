package com.pushpro.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pushpro.app.service.ForegroundKeeperService
import com.pushpro.app.service.NotificationRelayService
import com.pushpro.app.util.LogUtil

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            ForegroundKeeperService.start(context)
            NotificationRelayService.ensureBound(context)
            LogUtil.append(context, "BootCompleted: started keeper + rebind listener")
        } catch (_: Throwable) {}
    }
}
