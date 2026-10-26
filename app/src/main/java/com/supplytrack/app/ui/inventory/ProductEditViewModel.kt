package com.supplytrack.app.ui.inventory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supplytrack.app.data.Product
import com.supplytrack.app.data.StockMovement
import com.supplytrack.app.data.Supplier
import com.supplytrack.app.data.SupplyException
import com.supplytrack.app.data.SupplyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductForm(
    val sku: String = "",
    val name: String = "",
    val category: String = "",
    val unit: String = "pcs",
    val reorderPoint: String = "0",
    val unitCost: String = "0",
    val location: String = "",
    val supplierId: Long? = null,
    val openingStock: String = "0",
) {
    val reorderPointValue get() = reorderPoint.trim().toIntOrNull()
    val unitCostValue get() = unitCost.trim().replace(',', '.').toDoubleOrNull()
    val openingStockValue get() = openingStock.trim().toIntOrNull()

    val isValid: Boolean
        get() = sku.isNotBlank() && name.isNotBlank() && unit.isNotBlank() &&
            (reorderPointValue ?: -1) >= 0 && (unitCostValue ?: -1.0) >= 0.0 && (openingStockValue ?: -1) >= 0
}

class ProductEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SupplyRepository,
) : ViewModel() {
    val productId: Long = savedStateHandle.get<Long>("productId") ?: 0L
    val isNew: Boolean get() = productId == 0L

    var form by mutableStateOf(ProductForm())
        private set
    var showErrors by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var finished by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(isNew)
        private set

    val suppliers: StateFlow<List<Supplier>> =
        repository.allSuppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val product: StateFlow<Product?> =
        (if (isNew) flowOf<Product?>(null) else repository.product(productId))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val movements: StateFlow<List<StockMovement>> =
        (if (isNew) emptyFlow<List<StockMovement>>() else repository.movementsFor(productId))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (!isNew) {
            viewModelScope.launch {
                val p = repository.getProduct(productId)
                if (p == null) {
                    finished = true
                } else {
                    form = ProductForm(
                        sku = p.sku,
                        name = p.name,
                        category = p.category,
                        unit = p.unit,
                        reorderPoint = p.reorderPoint.toString(),
                        unitCost = p.unitCost.toString(),
                        location = p.location,
                        supplierId = p.supplierId,
                    )
                    loaded = true
                }
            }
        }
    }

    fun update(transform: (ProductForm) -> ProductForm) {
        form = transform(form)
    }

    fun save() {
        val f = form
        if (!f.isValid) {
            showErrors = true
            return
        }
        viewModelScope.launch {
            try {
                repository.saveProduct(
                    Product(
                        id = productId,
                        sku = f.sku.trim(),
                        name = f.name.trim(),
                        category = f.category.trim(),
                        unit = f.unit.trim(),
                        reorderPoint = f.reorderPointValue!!,
                        unitCost = f.unitCostValue!!,
                        location = f.location.trim(),
                        supplierId = f.supplierId,
                    ),
                    openingStock = if (isNew) f.openingStockValue!! else 0,
                )
                finished = true
            } catch (e: SupplyException) {
                message = e.message
            }
        }
    }

    fun adjustStock(delta: Int, reason: String) {
        viewModelScope.launch {
            try {
                repository.adjustStock(productId, delta, reason.ifBlank { if (delta > 0) "Manual receipt" else "Manual issue" })
                message = if (delta > 0) "Received $delta" else "Issued ${-delta}"
            } catch (e: SupplyException) {
                message = e.message
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.getProduct(productId)?.let { repository.deleteProduct(it) }
            finished = true
        }
    }

    fun messageShown() {
        message = null
    }
}
