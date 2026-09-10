package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.GestureType
import com.example.player.PlayerGestureHudState

@Composable
fun GestureHudOverlay(
    hudState: PlayerGestureHudState,
    doubleTapSeekDelta: Long?, // -10000 or +10000
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Brightness & Volume HUD
        AnimatedVisibility(
            visible = hudState.isVisible && (hudState.gestureType == GestureType.BRIGHTNESS || hudState.gestureType == GestureType.VOLUME),
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(300))
        ) {
            val isBrightness = hudState.gestureType == GestureType.BRIGHTNESS
            val percent = (hudState.valuePercent * 100).toInt().coerceIn(0, 100)

            Box(
                modifier = Modifier
                    .background(Color(0xCC090D16), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .testTag(if (isBrightness) "hud_brightness" else "hud_volume"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val icon = if (isBrightness) {
                        if (percent < 50) Icons.Default.BrightnessLow else Icons.Default.BrightnessMedium
                    } else {
                        when {
                            percent == 0 -> Icons.Default.VolumeMute
                            percent < 50 -> Icons.Default.VolumeDown
                            else -> Icons.Default.VolumeUp
                        }
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = if (isBrightness) "Kecerahan" else "Volume",
                        tint = if (isBrightness) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${if (isBrightness) "Kecerahan" else "Volume"}: $percent%",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { hudState.valuePercent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .width(130.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isBrightness) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                        trackColor = Color(0x33FFFFFF)
                    )
                }
            }
        }

        // Horizontal Drag Seeking HUD
        AnimatedVisibility(
            visible = hudState.isVisible && hudState.gestureType == GestureType.SEEK,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(300))
        ) {
            val deltaSec = hudState.seekDeltaMs / 1000
            val deltaPrefix = if (deltaSec >= 0) "+${deltaSec}s" else "${deltaSec}s"

            Box(
                modifier = Modifier
                    .background(Color(0xCC090D16), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .testTag("hud_seek"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (deltaSec >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = "Seek",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = formatTime(hudState.seekTargetMs),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Geser: $deltaPrefix",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Aspect Ratio Notification HUD
        AnimatedVisibility(
            visible = hudState.isVisible && hudState.gestureType == GestureType.ASPECT_RATIO,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .background(Color(0xCC090D16), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .testTag("hud_aspect_ratio"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Rasio Layar",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = hudState.message.ifBlank { "Rasio Diubah" },
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Screen Lock Status HUD
        AnimatedVisibility(
            visible = hudState.isVisible && hudState.gestureType == GestureType.LOCK_INFO,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(300))
        ) {
            val isLockedMsg = hudState.message.contains("Terkunci", ignoreCase = true)
            Box(
                modifier = Modifier
                    .background(Color(0xCC090D16), RoundedCornerShape(16.dp))
                    .border(1.dp, if (isLockedMsg) Color(0x66FBBF24) else Color(0x6638BDF8), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .testTag("hud_lock_info"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isLockedMsg) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Status Kunci",
                        tint = if (isLockedMsg) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = hudState.message,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Double Tap 10s Indicator (Positioned left or right based on seek direction)
        AnimatedVisibility(
            visible = doubleTapSeekDelta != null,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(if ((doubleTapSeekDelta ?: 0L) < 0) Alignment.CenterStart else Alignment.CenterEnd)
        ) {
            val isForward = (doubleTapSeekDelta ?: 0L) > 0
            Box(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .background(Color(0xDD090D16), RoundedCornerShape(24.dp))
                    .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .testTag(if (isForward) "hud_double_tap_forward" else "hud_double_tap_rewind"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isForward) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = "Double tap 10s",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isForward) "+10 Detik" else "-10 Detik",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
