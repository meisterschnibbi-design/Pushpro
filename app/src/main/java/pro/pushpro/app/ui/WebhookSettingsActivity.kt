package pro.pushpro.app.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import pro.pushpro.app.R
import pro.pushpro.app.util.applyStatusBarInset
import pro.pushpro.app.util.setSystemBars
import pro.pushpro.app.util.LogUtil
import pro.pushpro.app.util.Net
import java.net.HttpURLConnection
import java.net.URL

class WebhookSettingsActivity : AppCompatActivity() {

    private val methods = arrayOf("POST","GET","PUT","PATCH","DELETE","HEAD","OPTIONS")
    private val types = arrayOf("application/json","application/x-www-form-urlencoded","text/plain","application/xml")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webhook_settings)

        setSystemBars(this, ContextCompat.getColor(this, R.color.black), ContextCompat.getColor(this, R.color.black))
        applyStatusBarInset(findViewById(R.id.rootWebhook))

        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)

        val sw: Switch = findViewById(R.id.switchEnabled)
        val inputUrl: EditText = findViewById(R.id.inputUrl)
        val inputHeaders: EditText = findViewById(R.id.inputHeaders)
        val inputTemplate: EditText = findViewById(R.id.inputTemplate)
        val whitelist: EditText = findViewById(R.id.inputWhitelist)
        val contains: EditText = findViewById(R.id.inputContains)
        val spMethod: Spinner = findViewById(R.id.spinnerMethod)
        val spType: Spinner = findViewById(R.id.spinnerContentType)
        val btnSave: Button = findViewById(R.id.btnSave)
        val btnTest: Button = findViewById(R.id.btnSendTest)

        spMethod.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, methods)
        spType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        sw.isChecked = prefs.getBoolean("webhook_enabled", false)
        inputUrl.setText(prefs.getString("webhook_input1", "") ?: "")
        inputHeaders.setText(prefs.getString("webhook_input2", "") ?: "")
        inputTemplate.setText(prefs.getString("webhook_body_template", inputTemplate.text.toString()))
        whitelist.setText(prefs.getString("webhook_whitelist", "") ?: "")
        contains.setText(prefs.getString("webhook_contains", "") ?: "")

        val mIdx = methods.indexOf(prefs.getString("webhook_method", "POST"))
        spMethod.setSelection(if (mIdx>=0) mIdx else 0)
        val tIdx = types.indexOf(prefs.getString("webhook_content_type", "application/json"))
        spType.setSelection(if (tIdx>=0) tIdx else 0)

        
        // === Templates prefill (only-if-empty) ===
        fun defaultHeaders(ct: String): String = when (ct) {
            "application/x-www-form-urlencoded" -> "{\n  'Content-Type': 'application/x-www-form-urlencoded',\n  'User-Agent': 'PushPro/1.0'\n}"
            "text/plain" -> "{\n  'Content-Type': 'text/plain',\n  'User-Agent': 'PushPro/1.0'\n}"
            "application/xml" -> "{\n  'Content-Type': 'application/xml',\n  'User-Agent': 'PushPro/1.0'\n}"
            else -> "{\n  'Content-Type': 'application/json',\n  'User-Agent': 'PushPro/1.0',\n  'X-Device': '\${device}',\n  'X-Channel': '\${channel}'\n}"
        }
        fun defaultBody(ct: String): String = when (ct) {
            "application/x-www-form-urlencoded" -> "title=\${title}&text=\${text}&package=\${package}&time=\${time}"
            "text/plain" -> "\${title} - \${text} (\${package}) @ \${time}"
            "application/xml" -> "<msg><title>\${title}</title><text>\${text}</text><package>\${package}</package><time>\${time}</time></msg>"
            else -> "{\n  'title': '\${title}',\n  'text': '\${text}',\n  'package': '\${package}',\n  'time': '\${time}'\n}"
        }
        fun prefillIfEmpty() {
            val ct = types[spType.selectedItemPosition]
            if (inputHeaders.text.isNullOrBlank()) inputHeaders.setText(defaultHeaders(ct))
            if (inputTemplate.text.isNullOrBlank()) inputTemplate.setText(defaultBody(ct))
        }
        // Prefill on first open
        prefillIfEmpty()
        // Prefill on dropdown changes (only-if-empty)
        val onSel = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefillIfEmpty()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spMethod.onItemSelectedListener = onSel
        spType.onItemSelectedListener = onSel
        // === End templates prefill ===
btnSave.setOnClickListener {
            prefs.edit()
                .putBoolean("webhook_enabled", sw.isChecked)
                .putString("webhook_input1", inputUrl.text.toString())
                .putString("webhook_input2", inputHeaders.text.toString())
                .putString("webhook_body_template", inputTemplate.text.toString())
                .putString("webhook_whitelist", whitelist.text.toString())
                .putString("webhook_contains", contains.text.toString())
                .putString("webhook_method", methods[spMethod.selectedItemPosition])
                .putString("webhook_content_type", types[spType.selectedItemPosition])
                .apply()
            finish()
        }

        btnTest.setOnClickListener {
            val url = inputUrl.text.toString().trim()
            if (url.isBlank()) { Toast.makeText(this, "URL required", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            Thread {
                try {
                    val codeMsg = sendTest(url,
                        methods[spMethod.selectedItemPosition],
                        types[spType.selectedItemPosition],
                        inputTemplate.text.toString(),
                        inputHeaders.text.toString())
                    runOnUiThread {
                    Toast.makeText(this, if (codeMsg.first in 200..299) "Webhook test OK" else "Webhook test failed (HTTP " + codeMsg.first + ")", Toast.LENGTH_SHORT).show()
                }
                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this, "Test failed", Toast.LENGTH_SHORT).show() }
                }
            }.start()
        }
    }

    private fun parseHeaders(src: String): Map<String,String> {
        val out = mutableMapOf<String,String>()
        val s = src.trim()
        if (!(s.startsWith("{") && s.endsWith("}"))) return out
        val re = Regex("""'([^']*)'\s*:\s*'([^']*)'""")
        for (m in re.findAll(s)) out[m.groupValues[1]] = m.groupValues[2]
        return out
    }

    private fun sendTest(urlStr: String, method: String, contentType: String, bodyText: String, headersText: String): Pair<Int,String> {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection)
        conn.requestMethod = method
        val headers = parseHeaders(headersText)
        for ((k,v) in headers) conn.setRequestProperty(k, v)
        if (method != "GET" && method != "HEAD") {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", contentType)
            val sampleTitle = "PushPro Test"
            val sampleText = "It works"
            val samplePkg = "pro.pushpro.app"
            val sampleTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
            val bodyToSend = bodyText
                .replace(Regex("\\$\\{title\\}"), sampleTitle)
                .replace(Regex("\\$\\{text\\}"), sampleText)
                .replace(Regex("\\$\\{package\\}"), samplePkg)
                .replace(Regex("\\$\\{time\\}"), sampleTime)
            conn.outputStream.use { it.write(bodyToSend.toByteArray()) }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.readText() ?: ""
        conn.disconnect()
        if (code in 200..299) LogUtil.append(this, "Webhook sent successfully") else LogUtil.append(this, "Webhook failed: $code")
        return Pair(code, text.take(500))
    }
}
