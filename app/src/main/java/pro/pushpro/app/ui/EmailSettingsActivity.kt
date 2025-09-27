package com.pushpro.app.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.pushpro.R
import com.pushpro.app.util.applyStatusBarInset
import com.pushpro.app.util.setSystemBars

class EmailSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_settings)

        // Bars schwarz halten, ohne zusätzliches Top-Padding
        setSystemBars(
            this,
            ContextCompat.getColor(this, R.color.black),
            ContextCompat.getColor(this, R.color.black)
        )
        applyStatusBarInset(findViewById(R.id.rootEmail))

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)

        val swEnabled: Switch       = findViewById(R.id.switchEnabled)
        val inputHost: EditText     = findViewById(R.id.inputHost)
        val inputPort: EditText     = findViewById(R.id.inputPort)
        val inputUser: EditText     = findViewById(R.id.inputUser)
        val inputPass: EditText     = findViewById(R.id.inputPass)
        val spinnerTls: Spinner     = findViewById(R.id.spinnerTls)
        val inputFrom: EditText     = findViewById(R.id.inputFrom)
        val inputRecipient: EditText= findViewById(R.id.inputRecipient)
        val inputSubject: EditText  = findViewById(R.id.inputSubjectPrefix)
        val inputWhitelist: EditText= findViewById(R.id.inputWhitelist)
        val inputContains: EditText = findViewById(R.id.inputContains)
        val btnSave: Button         = findViewById(R.id.btnSave)
        val btnSendTest: Button     = findViewById(R.id.btnSendTestEmail)

        // TLS-Dropdown (Strings müssen in res/values/strings.xml existieren)
        val tlsItems = arrayOf(
            getString(R.string.tls_none),
            getString(R.string.tls_starttls),
            getString(R.string.tls_ssl)
        )
        spinnerTls.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            tlsItems
        )

        // Laden
        swEnabled.isChecked = prefs.getBoolean("email_enabled", false)
        inputHost.setText(prefs.getString("email_input_host", "") ?: "")
        inputPort.setText(prefs.getString("email_input_port", "") ?: "")
        inputUser.setText(prefs.getString("email_input_user", "") ?: "")
        inputPass.setText(prefs.getString("email_input_pass", "") ?: "")
        val tlsModeSaved = if (prefs.contains("email_input_tls_mode"))
            prefs.getInt("email_input_tls_mode", 1)
        else if (prefs.getBoolean("email_input_tls", true)) 1 else 0
        spinnerTls.setSelection(tlsModeSaved.coerceIn(0, 2))
        inputFrom.setText(prefs.getString("email_input_from", "") ?: "")
        inputRecipient.setText(prefs.getString("email_input_recipient", "") ?: "")
        inputSubject.setText(prefs.getString("email_input_subject_prefix", "") ?: "")
        inputWhitelist.setText(prefs.getString("email_input_whitelist", "") ?: "")
        inputContains.setText(prefs.getString("email_input_contains", "") ?: "")

        // Speichern
        btnSave.setOnClickListener {
            prefs.edit()
                .putBoolean("email_enabled", swEnabled.isChecked)
                .putString("email_input_host", inputHost.text.toString().trim())
                .putString("email_input_port", inputPort.text.toString().trim())
                .putString("email_input_user", inputUser.text.toString().trim())
                .putString("email_input_pass", inputPass.text.toString())
                .putInt("email_input_tls_mode", spinnerTls.selectedItemPosition)
                .putString("email_input_from", inputFrom.text.toString().trim())
                .putString("email_input_recipient", inputRecipient.text.toString().trim())
                .putString("email_input_subject_prefix", inputSubject.text.toString())
                .putString("email_input_whitelist", inputWhitelist.text.toString())
                .putString("email_input_contains", inputContains.text.toString())
                .apply()
            finish()
        }

        // Test (lokale Validierung; Ergebnisflag für LogViewer/Diag)
        btnSendTest.setOnClickListener {
            val ok = inputHost.text.isNotBlank() &&
                     inputPort.text.isNotBlank() &&
                     inputRecipient.text.isNotBlank()
            getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("last_send_error_email", !ok)
                .apply()
            android.widget.Toast.makeText(
                this,
                if (ok) "Email test OK" else "Email test failed",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
