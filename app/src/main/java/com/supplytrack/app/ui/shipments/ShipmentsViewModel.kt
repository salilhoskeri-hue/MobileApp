package com.supplytrack.app.ui.shipments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supplytrack.app.data.ShipmentDetails
import com.supplytrack.app.data.ShipmentStatus
import com.supplytrack.app.data.ShipmentType
import com.supplytrack.app.data.SupplyException
import com.supplytrack.app.data.SupplyRepository
import com.supplytrack.app.domain.ShipmentRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ShipmentFilter(val label: String) { ACTIVE("Active"), INBOUND("Inbound"), OUTBOUND("Outbound"), ALL("All") }

data class ShipmentsUiState(
    val shipments: List<ShipmentDetails> = emptyList(),
    val totalCount: Int = 0,
    val loading: Boolean = true,
)

class ShipmentsViewModel(private val repository: SupplyRepository) : ViewModel() {
    private val _filter = MutableStateFlow(ShipmentFilter.ACTIVE)
    val filter: StateFlow<ShipmentFilter> = _filter.asStateFlow()

    var message by mutableStateOf<String?>(null)
        private set

    val uiState: StateFlow<ShipmentsUiState> =
        combine(repository.allShipments, _filter) { all, filter ->
            val shown = when (filter) {
                ShipmentFilter.ACTIVE -> all.filter { ShipmentRules.isActive(it.shipment.status) }
                ShipmentFilter.INBOUND -> all.filter { it.shipment.type == ShipmentType.INBOUND }
                ShipmentFilter.OUTBOUND -> all.filter { it.shipment.type == ShipmentType.OUTBOUND }
                ShipmentFilter.ALL -> all
            }
            ShipmentsUiState(shipments = shown, totalCount = all.size, loading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShipmentsUiState())

    fun setFilter(filter: ShipmentFilter) {
        _filter.value = filter
    }

    fun moveTo(details: ShipmentDetails, target: ShipmentStatus) {
        viewModelScope.launch {
            try {
                repository.updateShipmentStatus(details.shipment.id, target)
                val s = details.shipment
                message = when {
                    target == ShipmentStatus.IN_TRANSIT && s.type == ShipmentType.OUTBOUND ->
                        "${s.reference} dispatched · ${s.quantity} ${details.productUnit} removed from stock"
                    target == ShipmentStatus.DELIVERED && s.type == ShipmentType.INBOUND ->
                        "${s.reference} received · ${s.quantity} ${details.productUnit} added to stock"
                    else -> "${s.reference} updated"
                }
            } catch (e: SupplyException) {
                message = e.message
            }
        }
    }

    fun messageShown() {
        message = null
    }
}
