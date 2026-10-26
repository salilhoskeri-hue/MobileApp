package com.supplytrack.app.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supplytrack.app.data.Product
import com.supplytrack.app.data.StockMovement
import com.supplytrack.app.data.Supplier
import com.supplytrack.app.data.isLowStock
import com.supplytrack.app.ui.AppViewModelProvider
import com.supplytrack.app.ui.common.BackButton
import com.supplytrack.app.ui.common.ConfirmDeleteDialog
import com.supplytrack.app.ui.common.DropdownField
import com.supplytrack.app.ui.common.FormField
import com.supplytrack.app.ui.common.LowStockBadge
import com.supplytrack.app.ui.common.SectionHeader
import com.supplytrack.app.ui.common.formatCurrency
import com.supplytrack.app.ui.common.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    onDone: () -> Unit,
    viewModel: ProductEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val product by viewModel.product.collectAsStateWithLifecycle()
    val movements by viewModel.movements.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var adjustDirection by rememberSaveable { mutableStateOf(0) } // +1 receive, -1 issue, 0 closed

    LaunchedEffect(viewModel.finished) { if (viewModel.finished) onDone() }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "New product" else "Product") },
                navigationIcon = { BackButton(onDone) },
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete product")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!viewModel.loaded) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            product?.let { p ->
                StockCard(
                    product = p,
                    onReceive = { adjustDirection = 1 },
                    onIssue = { adjustDirection = -1 },
                )
            }

            SectionHeader("Details")
            ProductFields(viewModel, suppliers)

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(if (viewModel.isNew) "Add product" else "Save changes")
            }

            if (!viewModel.isNew) {
                SectionHeader("Stock history")
                if (movements.isEmpty()) {
                    Text("No stock movements yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Card {
                        movements.forEachIndexed { i, m ->
                            if (i > 0) HorizontalDivider()
                            MovementRow(m)
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = "Delete product?",
            message = "This also removes its shipments and stock history.",
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }

    if (adjustDirection != 0) {
        AdjustStockDialog(
            receiving = adjustDirection > 0,
            unit = product?.unit ?: "",
            onConfirm = { qty, reason ->
                viewModel.adjustStock(qty * adjustDirection, reason)
                adjustDirection = 0
            },
            onDismiss = { adjustDirection = 0 },
        )
    }
}

@Composable
private fun StockCard(product: Product, onReceive: () -> Unit, onIssue: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("On hand", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${product.quantity} ${product.unit}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Reorder at ${product.reorderPoint} · value ${formatCurrency(product.quantity * product.unitCost)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (product.isLowStock()) LowStockBadge(outOfStock = product.quantity <= 0)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onReceive, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Receive", Modifier.padding(start = 4.dp))
                }
                OutlinedButton(onClick = onIssue, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Remove, contentDescription = null)
                    Text("Issue", Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun ProductFields(viewModel: ProductEditViewModel, suppliers: List<Supplier>) {
    val f = viewModel.form
    val err = viewModel.showErrors
    FormField(f.name, { v -> viewModel.update { it.copy(name = v) } }, "Name *", isError = err && f.name.isBlank())
    FormField(f.sku, { v -> viewModel.update { it.copy(sku = v) } }, "SKU *", isError = err && f.sku.isBlank())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormField(f.category, { v -> viewModel.update { it.copy(category = v) } }, "Category", Modifier.weight(1f))
        FormField(f.unit, { v -> viewModel.update { it.copy(unit = v) } }, "Unit *", Modifier.weight(1f), isError = err && f.unit.isBlank())
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormField(
            f.reorderPoint, { v -> viewModel.update { it.copy(reorderPoint = v) } }, "Reorder point", Modifier.weight(1f),
            keyboardType = KeyboardType.Number, isError = err && (f.reorderPointValue ?: -1) < 0,
        )
        FormField(
            f.unitCost, { v -> viewModel.update { it.copy(unitCost = v) } }, "Unit cost", Modifier.weight(1f),
            keyboardType = KeyboardType.Decimal, isError = err && (f.unitCostValue ?: -1.0) < 0.0,
        )
    }
    FormField(f.location, { v -> viewModel.update { it.copy(location = v) } }, "Warehouse bin / location")
    DropdownField(
        label = "Supplier",
        options = listOf<Supplier?>(null) + suppliers,
        selected = suppliers.firstOrNull { it.id == f.supplierId },
        optionLabel = { it?.name ?: "None" },
        onSelect = { s -> viewModel.update { it.copy(supplierId = s?.id) } },
    )
    if (viewModel.isNew) {
        FormField(
            f.openingStock, { v -> viewModel.update { it.copy(openingStock = v) } }, "Opening stock",
            keyboardType = KeyboardType.Number, isError = err && (f.openingStockValue ?: -1) < 0,
            supportingText = "Later changes go through Receive / Issue so they're logged.",
        )
    }
}

@Composable
private fun MovementRow(m: StockMovement) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(m.reason, style = MaterialTheme.typography.bodyMedium)
            Text(formatDateTime(m.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            if (m.delta > 0) "+${m.delta}" else m.delta.toString(),
            fontWeight = FontWeight.SemiBold,
            color = if (m.delta > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun AdjustStockDialog(
    receiving: Boolean,
    unit: String,
    onConfirm: (quantity: Int, reason: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var qty by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    val qtyValue = qty.trim().toIntOrNull()
    val valid = qtyValue != null && qtyValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (receiving) "Receive stock" else "Issue stock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(qty, { qty = it }, "Quantity ($unit)", keyboardType = KeyboardType.Number, isError = qty.isNotEmpty() && !valid)
                FormField(reason, { reason = it }, "Reason (optional)")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(qtyValue!!, reason.trim()) }, enabled = valid) {
                Text(if (receiving) "Receive" else "Issue")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
