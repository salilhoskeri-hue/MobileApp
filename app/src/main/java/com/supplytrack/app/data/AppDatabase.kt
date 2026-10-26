package com.supplytrack.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Supplier::class, Product::class, Shipment::class, StockMovement::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun supplierDao(): SupplierDao
    abstract fun productDao(): ProductDao
    abstract fun shipmentDao(): ShipmentDao
    abstract fun stockMovementDao(): StockMovementDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "supplytrack.db").build()
    }
}
