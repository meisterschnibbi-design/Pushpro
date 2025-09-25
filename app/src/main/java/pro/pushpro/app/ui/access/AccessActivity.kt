package pro.pushpro.app.ui.access
import android.Manifest; import android.app.NotificationManager; import android.content.Context; import android.content.Intent; import android.net.Uri
import android.os.Build; import android.os.Bundle; import android.os.PowerManager; import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts; import androidx.appcompat.app.AppCompatActivity
class AccessActivity: AppCompatActivity(){ private val req=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
  override fun onCreate(savedInstanceState: Bundle?){ super.onCreate(savedInstanceState)
    val p=mutableListOf<String>(); if(Build.VERSION.SDK_INT>=33){ p+=Manifest.permission.POST_NOTIFICATIONS; p+=Manifest.permission.READ_MEDIA_IMAGES }
    if(p.isNotEmpty()) req.launch(p.toTypedArray())
    val pm=getSystemService(Context.POWER_SERVICE) as PowerManager
    if(!pm.isIgnoringBatteryOptimizations(packageName)){ startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:"+packageName))) }
    val nm=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if(!nm.areNotificationsEnabled()){ val i=Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply{ putExtra(Settings.EXTRA_APP_PACKAGE, packageName) }; startActivity(i) }
    finish() } }
