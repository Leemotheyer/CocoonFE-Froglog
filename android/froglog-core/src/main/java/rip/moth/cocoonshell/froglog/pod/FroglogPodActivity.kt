package rip.moth.cocoonshell.froglog.pod

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import rip.moth.cocoonshell.froglog.R
import rip.moth.cocoonshell.froglog.bridge.FroglogRepository
import rip.moth.cocoonshell.froglog.databinding.ActivityFroglogPodBinding

class FroglogPodActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFroglogPodBinding
    private lateinit var repo: FroglogRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFroglogPodBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = FroglogRepository.get(this)

        binding.froglogWifiOnly.isChecked = repo.authState.value.let {
            rip.moth.cocoonshell.froglog.auth.FroglogAuthStore(this).wifiOnly()
        }
        binding.froglogWifiOnly.setOnCheckedChangeListener { _, checked ->
            rip.moth.cocoonshell.froglog.auth.FroglogAuthStore(this).setWifiOnly(checked)
        }

        binding.froglogLogin.setOnClickListener { doLogin() }
        binding.froglogRegister.setOnClickListener { doRegister() }
        binding.froglogSync.setOnClickListener { doSync() }
        binding.froglogSignOut.setOnClickListener {
            repo.signOut()
            render()
        }

        lifecycleScope.launch {
            repo.authState.collect { render() }
        }
        lifecycleScope.launch {
            repo.syncState.collect { render() }
        }
        render()
    }

    private fun render() {
        val auth = repo.authState.value
        val sync = repo.syncState.value
        binding.froglogStatus.text = buildString {
            if (auth.isSignedIn) {
                append(getString(R.string.froglog_status_signed_in, auth.username))
            } else {
                append(getString(R.string.froglog_status_signed_out))
            }
            append("\n")
            append(getString(R.string.froglog_pending_queue, sync.pendingCount))
            sync.lastSyncError?.let { append("\n").append(it) }
        }
        val signedIn = auth.isSignedIn
        binding.froglogUsernameLayout.visibility = if (signedIn) View.GONE else View.VISIBLE
        binding.froglogPasswordLayout.visibility = if (signedIn) View.GONE else View.VISIBLE
        binding.froglogLogin.visibility = if (signedIn) View.GONE else View.VISIBLE
        binding.froglogRegister.visibility = if (signedIn) View.GONE else View.VISIBLE
        binding.froglogSignOut.visibility = if (signedIn) View.VISIBLE else View.GONE
        binding.froglogSync.isEnabled = signedIn
    }

    private fun doLogin() {
        val user = binding.froglogUsername.text?.toString().orEmpty()
        val pass = binding.froglogPassword.text?.toString().orEmpty()
        lifecycleScope.launch {
            repo.login(user, pass)
                .onSuccess { toast("Signed in") }
                .onFailure { toast(it.message ?: getString(R.string.froglog_error_generic)) }
        }
    }

    private fun doRegister() {
        val user = binding.froglogUsername.text?.toString().orEmpty()
        val pass = binding.froglogPassword.text?.toString().orEmpty()
        lifecycleScope.launch {
            repo.register(user, pass)
                .onSuccess { toast("Account created") }
                .onFailure { toast(it.message ?: getString(R.string.froglog_error_generic)) }
        }
    }

    private fun doSync() {
        lifecycleScope.launch {
            repo.syncNow()
                .onSuccess { toast("Uploaded $it session(s)") }
                .onFailure { toast(it.message ?: getString(R.string.froglog_error_generic)) }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
