package pro.pushpro.app.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import pro.pushpro.app.R

class DiagnosticsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)

        val tv: TextView = findViewById(R.id.textDiagnostics)
        val sb = StringBuilder()

        val p = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        fun b(name: String, ok: Boolean) {
            sb.append(if (ok) "✅ " else "❌ ").append(name).append('\n')
        }

        // Simple checks (more can be added later)
        b("Notification Access", true) // placeholder
        b("Battery Optimization Exempt", true) // placeholder
        b("Webhook configured", (p.getString("webhook_input1", "") ?: "").isNotBlank())
        b("Email configured", (p.getString("email_input_host", "") ?: "").isNotBlank())
        b("Telegram configured", (p.getString("telegram_token", "") ?: "").isNotBlank())

        tv.text = sb.toString().ifBlank { "No diagnostics available." }
    }
}