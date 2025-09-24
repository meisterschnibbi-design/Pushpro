package pro.pushpro.app.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import pro.pushpro.app.R
import pro.pushpro.app.util.LogUtil
import java.net.HttpURLConnection
import java.net.URL

class WebhookSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webhook_settings)

        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        val urlInput = findViewById<EditText>(R.id.inputUrl)
        val wl   = findViewById<EditText?>(R.id.inputWhitelist)   // optional im Layout
        val filt = findViewById<EditText?>(R.id.inputContains)    // optional im Layout
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnTest = findViewById<Button>(R.id.btnSendTest)

        urlInput.setText(prefs.getString("wh_url", "") ?: "")

        btnSave.setOnClickListener {
            prefs.edit()
                .putString("wh_url", urlInput.text.toString().trim())
                .putString("whitelist", wl?.text?.toString()?.trim() ?: (prefs.getString("whitelist","") ?: ""))
                .putString("contains_webhook", filt?.text?.toString()?.trim() ?: "")
                .apply()
            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
        }

        btnTest.setOnClickListener {
            Thread {
                var code = -1
                var ok = false
                try {
                    val u = URL(urlInput.text.toString().trim())
                    val conn = (u.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 8000; readTimeout = 8000
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        doOutput = true
                    }
                    val payload = prefs.getString("wh_body", "{"ok":true}") ?: "{"ok":true}"
                    conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    code = conn.responseCode; conn.disconnect()
                    ok = code in 200..299
                } catch (_: Exception) { ok = false }
                LogUtil.append(this, (if (ok) "Webhook test success: " else "Webhook test fail: ") + code.toString())
                runOnUiThread { Toast.makeText(this, if (ok) "OK" else "Failed", Toast.LENGTH_SHORT).show() }
            }.start()
        }
    }
}
