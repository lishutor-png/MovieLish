package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.components.formatTime

@Composable
fun TvSeriesDetailScreen(
    seriesName: String,
    episodes: List<MediaItemEntity>,
    watchPositions: Map<String, WatchPositionEntity>,
    onPlayEpisode: (MediaItemEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seasons = remember(episodes) {
        episodes.mapNotNull { it.seasonNumber }.distinct().sorted()
            .ifEmpty { listOf(1) }
    }

    var selectedSeason by remember(seasons) {
        mutableIntStateOf(seasons.first())
    }

    val episodesInSeason = remember(episodes, selectedSeason) {
        episodes.filter { (it.seasonNumber ?: 1) == selectedSeason }
            .sortedBy { it.episodeNumber ?: 0 }
    }

    val sampleEpisode = episodes.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .testTag("series_detail_root")
    ) {
        // Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            if (!sampleEpisode?.thumbnailUri.isNullOrBlank()) {
                AsyncImage(
                    model = sampleEpisode?.thumbnailUri,
                    contentDescription = seriesName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = Color(0x33FFFFFF),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // Gradient Overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x99000000),
                                Color(0x33000000),
                                Color(0xFF090D16)
                            )
                        )
                    )
            )

            // Back Button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(top = 16.dp, start = 12.dp)
                    .background(Color(0x66000000), CircleShape)
                    .testTag("btn_series_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = Color.White
                )
            }

            // Bottom Title inside Hero
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0284C7), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SERIAL TELEVISI",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = seriesName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "${seasons.size} Musim • ${episodes.size} Total Episode • Pemindaian Pustaka Otomatis",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        // Season Selector Tabs
        if (seasons.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(seasons) { season ->
                    FilterChip(
                        selected = selectedSeason == season,
                        onClick = { selectedSeason = season },
                        label = { Text("Musim $season") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }

        // Episodes List
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(episodesInSeason, key = { it.id }) { ep ->
                val watchPos = watchPositions[ep.id]
                val progress = if (watchPos != null && watchPos.durationMs > 0) {
                    (watchPos.positionMs.toFloat() / watchPos.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPlayEpisode(ep) }
                        .testTag("episode_item_${ep.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail Preview
                        Box(
                            modifier = Modifier
                                .size(width = 100.dp, height = 62.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                        ) {
                            if (!ep.thumbnailUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = ep.thumbnailUri,
                                    contentDescription = ep.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color(0x990284C7), CircleShape)
                                    .padding(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Putar",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (progress > 0f) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(2.dp),
                                    color = Color(0xFF38BDF8),
                                    trackColor = Color(0x66000000)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Episode Meta
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Episode ${ep.episodeNumber ?: 1}: ${ep.episodeTitle ?: ep.title}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ep.format,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(text = "•", color = Color(0xFF64748B), fontSize = 10.sp)
                                Text(
                                    text = if (ep.durationMs > 0) formatTime(ep.durationMs) else "HD",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                                if (progress > 0f) {
                                    Text(text = "•", color = Color(0xFF64748B), fontSize = 10.sp)
                                    Text(
                                        text = "${(progress * 100).toInt()}% ditonton",
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
    }
}
