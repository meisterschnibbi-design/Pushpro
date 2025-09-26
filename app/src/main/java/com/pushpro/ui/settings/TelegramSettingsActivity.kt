package com.pushpro.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R

class TelegramSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram_settings)
        findViewById<android.view.View>(R.id.rootTelegram)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputToken)
        // If you use only one of these in your code, the extra is harmless:
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputChatId)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputChatIds)
        findViewById<android.widget.Spinner>(R.id.spinnerParse)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSendTestTelegram)
    }
}
