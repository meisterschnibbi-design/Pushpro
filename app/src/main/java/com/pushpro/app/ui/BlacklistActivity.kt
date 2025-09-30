package com.pushpro.app.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.pushpro.R

class BlacklistActivity : AppCompatActivity() {

    private lateinit var adapter: AppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blacklist)

        val recycler = findViewById<RecyclerView>(R.id.recyclerApps)
        recycler.layoutManager = LinearLayoutManager(this)

        // Aktuellen Blacklist-Stand laden (Kopie machen – getStringSet liefert Live-View!)
        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        val blocked = (prefs.getStringSet("blacklist_set", emptySet()) ?: emptySet()).toMutableSet()

        // Apps wie in "Einstellungen → Apps": nur installierte, bevorzugt launchbare / nicht System
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            // Bevorzugt nur Apps mit Launcher-Intent (sichtbare/benutzbare Apps)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null || it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { ai ->
                val name = runCatching { pm.getApplicationLabel(ai).toString() }.getOrDefault(ai.packageName)
                val icon = runCatching { pm.getApplicationIcon(ai) }.getOrNull()
                AppItem(name = name, pkg = ai.packageName, icon = icon)
            }
            .distinctBy { it.pkg }
            .sortedBy { it.name.lowercase() }
            .toList()

        adapter = AppsAdapter(apps, blocked) { pkg, shouldBlock ->
            val newSet = blocked.toMutableSet()
            if (shouldBlock) newSet.add(pkg) else newSet.remove(pkg)
            // WICHTIG: immer eine NEUE Menge schreiben (nicht die alte mutieren)
            prefs.edit().putStringSet("blacklist_set", newSet).apply()
            blocked.clear(); blocked.addAll(newSet)
        }

        recycler.adapter = adapter
    }
}

data class AppItem(
    val name: String,
    val pkg: String,
    val icon: Drawable?
)

private class AppsAdapter(
    initial: List<AppItem>,
    private val blocked: Set<String>,
    private val onToggle: (pkg: String, shouldBlock: Boolean) -> Unit
) : RecyclerView.Adapter<AppVH>() {

    private var items: List<AppItem> = initial

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): AppVH {
        val v = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.row_app_item, parent, false) as android.widget.LinearLayout
        return AppVH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: AppVH, position: Int) {
        val app = items[position]
        holder.bind(
            name = app.name,
            pkg = app.pkg,
            ic = app.icon,
            checked = blocked.contains(app.pkg)
        ) { isChecked ->
            onToggle(app.pkg, isChecked)
        }
    }

    fun submit(newItems: List<AppItem>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                items[oldItemPosition].pkg == newItems[newItemPosition].pkg
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                items[oldItemPosition] == newItems[newItemPosition]
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }
}

private class AppVH(private val root: android.widget.LinearLayout) : RecyclerView.ViewHolder(root) {
    private val icon: android.widget.ImageView = root.findViewById(R.id.appIcon)
    private val title: android.widget.TextView = root.findViewById(R.id.appTitle)
    private val subtitle: android.widget.TextView = root.findViewById(R.id.appPackage)
    private val toggle: SwitchMaterial = root.findViewById(R.id.appToggle)

    fun bind(
        name: String,
        pkg: String,
        ic: Drawable?,
        checked: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        icon.setImageDrawable(ic ?: root.context.getDrawable(R.mipmap.ic_launcher))
        title.text = name
        subtitle.text = pkg

        // Listener resetten, State setzen, Listener wieder setzen -> vermeidet Ghost-Events
        toggle.setOnCheckedChangeListener(null)
        toggle.isChecked = checked
        toggle.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            onToggle(isChecked)
        }
    }
}
