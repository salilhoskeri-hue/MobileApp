package com.supplytrack.app.ui.suppliers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supplytrack.app.data.Supplier
import com.supplytrack.app.data.SupplyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SuppliersViewModel(repository: SupplyRepository) : ViewModel() {
    /** Null until the first load completes. */
    val suppliers: StateFlow<List<Supplier>?> =
        repository.allSuppliers.map<List<Supplier>, List<Supplier>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

data class SupplierForm(
    val name: String = "",
    val contactName: String = "",
    val email: String = "",
    val phone: String = "",
    val leadTimeDays: String = "7",
) {
    val leadTimeValue get() = leadTimeDays.trim().toIntOrNull()
    val isValid get() = name.isNotBlank() && (leadTimeValue ?: -1) >= 0
}

class SupplierEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SupplyRepository,
) : ViewModel() {
    val supplierId: Long = savedStateHandle.get<Long>("supplierId") ?: 0L
    val isNew: Boolean get() = supplierId == 0L

    var form by mutableStateOf(SupplierForm())
        private set
    var showErrors by mutableStateOf(false)
        private set
    var finished by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(isNew)
        private set

    init {
        if (!isNew) {
            viewModelScope.launch {
                val s = repository.getSupplier(supplierId)
                if (s == null) {
                    finished = true
                } else {
                    form = SupplierForm(s.name, s.contactName, s.email, s.phone, s.leadTimeDays.toString())
                    loaded = true
                }
            }
        }
    }

    fun update(transform: (SupplierForm) -> SupplierForm) {
        form = transform(form)
    }

    fun save() {
        val f = form
        if (!f.isValid) {
            showErrors = true
            return
        }
        viewModelScope.launch {
            repository.saveSupplier(
                Supplier(
                    id = supplierId,
                    name = f.name.trim(),
                    contactName = f.contactName.trim(),
                    email = f.email.trim(),
                    phone = f.phone.trim(),
                    leadTimeDays = f.leadTimeValue!!,
                )
            )
            finished = true
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.getSupplier(supplierId)?.let { repository.deleteSupplier(it) }
            finished = true
        }
    }
}
