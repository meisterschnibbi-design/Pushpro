package com.pushpro.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pushpro.R
import com.pushpro.app.util.LogUtil

class OfflineQueueActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_queue)

        val btnResend: Button = findViewById(R.id.btnResendAll)
        val btnClear: Button = findViewById(R.id.btnClearQueue)
        val swEnable: Switch = findViewById(R.id.switchQueueEnabled)
        val list: RecyclerView = findViewById(R.id.recyclerQueue)

        list.layoutManager = LinearLayoutManager(this)
        val adapter = QueueAdapter(mutableListOf())
        list.adapter = adapter

        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        swEnable.isChecked = prefs.getBoolean("queue_enabled", false)

        fun loadQueue(): MutableList<String> {
            val joined = prefs.getString("queue", "") ?: ""
            val items = joined.replace("\r\n", "\n")
                .split("\n")
                .filter { it.isNotBlank() }
                .toMutableList()
            if (items.isEmpty()) items.add("Queue empty")
            return items
        }

        adapter.submit(loadQueue())

        swEnable.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("queue_enabled", checked).apply()
        }

        btnResend.setOnClickListener {
            val webhookEnabled  = prefs.getBoolean("webhook_enabled", false)
            val emailEnabled    = prefs.getBoolean("email_enabled", false) || prefs.getBoolean("email_input_enabled", false)
            val telegramEnabled = prefs.getBoolean("telegram_enabled", false)

            val q = loadQueue()
            LogUtil.append(
                this,
                "Resend requested: queue=${q.size}, channels: " + listOfNotNull(
                    if (webhookEnabled) "webhook" else null,
                    if (emailEnabled) "email" else null,
                    if (telegramEnabled) "telegram" else null
                ).joinToString(",")
            )

            // TODO: Hier würden die Einträge tatsächlich gesendet werden
        }

        btnClear.setOnClickListener {
            prefs.edit().remove("queue").apply()
            adapter.submit(loadQueue())
            LogUtil.append(this, "Queue cleared by user")
        }
    }
}

class QueueAdapter(private var data: MutableList<String>) : RecyclerView.Adapter<QueueVH>() {
    fun submit(items: MutableList<String>) { data = items; notifyDataSetChanged() }
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): QueueVH {
        val v = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.row_queue_item, parent, false)
        return QueueVH(v as android.widget.TextView)
    }
    override fun onBindViewHolder(holder: QueueVH, position: Int) { holder.bind(data[position]) }
    override fun getItemCount(): Int = data.size
}

class QueueVH(private val tv: android.widget.TextView) : RecyclerView.ViewHolder(tv) {
    fun bind(text: String) { tv.text = text }
}
