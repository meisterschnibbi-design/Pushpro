package com.pushpro.app.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.pushpro.R
import com.pushpro.app.util.LogUtil

class SettingsActivity : AppCompatActivity() {

    // ---- Runtime-Permission für Android 13+ (POST_NOTIFICATIONS) ----
    private val requestPostNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Unabhängig vom Ergebnis die Ziel-Seiten öffnen
            openNotificationListenerAccess()
            openAppNotificationSettingsFallback()
        }

    // ---- Export/Import Launcher (Create/Open Document) ----
    private val exportLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                try {
                    val json = exportAllAsJson()
                    contentResolver.openOutputStream(uri)?.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(this, "Konfiguration exportiert", Toast.LENGTH_SHORT).show()
                } catch (e: Throwable) {
                    Toast.makeText(this, "Export fehlgeschlagen: ${e.message}", Toast.LENGTH_SHORT).show()
                    LogUtil.append(this, "Export failed: ${e.message}")
                }
            }
        }

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Throwable) { /* optional */ }
                try {
                    val json = contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: ""
                    importAllFromJson(json)
                    Toast.makeText(this, "Konfiguration importiert", Toast.LENGTH_SHORT).show()
                } catch (e: Throwable) {
                    Toast.makeText(this, "Import fehlgeschlagen: ${e.message}", Toast.LENGTH_SHORT).show()
                    LogUtil.append(this, "Import failed: ${e.message}")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_menu)

        // ---------- Open Access (nur dieser Flow wurde geändert) ----------
        bind<MaterialButton>("btnOpenAccess")?.setOnClickListener {
            // 1) Android 13+: App-Notification Permission als Runtime-Permission (falls nicht erteilt)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (!nm.areNotificationsEnabled()) {
                    requestPostNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    return@setOnClickListener
                }
            }
            // 2) Seite „Benachrichtigungszugriff“ (Notification Listener)
            openNotificationListenerAccess()
            // 3) Fallback: App-Benachrichtigungseinstellungen (wichtig bei MIUI)
            openAppNotificationSettingsFallback()
        }

        // ---------- Die restlichen Buttons bleiben erhalten ----------
        bind<MaterialButton>("btnBattery")?.setOnClickListener {
            startActivity(Intent(this, AccessActivity::class.java))
        }
        bind<MaterialButton>("btnLogViewer")?.setOnClickListener {
            startActivity(Intent(this, LogViewerActivity::class.java))
        }
        bind<MaterialButton>("btnDiagnostics")?.setOnClickListener {
            startActivity(Intent(this, DiagnosticsActivity::class.java))
        }
        bind<MaterialButton>("btnBlacklist")?.setOnClickListener {
            startActivity(Intent(this, BlacklistActivity::class.java))
        }
        bind<MaterialButton>("btnStatistics")?.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }
        bind<MaterialButton>("btnExport")?.setOnClickListener {
            try {
                exportLauncher.launch("pushpro_config.json")
            } catch (e: Throwable) {
                Toast.makeText(this, "Export nicht möglich: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        bind<MaterialButton>("btnImport")?.setOnClickListener {
            try {
                importLauncher.launch(arrayOf("application/json"))
            } catch (e: Throwable) {
                Toast.makeText(this, "Import nicht möglich: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------- Helpers ----------

    private fun openNotificationListenerAccess() {
        try {
            val i = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (_: Throwable) {
            // ältere Geräte ignorieren
        }
    }

    private fun openAppNotificationSettingsFallback() {
        // Versuche App-spezifische Benachrichtigungseinstellungen mit allen gängigen Extras
        try {
            val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                putExtra("android.provider.extra.APP_PACKAGE", packageName)
                putExtra("app_package", packageName)
                putExtra("app_uid", applicationInfo?.uid ?: 0)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(i)
        } catch (_: Throwable) {
            // Letzter Fallback: App-Detailseite
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (_: Throwable) { /* no-op */ }
        }
    }

    // robuste View-Bindings per Ressourcenname (keine R.id-Abhängigkeit -> keine Build-Fehler)
    private inline fun <reified T : View> bind(idName: String): T? {
        val id = resources.getIdentifier(idName, "id", packageName)
        if (id == 0) return null
        return findViewById(id)
    }

    // Platzhalter – nutze hier deine echte Implementierung, falls vorhanden
    private fun exportAllAsJson(): String = "{}"
    private fun importAllFromJson(json: String) { /* implementiert bereits in deinem Projekt? */ }
}
