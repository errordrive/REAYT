package com.reayt.youtube.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reayt.youtube.ui.theme.YouTubeDarkBg
import com.reayt.youtube.ui.theme.YouTubeTextPrimary

enum class YouTubeNavTab {
    HOME, SUBSCRIPTIONS, YOU
}

@Composable
fun YouTubeBottomNav(
    selectedTab: YouTubeNavTab,
    onTabSelected: (YouTubeNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = YouTubeDarkBg,
        tonalElevation = 6.dp,
        modifier = modifier.height(58.dp)
    ) {
        // Home Tab
        NavigationBarItem(
            selected = selectedTab == YouTubeNavTab.HOME,
            onClick = { onTabSelected(YouTubeNavTab.HOME) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    text = "Home",
                    fontSize = 11.sp,
                    fontWeight = if (selectedTab == YouTubeNavTab.HOME) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = YouTubeTextPrimary,
                selectedTextColor = YouTubeTextPrimary,
                unselectedIconColor = Color(0xFFAAAAAA),
                unselectedTextColor = Color(0xFFAAAAAA),
                indicatorColor = Color.Transparent
            )
        )

        // Subscriptions Tab
        NavigationBarItem(
            selected = selectedTab == YouTubeNavTab.SUBSCRIPTIONS,
            onClick = { onTabSelected(YouTubeNavTab.SUBSCRIPTIONS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Subscriptions,
                    contentDescription = "Subscriptions",
                    modifier = Modifier.size(23.dp)
                )
            },
            label = {
                Text(
                    text = "Subscriptions",
                    fontSize = 11.sp,
                    fontWeight = if (selectedTab == YouTubeNavTab.SUBSCRIPTIONS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = YouTubeTextPrimary,
                selectedTextColor = YouTubeTextPrimary,
                unselectedIconColor = Color(0xFFAAAAAA),
                unselectedTextColor = Color(0xFFAAAAAA),
                indicatorColor = Color.Transparent
            )
        )

        // You Tab
        NavigationBarItem(
            selected = selectedTab == YouTubeNavTab.YOU,
            onClick = { onTabSelected(YouTubeNavTab.YOU) },
            icon = {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "You",
                    modifier = Modifier.size(23.dp)
                )
            },
            label = {
                Text(
                    text = "You",
                    fontSize = 11.sp,
                    fontWeight = if (selectedTab == YouTubeNavTab.YOU) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = YouTubeTextPrimary,
                selectedTextColor = YouTubeTextPrimary,
                unselectedIconColor = Color(0xFFAAAAAA),
                unselectedTextColor = Color(0xFFAAAAAA),
                indicatorColor = Color.Transparent
            )
        )
    }
}
