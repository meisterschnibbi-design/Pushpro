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

        // --- Open Access (nur diese Stelle geändert) ---
        findViewById<MaterialButton>(R.id.btnOpenAccess).setOnClickListener {
            // 1) Seite "Benachrichtigungszugriff" (Notification Listener)
            try {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            } catch (_: Exception) { /* ignore */ }

            // 2) App-Benachrichtigungseinstellungen mit vollen Extras (MIUI/Hersteller)
            try {
                val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    putExtra("android.provider.extra.APP_PACKAGE", packageName)
                    putExtra("app_package", packageName)
                    putExtra("app_uid", applicationInfo?.uid ?: 0)
                }
                startActivity(i)
            } catch (_: Exception) {
                // 3) Fallback: App-Detailseite
                try {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    })
                } catch (_: Exception) { /* ignore */ }
            }
        }
        // --- Ende der Änderung ---

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
        fun escape(s: String) = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        fun mapToJson(m: Map<String, *>): String {
            val sb = StringBuilder().append("{")
            val it = m.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                val value: String = when (val v = e.value) {
                    is Set<*> -> "[" + v.filterNotNull().joinToString(",") { "\"${escape(it.toString())}\"" } + "]"
                    else -> "\"${escape(v?.toString() ?: "")}\""
                }
                sb.append("\"").append(escape(e.key)).append("\": ").append(value)
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
            val key = "\"$section\""
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

            // Split by commas at top level (naiv, reicht für unsere flache Map)
            val out = mutableMapOf<String, String>()
            var i = 0
            var startItem = 0
            var inQuotes = false
            while (i < body.length) {
                val c = body[i]
                if (c == '"') inQuotes = !inQuotes
                if (!inQuotes && (c == ',')) {
                    val pair = body.substring(startItem, i)
                    val idx = pair.indexOf(':')
                    if (idx > 0) {
                        val k = pair.substring(0, idx).trim().trim('"')
                        val v = pair.substring(idx + 1).trim()
                            .trim() // keep quotes or brackets for type detection
                            .trim()
                        out[k] = v.trim()
                    }
                    startItem = i + 1
                }
                i++
            }
            // last item
            val last = body.substring(startItem).trim()
            if (last.isNotEmpty()) {
                val idx = last.indexOf(':')
                if (idx > 0) {
                    val k = last.substring(0, idx).trim().trim('"')
                    val v = last.substring(idx + 1).trim()
                    out[k] = v.trim()
                }
            }
            return out
        }

        val p1 = extract("prefs")
        val p2 = extract("pushpro_prefs")

        fun EditorPutTyped(editor: android.content.SharedPreferences.Editor, k: String, raw: String) {
            val v = raw.trim()

            // StringSet: ["a","b",...]
            if (v.startsWith("[") && v.endsWith("]")) {
                val inner = v.substring(1, v.length - 1).trim()
                val set = if (inner.isEmpty()) {
                    emptySet<String>()
                } else {
                    // split top-level by comma, remove quotes
                    inner.split(',').map { it.trim().trim('"') }.toSet()
                }
                editor.putStringSet(k, set)
                return
            }

            // Boolean
            if (v.equals("\"true\"", true) || v.equals("true", true)) {
                editor.putBoolean(k, true); return
            }
            if (v.equals("\"false\"", true) || v.equals("false", true)) {
                editor.putBoolean(k, false); return
            }

            // Int
            val intClean = v.trim('"')
            if (intClean.matches(Regex("^-?\\d+$"))) {
                runCatching { intClean.toInt() }.onSuccess { editor.putInt(k, it); return }
            }

            // Float
            if (intClean.matches(Regex("^-?\\d+\\.\\d+$"))) {
                runCatching { intClean.toFloat() }.onSuccess { editor.putFloat(k, it); return }
            }

            // Fallback String (strip surrounding quotes if present)
            editor.putString(k, intClean)
        }

        val sp1 = getSharedPreferences("prefs", MODE_PRIVATE).edit()
        for ((k, v) in p1) EditorPutTyped(sp1, k, v)
        sp1.apply()

        val sp2 = getSharedPreferences("pushpro_prefs", MODE_PRIVATE).edit()
        for ((k, v) in p2) EditorPutTyped(sp2, k, v)
        sp2.apply()
    }
}
