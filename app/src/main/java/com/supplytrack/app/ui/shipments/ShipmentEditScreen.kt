package com.supplytrack.app.ui.shipments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supplytrack.app.data.ShipmentType
import com.supplytrack.app.ui.AppViewModelProvider
import com.supplytrack.app.ui.common.BackButton
import com.supplytrack.app.ui.common.DropdownField
import com.supplytrack.app.ui.common.FormField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipmentEditScreen(
    onDone: () -> Unit,
    viewModel: ShipmentEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val f = viewModel.form
    val err = viewModel.showErrors
    val inbound = f.type == ShipmentType.INBOUND

    LaunchedEffect(viewModel.finished) { if (viewModel.finished) onDone() }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("New shipment") }, navigationIcon = { BackButton(onDone) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val types = ShipmentType.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                types.forEachIndexed { i, type ->
                    SegmentedButton(
                        selected = f.type == type,
                        onClick = { viewModel.setType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = types.size),
                    ) {
                        Text(if (type == ShipmentType.INBOUND) "Inbound (purchase)" else "Outbound (sale)")
                    }
                }
            }
            Text(
                if (inbound) "Stock is added when the shipment is received."
                else "Stock is removed when the shipment is dispatched.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DropdownField(
                label = "Product *",
                options = products,
                selected = products.firstOrNull { it.id == f.productId },
                optionLabel = { "${it.name} (${it.sku}) · ${it.quantity} ${it.unit}" },
                onSelect = viewModel::selectProduct,
                isError = err && f.productId == null,
            )
            FormField(
                f.quantity, { v -> viewModel.update { it.copy(quantity = v) } }, "Quantity *",
                keyboardType = KeyboardType.Number, isError = err && (f.quantityValue ?: 0) <= 0,
            )
            if (inbound && suppliers.isNotEmpty()) {
                DropdownField(
                    label = "Supplier *",
                    options = suppliers,
                    selected = suppliers.firstOrNull { it.name == f.partner },
                    optionLabel = { it.name },
                    onSelect = { s -> viewModel.update { it.copy(partner = s.name, etaDays = s.leadTimeDays.toString()) } },
                    isError = err && f.partner.isBlank(),
                )
            } else {
                FormField(
                    f.partner, { v -> viewModel.update { it.copy(partner = v) } },
                    if (inbound) "Supplier *" else "Customer *",
                    isError = err && f.partner.isBlank(),
                )
            }
            FormField(
                f.etaDays, { v -> viewModel.update { it.copy(etaDays = v) } }, "ETA (days from today)",
                keyboardType = KeyboardType.Number, isError = err && f.etaDays.isNotBlank() && (f.etaDaysValue ?: -1) < 0,
            )
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(if (inbound) "Create purchase order" else "Create sales shipment")
            }
        }
    }
}
