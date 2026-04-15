package com.glucodes.swarmdoc.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.glucodes.swarmdoc.ui.theme.*
import com.glucodes.swarmdoc.viewmodel.SettingsViewModel

data class LanguageOption(
    val code: String,
    val localName: String,
    val englishName: String,
)

@Composable
fun LanguageSelectionScreen(
    onLanguageSelected: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val languages = listOf(
        LanguageOption("hi", "हिन्दी", "Hindi"),
        LanguageOption("or", "ଓଡ଼ିଆ", "Odia"),
        LanguageOption("te", "తెలుగు", "Telugu"),
        LanguageOption("bn", "বাংলা", "Bengali"),
        LanguageOption("en", "English", "English"),
    )

    var selectedLanguage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ForestGreen),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Choose Your Language",
                style = MaterialTheme.typography.displaySmall,
                color = TurmericGold,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "अपनी भाषा चुनें",
                style = MaterialTheme.typography.headlineSmall,
                color = TurmericGoldLight,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            languages.forEachIndexed { index, lang ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(300, delayMillis = index * 80),
                    ) + fadeIn(animationSpec = tween(300, delayMillis = index * 80)),
                ) {
                    val isSelected = selectedLanguage == lang.code
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) TurmericGold else Parchment,
                        animationSpec = tween(200),
                        label = "langBg"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(vertical = 6.dp)
                            .semantics { contentDescription = "Select ${lang.englishName} language" },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (isSelected) 8.dp else 2.dp
                        ),
                        onClick = {
                            selectedLanguage = lang.code
                            settingsViewModel.setLanguage(lang.code)
                            // Apply locale via AppCompatDelegate for system-level translation
                            val localeList = LocaleListCompat.forLanguageTags(lang.code)
                            AppCompatDelegate.setApplicationLocales(localeList)
                            onLanguageSelected()
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = lang.localName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CharcoalBlack else ForestGreen,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (lang.localName != lang.englishName) {
                                Text(
                                    text = lang.englishName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = WarmGrey,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "SwarmDoc • Aarogya Jaal",
                style = MaterialTheme.typography.bodySmall,
                color = White.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
