package com.pushpro.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R
import com.pushpro.app.ui.EmailSettingsActivity
import com.pushpro.app.ui.SettingsActivity
import com.pushpro.app.ui.TelegramSettingsActivity
import com.pushpro.app.ui.WebhookSettingsActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnWebhook: Button   = findViewById(R.id.btnWebhookSettings)
        val btnEmail: Button     = findViewById(R.id.btnEmailSettings)
        val btnTelegram: Button  = findViewById(R.id.btnTelegramSettings)
        val btnSettings: Button  = findViewById(R.id.btnSettings)

        val switchEnable: Switch = findViewById(R.id.switchEnable)
        val txtStatus: TextView  = findViewById(R.id.txtStatus)

        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("global_enabled", false)
        switchEnable.isChecked = isEnabled
        txtStatus.text = if (isEnabled) getString(R.string.status_enabled) else getString(R.string.status_disabled)

        switchEnable.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("global_enabled", checked).apply()
            txtStatus.text = if (checked) getString(R.string.status_enabled) else getString(R.string.status_disabled)
        }

        btnWebhook.setOnClickListener { startActivity(Intent(this, WebhookSettingsActivity::class.java)) }
        btnEmail.setOnClickListener { startActivity(Intent(this, EmailSettingsActivity::class.java)) }
        btnTelegram.setOnClickListener { startActivity(Intent(this, TelegramSettingsActivity::class.java)) }
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        ensureNotificationAccess()
    }

    private fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.contains(packageName)
    }

    private fun ensureNotificationAccess() {
        if (!hasNotificationAccess()) {
            AlertDialog.Builder(this)
                .setTitle("Allow notification access")
                .setMessage("PushPro needs notification access to read incoming pushes.")
                .setPositiveButton("Open settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
}
