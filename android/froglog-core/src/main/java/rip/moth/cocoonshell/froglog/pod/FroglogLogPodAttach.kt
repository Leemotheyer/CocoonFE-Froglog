package rip.moth.cocoonshell.froglog.pod

import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import rip.moth.cocoonshell.froglog.bridge.FroglogRepository

/** Floating Froglog sync row on Log Pod (patched from LogPodActivity). */
object FroglogLogPodAttach {
    @JvmStatic
    fun attach(activity: Activity) {
        if (activity !is ComponentActivity) return
        if (activity.findViewById<ComposeView>(VIEW_ID) != null) return
        val repo = FroglogRepository.get(activity)
        repo.refreshConnectivity()
        val composeView = ComposeView(activity).apply {
            id = VIEW_ID
            setViewTreeLifecycleOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
            setContent {
                FroglogCocoonTheme {
                    val auth by repo.authState.collectAsStateWithLifecycle()
                    val sync by repo.syncState.collectAsStateWithLifecycle()
                    FroglogSyncAffordance(
                        auth = auth,
                        sync = sync,
                        onOpenOutbox = { FroglogPodLauncher.openOutbox(activity) },
                        onOpenFroglogPod = { FroglogPodLauncher.open(activity) },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
        val root = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ).apply { gravity = Gravity.TOP }
        root.addView(composeView, params)
    }

    private const val VIEW_ID = 0x7f0f0f01
}
