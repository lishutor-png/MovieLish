package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.MediaItemEntity
import com.example.data.local.entity.WatchPositionEntity
import com.example.data.model.MediaSource
import com.example.data.model.MediaType

@Composable
fun VideoCard(
    media: MediaItemEntity,
    watchPosition: WatchPositionEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (watchPosition != null && watchPosition.durationMs > 0) {
        (watchPosition.positionMs.toFloat() / watchPosition.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("video_card_${media.id}")
    ) {
        Column {
            // Poster / Thumbnail Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF0F172A))
            ) {
                if (!media.thumbnailUri.isNullOrBlank()) {
                    AsyncImage(
                        model = media.thumbnailUri,
                        contentDescription = media.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (media.mediaType == MediaType.TV_SERIES) Icons.Default.Tv else Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color(0x33FFFFFF),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Gradient Shade
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0x99000000)
                                )
                            )
                        )
                )

                // Format Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color(0xCC090D16), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = media.format,
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Source indicator (Cloud vs Offline)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (media.mediaSource == MediaSource.CLOUD || media.cloudProvider != null) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xCC090D16), CircleShape)
                                .padding(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Penyimpanan Awan",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    if (media.isOfflineAvailable) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xCC090D16), CircleShape)
                                .padding(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = "Offline Siap",
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Play Button Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0x990284C7), CircleShape)
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Duration Badge
                if (media.durationMs > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatTime(media.durationMs),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Watch Progress Bar along bottom edge of thumbnail
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0x66000000)
                    )
                }
            }

            // Info Body
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = media.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val detailText = buildString {
                        if (media.mediaType == MediaType.TV_SERIES && media.episodeNumber != null) {
                            append("Episode ${media.episodeNumber}")
                        } else if (media.releaseYear != null) {
                            append("${media.releaseYear}")
                        } else {
                            append("Film")
                        }
                        if (!media.fileSizeFormatted.isNullOrBlank()) {
                            append(" • ${media.fileSizeFormatted}")
                        }
                    }
                    Text(
                        text = detailText,
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        maxLines = 1
                    )

                    if (progress > 0f) {
                        Text(
                            text = "${(progress * 100).toInt()}% ditonton",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
