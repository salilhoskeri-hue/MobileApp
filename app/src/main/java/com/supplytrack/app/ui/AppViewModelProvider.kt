package com.supplytrack.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.supplytrack.app.SupplyTrackApp
import com.supplytrack.app.data.SupplyRepository
import com.supplytrack.app.ui.dashboard.DashboardViewModel
import com.supplytrack.app.ui.inventory.InventoryViewModel
import com.supplytrack.app.ui.inventory.ProductEditViewModel
import com.supplytrack.app.ui.shipments.ShipmentEditViewModel
import com.supplytrack.app.ui.shipments.ShipmentsViewModel
import com.supplytrack.app.ui.suppliers.SupplierEditViewModel
import com.supplytrack.app.ui.suppliers.SuppliersViewModel

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { DashboardViewModel(repository()) }
        initializer { InventoryViewModel(repository()) }
        initializer { ProductEditViewModel(createSavedStateHandle(), repository()) }
        initializer { ShipmentsViewModel(repository()) }
        initializer { ShipmentEditViewModel(repository()) }
        initializer { SuppliersViewModel(repository()) }
        initializer { SupplierEditViewModel(createSavedStateHandle(), repository()) }
    }
}

private fun CreationExtras.repository(): SupplyRepository =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SupplyTrackApp).repository
