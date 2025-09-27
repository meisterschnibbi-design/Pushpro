package com.pushpro.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R
import com.pushpro.app.util.LogUtil

class BaseSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val input1: EditText = findViewById(R.id.input1)
        val input2: EditText = findViewById(R.id.input2)
        val btnSendTest: Button = findViewById(R.id.btnSendTestGeneric)

        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        input1.setText(prefs.getString("generic_input1", "") ?: "")
        input2.setText(prefs.getString("generic_input2", "") ?: "")

        btnSendTest.setOnClickListener {
            val v1 = input1.text.toString().trim()
            val v2 = input2.text.toString().trim()

            prefs.edit()
                .putString("generic_input1", v1)
                .putString("generic_input2", v2)
                .apply()

            LogUtil.append(this, "Generic test sent with input1='$v1', input2='$v2'")
            android.widget.Toast.makeText(this, "Test values saved", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
