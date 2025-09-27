package com.pushpro.app.ui


import android.os.Bundle
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


val sw = findViewById<Switch>(R.id.switchEnabled)
val url = findViewById<EditText>(R.id.inputUrl)
val method = findViewById<Spinner>(R.id.spinnerMethod)
val template = findViewById<Spinner>(R.id.spinnerTemplate)
val headers = findViewById<EditText>(R.id.inputHeaders)


// TODO: Adapter für method/template setzen (z.B. aus arrays.xml)


// Load
sw.isChecked = prefs.getBoolean("wh_enabled", false)
url.setText(prefs.getString("wh_url", "") ?: "")
method.setSelection((prefs.getInt("wh_method", 0)).coerceIn(0, 3))
template.setSelection((prefs.getInt("wh_template", 0)).coerceIn(0, 3))
headers.setText(prefs.getString("wh_headers", "") ?: "")


findViewById<Button>(R.id.btnSave).setOnClickListener {
prefs.edit()
.putBoolean("wh_enabled", sw.isChecked)
.putString("wh_url", url.text.toString())
.putInt("wh_method", method.selectedItemPosition)
.putInt("wh_template", template.selectedItemPosition)
.putString("wh_headers", headers.text.toString())
.apply()
finish()
}
}
}