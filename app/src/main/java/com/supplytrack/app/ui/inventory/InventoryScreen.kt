package com.supplytrack.app.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supplytrack.app.data.Product
import com.supplytrack.app.data.isLowStock
import com.supplytrack.app.ui.AppViewModelProvider
import com.supplytrack.app.ui.common.EmptyState
import com.supplytrack.app.ui.common.LowStockBadge
import com.supplytrack.app.ui.common.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onProductClick: (Long) -> Unit,
    onAddProduct: () -> Unit,
    viewModel: InventoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val lowOnly by viewModel.lowStockOnly.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Inventory") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProduct,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Product") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search name, SKU, category, bin") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = lowOnly,
                    onClick = viewModel::toggleLowStockOnly,
                    label = { Text("Low stock only") },
                )
                Text(
                    "${state.products.size} of ${state.totalCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
            }

            if (!state.loading && state.products.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Inventory2,
                    title = if (state.totalCount == 0) "No products yet" else "No matches",
                    message = if (state.totalCount == 0) "Add your first product to start tracking stock." else "Try a different search or filter.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.products, key = { it.id }) { product ->
                        ProductCard(product, onClick = { onProductClick(product.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOf(product.sku, product.category, product.location.takeIf { it.isNotBlank() }?.let { "Bin $it" })
                        .filter { !it.isNullOrBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (product.isLowStock()) LowStockBadge(outOfStock = product.quantity <= 0)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${product.quantity}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (product.isLowStock()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Text(product.unit, style = MaterialTheme.typography.labelSmall)
                Text(
                    formatCurrency(product.quantity * product.unitCost),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
