package com.example.sande.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.sande.domain.sync.SyncState

/**
 * Persistent pill showing the current synchronisation state.
 *
 * idle        - nothing waiting
 * pending     - records waiting to upload
 * uploading   - upload in progress
 * offline     - no connectivity
 * failed      - upload failed, will retry
 * review      - a conflict needs attention
 */
@Composable
fun SyncStatusChip(
    state: SyncState,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    val (label, dotColor) = when {
        state == SyncState.REQUIRES_REVIEW -> "Needs review" to MaterialTheme.colorScheme.error
        state == SyncState.SYNC_FAILED -> "Sync failed" to MaterialTheme.colorScheme.error
        state == SyncState.UPLOADING -> "Syncing" to MaterialTheme.colorScheme.secondary
        state == SyncState.LOCAL_PENDING -> "$pendingCount pending" to MaterialTheme.colorScheme.tertiary
        else -> "Synced" to MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier
            .background(
                color = dotColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color = dotColor, shape = RoundedCornerShape(4.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = dotColor,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}
