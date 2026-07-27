package rip.moth.cocoonshell.froglog.pod

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rip.moth.cocoonshell.froglog.FroglogAuthState
import rip.moth.cocoonshell.froglog.FroglogSyncState
import rip.moth.cocoonshell.froglog.R

@Composable
fun FroglogSyncAffordance(
    auth: FroglogAuthState,
    sync: FroglogSyncState,
    onOpenOutbox: () -> Unit,
    onOpenFroglogPod: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headline = when {
        !auth.isSignedIn -> stringResource(R.string.froglog_affordance_sign_in)
        sync.isOffline && sync.pendingCount > 0 -> stringResource(
            R.string.froglog_affordance_queued_offline,
            sync.pendingCount,
        )
        sync.errorCount > 0 -> stringResource(
            R.string.froglog_affordance_needs_attention,
            sync.pendingCount,
            sync.errorCount,
        )
        sync.pendingCount > 0 -> stringResource(R.string.froglog_affordance_pending, sync.pendingCount)
        else -> stringResource(R.string.froglog_affordance_up_to_date)
    }
    val detail = when {
        !auth.isSignedIn -> stringResource(R.string.froglog_affordance_sign_in_detail)
        sync.isOffline && sync.pendingCount > 0 -> stringResource(R.string.froglog_affordance_offline_detail)
        sync.errorCount > 0 -> stringResource(R.string.froglog_affordance_outbox_detail)
        sync.pendingCount > 0 -> stringResource(R.string.froglog_affordance_tap_outbox)
        else -> stringResource(R.string.froglog_affordance_open_pod)
    }
    val icon = when {
        sync.isOffline && sync.pendingCount > 0 -> Icons.Default.CloudOff
        sync.errorCount > 0 -> Icons.Default.ErrorOutline
        sync.pendingCount > 0 -> Icons.Default.CloudQueue
        else -> Icons.Default.CloudQueue
    }
    val onClick = when {
        !auth.isSignedIn -> onOpenFroglogPod
        sync.pendingCount > 0 || sync.errorCount > 0 -> onOpenOutbox
        else -> onOpenFroglogPod
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = headline, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}
