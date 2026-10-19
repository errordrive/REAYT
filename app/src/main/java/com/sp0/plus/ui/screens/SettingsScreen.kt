package com.sp0.plus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val gaplessPlayback by SettingsManager.gaplessPlayback.collectAsState()
    val normalizeVolume by SettingsManager.normalizeVolume.collectAsState()
    val automix by SettingsManager.automix.collectAsState()
    val autoBackup by SettingsManager.autoBackup.collectAsState()
    val equalizerPreset by SettingsManager.equalizerPreset.collectAsState()
    val bassBoostEnabled by SettingsManager.bassBoostEnabled.collectAsState()
    val amoledMode by SettingsManager.amoledMode.collectAsState()
    val userName by SettingsManager.userName.collectAsState()
    val userEmail by SettingsManager.userEmail.collectAsState()
    val accountTier by SettingsManager.accountTier.collectAsState()
    val cacheSize by SettingsManager.cacheSizeFormatted.collectAsState()

    val visitorData by SettingsManager.visitorData.collectAsState()
    val adblockDns by SettingsManager.adblockDnsEnabled.collectAsState()
    var visitorInput by remember(visitorData) { mutableStateOf(visitorData) }
    var fetchingVisitor by remember { mutableStateOf(false) }
    val uiScope = rememberCoroutineScope()

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

                // YouTube Music Section
                item {
                    SettingsSectionTitle("YOUTUBE MUSIC")

                    SettingsCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "X-Goog-Visitor-Id",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Sent with every YouTube Music request. Paste yours from music.youtube.com (DevTools > Network), or generate one.",
                                color = Color(0xFF888888),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = visitorInput,
                                onValueChange = { visitorInput = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF1DB954),
                                    unfocusedBorderColor = Color(0xFF444444)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        SettingsManager.setVisitorData(visitorInput)
                                        Toast.makeText(context, "Visitor ID saved", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                                ) { Text("Save", color = Color.Black, fontSize = 12.sp) }

                                Button(
                                    enabled = !fetchingVisitor,
                                    onClick = {
                                        fetchingVisitor = true
                                        uiScope.launch {
                                            val fresh = YouTubeMusicClient.fetchVisitorData()
                                            fetchingVisitor = false
                                            if (fresh != null) {
                                                SettingsManager.setVisitorData(fresh)
                                                Toast.makeText(context, "New Visitor ID generated", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Couldn't generate one. Check your connection.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                                ) { Text(if (fetchingVisitor) "Generating..." else "Generate", color = Color.White, fontSize = 12.sp) }
                            }
                        }

                        SettingsSwitchRow(
                            title = "Ad-blocking DNS (in-app)",
                            subtitle = "Resolve this app's traffic through AdGuard DNS (DNS-over-HTTPS)",
                            checked = adblockDns,
                            onCheckedChange = { SettingsManager.setAdblockDnsEnabled(it) }
                        )

                        SettingsClickableRow(
                            title = "System Private DNS",
                            subtitle = "Copies ${NetworkModule.PRIVATE_DNS_HOSTNAME}, then opens network settings. Choose Private DNS > provider hostname and paste.",
                            onClick = {
                                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Private DNS", NetworkModule.PRIVATE_DNS_HOSTNAME))
                                Toast.makeText(context, "Hostname copied. Paste it under Private DNS.", Toast.LENGTH_LONG).show()
                                try {
                                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            }
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
                            onClick = {}
                        )

                        Divider(color = Color(0xFF333333))

                        SettingsClickableRow(
                            title = "Restore from File",
                            subtitle = "Import a previous Sp0+ backup file",
                            onClick = {}
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
                "Acoustic" to "Warm and balanced organic instrument profile"
            )

            AlertDialog(
                onDismissRequest = { showEqualizerDialog = false },
                title = { Text(text = "Equalizer & Sound Effects", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
