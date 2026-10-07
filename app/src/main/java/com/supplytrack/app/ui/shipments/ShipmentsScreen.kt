package com.supplytrack.app.ui.shipments

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supplytrack.app.data.ShipmentDetails
import com.supplytrack.app.data.ShipmentStatus
import com.supplytrack.app.data.ShipmentType
import com.supplytrack.app.domain.ShipmentRules
import com.supplytrack.app.ui.AppViewModelProvider
import com.supplytrack.app.ui.common.EmptyState
import com.supplytrack.app.ui.common.ShipmentTypeIcon
import com.supplytrack.app.ui.common.StatusBadge
import com.supplytrack.app.ui.common.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipmentsScreen(
    onNewShipment: () -> Unit,
    viewModel: ShipmentsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Shipments") }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewShipment,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Shipment") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShipmentFilter.entries.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { viewModel.setFilter(f) }, label = { Text(f.label) })
                }
            }
            if (!state.loading && state.shipments.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.LocalShipping,
                    title = if (state.totalCount == 0) "No shipments yet" else "Nothing here",
                    message = if (state.totalCount == 0) "Create a purchase order or sales shipment to get started." else "No shipments match this filter.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.shipments, key = { it.shipment.id }) { details ->
                        ShipmentCard(details, onMove = { viewModel.moveTo(details, it) })
                    }
                }
            }
        }
    }
}

private fun actionLabel(type: ShipmentType, target: ShipmentStatus): String = when (target) {
    ShipmentStatus.IN_TRANSIT -> if (type == ShipmentType.OUTBOUND) "Dispatch" else "Mark shipped"
    ShipmentStatus.DELIVERED -> if (type == ShipmentType.INBOUND) "Receive" else "Mark delivered"
    ShipmentStatus.CANCELLED -> "Cancel"
    ShipmentStatus.PENDING -> "Reopen"
}

@Composable
private fun ShipmentCard(details: ShipmentDetails, onMove: (ShipmentStatus) -> Unit) {
    val s = details.shipment
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShipmentTypeIcon(s.type)
                Spacer(Modifier.width(8.dp))
                Text(s.reference, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusBadge(s.status)
            }
            Text("${details.productName} (${details.productSku}) · ${s.quantity} ${details.productUnit}", style = MaterialTheme.typography.bodyMedium)
            Text(
                buildString {
                    append(if (s.type == ShipmentType.INBOUND) "From " else "To ")
                    append(s.partner)
                    s.expectedAt?.let { append(" · ETA ").append(formatDate(it)) }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val next = ShipmentRules.nextStatuses(s.status)
            if (next.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    next.filter { it == ShipmentStatus.CANCELLED }.forEach {
                        TextButton(onClick = { onMove(it) }) { Text(actionLabel(s.type, it)) }
                    }
                    next.filter { it != ShipmentStatus.CANCELLED }.forEach {
                        FilledTonalButton(onClick = { onMove(it) }) { Text(actionLabel(s.type, it)) }
                    }
                }
            }
        }
    }
}
