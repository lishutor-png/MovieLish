package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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

@Composable
fun ContinueWatchingSection(
    itemsWithProgress: List<Pair<MediaItemEntity, WatchPositionEntity>>,
    onPlay: (MediaItemEntity) -> Unit,
    modifier: Modifier = Modifier,
    onRemoveFromHistory: ((MediaItemEntity) -> Unit)? = null
) {
    if (itemsWithProgress.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Lanjutkan Menonton",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(itemsWithProgress, key = { it.first.id }) { (media, pos) ->
                val progress = if (pos.durationMs > 0) {
                    (pos.positionMs.toFloat() / pos.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                val remainingMs = (pos.durationMs - pos.positionMs).coerceAtLeast(0L)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .width(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPlay(media) }
                        .testTag("continue_card_${media.id}")
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(Color(0xFF0F172A))
                        ) {
                            if (!media.thumbnailUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = media.thumbnailUri,
                                    contentDescription = media.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color(0x99000000))
                                        )
                                    )
                            )
                            // Play Button in center
                            Box(
                                 modifier = Modifier
                                     .align(Alignment.Center)
                                     .background(Color(0xCC0284C7), CircleShape)
                                     .padding(8.dp)
                            ) {
                                 Icon(
                                     imageVector = Icons.Default.PlayArrow,
                                     contentDescription = "Lanjutkan",
                                     tint = Color.White,
                                     modifier = Modifier.size(22.dp)
                                 )
                            }

                            // Remove from Continue Watching history button
                            if (onRemoveFromHistory != null) {
                                IconButton(
                                    onClick = { onRemoveFromHistory(media) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(30.dp)
                                        .background(Color(0xD90F172A), CircleShape)
                                        .border(1.dp, Color(0x33EF4444), CircleShape)
                                        .testTag("btn_remove_continue_${media.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus dari Lanjutkan Menonton",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Remaining time badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                                    .background(Color(0xCC090D16), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Sisa ${formatTime(remainingMs)}",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Progress Bar
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

                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = media.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Lanjutkan dari ${formatTime(pos.positionMs)}",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
