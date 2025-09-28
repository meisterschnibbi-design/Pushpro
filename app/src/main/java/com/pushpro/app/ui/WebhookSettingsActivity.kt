package com.pushpro.app.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R
import com.pushpro.app.net.Sender

class WebhookSettingsActivity : AppCompatActivity() {

    private fun defaultTemplate(kind: String): String = when (kind) {
        "json"  -> """{ "title": "{title}", "text": "{text}", "package": "{package}", "time": "{time}" }"""
        "form"  -> """title={title}&text={text}&package={package}&time={time}"""
        "plain" -> "{title}\n{text}\n{package}\n{time}"
        else    -> """<push><title>{title}</title><text>{text}</text><package>{package}</package><time>{time}</time></push>"""
    }
    private fun tplKey(kind: String) = "wh_tpl_$kind"

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
        val inputContains: EditText = findViewById(R.id.inputContains)
        val inputTemplateBody: EditText = findViewById(R.id.inputTemplateBody)
        val btnSave: Button = findViewById(R.id.btnSave)
        val btnTest: Button = findViewById(R.id.btnSendTestWebhook)

        ArrayAdapter.createFromResource(this, R.array.webhook_methods, android.R.layout.simple_spinner_dropdown_item).also {
            spinnerMethod.adapter = it
        }
        ArrayAdapter.createFromResource(this, R.array.webhook_templates, android.R.layout.simple_spinner_dropdown_item).also {
            spinnerTemplate.adapter = it
        }

        fun currentKind(): String =
            resources.getStringArray(R.array.webhook_templates)[spinnerTemplate.selectedItemPosition]

        fun loadTemplateBody(kind: String): String =
            prefs.getString(tplKey(kind), "")?.takeIf { it.isNotBlank() } ?: defaultTemplate(kind)

        fun refreshTemplateBody() {
            inputTemplateBody.setText(loadTemplateBody(currentKind()))
        }

        // Load
        swEnabled.isChecked = prefs.getBoolean("wh_enabled", false)
        inputUrl.setText(prefs.getString("wh_url", "") ?: "")
        spinnerMethod.setSelection((prefs.getInt("wh_method", 1)).coerceIn(0, spinnerMethod.adapter.count - 1))
        spinnerTemplate.setSelection((prefs.getInt("wh_template", 0)).coerceIn(0, spinnerTemplate.adapter.count - 1))
        inputHeaders.setText(prefs.getString("wh_headers", "") ?: "")
        inputWhitelist.setText(prefs.getString("wh_whitelist", "") ?: "")
        inputContains.setText(prefs.getString("wh_contains", "") ?: "")
        refreshTemplateBody()

        spinnerTemplate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) = refreshTemplateBody()
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnSave.setOnClickListener {
            val kind = currentKind()
            prefs.edit()
                .putBoolean("wh_enabled", swEnabled.isChecked)
                .putString("wh_url", inputUrl.text.toString())
                .putInt("wh_method", spinnerMethod.selectedItemPosition)
                .putInt("wh_template", spinnerTemplate.selectedItemPosition)
                .putString("wh_headers", inputHeaders.text.toString())
                .putString("wh_whitelist", inputWhitelist.text.toString())
                .putString("wh_contains", inputContains.text.toString())
                .putString(tplKey(kind), inputTemplateBody.text.toString())
                .apply()
            finish()
        }

        btnTest.setOnClickListener {
            // Persist current template for selected kind before test
            val kind = currentKind()
            prefs.edit().putString(tplKey(kind), inputTemplateBody.text.toString()).apply()

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
