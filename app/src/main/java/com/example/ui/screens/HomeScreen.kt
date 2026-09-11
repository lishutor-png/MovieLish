package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MediaItemEntity
import com.example.data.local.entity.WatchPositionEntity
import com.example.data.model.MediaFolder
import com.example.data.model.MediaType
import com.example.ui.LibraryFilter
import com.example.ui.NightModeOption
import com.example.ui.ViewMode
import com.example.ui.components.ContinueWatchingSection
import com.example.ui.components.FolderCard
import com.example.ui.components.MoviLishCinemaLogo
import com.example.ui.components.TvSeriesCard
import com.example.ui.components.VideoCard

enum class VideoSortOrder(val label: String) {
    LATEST("Terbaru"),
    NAME("Nama A-Z"),
    DURATION("Durasi")
}

@Composable
fun HomeScreen(
    mediaList: List<MediaItemEntity>,
    watchPositions: Map<String, WatchPositionEntity>,
    selectedFilter: LibraryFilter,
    selectedFolder: String?,
    viewMode: ViewMode,
    searchQuery: String,
    nightMode: NightModeOption,
    isScanning: Boolean,
    scanMessage: String?,
    onFilterChange: (LibraryFilter) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleNightMode: () -> Unit,
    onScanDevice: () -> Unit,
    onClearScanMessage: () -> Unit,
    onPlayMedia: (MediaItemEntity) -> Unit,
    onSelectSeries: (String) -> Unit,
    hasStoragePermission: Boolean = true,
    onRequestPermission: () -> Unit = {},
    onPickVideo: () -> Unit = {},
    onDeleteSingleMedia: (MediaItemEntity) -> Unit = {},
    onDeleteMultipleMedia: (List<MediaItemEntity>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedMediaIds by remember { mutableStateOf(setOf<String>()) }
    var itemToDeleteSingle by remember { mutableStateOf<MediaItemEntity?>(null) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    // Intercept back button when in multi-select mode or inside a folder
    BackHandler(enabled = isMultiSelectMode || selectedFolder != null) {
        if (isMultiSelectMode) {
            isMultiSelectMode = false
            selectedMediaIds = emptySet()
        } else {
            onSelectFolder(null)
        }
    }

    var sortOrder by remember { mutableStateOf(VideoSortOrder.LATEST) }

    // Folders grouping according to physical directories where videos reside
    val folderGroups = remember(mediaList) {
        mediaList.groupBy { it.folderName }
            .map { (name, items) ->
                MediaFolder(
                    name = name,
                    path = items.firstOrNull()?.folderPath,
                    videoCount = items.size,
                    totalDurationMs = items.sumOf { it.durationMs },
                    latestMedia = items.maxByOrNull { it.dateAdded },
                    items = items
                )
            }
            .sortedByDescending { it.videoCount }
    }

    // Filtered folders by search query
    val filteredFolders = remember(folderGroups, searchQuery) {
        if (searchQuery.isBlank()) {
            folderGroups
        } else {
            folderGroups.filter { folder ->
                folder.name.contains(searchQuery, ignoreCase = true) ||
                    folder.items.any { it.title.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    // Videos inside currently opened folder (if inside a folder)
    val currentFolderItems = remember(mediaList, selectedFolder, selectedFilter, searchQuery, sortOrder) {
        if (selectedFolder == null) emptyList() else {
            val list = mediaList
                .filter { it.folderName == selectedFolder }
                .filter { item ->
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
            when (sortOrder) {
                VideoSortOrder.LATEST -> list.sortedByDescending { it.dateAdded }
                VideoSortOrder.NAME -> list.sortedBy { it.title.lowercase() }
                VideoSortOrder.DURATION -> list.sortedByDescending { it.durationMs }
            }
        }
    }

    // Filtered items across all media (when in ALL_VIDEOS mode)
    val allFilteredItems = remember(mediaList, selectedFilter, searchQuery, sortOrder) {
        val list = mediaList.filter { item ->
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
        when (sortOrder) {
            VideoSortOrder.LATEST -> list.sortedByDescending { it.dateAdded }
            VideoSortOrder.NAME -> list.sortedBy { it.title.lowercase() }
            VideoSortOrder.DURATION -> list.sortedByDescending { it.durationMs }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (nightMode == NightModeOption.AMOLED_BLACK) Color.Black else Color(0xFF090D16))
            .testTag("home_screen_root")
    ) {
        // App Header (or Multi-Select Action Mode Header or Folder Breadcrumb Header)
        if (isMultiSelectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            isMultiSelectMode = false
                            selectedMediaIds = emptySet()
                        },
                        modifier = Modifier.testTag("btn_close_multi_select")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Batal Pilihan",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${selectedMediaIds.size} dipilih",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            val allIds = if (selectedFolder != null) {
                                currentFolderItems.map { it.id }.toSet()
                            } else {
                                allFilteredItems.map { it.id }.toSet()
                            }
                            selectedMediaIds = if (selectedMediaIds.size == allIds.size) {
                                emptySet()
                            } else {
                                allIds
                            }
                        },
                        modifier = Modifier.testTag("btn_select_all_toggle")
                    ) {
                        Text(
                            text = if (selectedMediaIds.isNotEmpty()) "Batal Semua" else "Pilih Semua",
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = {
                            if (selectedMediaIds.isNotEmpty()) {
                                showBatchDeleteConfirm = true
                            }
                        },
                        enabled = selectedMediaIds.isNotEmpty(),
                        modifier = Modifier.testTag("btn_batch_delete")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Video Terpilih",
                            tint = if (selectedMediaIds.isNotEmpty()) Color(0xFFEF4444) else Color(0xFF64748B)
                        )
                    }
                }
            }
        } else if (selectedFolder == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MoviLishCinemaLogo(size = 38.dp)
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
                    // Enter Multi-Selection Mode
                    IconButton(
                        onClick = { isMultiSelectMode = true },
                        modifier = Modifier
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("btn_enter_selection_mode")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Pilih Beberapa Video",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Pick Video from Files
                    IconButton(
                        onClick = onPickVideo,
                        modifier = Modifier
                            .background(Color(0x2238BDF8), CircleShape)
                            .border(1.dp, Color(0x4438BDF8), CircleShape)
                            .testTag("btn_pick_file")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Buka File Video",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

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
                                contentDescription = "Pindai Media HP",
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
        } else {
            // Folder Breadcrumb & Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = { onSelectFolder(null) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .border(1.dp, Color(0x4438BDF8), CircleShape)
                            .testTag("btn_back_to_folders")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Daftar Folder",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedFolder,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${currentFolderItems.size} video • Ketuk untuk memutar",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Enter Multi-Selection Mode inside folder
                    IconButton(
                        onClick = { isMultiSelectMode = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("btn_enter_selection_mode_folder")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "Pilih Video",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Pick Video
                    IconButton(
                        onClick = onPickVideo,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x2238BDF8), CircleShape)
                            .border(1.dp, Color(0x4438BDF8), CircleShape)
                            .testTag("btn_pick_file_folder")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Buka File Video",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Night Mode Toggle
                    IconButton(
                        onClick = onToggleNightMode,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("btn_toggle_night_mode_folder")
                    ) {
                        Icon(
                            imageVector = if (nightMode == NightModeOption.LIGHT) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Ganti Mode Malam",
                            tint = if (nightMode == NightModeOption.LIGHT) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
            placeholder = {
                Text(
                    text = if (selectedFolder != null) "Cari video di folder $selectedFolder..." else "Cari folder atau judul video...",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp
                )
            },
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

        Spacer(modifier = Modifier.height(10.dp))

        // Mode and Filter Bar
        if (selectedFolder == null) {
            // View Mode Switcher: Berdasarkan Folder (Default) vs Semua Video Langsung
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${folderGroups.size} Folder Video HP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = viewMode == ViewMode.FOLDERS,
                        onClick = { onViewModeChange(ViewMode.FOLDERS) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Folder HP", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8),
                            iconColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("mode_folders")
                    )

                    FilterChip(
                        selected = viewMode == ViewMode.ALL_VIDEOS,
                        onClick = { onViewModeChange(ViewMode.ALL_VIDEOS) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Semua Video", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8),
                            iconColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("mode_all_videos")
                    )
                }
            }
        } else {
            // Sub-filter inside opened folder + Sort Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedFilter == LibraryFilter.ALL,
                        onClick = { onFilterChange(LibraryFilter.ALL) },
                        label = { Text("Semua (${currentFolderItems.size})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("filter_folder_all")
                    )
                    FilterChip(
                        selected = selectedFilter == LibraryFilter.MOVIES,
                        onClick = { onFilterChange(LibraryFilter.MOVIES) },
                        label = { Text("Film (${currentFolderItems.count { it.mediaType == MediaType.MOVIE }})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("filter_folder_movies")
                    )
                    FilterChip(
                        selected = selectedFilter == LibraryFilter.TV_SERIES,
                        onClick = { onFilterChange(LibraryFilter.TV_SERIES) },
                        label = { Text("Serial (${currentFolderItems.count { it.mediaType == MediaType.TV_SERIES }})", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("filter_folder_tv")
                    )
                }

                // Interactive Sort Order Pill
                FilterChip(
                    selected = true,
                    onClick = {
                        sortOrder = when (sortOrder) {
                            VideoSortOrder.LATEST -> VideoSortOrder.NAME
                            VideoSortOrder.NAME -> VideoSortOrder.DURATION
                            VideoSortOrder.DURATION -> VideoSortOrder.LATEST
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = "Urutkan",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    label = { Text(sortOrder.label, fontSize = 11.sp, color = Color(0xFF38BDF8)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x2238BDF8),
                        selectedLabelColor = Color(0xFF38BDF8)
                    ),
                    modifier = Modifier.testTag("btn_sort_order_folder")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // MAIN CONTENT AREA
        when {
            // Case A: A Folder is Opened -> Show Videos inside that Folder
            selectedFolder != null -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (currentFolderItems.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Tidak ada video yang cocok di folder ini",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { onSelectFolder(null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Kembali ke Daftar Folder")
                                    }
                                }
                            }
                        }
                    } else {
                        items(currentFolderItems, key = { it.id }) { video ->
                            VideoCard(
                                media = video,
                                watchPosition = watchPositions[video.id],
                                onClick = {
                                    if (isMultiSelectMode) {
                                        selectedMediaIds = if (selectedMediaIds.contains(video.id)) {
                                            selectedMediaIds - video.id
                                        } else {
                                            selectedMediaIds + video.id
                                        }
                                    } else {
                                        onPlayMedia(video)
                                    }
                                },
                                isSelectionMode = isMultiSelectMode,
                                isSelected = selectedMediaIds.contains(video.id),
                                onToggleSelect = {
                                    if (!isMultiSelectMode) isMultiSelectMode = true
                                    selectedMediaIds = if (selectedMediaIds.contains(video.id)) {
                                        selectedMediaIds - video.id
                                    } else {
                                        selectedMediaIds + video.id
                                    }
                                },
                                onDeleteClick = {
                                    itemToDeleteSingle = video
                                }
                            )
                        }
                    }
                }
            }

            // Case B: In Folder Browsing Mode (Default view requested by user)
            viewMode == ViewMode.FOLDERS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Continue Watching header (if any video watched)
                    if (searchQuery.isBlank() && inProgressItems.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ContinueWatchingSection(
                                itemsWithProgress = inProgressItems,
                                onPlay = onPlayMedia
                            )
                        }
                    }

                    // Folder Section Title
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pilih Folder untuk Membuka Film",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${filteredFolders.size} folder",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Folder Cards
                    items(filteredFolders, key = { "folder_${it.name}" }) { folder ->
                        FolderCard(
                            folder = folder,
                            onClick = { onSelectFolder(folder.name) }
                        )
                    }

                    // Empty State for Folders
                    if (filteredFolders.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyLibraryCard(
                                hasStoragePermission = hasStoragePermission,
                                searchQuery = searchQuery,
                                onRequestPermission = onRequestPermission,
                                onScanDevice = onScanDevice,
                                onPickVideo = onPickVideo
                            )
                        }
                    }
                }
            }

            // Case C: Flat All Videos View
            else -> {
                val tvSeriesGroups = remember(allFilteredItems) {
                    allFilteredItems
                        .filter { it.mediaType == MediaType.TV_SERIES && !it.seriesName.isNullOrBlank() }
                        .groupBy { it.seriesName!! }
                }

                val standaloneMovies = remember(allFilteredItems) {
                    allFilteredItems.filter { it.mediaType == MediaType.MOVIE }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (searchQuery.isBlank() && inProgressItems.isNotEmpty() && selectedFilter == LibraryFilter.ALL) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ContinueWatchingSection(
                                itemsWithProgress = inProgressItems,
                                onPlay = onPlayMedia
                            )
                        }
                    }

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
                                onClick = {
                                    if (isMultiSelectMode) {
                                        selectedMediaIds = if (selectedMediaIds.contains(movie.id)) {
                                            selectedMediaIds - movie.id
                                        } else {
                                            selectedMediaIds + movie.id
                                        }
                                    } else {
                                        onPlayMedia(movie)
                                    }
                                },
                                isSelectionMode = isMultiSelectMode,
                                isSelected = selectedMediaIds.contains(movie.id),
                                onToggleSelect = {
                                    if (!isMultiSelectMode) isMultiSelectMode = true
                                    selectedMediaIds = if (selectedMediaIds.contains(movie.id)) {
                                        selectedMediaIds - movie.id
                                    } else {
                                        selectedMediaIds + movie.id
                                    }
                                },
                                onDeleteClick = {
                                    itemToDeleteSingle = movie
                                }
                            )
                        }
                    }

                    if (allFilteredItems.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyLibraryCard(
                                hasStoragePermission = hasStoragePermission,
                                searchQuery = searchQuery,
                                onRequestPermission = onRequestPermission,
                                onScanDevice = onScanDevice,
                                onPickVideo = onPickVideo
                            )
                        }
                    }
                }
            }
        }
    }

    // Single Video Delete Dialog
    if (itemToDeleteSingle != null) {
        val target = itemToDeleteSingle
        AlertDialog(
            onDismissRequest = { itemToDeleteSingle = null },
            title = {
                Text("Hapus Video?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus '${target?.title}'? File video akan dihapus dari penyimpanan perangkat dan daftar putar.",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDeleteSingle = null
                        if (target != null) {
                            onDeleteSingleMedia(target)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("btn_confirm_delete_single")
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteSingle = null }) {
                    Text("Batal", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Batch Delete Dialog
    if (showBatchDeleteConfirm) {
        val count = selectedMediaIds.size
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = {
                Text("Hapus $count Video Sekaligus?", fontWeight = FontWeight.Bold, color = Color.White)
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus $count video yang dipilih secara permanen dari penyimpanan perangkat?",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targets = mediaList.filter { selectedMediaIds.contains(it.id) }
                        selectedMediaIds = emptySet()
                        isMultiSelectMode = false
                        showBatchDeleteConfirm = false
                        onDeleteMultipleMedia(targets)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("btn_confirm_batch_delete")
                ) {
                    Text("Hapus Semua ($count)", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("Batal", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
private fun EmptyLibraryCard(
    hasStoragePermission: Boolean,
    searchQuery: String,
    onRequestPermission: () -> Unit,
    onScanDevice: () -> Unit,
    onPickVideo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Icon(
                imageVector = if (!hasStoragePermission) Icons.Default.VideoLibrary else Icons.Default.FolderOpen,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = when {
                    !hasStoragePermission -> "Izin Akses Video Diperlukan"
                    searchQuery.isNotEmpty() -> "Tidak ada folder cocok dengan \"$searchQuery\""
                    else -> "Folder Video Kosong"
                },
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when {
                    !hasStoragePermission -> "Izinkan MoviLish membaca memori internal dan kartu SD eksternal HP Anda agar semua video terorganisir per folder otomatis."
                    searchQuery.isNotEmpty() -> "Coba kata kunci lain atau bersihkan kotak pencarian."
                    else -> "Aplikasi ini kosongan tanpa video demo. Semua video akan otomatis terorganisir rapi dalam folder dari memori internal dan memori eksternal HP Anda."
                },
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))

            if (!hasStoragePermission) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_request_permission")
                ) {
                    Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Berikan Izin & Muat Folder Video")
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onScanDevice,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_rescan_device")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pindai Ulang HP")
                    }

                    Button(
                        onClick = onPickVideo,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_pick_manual_video")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pilih File Video", color = Color.White)
                    }
                }
            }
        }
    }
}
