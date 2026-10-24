package com.reayt.youtube.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reayt.youtube.player.PlayerStateHolder
import com.reayt.youtube.ui.theme.AppIcons
import com.reayt.youtube.ui.theme.YouTubeRed
import com.reayt.youtube.ui.viewmodel.YouViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    playerStateHolder: PlayerStateHolder,
    youViewModel: YouViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val isAutoplay by playerStateHolder.isAutoplayEnabled.collectAsState()
    val isAudioOnly by playerStateHolder.isAudioOnly.collectAsState()
    val selectedQuality by playerStateHolder.selectedQuality.collectAsState()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val qualityOptions = listOf("Auto", "1080p", "720p", "480p", "360p", "240p")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Playback Section
            item {
                Text(
                    text = "Playback",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = YouTubeRed,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Default Video Quality") },
                    supportingContent = { Text(selectedQuality) },
                    leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    modifier = Modifier.clickable { showQualityDialog = true }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Autoplay next video") },
                    supportingContent = { Text("When you finish a video, another plays automatically") },
                    trailingContent = {
                        Switch(
                            checked = isAutoplay,
                            onCheckedChange = { /* toggle autoplay */ },
                            colors = SwitchDefaults.colors(checkedThumbColor = YouTubeRed)
                        )
                    }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Audio-only Mode") },
                    supportingContent = { Text("Play only audio to save data and battery") },
                    leadingContent = { Icon(AppIcons.Headphones, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = isAudioOnly,
                            onCheckedChange = { playerStateHolder.toggleAudioOnly() },
                            colors = SwitchDefaults.colors(checkedThumbColor = YouTubeRed)
                        )
                    }
                )
            }

            // Data & History Section
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "History & Privacy",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = YouTubeRed,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Clear watch history") },
                    supportingContent = { Text("Clear this device's watch history") },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                    modifier = Modifier.clickable { showClearHistoryDialog = true }
                )
            }

            // About Section
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "About",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = YouTubeRed,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("YouTube Client") },
                    supportingContent = { Text("Version 1.0.0 (Production Architecture)") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear watch history?") },
            text = { Text("Your watch history will be cleared from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        youViewModel.clearHistory()
                        showClearHistoryDialog = false
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Watch history cleared")
                        }
                    }
                ) {
                    Text("Clear", color = YouTubeRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Select Default Quality") },
            text = {
                Column {
                    qualityOptions.forEach { q ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerStateHolder.setQuality(q)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            RadioButton(
                                selected = (q == selectedQuality),
                                onClick = {
                                    playerStateHolder.setQuality(q)
                                    showQualityDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = q, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
