package pro.pushpro.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import pro.pushpro.app.R
import pro.pushpro.app.ui.access.AccessActivity
import pro.pushpro.app.ui.logs.LogViewerActivity
import pro.pushpro.app.ui.queue.OfflineQueueActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_menu)

        findViewById<View>(R.id.btnOpenAccess).setOnClickListener { startActivity(Intent(this, AccessActivity::class.java)) }
        findViewById<View>(R.id.btnWebhook).setOnClickListener { startActivity(Intent(this, WebhookSettingsActivity::class.java)) }
        findViewById<View>(R.id.btnEmail).setOnClickListener { startActivity(Intent(this, EmailSettingsActivity::class.java)) }
        findViewById<View>(R.id.btnTelegram).setOnClickListener { startActivity(Intent(this, TelegramSettingsActivity::class.java)) }
        findViewById<View>(R.id.btnLogViewer).setOnClickListener { startActivity(Intent(this, LogViewerActivity::class.java)) }
        findViewById<View>(R.id.btnOfflineQueue).setOnClickListener { startActivity(Intent(this, OfflineQueueActivity::class.java)) }
    }
}
