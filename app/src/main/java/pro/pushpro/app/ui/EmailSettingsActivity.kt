package com.pushpro.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Spinner
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.pushpro.R
import com.pushpro.app.util.applyStatusBarInset
import com.pushpro.app.util.setSystemBars

class EmailSettingsActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_email_settings)

    setSystemBars(this, ContextCompat.getColor(this, R.color.black), ContextCompat.getColor(this, R.color.black))
    applyStatusBarInset(findViewById(R.id.rootEmail))

    val prefs = getSharedPreferences("prefs", MODE_PRIVATE)

    val sw = findViewById<Switch>(R.id.switchEnabled)
    val host = findViewById<EditText>(R.id.inputHost)
    val port = findViewById<EditText>(R.id.inputPort)
    val user = findViewById<EditText>(R.id.inputUser)
    val pass = findViewById<EditText>(R.id.inputPass)
    val spinnerTls = findViewById<Spinner>(R.id.spinnerTls)
    val from = findViewById<EditText>(R.id.inputFrom)
    val recipient = findViewById<EditText>(R.id.inputRecipient)
    val subject = findViewById<EditText>(R.id.inputSubjectPrefix)
    val whitelist = findViewById<EditText>(R.id.inputWhitelist)
    val contains = findViewById<EditText>(R.id.inputContains)

    // TLS dropdown
    val tlsItems = arrayOf(getString(R.string.tls_none), getString(R.string.tls_starttls), getString(R.string.tls_ssl))
    spinnerTls.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tlsItems)

    // Load prefs
    sw.isChecked = prefs.getBoolean("email_enabled", false)
    host.setText(prefs.getString("email_input_host","") ?: "")
    port.setText(prefs.getString("email_input_port","") ?: "")
    user.setText(prefs.getString("email_input_user","") ?: "")
    pass.setText(prefs.getString("email_input_pass","") ?: "")
    val tlsModeSaved = if (prefs.contains("email_input_tls_mode")) prefs.getInt("email_input_tls_mode", 1)
                       else if (prefs.getBoolean("email_input_tls", true)) 1 else 0
    spinnerTls.setSelection(tlsModeSaved.coerceIn(0,2))
    from.setText(prefs.getString("email_input_from","") ?: "")
    recipient.setText(prefs.getString("email_input_recipient","") ?: "")
    subject.setText(prefs.getString("email_input_subject_prefix","") ?: "")
    whitelist.setText(prefs.getString("email_input_whitelist","") ?: "")
    contains.setText(prefs.getString("email_input_contains","") ?: "")

    findViewById<Button>(R.id.btnSave).setOnClickListener {
      prefs.edit()
        .putBoolean("email_enabled", sw.isChecked)
        .putString("email_input_host", host.text.toString())
        .putString("email_input_port", port.text.toString())
        .putString("email_input_user", user.text.toString())
        .putString("email_input_pass", pass.text.toString())
        .putInt("email_input_tls_mode", spinnerTls.selectedItemPosition)
        .putString("email_input_from", from.text.toString())
        .putString("email_input_recipient", recipient.text.toString())
        .putString("email_input_subject_prefix", subject.text.toString())
        .putString("email_input_whitelist", whitelist.text.toString())
        .putString("email_input_contains", contains.text.toString())
        .apply()

      finish()
    }

    // Send Test button
    findViewById<Button>(R.id.btnSendTestEmail).setOnClickListener {
      val hostStr = findViewById<EditText>(R.id.inputHost).text.toString().trim()
      val portStr = findViewById<EditText>(R.id.inputPort).text.toString().trim()
      val recipStr = findViewById<EditText>(R.id.inputRecipient).text.toString().trim()
      val ok = hostStr.isNotBlank() && portStr.isNotBlank() && recipStr.isNotBlank()
      getSharedPreferences("pushpro_prefs", MODE_PRIVATE).edit()
        .putBoolean("last_send_error_email", !ok)
        .apply()
      android.widget.Toast.makeText(this, if (ok) "Email test OK" else "Email test failed", android.widget.Toast.LENGTH_SHORT).show()
    }
  }
}
