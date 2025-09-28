package com.pushpro.app.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R
import com.pushpro.app.net.Sender

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
        val inputWhitelist: EditText = findViewById(R.id.inputWhitelist)
        val btnSave: Button = findViewById(R.id.btnSave)
        val btnTest: Button = findViewById(R.id.btnSendTestWebhook)
        val txtPreview: TextView = findViewById(R.id.txtTemplatePreview)

        ArrayAdapter.createFromResource(this, R.array.webhook_methods, android.R.layout.simple_spinner_dropdown_item).also {
            spinnerMethod.adapter = it
        }
        ArrayAdapter.createFromResource(this, R.array.webhook_templates, android.R.layout.simple_spinner_dropdown_item).also {
            spinnerTemplate.adapter = it
        }

        fun updatePreview() {
            val method = resources.getStringArray(R.array.webhook_methods)[spinnerMethod.selectedItemPosition]
            val template = resources.getStringArray(R.array.webhook_templates)[spinnerTemplate.selectedItemPosition]
            val sample = when (template) {
                "json" -> "{ \"title\": \"...\", \"text\": \"...\", \"package\": \"...\", \"time\": \"...\" }"
                "form" -> "title=...&text=...&package=...&time=..."
                "plain" -> "title\\ntext\\npackage\\ntime"
                else -> "<push><title>...</title><text>...</text><package>...</package><time>...</time></push>"
            }
            txtPreview.text = "Preview ($method / $template):\n$sample"
        }

        swEnabled.isChecked = prefs.getBoolean("wh_enabled", false)
        inputUrl.setText(prefs.getString("wh_url", "") ?: "")
        spinnerMethod.setSelection((prefs.getInt("wh_method", 1)).coerceIn(0, spinnerMethod.adapter.count - 1))
        spinnerTemplate.setSelection((prefs.getInt("wh_template", 0)).coerceIn(0, spinnerTemplate.adapter.count - 1))
        inputHeaders.setText(prefs.getString("wh_headers", "") ?: "")
        inputWhitelist.setText(prefs.getString("wh_whitelist", "") ?: "")
        updatePreview()

        spinnerMethod.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) = updatePreview()
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
        spinnerTemplate.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) = updatePreview()
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        btnSave.setOnClickListener {
            prefs.edit()
                .putBoolean("wh_enabled", swEnabled.isChecked)
                .putString("wh_url", inputUrl.text.toString())
                .putInt("wh_method", spinnerMethod.selectedItemPosition)
                .putInt("wh_template", spinnerTemplate.selectedItemPosition)
                .putString("wh_headers", inputHeaders.text.toString())
                .putString("wh_whitelist", inputWhitelist.text.toString())
                .apply()
            finish()
        }

        btnTest.setOnClickListener {
            Sender.sendWebhookTest(
                this,
                inputUrl.text.toString().trim(),
                spinnerMethod.selectedItemPosition,
                spinnerTemplate.selectedItemPosition,
                inputHeaders.text.toString()
            )
        }
    }
}
