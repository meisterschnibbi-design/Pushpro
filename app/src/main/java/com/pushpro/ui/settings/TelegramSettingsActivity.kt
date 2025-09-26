package com.pushpro.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R

class TelegramSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram_settings)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputToken)
        // Support singular/plural keys
        try { findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputChatId) } catch (_: Exception) {}
        try { findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputChatIds) } catch (_: Exception) {}
        findViewById<android.widget.AutoCompleteTextView>(R.id.parseModeDropdown)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSendTestTelegram)
    }
}
