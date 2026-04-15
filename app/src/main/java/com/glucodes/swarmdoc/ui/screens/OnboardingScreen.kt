package com.glucodes.swarmdoc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var workerName by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("Puri") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Welcome to SwarmDoc",
                style = MaterialTheme.typography.displaySmall,
                color = ForestGreen,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Please enter your details to set up your frontline health dashboard.",
                style = MaterialTheme.typography.bodyLarge,
                color = WarmGrey,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = workerName,
                onValueChange = { workerName = it },
                label = { Text("Your Name (ASHA Worker)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = ForestGreen,
                    focusedLabelColor = ForestGreen,
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = district,
                onValueChange = { district = it },
                label = { Text("District") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = ForestGreen,
                    focusedLabelColor = ForestGreen,
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) pin = it.filter { char -> char.isDigit() } },
                label = { Text("4-Digit PIN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = ForestGreen,
                    focusedLabelColor = ForestGreen,
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (workerName.isNotBlank() && pin.length == 4) {
                        scope.launch {
                            viewModel.setWorkerName(workerName)
                            viewModel.setWorkerPin(pin)
                            viewModel.setDistrict(district)
                            viewModel.setOnboarded(true)
                            onOnboardingComplete()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = workerName.isNotBlank() && pin.length == 4,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TurmericGold,
                    contentColor = CharcoalBlack,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
