
package pro.pushpro.app.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import pro.pushpro.app.R
import pro.pushpro.app.util.applyStatusBarInset
import pro.pushpro.app.util.setSystemBars
import pro.pushpro.app.util.LogUtil
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class TelegramSettingsActivity : AppCompatActivity() {

    private val parseModes = arrayOf("None","MarkdownV2","HTML")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram_settings)
        applyStatusBarInset(findViewById(R.id.rootTelegram))
        setSystemBars(this, ContextCompat.getColor(this, android.R.color.black), ContextCompat.getColor(this, android.R.color.black))

        val title = findViewById<TextView>(R.id.title)
        val enable = findViewById<Switch>(R.id.switchEnabled)
        val inputToken = findViewById<EditText>(R.id.inputToken)
        val inputChatIds = findViewById<EditText>(R.id.inputChatIds)
        val spinner = findViewById<Spinner>(R.id.spinnerParse)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnTest = findViewById<Button>(R.id.btnSendTestTelegram)

        // token sichtbar, Autofill unterdrücken
        inputToken.transformationMethod = null
        inputToken.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            inputToken.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }

        // Hints
        inputChatIds.hint = "Chat ID(s), comma-separated (numbers only)"

        title.text = getString(R.string.telegram_title)

        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, parseModes)

        val p = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        enable.isChecked = p.getBoolean("telegram_enable", false)
        inputToken.setText(p.getString("telegram_token", ""))
        inputChatIds.setText(p.getString("telegram_chat_ids", ""))
        val pmIdx = p.getInt("telegram_parse_mode_idx", 0)
        spinner.setSelection(if (pmIdx in parseModes.indices) pmIdx else 0)

        btnSave.setOnClickListener {
            val idx = spinner.selectedItemPosition
            getSharedPreferences("pushpro_prefs", MODE_PRIVATE).edit()
                .putBoolean("telegram_enable", enable.isChecked)
                .putString("telegram_token", inputToken.text.toString())
                .putString("telegram_chat_ids", inputChatIds.text.toString())
                .putInt("telegram_parse_mode_idx", idx)
                .apply()
            Toast.makeText(this, R.string.save, Toast.LENGTH_SHORT).show()
            finish()
        }

        btnTest.setOnClickListener {
            Thread {
                val tokenStr = inputToken.text.toString().trim()
                val idsStr = inputChatIds.text.toString().trim()
                val firstId = idsStr.split(",").map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: ""
                var ok = false
                var httpCode = -1
                var body = ""

                if (tokenStr.isNotEmpty() && firstId.isNotEmpty()) {
                    try {
                        val urlStr = "https://api.telegram.org/bot" + tokenStr +
                            "/sendMessage?chat_id=" + firstId +
                            "&text=" + URLEncoder.encode("Test from PushPro", "UTF-8")
                        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 10000
                            readTimeout = 15000
                        }
                        httpCode = conn.responseCode
                        val stream = if (httpCode in 200..299) conn.inputStream else conn.errorStream
                        body = stream?.bufferedReader()?.readText() ?: ""
                        conn.disconnect()
                        ok = body.contains("\"ok\":true")
                        LogUtil.append(this, "Telegram test HTTP $httpCode body=" + body.take(300).replace("\n"," "))
                    } catch (e: Exception) {
                        body = e.javaClass.simpleName + ": " + (e.message ?: "")
                        LogUtil.append(this, "Telegram test exception: $body")
                        ok = false
                    }
                } else {
                    LogUtil.append(this, "Telegram test failed: missing token or chat_id")
                }

                getSharedPreferences("pushpro_prefs", MODE_PRIVATE).edit()
                    .putBoolean("last_send_error_telegram", !ok)
                    .apply()

                if (!ok) {
                    val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
                    val prevQ = prefs.getString("queue","") ?: ""
                    val whenTs = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                    val reason = if (httpCode > 0) ("HTTP " + httpCode) else body
                    val entry = "{\"type\":\"telegram\",\"when\":\"" + whenTs + "\",\"reason\":\"" + reason.replace("\"","'") + "\",\"payload\":\"sendMessage\"}"
                    prefs.edit().putString("queue", prevQ + entry + "\n").apply()
                }

                runOnUiThread {
                    Toast.makeText(this, if (ok) "Telegram test OK" else "Telegram test failed", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }
}
