package pro.pushpro.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import pro.pushpro.app.R
import pro.pushpro.app.util.setSystemBars

class LogViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)

        // Keep bars black, but DON'T add extra top padding (no double black bar)
        setSystemBars(this, ContextCompat.getColor(this, R.color.black), ContextCompat.getColor(this, R.color.black))

        
                                        
                // Apply bottom inset so nav bar doesn't cover buttons
        val sw: Switch = findViewById(R.id.switchLogsEnabled)
        val recycler: RecyclerView = findViewById(R.id.recyclerLogs)
        val btnClear: Button = findViewById(R.id.btnClear)
        val btnSend: Button = findViewById(R.id.btnSend)

        recycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        val adapter = LogsAdapter(mutableListOf())
        recycler.adapter = adapter

        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        sw.isChecked = prefs.getBoolean("logs_enabled", true)

        fun reload() {
            val enabled = prefs.getBoolean("logs_enabled", true)
            sw.isChecked = enabled
            if (!enabled) {
                adapter.submit(mutableListOf("Logging disabled"))
                return
            }
            val joined = prefs.getString("logs", "") ?: ""
            val items = joined.replace("\r\n", "\n")
                .split("\n")
                .filter { it.isNotBlank() }
                .toMutableList()
            if (items.isEmpty()) items.add("No logs yet")
            adapter.submit(items)
            recycler.scrollToPosition(adapter.itemCount - 1)
        }

        sw.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("logs_enabled", isChecked).apply()
            reload()
        })

        btnClear.setOnClickListener {
            prefs.edit().remove("logs").apply()
            reload()
        }

        btnSend.setOnClickListener {
            val all = prefs.getString("logs", "") ?: "No logs"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "PushPro Logs")
                putExtra(Intent.EXTRA_TEXT, all)
            }
            startActivity(Intent.createChooser(intent, "Send logs as .txt"))
        }

        reload()
    }
}

class LogsAdapter(private var data: MutableList<String>) : RecyclerView.Adapter<LogRowVH>() {
    fun submit(items: MutableList<String>) {
        data = items
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): LogRowVH {
        val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.row_log_item, parent, false)
        return LogRowVH(v as android.widget.TextView)
    }
    override fun onBindViewHolder(holder: LogRowVH, position: Int) {
        holder.bind(data[position])
    }
    override fun getItemCount(): Int = data.size
}

class LogRowVH(private val tv: android.widget.TextView) : RecyclerView.ViewHolder(tv) {
    fun bind(text: String) { tv.text = text; tv.setTextIsSelectable(true) }
}