package com.example.sande.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sande.data.entities.Product
import com.example.sande.di.SandeApplication
import com.example.sande.ui.viewModelFactoryOf
import com.example.sande.ui.viewmodel.ProductFormViewModel

@Composable
fun ProductFormScreen(
    modifier: Modifier = Modifier,
    productToEdit: Product? = null,
    businessId: String = "default-business",
    onProductSaved: () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as SandeApplication
    val vm: ProductFormViewModel = viewModel(
        key = "product-form-" + (productToEdit?.id ?: "new"),
        factory = viewModelFactoryOf { ProductFormViewModel(app.productRepository, productToEdit) }
    )

    val name by vm.name.collectAsState()
    val sku by vm.sku.collectAsState()
    val category by vm.category.collectAsState()
    val priceCentsStr by vm.priceCentsStr.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (productToEdit == null) "Add product" else "Edit product",
            style = MaterialTheme.typography.headlineSmall
        )
        OutlinedTextField(
            value = name,
            onValueChange = vm::setName,
            label = { Text("Product name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = sku,
            onValueChange = vm::setSku,
            label = { Text("SKU (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = category,
            onValueChange = vm::setCategory,
            label = { Text("Category (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = priceCentsStr,
            onValueChange = { vm.setPriceCentsStr(it.filter { c -> c.isDigit() }) },
            label = { Text("Price in cents (22000 = 220.00)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                vm.saveProduct(businessId)
                onProductSaved()
            },
            enabled = name.isNotBlank() && priceCentsStr.toLongOrNull() != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (productToEdit == null) "Add product" else "Save changes") }
    }
}
