package com.reayt.youtube.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {

    val Home: ImageVector by lazy {
        ImageVector.Builder("Home", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(10f, 20f)
                verticalLineToRelative(-6f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(-8f)
                horizontalLineToRelative(3f)
                lineTo(12f, 3f)
                lineTo(2f, 12f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(8f)
                close()
            }
        }.build()
    }

    val HomeOutlined: ImageVector by lazy {
        ImageVector.Builder("HomeOutlined", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 5.69f)
                lineToRelative(5f, 4.5f)
                verticalLineTo(18f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-6f)
                horizontalLineTo(9f)
                verticalLineToRelative(6f)
                horizontalLineTo(7f)
                verticalLineToRelative(-7.81f)
                lineToRelative(5f, -4.5f)
                moveTo(12f, 3f)
                lineTo(2f, 12f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(8f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(-6f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(-8f)
                horizontalLineToRelative(3f)
                lineTo(12f, 3f)
                close()
            }
        }.build()
    }

    val Subscriptions: ImageVector by lazy {
        ImageVector.Builder("Subscriptions", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20f, 8f)
                horizontalLineTo(4f)
                verticalLineTo(6f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(2f)
                close()
                moveTo(18f, 2f)
                horizontalLineTo(6f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(12f)
                verticalLineTo(2f)
                close()
                moveTo(22f, 12f)
                verticalLineToRelative(8f)
                curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
                verticalLineToRelative(-8f)
                curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
                horizontalLineToRelative(16f)
                curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
                close()
                moveTo(10f, 18f)
                lineToRelative(6f, -3.5f)
                lineTo(10f, 11f)
                verticalLineToRelative(7f)
                close()
            }
        }.build()
    }

    val SubscriptionsOutlined: ImageVector by lazy {
        ImageVector.Builder("SubscriptionsOutlined", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 6f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(2f)
                horizontalLineTo(4f)
                close()
                moveTo(6f, 2f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(2f)
                horizontalLineTo(6f)
                close()
                moveTo(20f, 10f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(8f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(16f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineToRelative(-8f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(20f, 20f)
                horizontalLineTo(4f)
                verticalLineToRelative(-8f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(8f)
                close()
                moveTo(10f, 18f)
                lineToRelative(6f, -3.5f)
                lineTo(10f, 11f)
                close()
            }
        }.build()
    }

    val You: ImageVector by lazy {
        ImageVector.Builder("You", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 6f)
                horizontalLineTo(2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(-2f)
                horizontalLineTo(4f)
                verticalLineTo(6f)
                close()
                moveTo(20f, 2f)
                horizontalLineTo(8f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(12f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(4f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(12f, 14.5f)
                verticalLineToRelative(-9f)
                lineToRelative(6f, 4.5f)
                lineToRelative(-6f, 4.5f)
                close()
            }
        }.build()
    }

    val YouOutlined: ImageVector by lazy {
        ImageVector.Builder("YouOutlined", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 6f)
                horizontalLineTo(2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(-2f)
                horizontalLineTo(4f)
                verticalLineTo(6f)
                close()
                moveTo(20f, 2f)
                horizontalLineTo(8f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(12f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(4f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(20f, 16f)
                horizontalLineTo(8f)
                verticalLineTo(4f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(12f)
                close()
                moveTo(12f, 14.5f)
                lineToRelative(6f, -4.5f)
                lineToRelative(-6f, -4.5f)
                close()
            }
        }.build()
    }

    val Notifications: ImageVector by lazy {
        ImageVector.Builder("Notifications", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 22f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                horizontalLineToRelative(-4f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                close()
                moveTo(18f, 16f)
                verticalLineToRelative(-5f)
                curveToRelative(0f, -3.07f, -1.63f, -5.64f, -4.5f, -6.32f)
                verticalLineTo(4f)
                curveToRelative(0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
                reflectiveCurveToRelative(-1.5f, 0.67f, -1.5f, 1.5f)
                verticalLineToRelative(0.68f)
                curveTo(7.64f, 5.36f, 6f, 7.92f, 6f, 11f)
                verticalLineToRelative(5f)
                lineToRelative(-2f, 2f)
                verticalLineToRelative(1f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(-1f)
                lineToRelative(-2f, -2f)
                close()
            }
        }.build()
    }

    val Play: ImageVector by lazy {
        ImageVector.Builder("Play", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(8f, 5f)
                verticalLineToRelative(14f)
                lineToRelative(11f, -7f)
                close()
            }
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder("Pause", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 19f)
                horizontalLineToRelative(4f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                verticalLineToRelative(14f)
                close()
                moveTo(14f, 5f)
                verticalLineToRelative(14f)
                horizontalLineToRelative(4f)
                verticalLineTo(5f)
                horizontalLineToRelative(-4f)
                close()
            }
        }.build()
    }

    val Replay10: ImageVector by lazy {
        ImageVector.Builder("Replay10", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 5f)
                verticalLineTo(1f)
                lineTo(7f, 6f)
                lineToRelative(5f, 5f)
                verticalLineTo(7f)
                curveToRelative(3.31f, 0f, 6f, 2.69f, 6f, 6f)
                reflectiveCurveToRelative(-2.69f, 6f, -6f, 6f)
                reflectiveCurveToRelative(-6f, -2.69f, -6f, -6f)
                horizontalLineTo(4f)
                curveToRelative(0f, 4.42f, 3.58f, 8f, 8f, 8f)
                reflectiveCurveToRelative(8f, -3.58f, 8f, -8f)
                reflectiveCurveToRelative(-3.58f, -8f, -8f, -8f)
                close()
            }
        }.build()
    }

    val Forward10: ImageVector by lazy {
        ImageVector.Builder("Forward10", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 5f)
                verticalLineTo(1f)
                lineToRelative(5f, 5f)
                lineToRelative(-5f, 5f)
                verticalLineTo(7f)
                curveToRelative(-3.31f, 0f, -6f, 2.69f, -6f, 6f)
                reflectiveCurveToRelative(2.69f, 6f, 6f, 6f)
                reflectiveCurveToRelative(6f, -2.69f, 6f, -6f)
                horizontalLineToRelative(2f)
                curveToRelative(0f, 4.42f, -3.58f, 8f, -8f, 8f)
                reflectiveCurveToRelative(-8f, -3.58f, -8f, -8f)
                reflectiveCurveToRelative(3.58f, -8f, 8f, -8f)
                close()
            }
        }.build()
    }

    val Fullscreen: ImageVector by lazy {
        ImageVector.Builder("Fullscreen", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7f, 14f)
                horizontalLineTo(5f)
                verticalLineToRelative(5f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(-2f)
                horizontalLineTo(7f)
                verticalLineToRelative(-3f)
                close()
                moveTo(5f, 10f)
                horizontalLineToRelative(2f)
                verticalLineTo(7f)
                horizontalLineToRelative(3f)
                verticalLineTo(5f)
                horizontalLineTo(5f)
                verticalLineToRelative(5f)
                close()
                moveTo(17f, 17f)
                horizontalLineToRelative(-3f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(5f)
                verticalLineToRelative(-5f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(3f)
                close()
                moveTo(14f, 5f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(3f)
                horizontalLineToRelative(2f)
                verticalLineTo(5f)
                horizontalLineToRelative(-5f)
                close()
            }
        }.build()
    }

    val FullscreenExit: ImageVector by lazy {
        ImageVector.Builder("FullscreenExit", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(5f, 16f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(3f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-5f)
                horizontalLineTo(5f)
                verticalLineToRelative(2f)
                close()
                moveTo(8f, 8f)
                horizontalLineTo(5f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(5f)
                verticalLineTo(5f)
                horizontalLineTo(8f)
                verticalLineToRelative(3f)
                close()
                moveTo(14f, 19f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-3f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(-5f)
                verticalLineToRelative(5f)
                close()
                moveTo(16f, 8f)
                verticalLineTo(5f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(5f)
                horizontalLineToRelative(5f)
                verticalLineTo(8f)
                horizontalLineToRelative(-3f)
                close()
            }
        }.build()
    }

    val Headphones: ImageVector by lazy {
        ImageVector.Builder("Headphones", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 3f)
                curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
                verticalLineToRelative(7f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(-8f)
                horizontalLineTo(5f)
                verticalLineToRelative(-1f)
                curveToRelative(0f, -3.87f, 3.13f, -7f, 7f, -7f)
                reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
                verticalLineToRelative(1f)
                horizontalLineToRelative(-4f)
                verticalLineToRelative(8f)
                horizontalLineToRelative(4f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineToRelative(-7f)
                curveToRelative(0f, -4.97f, -4.03f, -9f, -9f, -9f)
                close()
            }
        }.build()
    }

    val WatchLater: ImageVector by lazy {
        ImageVector.Builder("WatchLater", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
                reflectiveCurveToRelative(4.5f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.5f, 10f, -10f)
                reflectiveCurveTo(17.5f, 2f, 12f, 2f)
                close()
                moveTo(16.2f, 16.2f)
                lineTo(11f, 13f)
                verticalLineTo(7f)
                horizontalLineToRelative(1.5f)
                verticalLineToRelative(5.2f)
                lineToRelative(4.5f, 2.7f)
                lineToRelative(-0.8f, 1.3f)
                close()
            }
        }.build()
    }

    val Download: ImageVector by lazy {
        ImageVector.Builder("Download", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(19f, 9f)
                horizontalLineToRelative(-4f)
                verticalLineTo(3f)
                horizontalLineTo(9f)
                verticalLineToRelative(6f)
                horizontalLineTo(5f)
                lineToRelative(7f, 7f)
                lineToRelative(7f, -7f)
                close()
                moveTo(5f, 18f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(-2f)
                horizontalLineTo(5f)
                close()
            }
        }.build()
    }

    val History: ImageVector by lazy {
        ImageVector.Builder("History", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(13f, 3f)
                curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
                horizontalLineTo(1f)
                lineToRelative(3.89f, 3.89f)
                lineToRelative(0.07f, 0.14f)
                lineTo(9f, 12f)
                horizontalLineTo(6f)
                curveToRelative(0f, -3.87f, 3.13f, -7f, 7f, -7f)
                reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
                reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f)
                curveToRelative(-1.93f, 0f, -3.68f, -0.79f, -4.94f, -2.06f)
                lineToRelative(-1.42f, 1.42f)
                curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
                curveToRelative(4.97f, 0f, 9f, -4.03f, 9f, -9f)
                reflectiveCurveToRelative(-4.03f, -9f, -9f, -9f)
                close()
                moveTo(12f, 8f)
                verticalLineToRelative(5f)
                lineToRelative(4.28f, 2.54f)
                lineToRelative(0.72f, -1.21f)
                lineToRelative(-3.5f, -2.08f)
                verticalLineTo(8f)
                horizontalLineTo(12f)
                close()
            }
        }.build()
    }

    val NorthWest: ImageVector by lazy {
        ImageVector.Builder("NorthWest", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(5f, 15f)
                verticalLineTo(5f)
                horizontalLineToRelative(10f)
                verticalLineToRelative(2f)
                horizontalLineTo(8.41f)
                lineToRelative(10.59f, 10.59f)
                lineTo(17.59f, 19f)
                lineTo(7f, 8.41f)
                verticalLineTo(15f)
                horizontalLineTo(5f)
                close()
            }
        }.build()
    }

    val KeyboardArrowDown: ImageVector by lazy {
        ImageVector.Builder("KeyboardArrowDown", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7.41f, 8.59f)
                lineTo(12f, 13.17f)
                lineToRelative(4.59f, -4.58f)
                lineTo(18f, 10f)
                lineToRelative(-6f, 6f)
                lineToRelative(-6f, -6f)
                lineToRelative(1.41f, -1.41f)
                close()
            }
        }.build()
    }

    val ThumbUp: ImageVector by lazy {
        ImageVector.Builder("ThumbUp", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(1f, 21f)
                horizontalLineToRelative(4f)
                verticalLineTo(9f)
                horizontalLineTo(1f)
                verticalLineToRelative(12f)
                close()
                moveTo(23f, 10f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineToRelative(-6.31f)
                lineToRelative(0.95f, -4.57f)
                lineToRelative(0.03f, -0.32f)
                curveToRelative(0f, -0.41f, -0.17f, -0.79f, -0.44f, -1.06f)
                lineTo(14.17f, 1f)
                lineTo(7.59f, 7.59f)
                curveTo(7.22f, 7.95f, 7f, 8.45f, 7f, 9f)
                verticalLineToRelative(10f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(9f)
                curveToRelative(0.83f, 0f, 1.54f, -0.5f, 1.84f, -1.22f)
                lineToRelative(3.02f, -7.05f)
                curveToRelative(0.09f, -0.23f, 0.14f, -0.47f, 0.14f, -0.73f)
                verticalLineToRelative(-2f)
                close()
            }
        }.build()
    }

    val ThumbUpOutlined: ImageVector by lazy {
        ImageVector.Builder("ThumbUpOutlined", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(9f, 21f)
                horizontalLineToRelative(9f)
                curveToRelative(0.83f, 0f, 1.54f, -0.5f, 1.84f, -1.22f)
                lineToRelative(3.02f, -7.05f)
                curveToRelative(0.09f, -0.23f, 0.14f, -0.47f, 0.14f, -0.73f)
                verticalLineToRelative(-2f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineToRelative(-6.31f)
                lineToRelative(0.95f, -4.57f)
                lineToRelative(0.03f, -0.32f)
                curveToRelative(0f, -0.41f, -0.17f, -0.79f, -0.44f, -1.06f)
                lineTo(14.17f, 1f)
                lineTo(7.58f, 7.59f)
                curveTo(7.22f, 7.95f, 7f, 8.45f, 7f, 9f)
                verticalLineToRelative(10f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                close()
                moveTo(9f, 9f)
                lineToRelative(4.34f, -4.34f)
                lineTo(12f, 10f)
                horizontalLineToRelative(9f)
                verticalLineToRelative(2f)
                lineToRelative(-3f, 7f)
                horizontalLineTo(9f)
                verticalLineTo(9f)
                close()
                moveTo(1f, 9f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(12f)
                horizontalLineTo(1f)
                close()
            }
        }.build()
    }
}
