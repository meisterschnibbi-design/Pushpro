package com.pushpro.app.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R

class TelegramSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram_settings)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)

        val swEnabled: Switch      = findViewById(R.id.switchEnabled)
        val inputBotToken: EditText = findViewById(R.id.inputBotToken)
        val inputChatId: EditText   = findViewById(R.id.inputChatId)
        val spinnerParseMode: Spinner = findViewById(R.id.spinnerParseMode)
        val inputHeaderPrefix: EditText = findViewById(R.id.inputHeaderPrefix)
        val btnSave: Button           = findViewById(R.id.btnSave)
        val btnSendTest: Button?      = findViewById(R.id.btnSendTestTelegram)

        // Parse modes (define in res/values/arrays.xml → telegram_parse_modes)
        ArrayAdapter.createFromResource(
            this,
            R.array.telegram_parse_modes, // e.g. ["None","Markdown","HTML"]
            android.R.layout.simple_spinner_dropdown_item
        ).also { spinnerParseMode.adapter = it }

        // Load saved values
        swEnabled.isChecked = prefs.getBoolean("tg_enabled", false)
        inputBotToken.setText(prefs.getString("tg_token", "") ?: "")
        inputChatId.setText(prefs.getString("tg_chat_id", "") ?: "")
        spinnerParseMode.setSelection((prefs.getInt("tg_parse_mode", 0))
            .coerceIn(0, spinnerParseMode.adapter.count - 1))
        inputHeaderPrefix.setText(prefs.getString("tg_header_prefix", "") ?: "")

        // Save
        btnSave.setOnClickListener {
            prefs.edit()
                .putBoolean("tg_enabled", swEnabled.isChecked)
                .putString("tg_token", inputBotToken.text.toString())
                .putString("tg_chat_id", inputChatId.text.toString())
                .putInt("tg_parse_mode", spinnerParseMode.selectedItemPosition)
                .putString("tg_header_prefix", inputHeaderPrefix.text.toString())
                .apply()
            finish()
        }

        // Optional: local validation test (no real API call)
        btnSendTest?.setOnClickListener {
            val ok = inputBotToken.text.isNotBlank() && inputChatId.text.isNotBlank()
            getSharedPreferences("pushpro_prefs", MODE_PRIVATE).edit()
                .putBoolean("last_send_error_tg", !ok)
                .apply()
            android.widget.Toast.makeText(
                this,
                if (ok) "Telegram test OK" else "Telegram test failed",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
