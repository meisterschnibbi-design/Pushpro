package com.pushpro.app.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R
import com.pushpro.app.net.EmailSender

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

        // Disable autofill & save prompts programmatically as well
        if (Build.VERSION.SDK_INT >= 26) {
            window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            listOf(inputUser, inputPass, inputRecipient).forEach {
                it.setAutofillHints(*emptyArray())
                it.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            }
        }

        val tlsItems = arrayOf("None (25)", "STARTTLS (587)", "SSL/TLS (465)")
        spinnerTls.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tlsItems)

        // Load
        swEnabled.isChecked = prefs.getBoolean("email_enabled", false)
        inputHost.setText(prefs.all["email_input_host"]?.toString() ?: "")
        inputPort.setText(prefs.all["email_input_port"]?.toString() ?: "")
        inputUser.setText(prefs.all["email_input_user"]?.toString() ?: "")
        inputPass.setText(prefs.all["email_input_pass"]?.toString() ?: "")
        inputRecipient.setText(prefs.all["email_input_recipient"]?.toString() ?: "")
        inputSubject.setText(prefs.all["email_input_subject_prefix"]?.toString() ?: "")
        inputWhitelist.setText(prefs.all["email_input_whitelist"]?.toString() ?: "")
        inputContains.setText(prefs.all["email_input_contains"]?.toString() ?: "")
        spinnerTls.setSelection((prefs.getInt("email_input_tls_mode", 1)).coerceIn(0, 2))

        spinnerTls.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val port = when (position) { 0 -> "25"; 1 -> "587"; else -> "465" }
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
        }

        btnSendTest.setOnClickListener {
            val host = inputHost.text.toString().trim()
            val port = inputPort.text.toString().trim()
            val user = inputUser.text.toString().trim()
            val pass = inputPass.text.toString()
            val tls = spinnerTls.selectedItemPosition
            val recipient = inputRecipient.text.toString().trim()
            val prefix = inputSubject.text.toString()

            Thread {
                val (ok, err) = EmailSender.sendTestEmail(this, host, port, user, pass, tls, recipient, prefix)
                getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("last_send_error_email", !ok)
                    .apply()
                runOnUiThread {
                    android.widget.Toast.makeText(this, if (ok) "Email test OK" else "Email test failed: $err", android.widget.Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }
}
