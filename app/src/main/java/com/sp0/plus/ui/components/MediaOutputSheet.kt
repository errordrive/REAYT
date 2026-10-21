package com.sp0.plus.ui.components

import android.media.AudioDeviceInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sp0.plus.R
import com.sp0.plus.player.AudioDeviceType
import com.sp0.plus.player.DeviceOutputManager
import com.sp0.plus.player.OutputDevice

fun iconForDeviceType(type: Int): ImageVector = when (type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> Icons.Default.Bluetooth
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_WIRED_HEADSET  -> Icons.Default.Headphones
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_USB_DEVICE     -> Icons.Default.Usb
    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> Icons.Default.Hearing
    AudioDeviceInfo.TYPE_BUS            -> Icons.Default.DirectionsBus
    else                               -> Icons.Default.PhoneAndroid
}

@Composable
fun MediaOutputSheet(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val availableDevices by DeviceOutputManager.availableDevices.collectAsState()
    val currentDevice by DeviceOutputManager.currentDevice.collectAsState()
    val currentVolume by DeviceOutputManager.currentVolume.collectAsState()
    val bothSpeakersEnabled by com.sp0.plus.data.SettingsManager.bothSpeakersEnabled.collectAsState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        color = Color(0xFF181818)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_down),
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Audio Output Devices",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { DeviceOutputManager.refreshDevices() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFFB3B3B3),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Route Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF282828),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1DB954),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = iconForDeviceType(currentDevice.deviceType),
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CURRENT AUDIO ROUTE",
                            color = Color(0xFF1DB954),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = currentDevice.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Device Volume Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeDown,
                    contentDescription = null,
                    tint = Color(0xFFB3B3B3),
                    modifier = Modifier.size(22.dp)
                )

                Slider(
                    value = currentVolume,
                    onValueChange = { DeviceOutputManager.setVolume(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF1DB954),
                        inactiveTrackColor = Color(0xFF404040)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = Color(0xFFB3B3B3),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AVAILABLE OUTPUTS",
                color = Color(0xFF888888),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(availableDevices) { device ->
                    val isSelected = device.id == currentDevice.id

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF2E2E2E) else Color(0xFF1F1F1F),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { DeviceOutputManager.selectDevice(device) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = iconForDeviceType(device.deviceType)

                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF1DB954) else Color(0xFFB3B3B3),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    color = if (isSelected) Color(0xFF1DB954) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isSelected) "Connected & Active" else "Ready to connect",
                                    color = Color(0xFF888888),
                                    fontSize = 12.sp
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = Color(0xFF1DB954),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SOUND EFFECTS",
                        color = Color(0xFF888888),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1F1F1F),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Both Speakers for Sound",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Plays through both the main speaker and the ear speaker simultaneously",
                                    color = Color(0xFF888888),
                                    fontSize = 12.sp
                                )
                            }
                            Switch(
                                checked = bothSpeakersEnabled,
                                onCheckedChange = { com.sp0.plus.data.SettingsManager.setBothSpeakersEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF1DB954),
                                    uncheckedThumbColor = Color(0xFFB3B3B3),
                                    uncheckedTrackColor = Color(0xFF333333)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
