package com.pushpro.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R

class BaseSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Beispiel-Views – bitte IDs mit deinem activity_settings.xml abgleichen
        val input1: EditText = findViewById(R.id.input1)
        val input2: EditText = findViewById(R.id.input2)
        val btnSendTest: Button = findViewById(R.id.btnSendTestGeneric)

        btnSendTest.setOnClickListener {
            val text1 = input1.text.toString()
            val text2 = input2.text.toString()
            // TODO: Logik einfügen (z.B. Test-Webhook oder Mailversand)
        }
    }
}
