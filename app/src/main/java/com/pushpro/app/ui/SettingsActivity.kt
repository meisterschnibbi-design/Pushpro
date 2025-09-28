package com.pushpro.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        // FULL RESET ONLY HERE (no per-screen resets)
        findViewById<MaterialButton>(R.id.btnResetSettings).setOnClickListener {
            // 1) Clear both stores
            getSharedPreferences("pushpro_prefs", MODE_PRIVATE).edit().clear().apply()
            getSharedPreferences("prefs", MODE_PRIVATE).edit().clear().apply()

            // 2) Restore defaults/templates in prefs
            val tplJson  = """{ "title": "{title}", "text": "{text}", "package": "{package}", "time": "{time}" }"""
            val tplForm  = """title={title}&text={text}&package={package}&time={time}"""
            val tplPlain = "{title}\n{text}\n{package}\n{time}"
            val tplXml   = """<push><title>{title}</title><text>{text}</text><package>{package}</package><time>{time}</time></push>"""
            val tgTpl    = "{title}\n{text}\n{package}\n{time}"

            getSharedPreferences("prefs", MODE_PRIVATE).edit()
                // Webhook defaults
                .putString("wh_tpl_json",  tplJson)
                .putString("wh_tpl_form",  tplForm)
                .putString("wh_tpl_plain", tplPlain)
                .putString("wh_tpl_xml",   tplXml)
                .putInt("wh_method", 1)     // POST
                .putInt("wh_template", 0)   // json
                .putString("wh_contains", "")
                .putString("wh_whitelist", "")
                // Telegram defaults
                .putString("tg_tpl", tgTpl)
                .putInt("tg_parse_mode", 0) // None
                .putString("tg_contains", "")
                .putString("tg_whitelist", "")
                // Channel enable flags -> OFF
                .putBoolean("wh_enabled", false)
                .putBoolean("tg_enabled", false)
                .putBoolean("email_enabled", false)
                .apply()

            LogUtil.append(this, "Settings reset to defaults (templates & modes restored)")
            Toast.makeText(this, "All settings & templates reset", Toast.LENGTH_SHORT).show()
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
