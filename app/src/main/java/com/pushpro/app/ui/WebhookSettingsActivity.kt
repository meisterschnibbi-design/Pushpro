package com.pushpro.app.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R

class WebhookSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webhook_settings)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)

        val swEnabled: Switch = findViewById(R.id.switchEnabled)
        val inputUrl: EditText = findViewById(R.id.inputUrl)
        val spinnerMethod: Spinner = findViewById(R.id.spinnerMethod)
        val spinnerTemplate: Spinner = findViewById(R.id.spinnerTemplate)
        val inputHeaders: EditText = findViewById(R.id.inputHeaders)
        val btnSave: Button = findViewById(R.id.btnSave)

        ArrayAdapter.createFromResource(
            this,
            R.array.webhook_methods,
            android.R.layout.simple_spinner_dropdown_item
        ).also { spinnerMethod.adapter = it }

        ArrayAdapter.createFromResource(
            this,
            R.array.webhook_templates,
            android.R.layout.simple_spinner_dropdown_item
        ).also { spinnerTemplate.adapter = it }

        swEnabled.isChecked = prefs.getBoolean("wh_enabled", false)
        inputUrl.setText(prefs.getString("wh_url", "") ?: "")
        spinnerMethod.setSelection((prefs.getInt("wh_method", 0)).coerceIn(0, spinnerMethod.adapter.count - 1))
        spinnerTemplate.setSelection((prefs.getInt("wh_template", 0)).coerceIn(0, spinnerTemplate.adapter.count - 1))
        inputHeaders.setText(prefs.getString("wh_headers", "") ?: "")

        btnSave.setOnClickListener {
            prefs.edit()
                .putBoolean("wh_enabled", swEnabled.isChecked)
                .putString("wh_url", inputUrl.text.toString())
                .putInt("wh_method", spinnerMethod.selectedItemPosition)
                .putInt("wh_template", spinnerTemplate.selectedItemPosition)
                .putString("wh_headers", inputHeaders.text.toString())
                .apply()
            finish()
        }
    }
}
