
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
import pro.pushpro.app.util.applyStatusBarInset
import pro.pushpro.app.util.setSystemBars

class MainActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val imgLamp = findViewById<ImageView>(R.id.imgLamp)
        val txtStatus = findViewById<TextView>(R.id.txtStatus)
        val enabled = prefs.getBoolean("global_enabled", false)
        updateLamp(imgLamp, txtStatus, enabled, prefs)
    }

    private fun isAnyChannelConfigured(prefs: android.content.SharedPreferences): Boolean {
        val whEnabled = prefs.getBoolean("webhook_enabled", false)
        val whUrl = prefs.getString("webhook_input1", "") ?: ""
        if (whEnabled && whUrl.isNotBlank()) return true

        val emEnabled = prefs.getBoolean("email_enabled", false)
        val host = prefs.getString("email_input_host", "") ?: ""
        val port = prefs.getString("email_input_port", "") ?: ""
        val recip = prefs.getString("email_input_recipient", "") ?: ""
        if (emEnabled && host.isNotBlank() && port.isNotBlank() && recip.isNotBlank()) return true

        val tgEnabled = prefs.getBoolean("telegram_enabled", false)
        val token = prefs.getString("telegram_input1", "") ?: ""
        val chat = prefs.getString("telegram_input2", "") ?: ""
        if (tgEnabled && token.isNotBlank() && chat.isNotBlank()) return true

        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSystemBars(this,
            ContextCompat.getColor(this, R.color.status_bar_bg),
            ContextCompat.getColor(this, R.color.nav_bar_bg)
        )
        applyStatusBarInset(findViewById(R.id.rootMain))

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val switch = findViewById<Switch>(R.id.switchGlobal)
        val imgLamp = findViewById<ImageView>(R.id.imgLamp)
        val txtStatus = findViewById<TextView>(R.id.txtStatus)

        val enabled = prefs.getBoolean("global_enabled", false)
        switch.isChecked = enabled
        updateLamp(imgLamp, txtStatus, enabled, prefs)

        switch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("global_enabled", isChecked).apply()
            updateLamp(imgLamp, txtStatus, isChecked, prefs)
        }

        findViewById<Button>(R.id.btnWebhook).setOnClickListener { startActivity(Intent(this, WebhookSettingsActivity::class.java)) }
        findViewById<Button>(R.id.btnEmail).setOnClickListener { startActivity(Intent(this, EmailSettingsActivity::class.java)) }
        findViewById<Button>(R.id.btnTelegram).setOnClickListener { startActivity(Intent(this, TelegramSettingsActivity::class.java)) }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    
    private fun updateLamp(img: ImageView, txt: TextView, isOn: Boolean, prefs: android.content.SharedPreferences) {
        val hasWebhookErr = prefs.getBoolean("last_send_error_webhook", false)
        val hasEmailErr = prefs.getBoolean("last_send_error_email", false)
        val hasTelegramErr = prefs.getBoolean("last_send_error_telegram", false)

        val colorRes = when {
            !isOn -> R.color.status_off
            hasWebhookErr || hasEmailErr || hasTelegramErr -> R.color.status_warn
            isAnyChannelConfigured(prefs) -> R.color.status_on
            else -> R.color.status_warn
        }

        val lamp = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_lamp)!!.mutate()
        lamp.setTint(androidx.core.content.ContextCompat.getColor(this, colorRes))
        img.setImageDrawable(lamp)

        txt.text = when {
            !isOn -> getString(R.string.status_disabled)
            hasWebhookErr -> getString(R.string.status_error_webhook)
            hasEmailErr -> getString(R.string.status_error_email)
            hasTelegramErr -> getString(R.string.status_error_telegram)
            else -> getString(R.string.status_enabled)
        }
    }


}
