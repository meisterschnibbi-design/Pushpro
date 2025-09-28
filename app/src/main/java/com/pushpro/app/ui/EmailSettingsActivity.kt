
package com.pushpro.app.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R
import com.pushpro.app.net.Sender

class EmailSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_settings)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)

        val swEnabled: Switch = findViewById(R.id.switchEnabled)
        val inputHost: EditText = findViewById(R.id.inputHost)
        val inputPort: EditText = findViewById(R.id.inputPort)
        val inputUser: EditText = findViewById(R.id.inputUser)
        val inputPass: EditText = findViewById(R.id.inputPass)
        val spinnerTls: Spinner = findViewById(R.id.spinnerTls)
        val inputRecipient: EditText = findViewById(R.id.inputRecipient)
        val inputSubject: EditText = findViewById(R.id.inputSubjectPrefix)
        val inputWhitelist: EditText = findViewById(R.id.inputWhitelist)
        val inputContains: EditText = findViewById(R.id.inputContains)
        val btnSave: Button = findViewById(R.id.btnSave)
        val btnSendTest: Button = findViewById(R.id.btnSendTestEmail)

        val tlsItems = arrayOf("None (25)", "STARTTLS (587)", "SSL/TLS (465)")
        spinnerTls.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tlsItems)

        swEnabled.isChecked = prefs.getBoolean("email_enabled", false)
        inputHost.setText(prefs.getString("email_input_host", "") ?: "")
        inputPort.setText(prefs.getString("email_input_port", "") ?: "")
        inputUser.setText(prefs.getString("email_input_user", "") ?: "")
        inputPass.setText(prefs.getString("email_input_pass", "") ?: "")
        inputRecipient.setText(prefs.getString("email_input_recipient", "") ?: "")
        inputSubject.setText(prefs.getString("email_input_subject_prefix", "") ?: "")
        inputWhitelist.setText(prefs.getString("email_input_whitelist", "") ?: "")
        inputContains.setText(prefs.getString("email_input_contains", "") ?: "")

        spinnerTls.setSelection((prefs.getInt("email_input_tls_mode", 1)).coerceIn(0,2))
        spinnerTls.setOnItemSelectedListener(object: android.widget.AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val port = when(position){ 0->"25"; 1->"587"; else->"465" }
                inputPort.setText(port)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })

        btnSave.setOnClickListener {
            prefs.edit()
                .putBoolean("email_enabled", swEnabled.isChecked)
                .putString("email_input_host", inputHost.text.toString().trim())
                .putString("email_input_port", inputPort.text.toString().trim())
                .putString("email_input_user", inputUser.text.toString().trim())
                .putString("email_input_pass", inputPass.text.toString())
                .putInt("email_input_tls_mode", spinnerTls.selectedItemPosition)
                .putString("email_input_recipient", inputRecipient.text.toString().trim())
                .putString("email_input_subject_prefix", inputSubject.text.toString())
                .putString("email_input_whitelist", inputWhitelist.text.toString())
                .putString("email_input_contains", inputContains.text.toString())
                .apply()
            finish()
        })

        btnSendTest.setOnClickListener {
            // Placeholder: only sets flag. Implement real SMTP later if desired.
            val ok = inputHost.text.isNotBlank() && inputPort.text.isNotBlank() && inputRecipient.text.isNotBlank()
            getSharedPreferences("pushpro_prefs", MODE_PRIVATE).edit()
                .putBoolean("last_send_error_email", !ok)
                .apply()
            android.widget.Toast.makeText(this, if (ok) "Email test OK" else "Email test failed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
