package com.glucodes.swarmdoc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.util.LocalStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialDiagnosticsScreen(
    onNavigateToCoughScreener: () -> Unit,
    onNavigateToAnemiaCheck: () -> Unit,
    onNavigateToRppg: () -> Unit,
    onNavigateToCapillaryRefill: () -> Unit,
    onNavigateBack: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val strings = LocalStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        TopAppBar(
            title = { Text(strings.diagnostics, fontWeight = FontWeight.Bold, color = ForestGreen) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = ForestGreen)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Parchment),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState)
        ) {

        Spacer(modifier = Modifier.height(32.dp))

        ToolCard(
            title = strings.coughScreener,
            description = strings.acousticCoughDescription,
            icon = Icons.Rounded.GraphicEq,
            onClick = onNavigateToCoughScreener
        )

        Spacer(modifier = Modifier.height(16.dp))

        ToolCard(
            title = strings.anemiaEyeCheck,
            description = strings.anemiaEyeDescription,
            icon = Icons.Rounded.Visibility,
            onClick = onNavigateToAnemiaCheck
        )

        Spacer(modifier = Modifier.height(16.dp))

        ToolCard(
            title = strings.contactlessVitals,
            description = strings.rppgDescription,
            icon = Icons.Rounded.Favorite,
            onClick = onNavigateToRppg
        )

        Spacer(modifier = Modifier.height(16.dp))

        ToolCard(
            title = strings.capillaryRefillTest,
            description = strings.capillaryDescription,
            icon = Icons.Rounded.TouchApp,
            onClick = onNavigateToCapillaryRefill
        )
        
        Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Parchment),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalBlack
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGrey
                )
            }
            
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = LightGrey
            )
        }
    }
}
