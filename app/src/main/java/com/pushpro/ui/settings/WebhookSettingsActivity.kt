package com.pushpro.ui.settings

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.pushpro.R

class WebhookSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webhook_settings)

        fun setup(id: Int, arr: Int) {
            val v = findViewById<MaterialAutoCompleteTextView>(id)
            val items = resources.getStringArray(arr)
            v.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, items))
            v.setOnClickListener { v.showDropDown() }
            v.setOnFocusChangeListener { _, has -> if (has) v.showDropDown() }
        }
        setup(R.id.inputHttpMethod, R.array.http_methods)
        setup(R.id.inputContentType, R.array.content_types)
        setup(R.id.inputHeaders, R.array.header_presets)
        setup(R.id.inputJsonTemplate, R.array.json_body_templates)

        findViewById<android.view.View>(R.id.btnSaveWebhook).setOnClickListener {
            Toast.makeText(this, "Webhook saved", Toast.LENGTH_SHORT).show()
        }
    }
}
