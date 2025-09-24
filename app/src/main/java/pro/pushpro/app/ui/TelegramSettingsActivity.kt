package pro.pushpro.app.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import pro.pushpro.app.R
import pro.pushpro.app.util.LogUtil
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class TelegramSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram_settings)

        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        val token = findViewById<EditText>(R.id.inputToken)
        val chats = findViewById<EditText>(R.id.inputChatIds)
        val wl   = findViewById<EditText?>(R.id.inputWhitelist)
        val filt = findViewById<EditText?>(R.id.inputContains)

        token.setText(prefs.getString("tg_token", "") ?: "")
        chats.setText(prefs.getString("tg_chat", "") ?: "")

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val NO = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            token.importantForAutofill = NO
            chats.importantForAutofill = NO
        }
        token.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            prefs.edit()
                .putString("tg_token", token.text.toString().trim())
                .putString("tg_chat", chats.text.toString().trim())
                .putString("whitelist", wl?.text?.toString()?.trim() ?: (prefs.getString("whitelist","") ?: ""))
                .putString("contains_telegram", filt?.text?.toString()?.trim() ?: "")
                .apply()
            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSendTestTelegram).setOnClickListener {
            Thread {
                val t = token.text.toString().trim()
                val list = chats.text.toString().trim().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                var ok = false
                try {
                    for (cid in list) {
                        val msg = URLEncoder.encode("PushPro Telegram Test", "UTF-8")
                        val u = URL("https://api.telegram.org/bot" + t + "/sendMessage?chat_id=" + cid + "&text=" + msg)
                        val c = (u.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 8000; readTimeout = 8000; requestMethod = "GET"
                        }
                        val code = c.responseCode; c.disconnect()
                        if (code in 200..299) { ok = true } else { ok = false; break }
                    }
                } catch (_: Exception) { ok = false }
                LogUtil.append(this, if (ok) "Telegram test success" else "Telegram test fail")
                runOnUiThread { Toast.makeText(this, if (ok) "OK" else "Failed", Toast.LENGTH_SHORT).show() }
            }.start()
        }
    }
}