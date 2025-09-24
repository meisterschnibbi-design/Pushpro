package pro.pushpro.app.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import pro.pushpro.app.R
import pro.pushpro.app.util.LogUtil
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.Properties

class EmailSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_settings)

        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        val host = findViewById<EditText>(R.id.inputHost)
        val port = findViewById<EditText>(R.id.inputPort)
        val user = findViewById<EditText>(R.id.inputUser)
        val pass = findViewById<EditText>(R.id.inputPass)
        val rcpt = findViewById<EditText>(R.id.inputRecipient)
        val tlsSpinner = findViewById<Spinner>(R.id.spinnerTls)

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val NO = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            host.importantForAutofill = NO
            pass.importantForAutofill = NO
            rcpt.importantForAutofill = NO
            user.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
        }

        host.setText(prefs.getString("email_host", "") ?: "")
        user.setText(prefs.getString("email_user", "") ?: "")
        pass.setText(prefs.getString("email_pass", "") ?: "")
        rcpt.setText(prefs.getString("email_recipient", "") ?: "")

        val tlsModes = arrayOf("None (25)", "STARTTLS (587)", "SSL/TLS (465)")
        tlsSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, tlsModes)
        val savedMode = prefs.getString("email_tls", "None") ?: "None"
        val idx = when (savedMode) { "STARTTLS" -> 1; "SSL" -> 2; else -> 0 }
        tlsSpinner.setSelection(idx)

        fun applyPortLock(index: Int) {
            val p = when (index) { 1 -> "587"; 2 -> "465"; else -> "25" }
            port.setText(p); port.isEnabled = false
        }
        applyPortLock(idx)
        tlsSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) { applyPortLock(position) }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val mode = when (tlsSpinner.selectedItemPosition) { 1 -> "STARTTLS"; 2 -> "SSL"; else -> "None" }
            prefs.edit()
                .putString("email_host", host.text.toString().trim())
                .putString("email_user", user.text.toString().trim())
                .putString("email_pass", pass.text.toString())
                .putString("email_recipient", rcpt.text.toString().trim())
                .putString("email_tls", mode)
                .putString("email_port", when (mode) { "STARTTLS" -> "587"; "SSL" -> "465"; else -> "25" })
                .apply()
            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnSendTestEmail).setOnClickListener {
            Thread {
                val mode = when (tlsSpinner.selectedItemPosition) { 1 -> "STARTTLS"; 2 -> "SSL"; else -> "None" }
                val hostV = host.text.toString().trim()
                val userV = user.text.toString().trim()
                val passV = pass.text.toString()
                val rcptV = rcpt.text.toString().trim()

                val props = Properties().apply {
                    put("mail.transport.protocol", "smtp")
                    put("mail.smtp.host", hostV)
                    put("mail.smtp.auth", "true")
                    when (mode) {
                        "STARTTLS" -> { put("mail.smtp.starttls.enable", "true"); put("mail.smtp.port", "587") }
                        "SSL"      -> { put("mail.smtp.ssl.enable", "true");       put("mail.smtp.port", "465") }
                        else       -> {                                          put("mail.smtp.port", "25")  }
                    }
                    put("mail.smtp.connectiontimeout", "8000")
                    put("mail.smtp.timeout", "8000")
                }

                var ok = false
                try {
                    val session = Session.getInstance(props, object: javax.mail.Authenticator() {
                        override fun getPasswordAuthentication() =
                            PasswordAuthentication(userV, passV)
                    })
                    val msg = MimeMessage(session).apply {
                        setFrom(InternetAddress(userV))
                        setRecipients(Message.RecipientType.TO, InternetAddress.parse(rcptV))
                        subject = "PushPro Email Test"
                        setText("This is a test message from PushPro.")
                    }
                    Transport.send(msg); ok = true
                } catch (_: Exception) { ok = false }

                LogUtil.append(this, if (ok) "Email test success" else "Email test fail")
                runOnUiThread { Toast.makeText(this, if (ok) "OK" else "Failed", Toast.LENGTH_SHORT).show() }
            }.start()
        }
    }
}