package com.pushpro.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R

class EmailSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_settings)
        findViewById<android.view.View>(R.id.rootEmail)
        findViewById<android.view.View>(R.id.inputFrom)
        findViewById<android.view.View>(R.id.inputSubjectPrefix)
    }
}
