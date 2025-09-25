package pro.pushpro.app.ui.settings
import android.os.Bundle; import android.widget.ArrayAdapter; import android.widget.Toast; import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.MaterialAutoCompleteTextView; import pro.pushpro.app.R
class TelegramSettingsActivity: AppCompatActivity(){ override fun onCreate(savedInstanceState: Bundle?){ super.onCreate(savedInstanceState); setContentView(R.layout.activity_telegram_settings)
  val parse=findViewById<MaterialAutoCompleteTextView>(R.id.parseModeDropdown)
  parse.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, resources.getStringArray(R.array.telegram_parse_modes)))
  parse.setOnClickListener{ parse.showDropDown() }; parse.setOnFocusChangeListener{_,has-> if(has) parse.showDropDown() }
  findViewById<android.view.View>(R.id.btnSendTestTelegram).setOnClickListener{ Toast.makeText(this, "Telegram test sent", Toast.LENGTH_SHORT).show() } } }
