package com.supplytrack.app.data

import java.util.concurrent.TimeUnit

/** Demo data shown on first launch so the app isn't empty. */
internal object SampleData {
    private val DAY = TimeUnit.DAYS.toMillis(1)

    suspend fun seed(repo: SupplyRepository, now: Long) = repo.inTransaction {
        val acme = repo.insertSeedSupplier(Supplier(name = "Acme Components", contactName = "Priya Nair", email = "orders@acme.example", phone = "+1 555 0100", leadTimeDays = 5))
        val north = repo.insertSeedSupplier(Supplier(name = "Northwind Packaging", contactName = "Tom Reyes", email = "sales@northwind.example", phone = "+1 555 0142", leadTimeDays = 10))
        val delta = repo.insertSeedSupplier(Supplier(name = "Delta Electronics", contactName = "Mei Chen", email = "supply@delta.example", phone = "+1 555 0177", leadTimeDays = 14))

        val bolts = repo.insertSeedProduct(Product(sku = "HW-1001", name = "Steel bolts M8", category = "Hardware", unit = "box", quantity = 120, reorderPoint = 40, unitCost = 12.5, location = "A-01", supplierId = acme))
        repo.insertSeedProduct(Product(sku = "HW-1002", name = "Hex nuts M8", category = "Hardware", unit = "box", quantity = 18, reorderPoint = 30, unitCost = 8.0, location = "A-02", supplierId = acme))
        val cartons = repo.insertSeedProduct(Product(sku = "PK-2001", name = "Shipping carton 40cm", category = "Packaging", unit = "pcs", quantity = 640, reorderPoint = 200, unitCost = 0.9, location = "B-11", supplierId = north))
        repo.insertSeedProduct(Product(sku = "PK-2002", name = "Bubble wrap roll", category = "Packaging", unit = "roll", quantity = 9, reorderPoint = 10, unitCost = 24.0, location = "B-12", supplierId = north))
        val boards = repo.insertSeedProduct(Product(sku = "EL-3001", name = "Controller board v2", category = "Electronics", unit = "pcs", quantity = 55, reorderPoint = 20, unitCost = 46.0, location = "C-03", supplierId = delta))
        repo.insertSeedProduct(Product(sku = "EL-3002", name = "Power supply 24V", category = "Electronics", unit = "pcs", quantity = 32, reorderPoint = 15, unitCost = 31.75, location = "C-04", supplierId = delta))

        repo.insertSeedShipment(Shipment(reference = "PO-00001", type = ShipmentType.INBOUND, productId = boards, quantity = 40, partner = "Delta Electronics", status = ShipmentStatus.IN_TRANSIT, createdAt = now - 6 * DAY, expectedAt = now + 2 * DAY))
        repo.insertSeedShipment(Shipment(reference = "PO-00002", type = ShipmentType.INBOUND, productId = bolts, quantity = 80, partner = "Acme Components", createdAt = now - 1 * DAY, expectedAt = now + 5 * DAY))
        repo.insertSeedShipment(Shipment(reference = "SO-00003", type = ShipmentType.OUTBOUND, productId = cartons, quantity = 150, partner = "Riverside Retail", createdAt = now - 2 * DAY, expectedAt = now + 1 * DAY))
        repo.insertSeedShipment(Shipment(reference = "SO-00004", type = ShipmentType.OUTBOUND, productId = boards, quantity = 10, partner = "Harbor Robotics", status = ShipmentStatus.DELIVERED, createdAt = now - 9 * DAY, expectedAt = now - 4 * DAY))
    }
}
