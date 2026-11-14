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
import com.example.sande.data.entities.SaleHeader
import com.example.sande.di.SandeApplication
import com.example.sande.ui.EmptyStateCard
import com.example.sande.ui.formatKes
import com.example.sande.ui.formatSaleDate
import com.example.sande.ui.theme.SandeShape
import com.example.sande.ui.viewModelFactoryOf
import com.example.sande.ui.viewmodel.SaleHistoryViewModel

@Composable
fun SaleHistoryScreen(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as SandeApplication
    val vm: SaleHistoryViewModel = viewModel(
        factory = viewModelFactoryOf { SaleHistoryViewModel(app.saleHeaderRepository) }
    )
    val sales by vm.sales.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("Sale history", style = MaterialTheme.typography.headlineSmall)
        if (sales.isEmpty()) {
            EmptyStateCard(
                title = "No sales yet",
                subtitle = "Record your first sale to see history here",
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sales, key = { it.id }) { sale -> SaleRow(sale) }
            }
        }
    }
}

@Composable
private fun SaleRow(sale: SaleHeader) {
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
                Text(sale.receiptNumber, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(
                    formatSaleDate(sale.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "KES ${formatKes(sale.totalCents)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
