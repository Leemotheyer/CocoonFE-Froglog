package rip.moth.cocoonshell.froglog.pod

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rip.moth.cocoonshell.froglog.FroglogOutboxItem
import rip.moth.cocoonshell.froglog.FroglogOutboxKind
import rip.moth.cocoonshell.froglog.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FroglogOutboxScreen(
    items: List<FroglogOutboxItem>,
    isOffline: Boolean,
    onBack: () -> Unit,
    onRetry: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onSyncAll: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.froglog_outbox_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isOffline) {
                Text(
                    text = stringResource(R.string.froglog_outbox_offline_banner),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.froglog_outbox_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                OutlinedButton(onClick = onSyncAll, modifier = Modifier.fillMaxWidth(), enabled = !isOffline) {
                    Text(stringResource(R.string.froglog_sync_now))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items, key = { it.key }) { item ->
                        OutboxRow(item = item, onRetry = onRetry, onDismiss = onDismiss, isOffline = isOffline)
                    }
                }
            }
        }
    }
}

@Composable
private fun OutboxRow(
    item: FroglogOutboxItem,
    onRetry: (String) -> Unit,
    onDismiss: (String) -> Unit,
    isOffline: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = when (item.kind) {
                    FroglogOutboxKind.SESSION -> stringResource(R.string.froglog_outbox_kind_session, item.subtitle)
                    FroglogOutboxKind.SCREENSHOT -> stringResource(R.string.froglog_outbox_kind_screenshot, item.subtitle)
                },
                style = MaterialTheme.typography.bodySmall,
            )
            item.error?.let { err ->
                Text(text = err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onDismiss(item.key) }) {
                    Text(stringResource(R.string.froglog_outbox_remove))
                }
                OutlinedButton(
                    onClick = { onRetry(item.key) },
                    enabled = !isOffline,
                ) {
                    Text(stringResource(R.string.froglog_outbox_retry))
                }
            }
        }
    }
}
