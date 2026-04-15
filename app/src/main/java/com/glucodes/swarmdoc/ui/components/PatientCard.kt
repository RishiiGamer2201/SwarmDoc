package com.glucodes.swarmdoc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.glucodes.swarmdoc.domain.models.RiskLevel
import com.glucodes.swarmdoc.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PatientCard(
    initials: String,
    name: String?,
    topSymptom: String,
    riskLevel: String,
    timestamp: Long,
    followUpDue: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    val riskColor = when (riskLevel.uppercase()) {
        "EMERGENCY" -> CoralRed
        "URGENT" -> WarmAmber
        else -> SageGreen
    }

    val timeString = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Color bar indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (compact) 40.dp else 48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(riskColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(if (compact) 40.dp else 48.dp)
                    .clip(CircleShape)
                    .background(Parchment),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    color = ForestGreen,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name ?: "Anonymous Patient",
                        style = MaterialTheme.typography.titleMedium,
                        color = CharcoalBlack,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    
                    if (followUpDue) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.AccessTime,
                            contentDescription = "Follow up due",
                            tint = WarmAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = topSymptom,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.bodySmall,
                        color = LightGrey,
                    )
                }
            }
        }
    }
}
