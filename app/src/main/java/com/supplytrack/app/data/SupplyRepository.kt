package com.supplytrack.app.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.supplytrack.app.domain.ShipmentRules
import kotlinx.coroutines.flow.Flow

class SupplyException(message: String) : Exception(message)

class SupplyRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val suppliers = db.supplierDao()
    private val products = db.productDao()
    private val shipments = db.shipmentDao()
    private val movements = db.stockMovementDao()

    val allSuppliers: Flow<List<Supplier>> = suppliers.observeAll()
    val allProducts: Flow<List<Product>> = products.observeAll()
    val allShipments: Flow<List<ShipmentDetails>> = shipments.observeAllDetails()

    fun product(id: Long): Flow<Product?> = products.observe(id)
    fun movementsFor(productId: Long): Flow<List<StockMovement>> = movements.observeForProduct(productId)

    suspend fun getProduct(id: Long): Product? = products.get(id)
    suspend fun getSupplier(id: Long): Supplier? = suppliers.get(id)

    // --- Suppliers ---

    suspend fun saveSupplier(supplier: Supplier) {
        if (supplier.id == 0L) suppliers.insert(supplier) else suppliers.update(supplier)
    }

    suspend fun deleteSupplier(supplier: Supplier) = suppliers.delete(supplier)

    // --- Products ---

    /**
     * Inserts or updates a product. For a new product, [openingStock] is recorded as its
     * first stock movement. For an existing product the stored quantity is kept: stock only
     * changes through [adjustStock] or shipments so every change is logged.
     */
    suspend fun saveProduct(product: Product, openingStock: Int = 0) {
        try {
            db.withTransaction {
                if (product.id == 0L) {
                    val id = products.insert(product.copy(quantity = openingStock))
                    if (openingStock != 0) {
                        movements.insert(StockMovement(productId = id, delta = openingStock, reason = "Opening stock", timestamp = clock()))
                    }
                } else {
                    val current = products.get(product.id) ?: throw SupplyException("Product no longer exists")
                    products.update(product.copy(quantity = current.quantity))
                }
            }
        } catch (e: SQLiteConstraintException) {
            throw SupplyException("SKU \"${product.sku}\" is already in use")
        }
    }

    suspend fun deleteProduct(product: Product) = products.delete(product)

    suspend fun adjustStock(productId: Long, delta: Int, reason: String) {
        if (delta == 0) return
        db.withTransaction { applyStockChange(productId, delta, reason) }
    }

    private suspend fun applyStockChange(productId: Long, delta: Int, reason: String) {
        val product = products.get(productId) ?: throw SupplyException("Product no longer exists")
        if (product.quantity + delta < 0) {
            throw SupplyException("Not enough stock: ${product.quantity} ${product.unit} on hand")
        }
        products.adjustQuantity(productId, delta)
        movements.insert(StockMovement(productId = productId, delta = delta, reason = reason, timestamp = clock()))
    }

    // --- Shipments ---

    suspend fun createShipment(
        type: ShipmentType,
        productId: Long,
        quantity: Int,
        partner: String,
        expectedAt: Long?,
    ) {
        db.withTransaction {
            val prefix = if (type == ShipmentType.INBOUND) "PO" else "SO"
            val reference = "$prefix-%05d".format(shipments.nextId())
            val now = clock()
            shipments.insert(
                Shipment(
                    reference = reference,
                    type = type,
                    productId = productId,
                    quantity = quantity,
                    partner = partner,
                    createdAt = now,
                    expectedAt = expectedAt,
                )
            )
        }
    }

    /** Moves a shipment to [target], applying any stock change the transition implies. */
    suspend fun updateShipmentStatus(shipmentId: Long, target: ShipmentStatus) {
        db.withTransaction {
            val shipment = shipments.get(shipmentId) ?: throw SupplyException("Shipment no longer exists")
            if (!ShipmentRules.canTransition(shipment.status, target)) {
                throw SupplyException("Cannot move ${shipment.reference} from ${shipment.status} to $target")
            }
            val delta = ShipmentRules.stockDelta(shipment.type, shipment.status, target, shipment.quantity)
            if (delta != 0) {
                applyStockChange(shipment.productId, delta, "Shipment ${shipment.reference}")
            }
            shipments.update(shipment.copy(status = target, updatedAt = clock()))
        }
    }

    // --- Sample data ---

    suspend fun seedIfEmpty() {
        if (products.count() > 0 || suppliers.count() > 0) return
        SampleData.seed(this, clock())
    }

    internal suspend fun insertSeedSupplier(supplier: Supplier): Long = suppliers.insert(supplier)
    internal suspend fun insertSeedProduct(product: Product): Long = products.insert(product)
    internal suspend fun insertSeedShipment(shipment: Shipment): Long = shipments.insert(shipment)
    internal suspend fun <R> inTransaction(block: suspend () -> R): R = db.withTransaction(block)
}
