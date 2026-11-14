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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sande.di.SandeApplication
import com.example.sande.ui.EmptyStateCard
import com.example.sande.ui.theme.SandeShape
import com.example.sande.ui.viewModelFactoryOf
import com.example.sande.ui.viewmodel.ReplenishmentAlertsViewModel
import com.example.sande.ui.viewmodel.StockAlert

@Composable
fun ReplenishmentAlertsScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as SandeApplication
    val vm: ReplenishmentAlertsViewModel = viewModel(
        factory = viewModelFactoryOf {
            ReplenishmentAlertsViewModel(app.movementRepository, app.productRepository)
        }
    )
    val alerts by vm.lowStockItems.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Low stock alerts", style = MaterialTheme.typography.headlineSmall)
        if (alerts.isEmpty()) {
            EmptyStateCard(
                title = "All stock levels are healthy",
                subtitle = "No products need replenishment",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            Text(
                "${alerts.size} product(s) need attention",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alerts, key = { it.product.id }) { alert -> AlertRow(alert) }
            }
        }
    }
}

@Composable
private fun AlertRow(alert: StockAlert) {
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
                Text(alert.product.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(
                    "On hand: ${alert.balance} units",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("Low", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
        }
    }
}
