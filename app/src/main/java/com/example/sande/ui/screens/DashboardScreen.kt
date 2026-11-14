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
import com.example.sande.ui.formatKes
import com.example.sande.ui.theme.SandeShape
import com.example.sande.ui.viewModelFactoryOf
import com.example.sande.ui.viewmodel.StockViewModel

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as SandeApplication
    val vm: StockViewModel = viewModel(
        factory = viewModelFactoryOf { StockViewModel(app.movementRepository, app.productRepository) }
    )
    val rows by vm.rows.collectAsState()

    val stockValueCents = rows.sumOf { it.balance.coerceAtLeast(0L) * it.product.priceCents }
    val lowStockCount = rows.count { it.balance <= 5L }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineSmall)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiCard("Stock value", "KES ${formatKes(stockValueCents)}", Modifier.weight(1f))
            KpiCard("Low stock", lowStockCount.toString(), Modifier.weight(1f))
            KpiCard("Products", rows.size.toString(), Modifier.weight(1f))
        }

        Text("Inventory", style = MaterialTheme.typography.titleMedium)

        if (rows.isEmpty()) {
            EmptyStateCard("No products yet", "Add samples from the Catalogue tab")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows, key = { it.product.id }) { row -> InventoryRow(row) }
            }
        }
    }
}

@Composable
private fun KpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = SandeShape.card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun InventoryRow(row: StockViewModel.StockRow) {
    val low = row.balance <= 5L
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
                Text(row.product.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(
                    "SKU: ${row.product.sku ?: "N/A"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${row.balance}", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (low) "Low" else "OK",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (low) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
