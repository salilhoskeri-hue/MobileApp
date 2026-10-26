package com.supplytrack.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supplytrack.app.data.Product
import com.supplytrack.app.data.ShipmentDetails
import com.supplytrack.app.data.ShipmentStatus
import com.supplytrack.app.data.SupplyRepository
import com.supplytrack.app.data.isLowStock
import com.supplytrack.app.domain.InventoryMetrics
import com.supplytrack.app.domain.ShipmentRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val loading: Boolean = true,
    val metrics: InventoryMetrics = InventoryMetrics.from(emptyList()),
    val lowStock: List<Product> = emptyList(),
    val activeShipments: List<ShipmentDetails> = emptyList(),
    val inTransitCount: Int = 0,
    val pendingCount: Int = 0,
)

class DashboardViewModel(repository: SupplyRepository) : ViewModel() {
    val uiState: StateFlow<DashboardUiState> =
        combine(repository.allProducts, repository.allShipments) { products, shipments ->
            val active = shipments.filter { ShipmentRules.isActive(it.shipment.status) }
            DashboardUiState(
                loading = false,
                metrics = InventoryMetrics.from(products),
                lowStock = products.filter { it.isLowStock() }.sortedBy { it.quantity - it.reorderPoint },
                activeShipments = active.sortedBy { it.shipment.expectedAt ?: Long.MAX_VALUE },
                inTransitCount = active.count { it.shipment.status == ShipmentStatus.IN_TRANSIT },
                pendingCount = active.count { it.shipment.status == ShipmentStatus.PENDING },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
