package com.example.sande.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sande.data.entities.Movement
import com.example.sande.data.entities.MovementType
import com.example.sande.di.SandeApplication
import com.example.sande.ui.EmptyStateCard
import com.example.sande.ui.formatKes
import com.example.sande.ui.formatSaleDate
import com.example.sande.ui.theme.SandeShape
import com.example.sande.ui.viewModelFactoryOf
import com.example.sande.ui.viewmodel.AuditViewerViewModel

@Composable
fun AuditViewerScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as SandeApplication
    val vm: AuditViewerViewModel = viewModel(
        factory = viewModelFactoryOf { AuditViewerViewModel(app.movementRepository) }
    )
    val movements by vm.movements.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Audit log", style = MaterialTheme.typography.headlineSmall)
        if (movements.isEmpty()) {
            EmptyStateCard(
                title = "No audit records",
                subtitle = "Inventory movements will appear here",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(movements, key = { it.id }) { m -> AuditRow(m) }
            }
        }
    }
}

@Composable
private fun AuditRow(movement: Movement) {
    val typeColor: Color = when (movement.type) {
        MovementType.SALE -> MaterialTheme.colorScheme.primary
        MovementType.RECEIVE -> MaterialTheme.colorScheme.secondary
        MovementType.ADJUST -> MaterialTheme.colorScheme.tertiary
        MovementType.VOID -> MaterialTheme.colorScheme.error
    }
    val label = movement.type.name.lowercase().replaceFirstChar { it.uppercase() }
    val valueCents = (movement.unitPriceCents ?: 0L) * movement.quantity

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SandeShape.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, color = typeColor)
                Text(
                    "Product: ${movement.productId} · Qty: ${movement.quantity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    formatSaleDate(movement.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (valueCents != 0L) {
                Text(formatKes(valueCents), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
