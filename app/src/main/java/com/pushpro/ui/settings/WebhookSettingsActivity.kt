package com.pushpro.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R

class WebhookSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webhook_settings)
        findViewById<android.view.View>(R.id.rootWebhook)
        findViewById<android.view.View>(R.id.inputHttpMethod)
        findViewById<android.view.View>(R.id.inputHeaders)
        findViewById<android.view.View>(R.id.inputTemplate)
        findViewById<android.view.View>(R.id.spinnerMethod)
        findViewById<android.view.View>(R.id.spinnerContentType)
    }
}
