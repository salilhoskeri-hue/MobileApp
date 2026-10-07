package com.supplytrack.app.domain

import com.supplytrack.app.data.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryMetricsTest {

    @Test
    fun computesTotalsAndAlerts() {
        val products = listOf(
            Product(sku = "A", name = "A", quantity = 10, reorderPoint = 5, unitCost = 2.0),
            Product(sku = "B", name = "B", quantity = 5, reorderPoint = 5, unitCost = 1.5),
            Product(sku = "C", name = "C", quantity = 0, reorderPoint = 3, unitCost = 9.0),
        )

        val metrics = InventoryMetrics.from(products)

        assertEquals(3, metrics.skuCount)
        assertEquals(15, metrics.totalUnits)
        assertEquals(27.5, metrics.totalValue, 0.0001)
        assertEquals(2, metrics.lowStockCount) // at or below reorder point
        assertEquals(1, metrics.outOfStockCount)
    }

    @Test
    fun emptyInventory() {
        assertEquals(InventoryMetrics(0, 0, 0.0, 0, 0), InventoryMetrics.from(emptyList()))
    }
}
