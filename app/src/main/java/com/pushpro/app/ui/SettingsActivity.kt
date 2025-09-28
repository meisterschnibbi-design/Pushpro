package com.pushpro.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.pushpro.R
import com.pushpro.app.util.LogUtil

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_menu)

        findViewById<MaterialButton>(R.id.btnOpenAccess).setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        findViewById<MaterialButton>(R.id.btnBatterySettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }

        findViewById<MaterialButton>(R.id.btnLogViewer).setOnClickListener {
            startActivity(Intent(this, LogViewerActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnDiagnostics).setOnClickListener {
            val p = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
            val checks = listOf(
                "global_enabled=" + p.getBoolean("global_enabled", false),
                "webhook_enabled=" + p.getBoolean("webhook_enabled", false),
                "email_enabled=" + (p.getBoolean("email_enabled", false) || p.getBoolean("email_input_enabled", false)),
                "telegram_enabled=" + p.getBoolean("telegram_enabled", false)
            )
            LogUtil.append(this, "Diagnostics: " + checks.joinToString(", "))
            Toast.makeText(this, "Diagnostics written to logs", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnResetSettings).setOnClickListener {
            val p = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
            p.edit().clear().apply()
            LogUtil.append(this, "Settings reset to defaults")
            Toast.makeText(this, "Settings reset", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnExportConfig).setOnClickListener {
            val p = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
            val all = p.all
            val sb = StringBuilder().append("{\n")
            val it = all.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                sb.append("  \"").append(e.key).append("\": \"").append((e.value)?.toString() ?: "").append("\"")
                if (it.hasNext()) sb.append(",")
                sb.append("\n")
            }
            sb.append("}")
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "PushPro Config")
                putExtra(Intent.EXTRA_TEXT, sb.toString())
            }
            startActivity(Intent.createChooser(share, "Export config"))
        }
    }
}
