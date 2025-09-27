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

        // Status bar styling (keine doppelte Top-Inset)
        findViewById<ImageView?>(R.id.main_root_view)?.applyStatusBarInset()
        setSystemBars(
            window,
            ContextCompat.getColor(this, R.color.black)
        )

        // Navigation-Buttons
        findViewById<Button?>(R.id.btnEmailSettings)?.setOnClickListener {
            startActivity(Intent(this, EmailSettingsActivity::class.java))
        }
        findViewById<Button?>(R.id.btnTelegramSettings)?.setOnClickListener {
            startActivity(Intent(this, TelegramSettingsActivity::class.java))
        }
        findViewById<Button?>(R.id.btnWebhookSettings)?.setOnClickListener {
            startActivity(Intent(this, WebhookSettingsActivity::class.java))
        }
        findViewById<Button?>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Global Enable Switch (passt zu Diagnostics/Settings)
        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        val switchEnable: Switch? = findViewById(R.id.switchEnable)
        val txtStatus: TextView? = findViewById(R.id.txtStatus)

        val enabledInit = prefs.getBoolean("global_enabled", false)
        switchEnable?.isChecked = enabledInit
        txtStatus?.text = if (enabledInit) getString(R.string.enabled) else getString(R.string.disabled)

        switchEnable?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("global_enabled", isChecked).apply()
            txtStatus?.text = if (isChecked) getString(R.string.enabled) else getString(R.string.disabled)
        }
    }
}
