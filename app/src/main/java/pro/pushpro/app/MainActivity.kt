package com.pushpro.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.pushpro.R
import com.pushpro.app.ui.EmailSettingsActivity
import com.pushpro.app.ui.SettingsActivity
import com.pushpro.app.ui.TelegramSettingsActivity
import com.pushpro.app.ui.WebhookSettingsActivity
import com.pushpro.app.util.applyStatusBarInset
import com.pushpro.app.util.setSystemBars

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // StatusBar / Insets
        val rootView: ImageView = findViewById(R.id.main_root_view)
        rootView.applyStatusBarInset()
        setSystemBars(window, ContextCompat.getColor(this, R.color.black))

        // Beispiel-Buttons (IDs musst du mit deinem XML abgleichen!)
        val btnEmail: Button = findViewById(R.id.btnEmailSettings)
        val btnTelegram: Button = findViewById(R.id.btnTelegramSettings)
        val btnWebhook: Button = findViewById(R.id.btnWebhookSettings)
        val btnSettings: Button = findViewById(R.id.btnSettings)

        btnEmail.setOnClickListener {
            startActivity(Intent(this, EmailSettingsActivity::class.java))
        }

        btnTelegram.setOnClickListener {
            startActivity(Intent(this, TelegramSettingsActivity::class.java))
        }

        btnWebhook.setOnClickListener {
            startActivity(Intent(this, WebhookSettingsActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Beispiel-Switch oder TextView (falls vorhanden)
        val switchEnable: Switch = findViewById(R.id.switchEnable)
        val txtStatus: TextView = findViewById(R.id.txtStatus)

        switchEnable.setOnCheckedChangeListener { _, isChecked ->
            txtStatus.text = if (isChecked) {
                getString(R.string.enabled)
            } else {
                getString(R.string.disabled)
            }
        }
    }
}
