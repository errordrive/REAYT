package com.reayt.youtube.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reayt.youtube.ui.theme.AppIcons

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
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 4.dp,
        modifier = modifier.height(56.dp)
    ) {
        val isHome = selectedTab == YouTubeNavTab.HOME
        NavigationBarItem(
            selected = isHome,
            onClick = { onTabSelected(YouTubeNavTab.HOME) },
            icon = {
                Icon(
                    imageVector = if (isHome) AppIcons.Home else AppIcons.HomeOutlined,
                    contentDescription = "Home",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    text = "Home",
                    fontSize = 11.sp,
                    fontWeight = if (isHome) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = Color.Transparent
            )
        )

        val isSubs = selectedTab == YouTubeNavTab.SUBSCRIPTIONS
        NavigationBarItem(
            selected = isSubs,
            onClick = { onTabSelected(YouTubeNavTab.SUBSCRIPTIONS) },
            icon = {
                Icon(
                    imageVector = if (isSubs) AppIcons.Subscriptions else AppIcons.SubscriptionsOutlined,
                    contentDescription = "Subscriptions",
                    modifier = Modifier.size(23.dp)
                )
            },
            label = {
                Text(
                    text = "Subscriptions",
                    fontSize = 11.sp,
                    fontWeight = if (isSubs) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = Color.Transparent
            )
        )

        val isYou = selectedTab == YouTubeNavTab.YOU
        NavigationBarItem(
            selected = isYou,
            onClick = { onTabSelected(YouTubeNavTab.YOU) },
            icon = {
                Icon(
                    imageVector = if (isYou) AppIcons.You else AppIcons.YouOutlined,
                    contentDescription = "You",
                    modifier = Modifier.size(23.dp)
                )
            },
            label = {
                Text(
                    text = "You",
                    fontSize = 11.sp,
                    fontWeight = if (isYou) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = Color.Transparent
            )
        )
    }
}
