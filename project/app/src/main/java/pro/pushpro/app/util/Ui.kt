
package pro.pushpro.app.util

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun applyStatusBarInset(root: View) {
    ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        v.updatePadding(top = v.paddingTop + top)
        insets
    }
}

fun setSystemBars(activity: Activity, statusColor: Int, navColor: Int) {
    activity.window.statusBarColor = statusColor
    activity.window.navigationBarColor = navColor
}
