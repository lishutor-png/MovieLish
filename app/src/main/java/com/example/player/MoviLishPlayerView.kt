package com.example.player

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.entity.MediaItemEntity
import com.example.data.model.SubtitleCue
import com.example.data.model.SubtitleStyleConfig
import com.example.data.model.SubtitleTrack
import com.example.ui.components.GestureHudOverlay
import com.example.ui.components.SubtitleOverlay
import com.example.ui.components.SubtitleSettingsSheet
import com.example.ui.components.formatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun MoviLishPlayerView(
    media: MediaItemEntity,
    initialPositionMs: Long,
    subtitleTracks: List<SubtitleTrack>,
    subtitleStyleConfig: SubtitleStyleConfig,
    onSavePosition: (positionMs: Long, durationMs: Long, isCompleted: Boolean) -> Unit,
    onUpdateSubtitleConfig: (SubtitleStyleConfig) -> Unit,
    onPickExternalSubtitle: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPrepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(initialPositionMs) }
    var totalDurationMs by remember { mutableLongStateOf(media.durationMs) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var aspectRatioMode by remember { mutableStateOf(AspectRatioMode.FIT) }

    // Controls visibility & auto-hide
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }

    // Active Subtitle
    var selectedSubtitleTrackId by remember {
        mutableStateOf<String?>(subtitleTracks.firstOrNull()?.id)
    }
    var activeCues by remember { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var currentSubtitleText by remember { mutableStateOf<String?>(null) }

    // Gesture State
    var gestureHudState by remember { mutableStateOf(PlayerGestureHudState()) }
    var doubleTapSeekDelta by remember { mutableStateOf<Long?>(null) }
    var initialGestureBrightness by remember { mutableFloatStateOf(0.5f) }
    var initialGestureVolume by remember { mutableFloatStateOf(0.5f) }

    val coroutineScope = rememberCoroutineScope()

    // Handle Back Press
    BackHandler {
        mediaPlayer?.let { player ->
            val pos = player.currentPosition.toLong()
            val dur = player.duration.toLong().coerceAtLeast(1L)
            onSavePosition(pos, dur, pos >= dur - 5000L)
        }
        onBack()
    }

    // Keep screen on while playing
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Load active subtitle cues
    LaunchedEffect(selectedSubtitleTrackId, subtitleTracks, totalDurationMs) {
        val track = subtitleTracks.find { it.id == selectedSubtitleTrackId }
        activeCues = if (track != null) {
            if (track.isAutoGenerated) {
                SubtitleParser.generateAutoSubtitles(media.title, totalDurationMs, track.language)
            } else {
                SubtitleParser.parse(track.uriOrContent, track.format)
            }
        } else {
            emptyList()
        }
    }

    // Playback ticker: updates position, saves progress, and syncs subtitles
    LaunchedEffect(isPlaying, isPrepared) {
        while (isPlaying && isPrepared) {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    val pos = player.currentPosition.toLong()
                    val dur = player.duration.toLong().coerceAtLeast(1L)
                    currentPositionMs = pos
                    totalDurationMs = dur

                    // Update Subtitle
                    currentSubtitleText = SubtitleParser.findCueAt(
                        activeCues,
                        pos,
                        subtitleStyleConfig.timingOffsetMs
                    )?.text

                    // Periodic save (every 4 seconds)
                    if (pos % 4000L < 300L) {
                        onSavePosition(pos, dur, pos >= dur - 5000L)
                    }
                }
            }
            delay(250)
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4500)
            showControls = false
        }
    }

    // Dismiss HUD after gesture ends
    LaunchedEffect(gestureHudState.isVisible) {
        if (gestureHudState.isVisible) {
            delay(1200)
            gestureHudState = gestureHudState.copy(isVisible = false)
        }
    }

    // Dismiss Double Tap Ripple
    LaunchedEffect(doubleTapSeekDelta) {
        if (doubleTapSeekDelta != null) {
            delay(650)
            doubleTapSeekDelta = null
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen_root")
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Surface / TextureView for Video
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                            val surface = Surface(surfaceTexture)
                            val player = MediaPlayer().apply {
                                setSurface(surface)
                                try {
                                    setDataSource(ctx, Uri.parse(media.fileUri))
                                    prepareAsync()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                setOnPreparedListener { mp ->
                                    isPrepared = true
                                    isBuffering = false
                                    totalDurationMs = mp.duration.toLong()
                                    if (initialPositionMs in 1000 until totalDurationMs - 5000L) {
                                        mp.seekTo(initialPositionMs.toInt())
                                    }
                                    mp.start()
                                    isPlaying = true
                                }
                                setOnBufferingUpdateListener { _, _ -> }
                                setOnInfoListener { _, what, _ ->
                                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                                        isBuffering = true
                                    } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                                        isBuffering = false
                                    }
                                    true
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                    onSavePosition(totalDurationMs, totalDurationMs, true)
                                }
                            }
                            mediaPlayer = player
                        }

                        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {}
                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                            mediaPlayer?.let { player ->
                                val pos = player.currentPosition.toLong()
                                val dur = player.duration.toLong().coerceAtLeast(1L)
                                onSavePosition(pos, dur, pos >= dur - 5000L)
                                player.stop()
                                player.release()
                            }
                            mediaPlayer = null
                            return true
                        }
                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Subtitle Overlay (renders on top of video, below HUD/controls)
        SubtitleOverlay(
            text = currentSubtitleText,
            config = subtitleStyleConfig
        )

        // Gesture Touch Handler Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    if (isLocked) {
                        detectTapGestures(
                            onTap = { showControls = !showControls }
                        )
                        return@pointerInput
                    }

                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val isRightSide = offset.x > size.width / 2
                            val delta = if (isRightSide) 10000L else -10000L
                            doubleTapSeekDelta = delta
                            mediaPlayer?.let { player ->
                                val target = (player.currentPosition + delta)
                                    .coerceIn(0L, totalDurationMs)
                                player.seekTo(target.toInt())
                                currentPositionMs = target
                            }
                        },
                        onTap = {
                            showControls = !showControls
                        }
                    )
                }
                .pointerInput(isLocked) {
                    if (isLocked) return@pointerInput

                    var totalDragX = 0f
                    var totalDragY = 0f
                    var activeGesture = GestureType.NONE
                    var startPositionMs = 0L

                    detectDragGestures(
                        onDragStart = { offset ->
                            totalDragX = 0f
                            totalDragY = 0f
                            startPositionMs = mediaPlayer?.currentPosition?.toLong() ?: 0L
                            val isLeft = offset.x < size.width / 2
                            activeGesture = if (isLeft) GestureType.BRIGHTNESS else GestureType.VOLUME

                            // read current brightness
                            val window = (context as? Activity)?.window
                            val curBright = window?.attributes?.screenBrightness ?: -1f
                            initialGestureBrightness = if (curBright < 0f) 0.5f else curBright

                            // read current volume
                            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            initialGestureVolume = curVol.toFloat() / maxVolume
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            if (abs(totalDragX) > abs(totalDragY) * 1.5f && abs(totalDragX) > 20f) {
                                // Horizontal Seek Gesture
                                activeGesture = GestureType.SEEK
                                val seekRatio = totalDragX / size.width
                                val seekDelta = (seekRatio * 90000L).toLong() // +/- 90 seconds max per drag
                                val target = (startPositionMs + seekDelta).coerceIn(0L, totalDurationMs)

                                gestureHudState = PlayerGestureHudState(
                                    gestureType = GestureType.SEEK,
                                    seekTargetMs = target,
                                    seekDeltaMs = seekDelta,
                                    isVisible = true
                                )
                            } else if (activeGesture == GestureType.BRIGHTNESS) {
                                // Left vertical drag: Brightness
                                val delta = -totalDragY / size.height
                                val newBright = (initialGestureBrightness + delta).coerceIn(0.05f, 1f)
                                (context as? Activity)?.window?.let { win ->
                                    val lp = win.attributes
                                    lp.screenBrightness = newBright
                                    win.attributes = lp
                                }
                                gestureHudState = PlayerGestureHudState(
                                    gestureType = GestureType.BRIGHTNESS,
                                    valuePercent = newBright,
                                    isVisible = true
                                )
                            } else if (activeGesture == GestureType.VOLUME) {
                                // Right vertical drag: Volume
                                val delta = -totalDragY / size.height
                                val newVolPercent = (initialGestureVolume + delta).coerceIn(0f, 1f)
                                val targetVol = (newVolPercent * maxVolume).toInt()
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)

                                gestureHudState = PlayerGestureHudState(
                                    gestureType = GestureType.VOLUME,
                                    valuePercent = newVolPercent,
                                    isVisible = true
                                )
                            }
                        },
                        onDragEnd = {
                            if (activeGesture == GestureType.SEEK) {
                                mediaPlayer?.seekTo(gestureHudState.seekTargetMs.toInt())
                                currentPositionMs = gestureHudState.seekTargetMs
                            }
                            // Auto-hide HUD after 1 second
                            coroutineScope.launch {
                                delay(900)
                                gestureHudState = gestureHudState.copy(isVisible = false)
                            }
                        }
                    )
                }
        )

        // Gesture HUD Overlay (Brightness / Volume / Seek / Double-Tap)
        GestureHudOverlay(
            hudState = gestureHudState,
            doubleTapSeekDelta = doubleTapSeekDelta
        )

        // Buffering Indicator
        if (isBuffering && !isPrepared) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Memuat pemutaran video...",
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Lock button (always accessible when controls are visible or toggled)
        AnimatedVisibility(
            visible = showControls || isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        ) {
            IconButton(
                onClick = {
                    isLocked = !isLocked
                    if (isLocked) showControls = false
                },
                modifier = Modifier
                    .background(Color(0x99090D16), CircleShape)
                    .size(46.dp)
                    .testTag("btn_lock_screen")
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isLocked) "Buka Kunci" else "Kunci Layar",
                    tint = if (isLocked) Color(0xFFFBBF24) else Color.White
                )
            }
        }

        // Full Controls Overlay (Hidden when locked)
        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xCC000000),
                                Color.Transparent,
                                Color(0xEE000000)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            mediaPlayer?.let { player ->
                                val pos = player.currentPosition.toLong()
                                val dur = player.duration.toLong().coerceAtLeast(1L)
                                onSavePosition(pos, dur, pos >= dur - 5000L)
                            }
                            onBack()
                        },
                        modifier = Modifier.testTag("btn_player_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = media.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        val subInfo = buildString {
                            if (!media.seriesName.isNullOrBlank()) {
                                append(media.seriesName)
                                if (media.seasonNumber != null && media.episodeNumber != null) {
                                    append(" • S${media.seasonNumber}E${media.episodeNumber}")
                                }
                                append(" • ")
                            }
                            append(media.format)
                            if (media.cloudProvider != null) {
                                append(" • ${media.cloudProvider}")
                            }
                        }
                        Text(
                            text = subInfo,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    // Aspect Ratio Button
                    IconButton(
                        onClick = {
                            aspectRatioMode = when (aspectRatioMode) {
                                AspectRatioMode.FIT -> AspectRatioMode.FILL_CROP
                                AspectRatioMode.FILL_CROP -> AspectRatioMode.RATIO_16_9
                                AspectRatioMode.RATIO_16_9 -> AspectRatioMode.RATIO_4_3
                                AspectRatioMode.RATIO_4_3 -> AspectRatioMode.FIT
                            }
                        },
                        modifier = Modifier.testTag("btn_aspect_ratio")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Rasio Layar",
                            tint = Color.White
                        )
                    }

                    // Subtitle Button
                    IconButton(
                        onClick = { showSubtitleSheet = true },
                        modifier = Modifier.testTag("btn_open_subtitles")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Subtitle",
                            tint = if (selectedSubtitleTrackId != null) Color(0xFF38BDF8) else Color.White
                        )
                    }

                    // Playback Speed Button & Menu
                    Box {
                        IconButton(
                            onClick = { showSpeedMenu = true },
                            modifier = Modifier.testTag("btn_speed")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Kecepatan Putar",
                                tint = if (playbackSpeed != 1.0f) Color(0xFF38BDF8) else Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${speed}x",
                                            color = if (playbackSpeed == speed) Color(0xFF38BDF8) else Color.White,
                                            fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        playbackSpeed = speed
                                        showSpeedMenu = false
                                        try {
                                            mediaPlayer?.playbackParams = PlaybackParams().setSpeed(speed)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Middle Center Play/Pause & 10s Skips
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s
                        IconButton(
                            onClick = {
                                doubleTapSeekDelta = -10000L
                                mediaPlayer?.let { player ->
                                    val target = (player.currentPosition - 10000L).coerceAtLeast(0L)
                                    player.seekTo(target.toInt())
                                    currentPositionMs = target
                                }
                            },
                            modifier = Modifier
                                .background(Color(0x66090D16), CircleShape)
                                .size(48.dp)
                                .testTag("btn_rewind_10s")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Mundur 10 Detik",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Play / Pause
                        IconButton(
                            onClick = {
                                mediaPlayer?.let { player ->
                                    if (player.isPlaying) {
                                        player.pause()
                                        isPlaying = false
                                        onSavePosition(player.currentPosition.toLong(), totalDurationMs, false)
                                    } else {
                                        player.start()
                                        isPlaying = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .background(Color(0xFF0284C7), CircleShape)
                                .size(64.dp)
                                .testTag("btn_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        // Forward 10s
                        IconButton(
                            onClick = {
                                doubleTapSeekDelta = 10000L
                                mediaPlayer?.let { player ->
                                    val target = (player.currentPosition + 10000L).coerceIn(0L, totalDurationMs)
                                    player.seekTo(target.toInt())
                                    currentPositionMs = target
                                }
                            },
                            modifier = Modifier
                                .background(Color(0x66090D16), CircleShape)
                                .size(48.dp)
                                .testTag("btn_forward_10s")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Maju 10 Detik",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Bottom Timeline & Controls Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Timeline Slider
                    Slider(
                        value = currentPositionMs.toFloat().coerceIn(0f, totalDurationMs.toFloat().coerceAtLeast(1f)),
                        onValueChange = { newPos ->
                            currentPositionMs = newPos.toLong()
                        },
                        onValueChangeFinished = {
                            mediaPlayer?.seekTo(currentPositionMs.toInt())
                        },
                        valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF0284C7),
                            inactiveTrackColor = Color(0x4DFFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_progress_slider")
                    )

                    // Timestamps & Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatTime(currentPositionMs)} / ${formatTime(totalDurationMs)}",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (playbackSpeed != 1.0f) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (selectedSubtitleTrackId != null) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x3338BDF8), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CC ON",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subtitle Settings Bottom Sheet
        if (showSubtitleSheet) {
            SubtitleSettingsSheet(
                tracks = subtitleTracks,
                selectedTrackId = selectedSubtitleTrackId,
                config = subtitleStyleConfig,
                onSelectTrack = { trackId ->
                    selectedSubtitleTrackId = trackId
                },
                onUpdateConfig = onUpdateSubtitleConfig,
                onPickExternalFile = {
                    onPickExternalSubtitle()
                    showSubtitleSheet = false
                },
                onDismiss = { showSubtitleSheet = false }
            )
        }
    }
}
