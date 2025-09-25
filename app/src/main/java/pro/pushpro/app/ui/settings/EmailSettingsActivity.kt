package pro.pushpro.app.ui.settings
import android.os.Bundle; import android.widget.ArrayAdapter; import android.widget.Toast; import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch; import com.google.android.material.textfield.MaterialAutoCompleteTextView; import pro.pushpro.app.R
class EmailSettingsActivity: AppCompatActivity(){ override fun onCreate(savedInstanceState: Bundle?){ super.onCreate(savedInstanceState); setContentView(R.layout.activity_email_settings)
  val enable=findViewById<MaterialSwitch>(R.id.switchEnableEmail); enable.isChecked=true
  val tls=findViewById<MaterialAutoCompleteTextView>(R.id.spinnerTls); tls.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, resources.getStringArray(R.array.email_tls_modes)))
  tls.setOnClickListener{ tls.showDropDown() }; tls.setOnFocusChangeListener{_,has-> if(has) tls.showDropDown() }
  findViewById<android.view.View>(R.id.btnSendTestEmail).setOnClickListener{ Toast.makeText(this, "Email test sent", Toast.LENGTH_SHORT).show() } } }
