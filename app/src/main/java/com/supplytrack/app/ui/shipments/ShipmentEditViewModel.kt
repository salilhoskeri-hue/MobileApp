package com.supplytrack.app.ui.shipments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supplytrack.app.data.Product
import com.supplytrack.app.data.ShipmentType
import com.supplytrack.app.data.Supplier
import com.supplytrack.app.data.SupplyException
import com.supplytrack.app.data.SupplyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class ShipmentForm(
    val type: ShipmentType = ShipmentType.INBOUND,
    val productId: Long? = null,
    val quantity: String = "",
    val partner: String = "",
    val etaDays: String = "",
) {
    val quantityValue get() = quantity.trim().toIntOrNull()
    val etaDaysValue get() = etaDays.trim().toIntOrNull()

    val isValid: Boolean
        get() = productId != null && (quantityValue ?: 0) > 0 && partner.isNotBlank() &&
            (etaDays.isBlank() || (etaDaysValue ?: -1) >= 0)
}

class ShipmentEditViewModel(private val repository: SupplyRepository) : ViewModel() {
    var form by mutableStateOf(ShipmentForm())
        private set
    var showErrors by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var finished by mutableStateOf(false)
        private set

    val products: StateFlow<List<Product>> =
        repository.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val suppliers: StateFlow<List<Supplier>> =
        repository.allSuppliers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setType(type: ShipmentType) {
        if (type != form.type) form = form.copy(type = type, partner = "")
    }

    /** Selecting a product for a purchase order pre-fills its supplier and lead time. */
    fun selectProduct(product: Product) {
        var next = form.copy(productId = product.id)
        if (form.type == ShipmentType.INBOUND) {
            suppliers.value.firstOrNull { it.id == product.supplierId }?.let { s ->
                next = next.copy(partner = s.name, etaDays = s.leadTimeDays.toString())
            }
        }
        form = next
    }

    fun update(transform: (ShipmentForm) -> ShipmentForm) {
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
                repository.createShipment(
                    type = f.type,
                    productId = f.productId!!,
                    quantity = f.quantityValue!!,
                    partner = f.partner.trim(),
                    expectedAt = f.etaDaysValue?.let { System.currentTimeMillis() + TimeUnit.DAYS.toMillis(it.toLong()) },
                )
                finished = true
            } catch (e: SupplyException) {
                message = e.message
            }
        }
    }

    fun messageShown() {
        message = null
    }
}
