package com.supplytrack.app.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ShipmentType { INBOUND, OUTBOUND }

enum class ShipmentStatus { PENDING, IN_TRANSIT, DELIVERED, CANCELLED }

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contactName: String = "",
    val email: String = "",
    val phone: String = "",
    val leadTimeDays: Int = 7,
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["sku"], unique = true), Index("supplierId")],
    foreignKeys = [
        ForeignKey(
            entity = Supplier::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String,
    val name: String,
    val category: String = "",
    val unit: String = "pcs",
    val quantity: Int = 0,
    val reorderPoint: Int = 0,
    val unitCost: Double = 0.0,
    val location: String = "",
    val supplierId: Long? = null,
)

fun Product.isLowStock(): Boolean = quantity <= reorderPoint

@Entity(
    tableName = "shipments",
    indices = [Index("productId")],
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class Shipment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val type: ShipmentType,
    val productId: Long,
    val quantity: Int,
    /** Supplier name for inbound shipments, customer name for outbound ones. */
    val partner: String,
    val status: ShipmentStatus = ShipmentStatus.PENDING,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val expectedAt: Long? = null,
)

/** A shipment joined with the product it carries, for list screens. */
data class ShipmentDetails(
    @Embedded val shipment: Shipment,
    @ColumnInfo(name = "productName") val productName: String,
    @ColumnInfo(name = "productSku") val productSku: String,
    @ColumnInfo(name = "productUnit") val productUnit: String,
)

/** Audit log of every change to a product's on-hand quantity. */
@Entity(
    tableName = "stock_movements",
    indices = [Index("productId")],
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val delta: Int,
    val reason: String,
    val timestamp: Long,
)
