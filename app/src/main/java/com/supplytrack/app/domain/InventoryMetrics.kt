package com.supplytrack.app.domain

import com.supplytrack.app.data.Product
import com.supplytrack.app.data.isLowStock

data class InventoryMetrics(
    val skuCount: Int,
    val totalUnits: Int,
    val totalValue: Double,
    val lowStockCount: Int,
    val outOfStockCount: Int,
) {
    companion object {
        fun from(products: List<Product>) = InventoryMetrics(
            skuCount = products.size,
            totalUnits = products.sumOf { it.quantity },
            totalValue = products.sumOf { it.quantity * it.unitCost },
            lowStockCount = products.count { it.isLowStock() },
            outOfStockCount = products.count { it.quantity <= 0 },
        )
    }
}
