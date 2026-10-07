package com.supplytrack.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun get(id: Long): Supplier?

    @Query("SELECT COUNT(*) FROM suppliers")
    suspend fun count(): Int

    @Insert
    suspend fun insert(supplier: Supplier): Long

    @Update
    suspend fun update(supplier: Supplier)

    @Delete
    suspend fun delete(supplier: Supplier)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observe(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun get(id: Long): Product?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Insert
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("UPDATE products SET quantity = quantity + :delta WHERE id = :id")
    suspend fun adjustQuantity(id: Long, delta: Int)
}

@Dao
interface ShipmentDao {
    @Query(
        """
        SELECT s.*, p.name AS productName, p.sku AS productSku, p.unit AS productUnit
        FROM shipments s INNER JOIN products p ON p.id = s.productId
        ORDER BY s.createdAt DESC
        """
    )
    fun observeAllDetails(): Flow<List<ShipmentDetails>>

    @Query("SELECT * FROM shipments WHERE id = :id")
    suspend fun get(id: Long): Shipment?

    @Query("SELECT IFNULL(MAX(id), 0) + 1 FROM shipments")
    suspend fun nextId(): Long

    @Insert
    suspend fun insert(shipment: Shipment): Long

    @Update
    suspend fun update(shipment: Shipment)
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC, id DESC")
    fun observeForProduct(productId: Long): Flow<List<StockMovement>>

    @Insert
    suspend fun insert(movement: StockMovement): Long
}
