
package pro.pushpro.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import pro.pushpro.app.R
import pro.pushpro.app.util.applyStatusBarInset
import pro.pushpro.app.util.setSystemBars

abstract class BaseSettingsActivity : AppCompatActivity() {

    abstract val titleText: String
    abstract val keyPrefix: String
    abstract val hint1: String
    abstract val hint2: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setSystemBars(this,
            ContextCompat.getColor(this, R.color.status_bar_bg),
            ContextCompat.getColor(this, R.color.nav_bar_bg)
        )
        applyStatusBarInset(findViewById(R.id.rootSettings))

        findViewById<TextView>(R.id.title).text = titleText
        val sw = findViewById<Switch>(R.id.switchEnabled)
        val input1 = findViewById<EditText>(R.id.input1)
        val input2 = findViewById<EditText>(R.id.input2)
        val inputWhitelist = findViewById<EditText>(R.id.inputWhitelist)
        val inputContains = findViewById<EditText>(R.id.inputContains)
        val btnSave = findViewById<Button>(R.id.btnSave)

        input1.hint = hint1
        input2.hint = hint2

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        sw.isChecked = prefs.getBoolean("${keyPrefix}_enabled", false)
        input1.setText(prefs.getString("${keyPrefix}_input1", "") ?: "")
        input2.setText(prefs.getString("${keyPrefix}_input2", "") ?: "")
        inputWhitelist.setText(prefs.getString("${keyPrefix}_whitelist", "") ?: "")
        inputContains.setText(prefs.getString("${keyPrefix}_contains", "") ?: "")

        btnSave.setOnClickListener {
            prefs.edit()
                .putBoolean("${keyPrefix}_enabled", sw.isChecked)
                .putString("${keyPrefix}_input1", input1.text.toString())
                .putString("${keyPrefix}_input2", input2.text.toString())
                .putString("${keyPrefix}_whitelist", inputWhitelist.text.toString())
                .putString("${keyPrefix}_contains", inputContains.text.toString())
                .apply()
            finish()
        }

        // Send Test for Telegram
        findViewById<Button>(R.id.btnSendTestGeneric)?.setOnClickListener {
            val ok = if (keyPrefix == "telegram") {
                val token = findViewById<EditText>(R.id.input1).text.toString().trim()
                val chat = findViewById<EditText>(R.id.input2).text.toString().trim()
                token.isNotBlank() && chat.isNotBlank()
            } else true
            getSharedPreferences("pushpro_prefs", MODE_PRIVATE).edit()
                .putBoolean("last_send_error_telegram", !ok)
                .apply()
            android.widget.Toast.makeText(this, if (ok) "Telegram test OK" else "Telegram test failed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
