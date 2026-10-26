package com.supplytrack.app.ui.suppliers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.supplytrack.app.ui.AppViewModelProvider
import com.supplytrack.app.ui.common.BackButton
import com.supplytrack.app.ui.common.ConfirmDeleteDialog
import com.supplytrack.app.ui.common.FormField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierEditScreen(
    onDone: () -> Unit,
    viewModel: SupplierEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val f = viewModel.form
    val err = viewModel.showErrors

    LaunchedEffect(viewModel.finished) { if (viewModel.finished) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "New supplier" else "Supplier") },
                navigationIcon = { BackButton(onDone) },
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete supplier")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!viewModel.loaded) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormField(f.name, { v -> viewModel.update { it.copy(name = v) } }, "Company name *", isError = err && f.name.isBlank())
            FormField(f.contactName, { v -> viewModel.update { it.copy(contactName = v) } }, "Contact person")
            FormField(f.email, { v -> viewModel.update { it.copy(email = v) } }, "Email", keyboardType = KeyboardType.Email)
            FormField(f.phone, { v -> viewModel.update { it.copy(phone = v) } }, "Phone", keyboardType = KeyboardType.Phone)
            FormField(
                f.leadTimeDays, { v -> viewModel.update { it.copy(leadTimeDays = v) } }, "Lead time (days)",
                keyboardType = KeyboardType.Number, isError = err && (f.leadTimeValue ?: -1) < 0,
                supportingText = "Used as the default ETA for purchase orders.",
            )
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(if (viewModel.isNew) "Add supplier" else "Save changes")
            }
        }
    }

    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = "Delete supplier?",
            message = "Products linked to this supplier will be kept but unlinked.",
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
