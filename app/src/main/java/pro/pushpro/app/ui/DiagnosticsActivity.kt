package com.pushpro.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R
import com.pushpro.app.util.LogUtil

class DiagnosticsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)

        val txtDiagnostics: TextView = findViewById(R.id.txtDiagnostics)
        val btnRunDiagnostics: Button = findViewById(R.id.btnRunDiagnostics)

        btnRunDiagnostics.setOnClickListener {
            val p = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)

            val checks = linkedMapOf(
                "global_enabled"   to p.getBoolean("global_enabled", false),
                "webhook_enabled"  to p.getBoolean("webhook_enabled", false),
                "email_enabled"    to (p.getBoolean("email_enabled", false) || p.getBoolean("email_input_enabled", false)),
                "telegram_enabled" to p.getBoolean("telegram_enabled", false),
                "queue_enabled"    to p.getBoolean("queue_enabled", false)
            )

            // Compose readable report
            val report = buildString {
                append(getString(R.string.diagnostics_running)).append('\n')
                checks.forEach { (k, v) -> append(k).append(" = ").append(v).append('\n') }
            }

            // Show in UI
            txtDiagnostics.text = report.trimEnd()

            // Persist to internal log
            LogUtil.append(this, "Diagnostics run → " + checks.entries.joinToString(", ") { "${it.key}=${it.value}" })
        }
    }
}
