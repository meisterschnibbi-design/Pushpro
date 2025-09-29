
package com.pushpro.app.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R
import com.pushpro.app.util.LogUtil

class StatisticsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        val tv = findViewById<TextView>(R.id.txtStats)
        val logs = LogUtil.getAll(this)
        val lines = logs.split("\n".toRegex()).filter { it.isNotBlank() }
        val total = lines.size
        val errors = lines.count { it.contains("FAILED") }
        val tg = lines.count { it.contains("Telegram ") }
        val wh = lines.count { it.contains("Webhook ") }
        val em = lines.count { it.contains("Email ") }

        tv.text = "Total: %d\nErrors: %d\nTelegram: %d\nWebhook: %d\nEmail: %d".format(total, errors, tg, wh, em)
    }
}
