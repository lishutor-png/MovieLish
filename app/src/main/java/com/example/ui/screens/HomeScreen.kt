package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MediaItemEntity
import com.example.data.local.entity.WatchPositionEntity
import com.example.data.model.MediaType
import com.example.ui.LibraryFilter
import com.example.ui.NightModeOption
import com.example.ui.components.ContinueWatchingSection
import com.example.ui.components.TvSeriesCard
import com.example.ui.components.VideoCard

@Composable
fun HomeScreen(
    mediaList: List<MediaItemEntity>,
    watchPositions: Map<String, WatchPositionEntity>,
    selectedFilter: LibraryFilter,
    searchQuery: String,
    nightMode: NightModeOption,
    isScanning: Boolean,
    scanMessage: String?,
    onFilterChange: (LibraryFilter) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleNightMode: () -> Unit,
    onScanDevice: () -> Unit,
    onClearScanMessage: () -> Unit,
    onPlayMedia: (MediaItemEntity) -> Unit,
    onSelectSeries: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter & Search
    val filteredItems = remember(mediaList, selectedFilter, searchQuery) {
        mediaList.filter { item ->
            val matchesType = when (selectedFilter) {
                LibraryFilter.ALL -> true
                LibraryFilter.MOVIES -> item.mediaType == MediaType.MOVIE
                LibraryFilter.TV_SERIES -> item.mediaType == MediaType.TV_SERIES
            }
            val matchesQuery = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                    (item.seriesName?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesType && matchesQuery
        }
    }

    // In-progress videos for "Continue Watching"
    val inProgressItems = remember(mediaList, watchPositions) {
        watchPositions.values
            .filter { !it.isCompleted && it.positionMs > 5000L && it.positionMs < (it.durationMs - 5000L) }
            .sortedByDescending { it.lastWatchedTimestamp }
            .mapNotNull { pos ->
                val media = mediaList.find { it.id == pos.mediaId }
                if (media != null) Pair(media, pos) else null
            }
    }

    // Group TV Series when filter is TV_SERIES or ALL
    val tvSeriesGroups = remember(filteredItems) {
        filteredItems
            .filter { it.mediaType == MediaType.TV_SERIES && !it.seriesName.isNullOrBlank() }
            .groupBy { it.seriesName!! }
    }

    val standaloneMovies = remember(filteredItems) {
        filteredItems.filter { it.mediaType == MediaType.MOVIE }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (nightMode == NightModeOption.AMOLED_BLACK) Color.Black else Color(0xFF090D16))
            .testTag("home_screen_root")
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.example.ui.components.MoviLishCinemaLogo(size = 38.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "MoviLish",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "Pemutar Video Sinema & Serial Cerdas",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Smart Media Scanner Button
                IconButton(
                    onClick = onScanDevice,
                    modifier = Modifier
                        .background(Color(0x2238BDF8), CircleShape)
                        .border(1.dp, Color(0x4438BDF8), CircleShape)
                        .testTag("btn_scan_device")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Pindai Media Cerdas",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Night Mode Toggle
                IconButton(
                    onClick = onToggleNightMode,
                    modifier = Modifier
                        .background(Color(0xFF1E293B), CircleShape)
                        .testTag("btn_toggle_night_mode")
                ) {
                    Icon(
                        imageVector = if (nightMode == NightModeOption.LIGHT) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Ganti Mode Malam",
                        tint = if (nightMode == NightModeOption.LIGHT) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Notification / Scan Result Banner
        if (!scanMessage.isNullOrBlank()) {
            Snackbar(
                action = {
                    TextButton(onClick = onClearScanMessage) {
                        Text("OK", color = Color(0xFF38BDF8))
                    }
                },
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Text(scanMessage, fontSize = 13.sp)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari judul film, serial, atau episode...", color = Color(0xFF64748B), fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Cari", tint = Color(0xFF94A3B8))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = Color(0xFF94A3B8))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A),
                focusedBorderColor = Color(0xFF0284C7),
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("search_bar")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == LibraryFilter.ALL,
                onClick = { onFilterChange(LibraryFilter.ALL) },
                label = { Text("Semua (${mediaList.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF1E293B),
                    labelColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier.testTag("filter_all")
            )
            FilterChip(
                selected = selectedFilter == LibraryFilter.MOVIES,
                onClick = { onFilterChange(LibraryFilter.MOVIES) },
                label = { Text("Film (${mediaList.count { it.mediaType == MediaType.MOVIE }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF1E293B),
                    labelColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier.testTag("filter_movies")
            )
            FilterChip(
                selected = selectedFilter == LibraryFilter.TV_SERIES,
                onClick = { onFilterChange(LibraryFilter.TV_SERIES) },
                label = { Text("Serial TV (${tvSeriesGroups.size} Serial)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF1E293B),
                    labelColor = Color(0xFF94A3B8)
                ),
                modifier = Modifier.testTag("filter_tv")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Grid with Continue Watching header
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Continue Watching Header (Only when search is blank)
            if (searchQuery.isBlank() && inProgressItems.isNotEmpty() && selectedFilter == LibraryFilter.ALL) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ContinueWatchingSection(
                        itemsWithProgress = inProgressItems,
                        onPlay = onPlayMedia
                    )
                }
            }

            // TV Series Section (If filtered to TV or ALL)
            if ((selectedFilter == LibraryFilter.ALL || selectedFilter == LibraryFilter.TV_SERIES) && tvSeriesGroups.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Serial Televisi Terkategori",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${tvSeriesGroups.size} judul",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                items(tvSeriesGroups.keys.toList(), key = { "series_$it" }) { seriesName ->
                    val episodes = tvSeriesGroups[seriesName] ?: emptyList()
                    TvSeriesCard(
                        seriesName = seriesName,
                        episodes = episodes,
                        onClick = { onSelectSeries(seriesName) }
                    )
                }
            }

            // Movies Section (If filtered to Movies or ALL)
            if ((selectedFilter == LibraryFilter.ALL || selectedFilter == LibraryFilter.MOVIES) && standaloneMovies.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Koleksi Film",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${standaloneMovies.size} film",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                items(standaloneMovies, key = { it.id }) { movie ->
                    VideoCard(
                        media = movie,
                        watchPosition = watchPositions[movie.id],
                        onClick = { onPlayMedia(movie) }
                    )
                }
            }

            // Empty State
            if (filteredItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Tidak ada media cocok dengan pencarian" else "Belum ada media terdaftar",
                                color = Color(0xFF94A3B8),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onScanDevice,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pindai Perangkat Sekarang")
                            }
                        }
                    }
                }
            }
        }
    }
}
