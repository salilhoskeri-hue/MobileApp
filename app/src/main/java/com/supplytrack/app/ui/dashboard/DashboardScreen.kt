package com.supplytrack.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supplytrack.app.ui.AppViewModelProvider
import com.supplytrack.app.ui.common.KpiCard
import com.supplytrack.app.ui.common.LowStockBadge
import com.supplytrack.app.ui.common.ShipmentTypeIcon
import com.supplytrack.app.ui.common.StatusBadge
import com.supplytrack.app.ui.common.formatCurrency
import com.supplytrack.app.ui.common.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onProductClick: (Long) -> Unit,
    onOpenInventory: () -> Unit,
    onOpenShipments: () -> Unit,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Supply overview") }) }) { padding ->
        if (state.loading) {
            Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.padding(32.dp))
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        label = "Products",
                        value = state.metrics.skuCount.toString(),
                        icon = Icons.Filled.Inventory2,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenInventory,
                    )
                    KpiCard(
                        label = "Inventory value",
                        value = formatCurrency(state.metrics.totalValue),
                        icon = Icons.Filled.Payments,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        label = "Low stock",
                        value = state.metrics.lowStockCount.toString(),
                        icon = Icons.Filled.Warning,
                        modifier = Modifier.weight(1f),
                        highlight = state.metrics.lowStockCount > 0,
                        onClick = onOpenInventory,
                    )
                    KpiCard(
                        label = "In transit · ${state.pendingCount} pending",
                        value = state.inTransitCount.toString(),
                        icon = Icons.Filled.LocalShipping,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenShipments,
                    )
                }
            }

            item { SectionTitle("Reorder alerts", actionLabel = "Inventory", onAction = onOpenInventory) }
            if (state.lowStock.isEmpty()) {
                item { AllClearCard("All products are above their reorder point.") }
            } else {
                items(state.lowStock, key = { "p${it.id}" }) { product ->
                    Card(onClick = { onProductClick(product.id) }) {
                        ListItem(
                            headlineContent = { Text(product.name) },
                            supportingContent = {
                                Text("${product.sku} · ${product.quantity} ${product.unit} on hand · reorder at ${product.reorderPoint}")
                            },
                            trailingContent = { LowStockBadge(outOfStock = product.quantity <= 0) },
                        )
                    }
                }
            }

            item { SectionTitle("Active shipments", actionLabel = "Shipments", onAction = onOpenShipments) }
            if (state.activeShipments.isEmpty()) {
                item { AllClearCard("No pending or in-transit shipments.") }
            } else {
                items(state.activeShipments, key = { "s${it.shipment.id}" }) { details ->
                    val s = details.shipment
                    Card(onClick = onOpenShipments) {
                        ListItem(
                            leadingContent = {
                                ShipmentTypeIcon(s.type)
                            },
                            headlineContent = { Text("${s.reference} · ${details.productName}") },
                            supportingContent = {
                                val eta = s.expectedAt?.let { " · ETA ${formatDate(it)}" } ?: ""
                                Text("${s.quantity} ${details.productUnit} · ${s.partner}$eta")
                            },
                            trailingContent = { StatusBadge(s.status) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun AllClearCard(message: String) {
    Card {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
