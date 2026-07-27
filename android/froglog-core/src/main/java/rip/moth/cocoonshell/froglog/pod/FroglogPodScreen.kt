package rip.moth.cocoonshell.froglog.pod

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rip.moth.cocoonshell.froglog.FroglogAuthState
import rip.moth.cocoonshell.froglog.FroglogSyncState
import rip.moth.cocoonshell.froglog.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FroglogPodScreen(
    auth: FroglogAuthState,
    sync: FroglogSyncState,
    wifiOnly: Boolean,
    picnicAutoUpload: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
    onPicnicAutoUploadChange: (Boolean) -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onSync: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val (gradStart, gradEnd) = FroglogPodChrome.breezeGradient()
    val background = Brush.verticalGradient(
        colors = listOf(Color(gradStart.toULong()), Color(gradEnd.toULong())),
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.froglog_pod_title),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.froglog_pod_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCard(auth = auth, sync = sync)
            if (!auth.isSignedIn) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.froglog_username)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.froglog_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(
                    onClick = { onLogin(username, password) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.froglog_login))
                }
                OutlinedButton(
                    onClick = { onRegister(username, password) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.froglog_register))
                }
            } else {
                Button(
                    onClick = onSync,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = sync.pendingCount > 0,
                ) {
                    Text(stringResource(R.string.froglog_sync_now))
                }
                TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.froglog_sign_out))
                }
            }
            SettingRow(
                label = stringResource(R.string.froglog_wifi_only),
                checked = wifiOnly,
                onCheckedChange = onWifiOnlyChange,
            )
            SettingRow(
                label = stringResource(R.string.froglog_picnic_auto_upload),
                checked = picnicAutoUpload,
                onCheckedChange = onPicnicAutoUploadChange,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusCard(auth: FroglogAuthState, sync: FroglogSyncState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (auth.isSignedIn) {
                    stringResource(R.string.froglog_status_signed_in, auth.username.orEmpty())
                } else {
                    stringResource(R.string.froglog_status_signed_out)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.froglog_pending_queue_detail,
                    sync.pendingSessionCount,
                    sync.pendingScreenshotCount,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            sync.lastSyncError?.let { err ->
                Text(text = err, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
