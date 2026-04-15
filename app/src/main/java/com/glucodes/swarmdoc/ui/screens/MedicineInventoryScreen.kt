package com.glucodes.swarmdoc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.data.local.entities.MedicineWithInventory
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.viewmodel.MedicineInventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineInventoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: MedicineInventoryViewModel = hiltViewModel(),
) {
    val medicines by viewModel.medicines.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = if (searchQuery.isBlank()) medicines
    else medicines.filter {
        it.medicine.genericName.contains(searchQuery, ignoreCase = true) ||
        it.medicine.localName.contains(searchQuery, ignoreCase = true) ||
        it.medicine.category.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(Parchment)) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalHospital, contentDescription = null, tint = ForestGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hospital Medicine Inventory", fontWeight = FontWeight.Bold, color = ForestGreen)
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = ForestGreen)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Parchment),
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search medicines") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            colors = swarmDocTextFieldColors(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "${filtered.size} medicines",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.labelSmall,
            color = WarmGrey,
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(filtered, key = { it.medicine.id }) { item ->
                MedicineInventoryItem(
                    item = item,
                    onToggleStock = { inStock -> viewModel.toggleStock(item.medicine.id, inStock) },
                    onSaveDetails = { quantity, unitPrice, mfdDate, expiryDate ->
                        viewModel.updateDetails(
                            medicineId = item.medicine.id,
                            quantity = quantity,
                            unitPrice = unitPrice,
                            mfdDate = mfdDate,
                            expiryDate = expiryDate,
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun MedicineInventoryItem(
    item: MedicineWithInventory,
    onToggleStock: (Boolean) -> Unit,
    onSaveDetails: (quantity: Int, unitPrice: Double, mfdDate: String, expiryDate: String) -> Unit,
) {
    val inStock = item.inventory?.inStock ?: true
    var quantityText by remember(item.medicine.id, item.inventory?.quantity) {
        mutableStateOf((item.inventory?.quantity ?: 0).toString())
    }
    var unitPriceText by remember(item.medicine.id, item.inventory?.unitPrice) {
        mutableStateOf(
            if ((item.inventory?.unitPrice ?: 0.0) == 0.0) "" else (item.inventory?.unitPrice ?: 0.0).toString()
        )
    }
    var mfdDate by remember(item.medicine.id, item.inventory?.mfdDate) {
        mutableStateOf(item.inventory?.mfdDate ?: "")
    }
    var expiryDate by remember(item.medicine.id, item.inventory?.expiryDate) {
        mutableStateOf(item.inventory?.expiryDate ?: "")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.medicine.genericName, fontWeight = FontWeight.Bold, color = CharcoalBlack, fontSize = 15.sp)
                    Text(item.medicine.category, style = MaterialTheme.typography.labelSmall, color = WarmGrey)
                }
                Switch(
                    checked = inStock,
                    onCheckedChange = { onToggleStock(it) },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = SageGreen,
                        uncheckedTrackColor = CoralRed.copy(alpha = 0.3f),
                    ),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Quantity") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = swarmDocTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = unitPriceText,
                onValueChange = { unitPriceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Unit Price (INR)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = swarmDocTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = mfdDate,
                onValueChange = { mfdDate = it },
                label = { Text("MFD Date (DD/MM/YYYY)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = swarmDocTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = expiryDate,
                onValueChange = { expiryDate = it },
                label = { Text("Expiry Date (DD/MM/YYYY)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = swarmDocTextFieldColors(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    onSaveDetails(
                        quantityText.toIntOrNull() ?: 0,
                        unitPriceText.toDoubleOrNull() ?: 0.0,
                        mfdDate,
                        expiryDate
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save Details")
            }
        }
    }
}
