
package com.pushpro.app.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.pushpro.R

class BlacklistActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blacklist)

        val rv = findViewById<RecyclerView>(R.id.recyclerApps)
        rv.layoutManager = LinearLayoutManager(this)

        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 } // user apps first pass
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }

        val prefs = getSharedPreferences("pushpro_prefs", MODE_PRIVATE)
        val blocked = prefs.getStringSet("blacklist_set", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        rv.adapter = object : RecyclerView.Adapter<AppVH>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): AppVH {
                val v = layoutInflater.inflate(R.layout.row_app_item, parent, false)
                return AppVH(v as android.widget.LinearLayout)
            }
            override fun getItemCount(): Int = apps.size
            override fun onBindViewHolder(holder: AppVH, position: Int) {
                val ai = apps[position]
                val name = pm.getApplicationLabel(ai).toString()
                val pkg = ai.packageName
                holder.bind(name, pkg, pm.getApplicationIcon(ai), blocked.contains(pkg)) { isChecked ->
                    if (isChecked) blocked.add(pkg) else blocked.remove(pkg)
                    prefs.edit().putStringSet("blacklist_set", blocked).apply()
                }
            }
        }
    }
}

class AppVH(private val root: android.widget.LinearLayout) : RecyclerView.ViewHolder(root) {
    private val icon: android.widget.ImageView = root.findViewById(R.id.appIcon)
    private val title: android.widget.TextView = root.findViewById(R.id.appTitle)
    private val subtitle: android.widget.TextView = root.findViewById(R.id.appPackage)
    private val toggle: SwitchMaterial = root.findViewById(R.id.appToggle)

    fun bind(name: String, pkg: String, ic: android.graphics.drawable.Drawable, checked: Boolean, onToggle: (Boolean)->Unit) {
        icon.setImageDrawable(ic)
        title.text = name
        subtitle.text = pkg
        toggle.setOnCheckedChangeListener(null)
        toggle.isChecked = checked
        toggle.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean -> onToggle(isChecked) }
    }
}
