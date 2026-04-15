package com.glucodes.swarmdoc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glucodes.swarmdoc.ui.theme.CharcoalBlack
import com.glucodes.swarmdoc.ui.theme.Parchment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickHideScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Hide") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Parchment,
                    titleContentColor = CharcoalBlack,
                )
            )
        },
        containerColor = Parchment
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Quick Hide Content (Placeholder)")
        }
    }
}
