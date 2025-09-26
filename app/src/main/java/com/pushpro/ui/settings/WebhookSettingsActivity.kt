package com.pushpro.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R

class WebhookSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webhook_settings)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputUrl)
        findViewById<android.widget.AutoCompleteTextView>(R.id.inputHttpMethod)
        findViewById<android.widget.AutoCompleteTextView>(R.id.inputContentType)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputJsonTemplate)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSendTest)
    }
}
