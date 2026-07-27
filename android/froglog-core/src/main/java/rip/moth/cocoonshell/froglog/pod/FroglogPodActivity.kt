package rip.moth.cocoonshell.froglog.pod

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import rip.moth.cocoonshell.froglog.R
import rip.moth.cocoonshell.froglog.auth.FroglogAuthStore
import rip.moth.cocoonshell.froglog.bridge.FroglogRepository

class FroglogPodActivity : ComponentActivity() {
    private lateinit var repo: FroglogRepository
    private lateinit var authStore: FroglogAuthStore
    private val showOutboxState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FroglogPodChrome.apply(this)
        repo = FroglogRepository.get(this)
        authStore = FroglogAuthStore(this)
        showOutboxState.value = intent.getBooleanExtra(EXTRA_OPEN_OUTBOX, false)
        repo.refreshConnectivity()

        setContent {
            val auth by repo.authState.collectAsStateWithLifecycle()
            val sync by repo.syncState.collectAsStateWithLifecycle()
            val outbox = repo.outboxItems()
            val showOutbox by showOutboxState
            FroglogPodScreen(
                auth = auth,
                sync = sync,
                outboxItems = outbox,
                showOutbox = showOutbox,
                wifiOnly = authStore.wifiOnly(),
                onWifiOnlyChange = { authStore.setWifiOnly(it) },
                onLogin = { user, pass ->
                    lifecycleScope.launch {
                        repo.login(user, pass)
                            .onSuccess { toast(getString(R.string.froglog_toast_signed_in)) }
                            .onFailure { toast(it.message ?: getString(R.string.froglog_error_generic)) }
                    }
                },
                onRegister = { user, pass ->
                    lifecycleScope.launch {
                        repo.register(user, pass)
                            .onSuccess { toast(getString(R.string.froglog_toast_registered)) }
                            .onFailure { toast(it.message ?: getString(R.string.froglog_error_generic)) }
                    }
                },
                onSync = {
                    lifecycleScope.launch {
                        if (sync.isOffline) {
                            toast(getString(R.string.froglog_toast_offline_queued))
                            return@launch
                        }
                        repo.syncNow()
                            .onSuccess { toast(getString(R.string.froglog_toast_synced, it)) }
                            .onFailure { e ->
                                if (e.message == "offline") {
                                    toast(getString(R.string.froglog_toast_offline_queued))
                                } else {
                                    toast(e.message ?: getString(R.string.froglog_error_generic))
                                }
                            }
                    }
                },
                onSignOut = {
                    repo.signOut()
                    toast(getString(R.string.froglog_toast_signed_out))
                },
                onOpenOutbox = { showOutboxState.value = true },
                onCloseOutbox = { showOutboxState.value = false },
                onRetryOutboxItem = { key ->
                    lifecycleScope.launch {
                        repo.retryOutboxItem(key)
                            .onSuccess { toast(getString(R.string.froglog_toast_item_synced)) }
                            .onFailure { e ->
                                if (e.message == "offline") {
                                    toast(getString(R.string.froglog_toast_offline_queued))
                                } else {
                                    toast(e.message ?: getString(R.string.froglog_error_generic))
                                }
                            }
                    }
                },
                onDismissOutboxItem = { key ->
                    repo.dismissOutboxItem(key)
                    toast(getString(R.string.froglog_toast_item_removed))
                },
                onBack = { finish() },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        repo.refreshConnectivity()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_OPEN_OUTBOX = "froglog_open_outbox"
    }
}
