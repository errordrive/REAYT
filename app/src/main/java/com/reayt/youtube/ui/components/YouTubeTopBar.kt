package com.reayt.youtube.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reayt.youtube.ui.theme.AppIcons
import com.reayt.youtube.ui.theme.YouTubeRed

@Composable
fun YouTubeTopBar(
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Official YouTube Logo & Brand Mark
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 30.dp, height = 21.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(YouTubeRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "YouTube",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }

            Text(
                text = "YouTube",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.6).sp
            )
        }

        // Action Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = AppIcons.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(23.dp)
                )
            }

            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(23.dp)
                )
            }

            // User Avatar
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "U",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
