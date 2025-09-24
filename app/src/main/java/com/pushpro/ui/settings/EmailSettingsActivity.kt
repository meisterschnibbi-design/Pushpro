package com.pushpro.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.pushpro.R

class EmailSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_settings)
        val enable = findViewById<MaterialSwitch>(R.id.switchEnableEmail)
        enable.isChecked = true
    }
}
