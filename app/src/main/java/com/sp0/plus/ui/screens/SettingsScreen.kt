package com.sp0.plus.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sp0.plus.R
import com.sp0.plus.data.AudioQuality
import com.sp0.plus.data.NetworkModule
import com.sp0.plus.data.SettingsManager
import com.sp0.plus.data.YouTubeMusicClient
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenMediaOutput: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val wifiQuality by SettingsManager.wifiQuality.collectAsState()
    val cellularQuality by SettingsManager.cellularQuality.collectAsState()
    val crossfadeEnabled by SettingsManager.crossfadeEnabled.collectAsState()
    val crossfadeDurationMs by SettingsManager.crossfadeDurationMs.collectAsState()
    val fadeEffectsEnabled by SettingsManager.fadeEffectsEnabled.collectAsState()
    val forceMonoAudio by SettingsManager.forceMonoAudio.collectAsState()
    val gaplessPlayback by SettingsManager.gaplessPlayback.collectAsState()
    val normalizeVolume by SettingsManager.normalizeVolume.collectAsState()
    val automix by SettingsManager.automix.collectAsState()
    val autoBackup by SettingsManager.autoBackup.collectAsState()
    val equalizerPreset by SettingsManager.equalizerPreset.collectAsState()
    val equalizerBands by SettingsManager.equalizerBands.collectAsState()
    val bassBoostEnabled by SettingsManager.bassBoostEnabled.collectAsState()
    val amoledMode by SettingsManager.amoledMode.collectAsState()
    val userName by SettingsManager.userName.collectAsState()
    val userEmail by SettingsManager.userEmail.collectAsState()
    val accountTier by SettingsManager.accountTier.collectAsState()
    val cacheSize by SettingsManager.cacheSizeFormatted.collectAsState()

    val uiScope = rememberCoroutineScope()

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = SettingsManager.buildBackupJson(context)
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Backup saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: ""
                val success = SettingsManager.restoreFromJson(context, json)
                if (success) {
                    Toast.makeText(context, "Restored successfully from file", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Invalid backup file", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var showQualityDialogFor by remember { mutableStateOf<String?>(null) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = Color(0xFF121212)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Account Section
                item {
                    SettingsSectionTitle("ACCOUNT")
                    SettingsCard {
                        Surface(
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenProfile() }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1DB954),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = userName.take(1).uppercase(),
                                            color = Color.Black,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userName,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = accountTier,
                                        color = Color(0xFF1DB954),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = userEmail,
                                        color = Color(0xFF888888),
                                        fontSize = 11.sp
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFF888888)
                                )
                            }
                        }

                        Divider(color = Color(0xFF333333))

                        SettingsClickableRow(
                            title = "Log Out",
                            subtitle = "Signed in as $userName • Tap to switch account",
                            onClick = { showLogoutDialog = true }
                        )
                    }
                }

                // Audio Quality Section
                item {
                    SettingsSectionTitle("AUDIO QUALITY")

                    SettingsCard {
                        SettingsClickableRow(
                            title = "Streaming over Wi-Fi",
                            subtitle = wifiQuality.badge,
                            onClick = { showQualityDialogFor = "wifi" }
                        )

                        Divider(color = Color(0xFF333333))

                        SettingsClickableRow(
                            title = "Streaming over cellular",
                            subtitle = cellularQuality.badge,
                            onClick = { showQualityDialogFor = "cellular" }
                        )

                        Divider(color = Color(0xFF333333))

                        SettingsClickableRow(
                            title = "Equalizer & Sound Effects",
                            subtitle = "$equalizerPreset • ${if (bassBoostEnabled) "Bass Boost ON" else "Bass Boost OFF"}",
                            onClick = { showEqualizerDialog = true }
                        )
                    }
                }

                // Playback Section
                item {
                    SettingsSectionTitle("PLAYBACK")

                    SettingsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Crossfade",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${crossfadeDurationMs / 1000}s",
                                    color = Color(0xFF1DB954),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = (crossfadeDurationMs / 1000).toFloat(),
                                onValueChange = {
                                    SettingsManager.setCrossfadeDurationMs((it * 1000).toInt())
                                },
                                valueRange = 0f..12f,
                                steps = 11,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF1DB954),
                                    inactiveTrackColor = Color(0xFF404040)
                                )
                            )
                        }

                        Divider(color = Color(0xFF333333))

                        SettingsSwitchRow(
                            title = "Fade in & Fade out effects",
                            subtitle = "Smooth volume fades when playing, pausing, and skipping tracks",
                            checked = fadeEffectsEnabled,
                            onCheckedChange = { SettingsManager.setFadeEffectsEnabled(it) }
                        )

                        Divider(color = Color(0xFF333333))

                        SettingsSwitchRow(
                            title = "Force Mono Audio",
                            subtitle = "Downmix stereo to mono. Recommended for single-speaker phones to prevent missing audio channels",
                            checked = forceMonoAudio,
                            onCheckedChange = { SettingsManager.setForceMonoAudio(it) }
                        )

                        Divider(color = Color(0xFF333333))

                        SettingsSwitchRow(
                            title = "Gapless playback",
                            subtitle = "Allows seamless playback between tracks",
                            checked = gaplessPlayback,
                            onCheckedChange = { SettingsManager.setGaplessPlayback(it) }
                        )

                        Divider(color = Color(0xFF333333))

                        SettingsSwitchRow(
                            title = "Automix",
                            subtitle = "Allows smooth transitions between songs in a playlist",
                            checked = automix,
                            onCheckedChange = { SettingsManager.setAutomix(it) }
                        )

                        Divider(color = Color(0xFF333333))

                        SettingsSwitchRow(
                            title = "Normalize volume",
                            subtitle = "Set the same volume level for all tracks",
                            checked = normalizeVolume,
                            onCheckedChange = { SettingsManager.setNormalizeVolume(it) }
                        )
                    }
                }

                // Media Output & Devices
                item {
                    SettingsSectionTitle("DEVICES")

                    SettingsCard {
                        SettingsClickableRow(
                            title = "Audio output devices",
                            subtitle = "Select audio route or speaker",
                            onClick = onOpenMediaOutput
                        )
                    }
                }

                // Display & Theme Section
                item {
                    SettingsSectionTitle("DISPLAY & THEME")

                    SettingsCard {
                        SettingsSwitchRow(
                            title = "Pure AMOLED Black",
                            subtitle = "Optimize UI for OLED displays and battery savings",
                            checked = amoledMode,
                            onCheckedChange = { SettingsManager.setAmoledMode(it) }
                        )
                    }
                }

                // Storage & Cache Section
                item {
                    SettingsSectionTitle("STORAGE & CACHE")

                    SettingsCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Cached Audio Data",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = cacheSize,
                                    color = Color(0xFF888888),
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { showClearCacheDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                            ) {
                                Text(text = "Clear Cache", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Backup & Restore Section
                item {
                    SettingsSectionTitle("BACKUP & RESTORE")

                    SettingsCard {
                        SettingsSwitchRow(
                            title = "Auto-backup",
                            subtitle = "Automatically saves playlists and likes on exit",
                            checked = autoBackup,
                            onCheckedChange = { SettingsManager.setAutoBackup(it) }
                        )

                        Divider(color = Color(0xFF333333))

                        SettingsClickableRow(
                            title = "Back Up Now",
                            subtitle = "Export current playlists, history, and settings",
                            onClick = {
                                backupLauncher.launch("sp0_backup_${System.currentTimeMillis()}.json")
                            }
                        )

                        Divider(color = Color(0xFF333333))

                        SettingsClickableRow(
                            title = "Restore from File",
                            subtitle = "Import a previous Sp0+ backup file",
                            onClick = {
                                restoreLauncher.launch("application/json")
                            }
                        )
                    }
                }

                // About Section
                item {
                    SettingsSectionTitle("ABOUT")

                    SettingsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Sp0+ (Spotify Plus)",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Version 9.0.9876 (Build 909876)",
                                color = Color(0xFF1DB954),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AndroidX Media3 ExoPlayer • Jetpack Compose Material 3",
                                color = Color(0xFF888888),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Audio Quality Selector Dialog
        if (showQualityDialogFor != null) {
            val isWifi = showQualityDialogFor == "wifi"
            val currentQ = if (isWifi) wifiQuality else cellularQuality

            AlertDialog(
                onDismissRequest = { showQualityDialogFor = null },
                title = {
                    Text(
                        text = if (isWifi) "Wi-Fi Streaming Quality" else "Cellular Streaming Quality",
                        color = Color.White
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AudioQuality.values().forEach { q ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isWifi) SettingsManager.setWifiQuality(q) else SettingsManager.setCellularQuality(q)
                                        showQualityDialogFor = null
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = q.title,
                                        color = if (q == currentQ) Color(0xFF1DB954) else Color.White,
                                        fontWeight = if (q == currentQ) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = q.bitrate,
                                        color = Color(0xFF888888),
                                        fontSize = 11.sp
                                    )
                                }
                                if (q == currentQ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF1DB954)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showQualityDialogFor = null }) {
                        Text(text = "Close", color = Color(0xFF1DB954))
                    }
                },
                containerColor = Color(0xFF282828)
            )
        }

        // Clear Cache Confirmation Dialog
        if (showClearCacheDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheDialog = false },
                title = { Text(text = "Clear Cache?", color = Color.White) },
                text = {
                    Text(
                        text = "This will free up storage space. Your liked songs and playlists will not be affected.",
                        color = Color(0xFFB3B3B3)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            SettingsManager.clearCache(context)
                            showClearCacheDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                    ) {
                        Text("Clear", color = Color.Black)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF282828)
            )
        }

        // Equalizer & Sound Effects Dialog
        if (showEqualizerDialog) {
            val presets = listOf(
                "Flat (Lossless)" to "Natural, unmodified bit-perfect audio reproduction",
                "Bass Boost" to "Enhanced low frequencies for hip-hop and electronic",
                "Vocal Booster" to "Crisp, elevated midrange for podcasts and acoustic",
                "Electronic / EDM" to "Dynamic treble and punchy sub-bass",
                "Rock & Metal" to "Aggressive guitar drive and energetic rhythm",
                "Acoustic" to "Warm and balanced organic instrument profile",
                "Custom" to "Custom 5-band frequency profile"
            )

            AlertDialog(
                onDismissRequest = { showEqualizerDialog = false },
                title = { Text(text = "Equalizer & Sound Effects", color = Color.White) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E1E))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Dynamic Bass Boost", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Deep low-end harmonic saturation", color = Color(0xFF888888), fontSize = 11.sp)
                            }
                            Switch(
                                checked = bassBoostEnabled,
                                onCheckedChange = { SettingsManager.setBassBoostEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF1DB954)
                                )
                            )
                        }

                        Divider(color = Color(0xFF333333))

                        Text(text = "SOUND PRESETS", color = Color(0xFF888888), fontSize = 10.sp, fontWeight = FontWeight.Bold)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            presets.forEach { (presetName, desc) ->
                                val isSelected = equalizerPreset == presetName
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { SettingsManager.setEqualizerPreset(presetName) }
                                        .background(if (isSelected) Color(0xFF1DB954).copy(alpha = 0.15f) else Color.Transparent)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = presetName,
                                            color = if (isSelected) Color(0xFF1DB954) else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        Text(text = desc, color = Color(0xFF888888), fontSize = 10.sp)
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF1DB954),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (equalizerPreset == "Custom") {
                            Divider(color = Color(0xFF333333))
                            val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1A1A1A))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "CUSTOM 5-BAND FREQUENCY LEVELS",
                                    color = Color(0xFF1DB954),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                bandLabels.forEachIndexed { index, label ->
                                    val level = equalizerBands.getOrElse(index) { 0.toShort() }.toFloat()
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = label, color = Color.White, fontSize = 12.sp)
                                            val db = (level / 100).toInt()
                                            Text(text = "${if (db > 0) "+$db" else "$db"} dB", color = Color(0xFF888888), fontSize = 12.sp)
                                        }
                                        Slider(
                                            value = level,
                                            onValueChange = { newVal ->
                                                val updated = equalizerBands.copyOf()
                                                if (index < updated.size) {
                                                    updated[index] = newVal.toInt().toShort()
                                                    SettingsManager.setEqualizerBands(updated)
                                                }
                                            },
                                            valueRange = -1000f..1000f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF1DB954),
                                                activeTrackColor = Color(0xFF1DB954),
                                                inactiveTrackColor = Color(0xFF404040)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showEqualizerDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                    ) {
                        Text("Done", color = Color.Black)
                    }
                },
                containerColor = Color(0xFF282828)
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text(text = "Log out of Sp0+?", color = Color.White) },
                text = {
                    Text(
                        text = "You will be signed out and returned to the login screen.",
                        color = Color(0xFFB3B3B3)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            SettingsManager.logout()
                            showLogoutDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91429))
                    ) {
                        Text("Log Out", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF282828)
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = Color(0xFF888888),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E1E),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color(0xFF888888),
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF888888)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color(0xFF888888),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1DB954),
                uncheckedThumbColor = Color(0xFF888888),
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}
