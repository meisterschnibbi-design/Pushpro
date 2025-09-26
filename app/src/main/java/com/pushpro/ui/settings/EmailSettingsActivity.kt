package com.pushpro.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R

class EmailSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_settings)

        // Views referenced by code — must exist in XML:
        findViewById<android.view.View>(R.id.rootEmail)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputFrom)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputSubjectPrefix)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputHost)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputPort)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputUser)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputPass)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputRecipient)
        findViewById<android.widget.Spinner>(R.id.spinnerTls)
        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switchEnableEmail)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSendTestEmail)
    }
}
