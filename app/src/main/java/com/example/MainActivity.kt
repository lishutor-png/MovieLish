package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.player.MoviLishPlayerView
import com.example.ui.MainViewModel
import com.example.ui.NightModeOption
import com.example.ui.screens.CloudStorageScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OfflineDownloadsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TvSeriesDetailScreen
import com.example.ui.theme.MoviLishTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val nightMode by viewModel.nightMode.collectAsStateWithLifecycle()

            MoviLishTheme(nightMode = nightMode) {
                MoviLishApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MoviLishApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    // State Collection
    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val offlineMedia by viewModel.offlineMedia.collectAsStateWithLifecycle()
    val cloudMedia by viewModel.cloudMedia.collectAsStateWithLifecycle()
    val watchPositions by viewModel.watchPositions.collectAsStateWithLifecycle()
    val cloudAccounts by viewModel.cloudAccounts.collectAsStateWithLifecycle()

    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val nightMode by viewModel.nightMode.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanMessage by viewModel.scanMessage.collectAsStateWithLifecycle()
    val isSyncingCloud by viewModel.isSyncingCloud.collectAsStateWithLifecycle()

    val activePlayingMedia by viewModel.activePlayingMedia.collectAsStateWithLifecycle()
    val activeSubtitleTracks by viewModel.activeSubtitleTracks.collectAsStateWithLifecycle()
    val subtitleConfig by viewModel.subtitleConfig.collectAsStateWithLifecycle()
    val initialResumePositionMs by viewModel.initialResumePositionMs.collectAsStateWithLifecycle()
    val selectedSeriesName by viewModel.selectedSeriesName.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }

    // Storage Permission Handling (Auto-request & Auto-load from Internal & External Storage)
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
        if (isGranted) {
            viewModel.scanDeviceLibrary()
        }
    }

    // Video File Picker (Manual file selector for picking any video file from internal/external memory)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importPickedVideo(it)
        }
    }

    // Auto-prompt permission or auto-scan on initial launch
    LaunchedEffect(Unit) {
        if (!hasStoragePermission) {
            permissionLauncher.launch(permissionToRequest)
        } else {
            viewModel.scanDeviceLibrary()
        }
    }

    // File Picker for External Subtitles (.srt, .vtt)
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                    reader.readText()
                }
                if (!content.isNullOrBlank()) {
                    val displayName = it.lastPathSegment?.substringAfterLast('/') ?: "Eksternal Subtitle"
                    val format = if (content.startsWith("WEBVTT")) "VTT" else "SRT"
                    viewModel.addExternalSubtitle(displayName, content, format)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // If a video is playing, show the fullscreen cinema player
    if (activePlayingMedia != null) {
        MoviLishPlayerView(
            media = activePlayingMedia!!,
            initialPositionMs = initialResumePositionMs,
            subtitleTracks = activeSubtitleTracks,
            subtitleStyleConfig = subtitleConfig,
            onSavePosition = { pos, dur, completed ->
                viewModel.savePlaybackPosition(pos, dur, completed)
            },
            onUpdateSubtitleConfig = { newConfig ->
                viewModel.updateSubtitleConfig(newConfig)
            },
            onPickExternalSubtitle = {
                subtitlePickerLauncher.launch(arrayOf("*/*", "text/plain", "application/x-subrip"))
            },
            onBack = {
                viewModel.closePlayer()
            }
        )
        return
    }

    // If viewing a TV Series detail page
    if (selectedSeriesName != null) {
        val seriesEpisodes = remember(allMedia, selectedSeriesName) {
            allMedia.filter { it.seriesName == selectedSeriesName }
        }
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TvSeriesDetailScreen(
                seriesName = selectedSeriesName!!,
                episodes = seriesEpisodes,
                watchPositions = watchPositions,
                onPlayEpisode = { viewModel.playMedia(it) },
                onBack = { viewModel.selectSeries(null) }
            )
        }
        return
    }

    // Main App Shell
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = if (nightMode == NightModeOption.AMOLED_BLACK) Color.Black else Color(0xFF0F172A),
                contentColor = Color.White,
                tonalElevation = 0.dp,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                // Tab 0: Library / Pustaka
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Filled.Movie else Icons.Outlined.Movie,
                            contentDescription = "Pustaka"
                        )
                    },
                    label = { Text("Pustaka", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        selectedTextColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0x3338BDF8),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_library")
                )

                // Tab 1: Cloud Storage
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Filled.Cloud else Icons.Outlined.Cloud,
                            contentDescription = "Cloud"
                        )
                    },
                    label = { Text("Cloud", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        selectedTextColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0x3338BDF8),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_cloud")
                )

                // Tab 2: Offline Player
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 2) Icons.Filled.DownloadDone else Icons.Outlined.DownloadDone,
                            contentDescription = "Offline"
                        )
                    },
                    label = { Text("Offline", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        selectedTextColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0x3338BDF8),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_offline")
                )

                // Tab 3: Settings
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Pengaturan"
                        )
                    },
                    label = { Text("Pengaturan", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF38BDF8),
                        selectedTextColor = Color(0xFF38BDF8),
                        indicatorColor = Color(0x3338BDF8),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> HomeScreen(
                    mediaList = allMedia,
                    watchPositions = watchPositions,
                    selectedFilter = selectedFilter,
                    selectedFolder = selectedFolder,
                    viewMode = viewMode,
                    searchQuery = searchQuery,
                    nightMode = nightMode,
                    isScanning = isScanning,
                    scanMessage = scanMessage,
                    onFilterChange = { viewModel.setFilter(it) },
                    onSelectFolder = { viewModel.selectFolder(it) },
                    onViewModeChange = { viewModel.setViewMode(it) },
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onToggleNightMode = {
                        val next = when (nightMode) {
                            NightModeOption.DARK -> NightModeOption.AMOLED_BLACK
                            NightModeOption.AMOLED_BLACK -> NightModeOption.LIGHT
                            NightModeOption.LIGHT -> NightModeOption.DARK
                            NightModeOption.SYSTEM -> NightModeOption.DARK
                        }
                        viewModel.setNightMode(next)
                    },
                    onScanDevice = {
                        if (!hasStoragePermission) {
                            permissionLauncher.launch(permissionToRequest)
                        } else {
                            viewModel.scanDeviceLibrary()
                        }
                    },
                    onClearScanMessage = { viewModel.clearScanMessage() },
                    onPlayMedia = { viewModel.playMedia(it) },
                    onSelectSeries = { viewModel.selectSeries(it) },
                    hasStoragePermission = hasStoragePermission,
                    onRequestPermission = { permissionLauncher.launch(permissionToRequest) },
                    onPickVideo = { videoPickerLauncher.launch(arrayOf("video/*")) },
                    onDeleteSingleMedia = { viewModel.deleteSingleMedia(it) },
                    onDeleteMultipleMedia = { viewModel.deleteMultipleMedia(it) },
                    onRenameMedia = { item, newName -> viewModel.renameMedia(item, newName) },
                    onRemoveFromContinueWatching = { viewModel.removeFromContinueWatching(it.id) }
                )

                1 -> CloudStorageScreen(
                    cloudAccounts = cloudAccounts,
                    cloudMediaList = cloudMedia,
                    watchPositions = watchPositions,
                    isSyncing = isSyncingCloud,
                    onSyncAll = { viewModel.syncAllCloudAccounts() },
                    onToggleAccount = { providerId, isConnected ->
                        viewModel.toggleCloudConnection(providerId, isConnected)
                    },
                    onPlayMedia = { viewModel.playMedia(it) },
                    onToggleOffline = { viewModel.toggleOfflineDownload(it) }
                )

                2 -> OfflineDownloadsScreen(
                    offlineMediaList = offlineMedia,
                    watchPositions = watchPositions,
                    onPlayMedia = { viewModel.playMedia(it) },
                    onRemoveOffline = { viewModel.toggleOfflineDownload(it) }
                )

                3 -> SettingsScreen(
                    currentNightMode = nightMode,
                    onSelectNightMode = { viewModel.setNightMode(it) }
                )
            }
        }
    }
}
