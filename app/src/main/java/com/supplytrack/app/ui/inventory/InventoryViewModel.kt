package com.supplytrack.app.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supplytrack.app.data.Product
import com.supplytrack.app.data.SupplyRepository
import com.supplytrack.app.data.isLowStock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class InventoryUiState(
    val products: List<Product> = emptyList(),
    val totalCount: Int = 0,
    val loading: Boolean = true,
)

class InventoryViewModel(repository: SupplyRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _lowStockOnly = MutableStateFlow(false)
    val lowStockOnly: StateFlow<Boolean> = _lowStockOnly.asStateFlow()

    val uiState: StateFlow<InventoryUiState> =
        combine(repository.allProducts, _query, _lowStockOnly) { products, query, lowOnly ->
            val q = query.trim()
            val filtered = products.filter { p ->
                (!lowOnly || p.isLowStock()) &&
                    (q.isEmpty() || p.name.contains(q, true) || p.sku.contains(q, true) ||
                        p.category.contains(q, true) || p.location.contains(q, true))
            }
            InventoryUiState(products = filtered, totalCount = products.size, loading = false)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InventoryUiState())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun toggleLowStockOnly() {
        _lowStockOnly.value = !_lowStockOnly.value
    }
}
