package com.supplytrack.app.ui.suppliers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supplytrack.app.ui.AppViewModelProvider
import com.supplytrack.app.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    onSupplierClick: (Long) -> Unit,
    onAddSupplier: () -> Unit,
    viewModel: SuppliersViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Suppliers") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSupplier,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Supplier") },
            )
        },
    ) { padding ->
        val list = suppliers ?: return@Scaffold
        if (list.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Factory,
                title = "No suppliers yet",
                message = "Add the vendors you buy from to speed up purchase orders.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(list, key = { it.id }) { s ->
                    Card(onClick = { onSupplierClick(s.id) }) {
                        ListItem(
                            headlineContent = { Text(s.name) },
                            supportingContent = {
                                Text(listOf(s.contactName, s.email, s.phone).filter { it.isNotBlank() }.joinToString(" · "))
                            },
                            trailingContent = { Text("${s.leadTimeDays}d lead") },
                        )
                    }
                }
            }
        }
    }
}
