package rip.moth.cocoonshell.froglog.pod

import android.content.Context
import android.content.Intent
import rip.moth.cocoonshell.froglog.pod.FroglogPodActivity

/** Launches Froglog Pod (used from patched Cocoon pod menu smali). */
object FroglogPodLauncher {
    @JvmStatic
    fun open(context: Context) {
        val intent = Intent(context, FroglogPodActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
