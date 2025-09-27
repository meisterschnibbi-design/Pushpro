package com.pushpro.app.ui


import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pushpro.R


class DiagnosticsActivity : AppCompatActivity() {
override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
setContentView(R.layout.activity_diagnostics)


val txtDiagnostics: TextView? = findViewById(R.id.txtDiagnostics)
val btnRunDiagnostics: Button? = findViewById(R.id.btnRunDiagnostics)


btnRunDiagnostics?.setOnClickListener {
txtDiagnostics?.text = getString(R.string.diagnostics_running)
// TODO: Diagnose-Checks ausführen
}
}
}