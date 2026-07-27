package rip.moth.cocoonshell.froglog.pod

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Matches Cocoon Pod window chrome (edge-to-edge, light status icons) without depending on app modules.
 */
object FroglogPodChrome {
    fun apply(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        applyLightStatusBar(activity.window, darkBackground = true)
        notifyCocoonPodOpened()
    }

    private fun applyLightStatusBar(window: Window, darkBackground: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = !darkBackground
        controller.isAppearanceLightNavigationBars = !darkBackground
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    }

    private fun notifyCocoonPodOpened() {
        try {
            val kdA = Class.forName("kd.a")
            @Suppress("UNCHECKED_CAST")
            val froglog = java.lang.Enum.valueOf(kdA as Class<out Enum<*>>, "FROGLOG")
            val kdV = Class.forName("kd.v").getDeclaredField("a").get(null)
            kdV.javaClass.getMethod("R", kdA).invoke(kdV, froglog)
        } catch (_: Exception) {
            // Running outside merged Cocoon APK (e.g. injector).
        }
    }

    fun breezeGradient(): Pair<Long, Long> {
        return try {
            val breeze = java.lang.Enum.valueOf(
                Class.forName("mf.c") as Class<out Enum<*>>,
                "BREEZE",
            )
            val start = breeze.javaClass.getMethod("getGradientStart").invoke(breeze) as Long
            val end = breeze.javaClass.getMethod("getGradientEnd").invoke(breeze) as Long
            start to end
        } catch (_: Exception) {
            0xFF1B2A1FL to 0xFF0E1512L
        }
    }
}
