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


val sw = findViewById<Switch>(R.id.switchEnabled)
val token = findViewById<EditText>(R.id.inputBotToken)
val chatId = findViewById<EditText>(R.id.inputChatId)
val parseMode = findViewById<Spinner>(R.id.spinnerParseMode)
val headerPrefix = findViewById<EditText>(R.id.inputHeaderPrefix)


// Parse modes aus arrays.xml (telegram_parse_modes)
ArrayAdapter.createFromResource(
this,
R.array.telegram_parse_modes,
android.R.layout.simple_spinner_dropdown_item
).also { parseMode.adapter = it }


// Load
sw.isChecked = prefs.getBoolean("tg_enabled", false)
token.setText(prefs.getString("tg_token", "") ?: "")
chatId.setText(prefs.getString("tg_chat_id", "") ?: "")
parseMode.setSelection((prefs.getInt("tg_parse_mode", 0)).coerceIn(0, 2))
headerPrefix.setText(prefs.getString("tg_header_prefix", "") ?: "")


findViewById<Button>(R.id.btnSave).setOnClickListener {
prefs.edit()
.putBoolean("tg_enabled", sw.isChecked)
.putString("tg_token", token.text.toString())
.putString("tg_chat_id", chatId.text.toString())
.putInt("tg_parse_mode", parseMode.selectedItemPosition)
.putString("tg_header_prefix", headerPrefix.text.toString())
.apply()
finish()
}


findViewById<Button?>(R.id.btnSendTestTelegram)?.setOnClickListener {
// TODO: Optional – Testnachricht via Bot senden
}
}
}