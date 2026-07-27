package rip.moth.cocoonshell.froglog.pod

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import rip.moth.cocoonshell.froglog.R
import rip.moth.cocoonshell.froglog.auth.FroglogAuthStore
import rip.moth.cocoonshell.froglog.bridge.FroglogRepository

class FroglogPodActivity : ComponentActivity() {
    private lateinit var repo: FroglogRepository
    private lateinit var authStore: FroglogAuthStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FroglogPodChrome.apply(this)
        repo = FroglogRepository.get(this)
        authStore = FroglogAuthStore(this)

        setContent {
            val auth by repo.authState.collectAsStateWithLifecycle()
            val sync by repo.syncState.collectAsStateWithLifecycle()
            val scheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = scheme) {
                FroglogPodScreen(
                    auth = auth,
                    sync = sync,
                    wifiOnly = authStore.wifiOnly(),
                    picnicAutoUpload = authStore.picnicAutoUpload(),
                    onWifiOnlyChange = { authStore.setWifiOnly(it) },
                    onPicnicAutoUploadChange = { authStore.setPicnicAutoUpload(it) },
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
                            repo.syncNow()
                                .onSuccess { toast(getString(R.string.froglog_toast_synced, it)) }
                                .onFailure { toast(it.message ?: getString(R.string.froglog_error_generic)) }
                        }
                    },
                    onSignOut = {
                        repo.signOut()
                        toast(getString(R.string.froglog_toast_signed_out))
                    },
                    onBack = { finish() },
                )
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
