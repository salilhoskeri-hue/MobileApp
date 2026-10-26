package com.supplytrack.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.supplytrack.app.ui.dashboard.DashboardScreen
import com.supplytrack.app.ui.inventory.InventoryScreen
import com.supplytrack.app.ui.inventory.ProductEditScreen
import com.supplytrack.app.ui.shipments.ShipmentEditScreen
import com.supplytrack.app.ui.shipments.ShipmentsScreen
import com.supplytrack.app.ui.suppliers.SupplierEditScreen
import com.supplytrack.app.ui.suppliers.SuppliersScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val INVENTORY = "inventory"
    const val SHIPMENTS = "shipments"
    const val SUPPLIERS = "suppliers"
    const val PRODUCT = "product/{productId}"
    const val NEW_SHIPMENT = "shipment/new"
    const val SUPPLIER = "supplier/{supplierId}"

    /** Use id 0 to create a new product. */
    fun product(id: Long) = "product/$id"

    /** Use id 0 to create a new supplier. */
    fun supplier(id: Long) = "supplier/$id"
}

private enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    Dashboard(Routes.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
    Inventory(Routes.INVENTORY, "Inventory", Icons.Filled.Inventory2),
    Shipments(Routes.SHIPMENTS, "Shipments", Icons.Filled.LocalShipping),
    Suppliers(Routes.SUPPLIERS, "Suppliers", Icons.Filled.Factory),
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun SupplyTrackNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevel.entries.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevel.entries.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { navController.navigateTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onProductClick = { navController.navigate(Routes.product(it)) },
                    onOpenInventory = { navController.navigateTopLevel(Routes.INVENTORY) },
                    onOpenShipments = { navController.navigateTopLevel(Routes.SHIPMENTS) },
                )
            }
            composable(Routes.INVENTORY) {
                InventoryScreen(
                    onProductClick = { navController.navigate(Routes.product(it)) },
                    onAddProduct = { navController.navigate(Routes.product(0)) },
                )
            }
            composable(Routes.SHIPMENTS) {
                ShipmentsScreen(onNewShipment = { navController.navigate(Routes.NEW_SHIPMENT) })
            }
            composable(Routes.SUPPLIERS) {
                SuppliersScreen(
                    onSupplierClick = { navController.navigate(Routes.supplier(it)) },
                    onAddSupplier = { navController.navigate(Routes.supplier(0)) },
                )
            }
            composable(
                Routes.PRODUCT,
                arguments = listOf(navArgument("productId") { type = NavType.LongType }),
            ) {
                ProductEditScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.NEW_SHIPMENT) {
                ShipmentEditScreen(onDone = { navController.popBackStack() })
            }
            composable(
                Routes.SUPPLIER,
                arguments = listOf(navArgument("supplierId") { type = NavType.LongType }),
            ) {
                SupplierEditScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}
