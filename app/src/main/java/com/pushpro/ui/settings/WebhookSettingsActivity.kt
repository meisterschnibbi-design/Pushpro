package com.pushpro.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R

class WebhookSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webhook_settings)
        findViewById<android.view.View>(R.id.rootWebhook)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputUrl)
        findViewById<android.widget.Spinner>(R.id.spinnerMethod)
        findViewById<android.widget.Spinner>(R.id.spinnerContentType)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputHeaders)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputTemplate)
        findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.inputJsonTemplate)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSendTest)
    }
}
