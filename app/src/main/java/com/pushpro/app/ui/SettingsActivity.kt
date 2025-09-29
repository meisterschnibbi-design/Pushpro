
package com.pushpro.app.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.pushpro.R
import com.pushpro.app.util.LogUtil

class SettingsActivity : AppCompatActivity() {

    private val REQ_EXPORT = 1001
    private val REQ_IMPORT = 1002

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
            startActivity(Intent(this, DiagnosticsActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnResetSettings).setOnClickListener {
            val px = getSharedPreferences("prefs", MODE_PRIVATE)
            val pp = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
            px.edit()
                .clear()
                .apply()
            pp.edit()
                .putBoolean("global_enabled", false)
                .putBoolean("webhook_enabled", false)
                .putBoolean("email_enabled", false)
                .putBoolean("telegram_enabled", false)
                .apply()

            LogUtil.append(this, "Settings reset to defaults (templates & modes restored)")
            Toast.makeText(this, "All settings & templates reset", Toast.LENGTH_SHORT).show()
        }

        // New: Blacklist & Statistics
        findViewById<MaterialButton>(R.id.btnBlacklist)?.setOnClickListener {
            startActivity(Intent(this, BlacklistActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnStatistics)?.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }

        // Export via SAF
        findViewById<MaterialButton>(R.id.btnExportConfig).setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "pushpro-config.json")
            }
            startActivityForResult(intent, REQ_EXPORT)
        }

        // Import via SAF
        findViewById<MaterialButton>(R.id.btnImportConfig)?.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            startActivityForResult(intent, REQ_IMPORT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return
        val uri: Uri = data.data ?: return

        when (requestCode) {
            REQ_EXPORT -> {
                try {
                    val json = exportAllAsJson()
                    contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                    Toast.makeText(this, "Config exported", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Export failed: " + (e.message ?: "error"), Toast.LENGTH_LONG).show()
                }
            }
            REQ_IMPORT -> {
                try {
                    val json = contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                    importAllFromJson(json)
                    Toast.makeText(this, "Config imported", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Import failed: " + (e.message ?: "error"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun exportAllAsJson(): String {
        val p1 = getSharedPreferences("prefs", MODE_PRIVATE).all
        val p2 = getSharedPreferences("pushpro_prefs", MODE_PRIVATE).all
        fun mapToJson(m: Map<String, *>): String {
            val sb = StringBuilder().append("{")
            val it = m.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                sb.append("\"").append(e.key).append("\": \"").append((e.value)?.toString() ?: "").append("\"")
                if (it.hasNext()) sb.append(",")
            }
            sb.append("}")
            return sb.toString()
        }
        return "{\n\"prefs\": " + mapToJson(p1) + ", \n\"pushpro_prefs\": " + mapToJson(p2) + "\n}"
    }

    private fun importAllFromJson(json: String) {
        // very simple parser expecting {"prefs": {...}, "pushpro_prefs": {...}}
        fun extract(section: String): Map<String, String> {
            val key = "\"" + section + "\""
            val start = json.indexOf(key)
            if (start < 0) return emptyMap()
            val brace = json.indexOf('{', start)
            var depth = 0
            var end = -1
            for (i in brace until json.length) {
                if (json[i] == '{') depth += 1
                if (json[i] == '}') {
                    depth -= 1
                    if (depth == 0) { end = i; break }
                }
            }
            if (end < 0) return emptyMap()
            val body = json.substring(brace + 1, end)
            val out = mutableMapOf<String, String>()
            for (pair in body.split(',')) {
                val idx = pair.indexOf(':')
                if (idx > 0) {
                    val k = pair.substring(0, idx).trim().trim('"')
                    val v = pair.substring(idx + 1).trim().trim('"')
                    out[k] = v
                }
            }
            return out
        }
        val p1 = extract("prefs")
        val p2 = extract("pushpro_prefs")

        val sp1 = getSharedPreferences("prefs", MODE_PRIVATE).edit()
        for ((k, v) in p1) sp1.putString(k, v)
        sp1.apply()

        val sp2 = getSharedPreferences("pushpro_prefs", MODE_PRIVATE).edit()
        for ((k, v) in p2) sp2.putString(k, v)
        sp2.apply()
    }
}
