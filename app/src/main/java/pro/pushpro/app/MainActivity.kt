package pro.pushpro.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import pro.pushpro.app.ui.EmailSettingsActivity
import pro.pushpro.app.ui.SettingsActivity
import pro.pushpro.app.ui.TelegramSettingsActivity
import pro.pushpro.app.ui.WebhookSettingsActivity
import pro.pushpro.app.util.LogUtil

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        val lamp = findViewById<ImageView>(R.id.imgLamp)
        val statusText = findViewById<TextView>(R.id.txtStatus)
        val sw = findViewById<Switch>(R.id.switchGlobal)

        // Primary navigation buttons (ids as defined in activity_main.xml)
        findViewById<Button?>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button?>(R.id.btnWebhook)?.setOnClickListener {
            startActivity(Intent(this, WebhookSettingsActivity::class.java))
        }
        findViewById<Button?>(R.id.btnEmail)?.setOnClickListener {
            startActivity(Intent(this, EmailSettingsActivity::class.java))
        }
        findViewById<Button?>(R.id.btnTelegram)?.setOnClickListener {
            startActivity(Intent(this, TelegramSettingsActivity::class.java))
        }

        fun anyChannelConfigured(): Boolean {
            val emailConfigured = !prefs.getString("email_host","")!!.isBlank()
                    && !prefs.getString("email_user","")!!.isBlank()
                    && !prefs.getString("email_pass","")!!.isBlank()
                    && !prefs.getString("email_recipient","")!!.isBlank()
            val tgConfigured = !prefs.getString("tg_token","")!!.isBlank()
                    && !prefs.getString("tg_chat","")!!.isBlank()
            val whConfigured = !prefs.getString("wh_url","")!!.isBlank()
            return emailConfigured || tgConfigured || whConfigured
        }

        fun refreshLamp() {
            val on = prefs.getBoolean("global_on", false)
            val any = anyChannelConfigured()
            val color = when {
                !on       -> R.color.status_off     // Rot
                on && !any -> R.color.status_warn   // Orange
                else      -> R.color.status_on      // Grün
            }
            lamp.setColorFilter(ContextCompat.getColor(this, color))
            statusText.text = when {
                !on       -> getString(R.string.status_disabled)
                on && !any -> getString(R.string.status_no_channel)
                else      -> getString(R.string.status_enabled)
            }
            sw.isChecked = on
        }

        refreshLamp()

        sw.setOnCheckedChangeListener { _, isChecked ->
            val prev = prefs.getBoolean("global_on", false)
            prefs.edit().putBoolean("global_on", isChecked).apply()
            if (prev != isChecked) LogUtil.append(this, if (isChecked) "Global ON" else "Global OFF")
            refreshLamp()
        }
    }
}
