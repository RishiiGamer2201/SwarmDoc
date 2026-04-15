package com.glucodes.swarmdoc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.util.LocalStrings
import com.glucodes.swarmdoc.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onSetupComplete: () -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment),
    ) {
        TopAppBar(
            title = { Text(strings.profileSetup, fontWeight = FontWeight.Bold, color = ForestGreen) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ForestGreen)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Parchment),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
        Text(strings.welcomeAshaWorker, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ForestGreen)
        Text(strings.setupYourProfile, style = MaterialTheme.typography.bodyMedium, color = WarmGrey)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(strings.yourName) },
            modifier = Modifier.fillMaxWidth(),
            colors = swarmDocTextFieldColors(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = district,
            onValueChange = { district = it },
            label = { Text(strings.district) },
            modifier = Modifier.fillMaxWidth(),
            colors = swarmDocTextFieldColors(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text(strings.setPin) },
            modifier = Modifier.fillMaxWidth(),
            colors = swarmDocTextFieldColors(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.completeSetup(name, district, pin)
                onSetupComplete()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = name.isNotBlank() && district.isNotBlank() && pin.length >= 4,
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.completeSetup, fontWeight = FontWeight.Bold, color = White)
        }
        }
    }
}
