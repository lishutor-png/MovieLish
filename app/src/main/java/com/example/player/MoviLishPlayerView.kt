package com.example.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUp
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
import com.example.ui.components.SleepTimerSheet
import com.example.ui.components.SubtitleOverlay
import com.example.ui.components.SubtitleSettingsSheet
import com.example.ui.components.formatTime
import kotlinx.coroutines.Job
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

    // Audio Mute State
    var isMuted by remember { mutableStateOf(false) }
    var savedVolumeBeforeMute by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }

    // Controls visibility & screen lock
    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var sleepTimerState by remember { mutableStateOf(SleepTimerState()) }
    var sleepTimerRemainingSeconds by remember { mutableLongStateOf(0L) }

    // Active Subtitle
    var selectedSubtitleTrackId by remember {
        mutableStateOf<String?>(subtitleTracks.firstOrNull()?.id)
    }
    var activeCues by remember { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var currentSubtitleText by remember { mutableStateOf<String?>(null) }

    // Gesture State
    var gestureHudState by remember { mutableStateOf(PlayerGestureHudState()) }
    var doubleTapSeekDelta by remember { mutableStateOf<Long?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var singleTapJob by remember { mutableStateOf<Job?>(null) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var lastTapPositionX by remember { mutableFloatStateOf(0f) }

    // Screen Orientation Management (Auto-landscape on open, restore on exit, with toggle)
    val activity = remember(context) { context.findActivity() }
    var orientationMode by remember { mutableStateOf(ScreenOrientationMode.LANDSCAPE) }

    DisposableEffect(activity) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        // Otomatis posisi landscape saat video dibuka
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            // Kembalikan ke orientasi asal saat pemutar ditutup
            activity?.requestedOrientation = originalOrientation
        }
    }

    fun toggleOrientation() {
        val nextMode = when (orientationMode) {
            ScreenOrientationMode.LANDSCAPE -> ScreenOrientationMode.PORTRAIT
            ScreenOrientationMode.PORTRAIT -> ScreenOrientationMode.SENSOR
            ScreenOrientationMode.SENSOR -> ScreenOrientationMode.LANDSCAPE
        }
        orientationMode = nextMode
        when (nextMode) {
            ScreenOrientationMode.LANDSCAPE -> {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                gestureHudState = PlayerGestureHudState(
                    gestureType = GestureType.ORIENTATION,
                    message = "Orientasi: Landscape",
                    isVisible = true
                )
            }
            ScreenOrientationMode.PORTRAIT -> {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                gestureHudState = PlayerGestureHudState(
                    gestureType = GestureType.ORIENTATION,
                    message = "Orientasi: Potret (Portrait)",
                    isVisible = true
                )
            }
            ScreenOrientationMode.SENSOR -> {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                gestureHudState = PlayerGestureHudState(
                    gestureType = GestureType.ORIENTATION,
                    message = "Orientasi: Otomatis (Sensor)",
                    isVisible = true
                )
            }
        }
    }

    // Handle Back Press
    BackHandler {
        mediaPlayer?.let { player ->
            val pos = player.currentPosition.toLong()
            val dur = player.duration.toLong().coerceAtLeast(1L)
            onSavePosition(pos, dur, pos >= dur - 5000L)
        }
        onBack()
    }

    // Keep screen on while player is active
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

    // Auto-hide controls timer (4.5s)
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

    // Sleep Timer countdown & auto-pause engine
    LaunchedEffect(sleepTimerState, isPlaying) {
        while (sleepTimerState.option != SleepTimerOption.OFF && isPlaying) {
            if (sleepTimerState.option == SleepTimerOption.END_OF_VIDEO) {
                if (currentPositionMs >= totalDurationMs - 1500L && totalDurationMs > 0) {
                    mediaPlayer?.pause()
                    isPlaying = false
                    sleepTimerState = SleepTimerState()
                    gestureHudState = PlayerGestureHudState(
                        gestureType = GestureType.SLEEP_TIMER,
                        message = "🌙 Waktu Tidur • Video Selesai",
                        isVisible = true
                    )
                    break
                }
            } else {
                val remaining = (sleepTimerState.targetTimestampMs - System.currentTimeMillis()) / 1000
                sleepTimerRemainingSeconds = remaining.coerceAtLeast(0)
                if (remaining <= 0) {
                    mediaPlayer?.pause()
                    isPlaying = false
                    val wasFaded = sleepTimerState.fadeOutAudio
                    sleepTimerState = SleepTimerState()
                    gestureHudState = PlayerGestureHudState(
                        gestureType = GestureType.SLEEP_TIMER,
                        message = "🌙 Waktu Tidur Tiba • Pemutaran Dijeda",
                        isVisible = true
                    )
                    if (wasFaded && savedVolumeBeforeMute > 0) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolumeBeforeMute, 0)
                    }
                    break
                } else if (sleepTimerState.fadeOutAudio && remaining in 1..60) {
                    val factor = remaining.toFloat() / 60f
                    val targetVol = (savedVolumeBeforeMute * factor).toInt().coerceAtLeast(0)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                }
            }
            delay(1000)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen_root")
    ) {
        // Surface / TextureView for Video with Aspect Ratio Mode
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val videoModifier = when (aspectRatioMode) {
                AspectRatioMode.FIT -> Modifier.fillMaxSize()
                AspectRatioMode.FILL_CROP -> Modifier.fillMaxSize()
                AspectRatioMode.RATIO_16_9 -> Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                AspectRatioMode.RATIO_4_3 -> Modifier.fillMaxWidth().aspectRatio(4f / 3f)
            }

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
                modifier = videoModifier
            )
        }

        // Subtitle Overlay (renders on top of video, below HUD/controls)
        SubtitleOverlay(
            text = currentSubtitleText,
            config = subtitleStyleConfig
        )

        // Unified High-Responsiveness Gesture Touch Handler Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked) {
                    if (isLocked) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.changedToUp()) {
                                    change.consume()
                                    // Tapping when locked shows the lock hint
                                    gestureHudState = PlayerGestureHudState(
                                        gestureType = GestureType.LOCK_INFO,
                                        message = "Layar Terkunci • Ketuk buka kunci",
                                        isVisible = true
                                    )
                                    break
                                }
                            }
                        }
                        return@pointerInput
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        val startY = down.position.y
                        val startTime = down.uptimeMillis
                        var totalDragX = 0f
                        var totalDragY = 0f
                        var activeGesture = GestureType.NONE
                        var gestureCommitted = false
                        val startPositionMs = mediaPlayer?.currentPosition?.toLong() ?: 0L

                        val window = (context as? Activity)?.window
                        val curBright = window?.attributes?.screenBrightness ?: -1f
                        val initBrightness = if (curBright < 0f) 0.5f else curBright
                        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val initVolume = curVol.toFloat() / maxVolume

                        val pointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                            if (change.changedToUp()) {
                                change.consume()
                                if (gestureCommitted) {
                                    if (activeGesture == GestureType.SEEK) {
                                        mediaPlayer?.seekTo(gestureHudState.seekTargetMs.toInt())
                                        currentPositionMs = gestureHudState.seekTargetMs
                                    }
                                    coroutineScope.launch {
                                        delay(800)
                                        gestureHudState = gestureHudState.copy(isVisible = false)
                                    }
                                } else {
                                    // Touch released before dragging -> Tap Detection
                                    val elapsed = change.uptimeMillis - startTime
                                    if (elapsed < 320L) {
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapTimestamp < 350L && abs(startX - lastTapPositionX) < 140f) {
                                            // DOUBLE TAP DETECTED
                                            singleTapJob?.cancel()
                                            singleTapJob = null
                                            lastTapTimestamp = 0L

                                            val isRightSide = startX > size.width / 2
                                            val delta = if (isRightSide) 10000L else -10000L
                                            doubleTapSeekDelta = delta

                                            mediaPlayer?.let { player ->
                                                val target = (player.currentPosition + delta).coerceIn(0L, totalDurationMs)
                                                player.seekTo(target.toInt())
                                                currentPositionMs = target
                                            }
                                        } else {
                                            // FIRST TAP (wait briefly to distinguish single tap vs double tap)
                                            lastTapTimestamp = now
                                            lastTapPositionX = startX
                                            singleTapJob?.cancel()
                                            singleTapJob = coroutineScope.launch {
                                                delay(300)
                                                showControls = !showControls
                                            }
                                        }
                                    }
                                }
                                break
                            }

                            val dragX = change.position.x - startX
                            val dragY = change.position.y - startY
                            totalDragX = dragX
                            totalDragY = dragY

                            if (!gestureCommitted) {
                                val distSq = dragX * dragX + dragY * dragY
                                if (distSq > 400f) { // 20px threshold
                                    singleTapJob?.cancel()
                                    gestureCommitted = true
                                    activeGesture = if (abs(dragX) > abs(dragY) * 1.15f) {
                                        GestureType.SEEK
                                    } else if (startX < size.width / 2) {
                                        GestureType.BRIGHTNESS
                                    } else {
                                        GestureType.VOLUME
                                    }
                                }
                            }

                            if (gestureCommitted) {
                                change.consume()
                                when (activeGesture) {
                                    GestureType.SEEK -> {
                                        val seekRatio = totalDragX / size.width
                                        val seekDelta = (seekRatio * 90000L).toLong()
                                        val target = (startPositionMs + seekDelta).coerceIn(0L, totalDurationMs)
                                        gestureHudState = PlayerGestureHudState(
                                            gestureType = GestureType.SEEK,
                                            seekTargetMs = target,
                                            seekDeltaMs = seekDelta,
                                            isVisible = true
                                        )
                                    }
                                    GestureType.BRIGHTNESS -> {
                                        val delta = -totalDragY / size.height
                                        val newBright = (initBrightness + delta).coerceIn(0.05f, 1f)
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
                                    }
                                    GestureType.VOLUME -> {
                                        val delta = -totalDragY / size.height
                                        val newVolPercent = (initVolume + delta).coerceIn(0f, 1f)
                                        val targetVol = (newVolPercent * maxVolume).toInt()
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                        gestureHudState = PlayerGestureHudState(
                                            gestureType = GestureType.VOLUME,
                                            valuePercent = newVolPercent,
                                            isVisible = true
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
        )

        // Gesture HUD Overlay (Brightness / Volume / Seek / Double-Tap / Aspect Ratio / Lock Info)
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

        // Floating Lock Status / Unlock Button (Visible when locked or toggled)
        AnimatedVisibility(
            visible = isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xDD0F172A))
                    .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(20.dp))
                    .clickable {
                        isLocked = false
                        showControls = true
                        gestureHudState = PlayerGestureHudState(
                            gestureType = GestureType.LOCK_INFO,
                            message = "Kunci Layar Terbuka",
                            isVisible = true
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("btn_unlock_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Buka Kunci Layar",
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Layar Terkunci • Ketuk untuk membuka",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Lock Toggle Button at Center Left
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
                    if (isLocked) {
                        showControls = false
                        gestureHudState = PlayerGestureHudState(
                            gestureType = GestureType.LOCK_INFO,
                            message = "Layar Dikunci",
                            isVisible = true
                        )
                    } else {
                        showControls = true
                        gestureHudState = PlayerGestureHudState(
                            gestureType = GestureType.LOCK_INFO,
                            message = "Kunci Terbuka",
                            isVisible = true
                        )
                    }
                },
                modifier = Modifier
                    .background(Color(0xCC090D16), CircleShape)
                    .border(1.dp, if (isLocked) Color(0xFFFBBF24) else Color(0x33FFFFFF), CircleShape)
                    .size(48.dp)
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
                                Color(0xDD000000),
                                Color.Transparent,
                                Color(0xEE000000)
                            )
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showControls = false
                    }
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
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

                    Spacer(modifier = Modifier.width(6.dp))

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

                    // Mute / Unmute Quick Audio Button
                    IconButton(
                        onClick = {
                            if (isMuted) {
                                val restoreVol = if (savedVolumeBeforeMute > 0) savedVolumeBeforeMute else (maxVolume / 2)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, restoreVol, 0)
                                isMuted = false
                                gestureHudState = PlayerGestureHudState(
                                    gestureType = GestureType.VOLUME,
                                    valuePercent = restoreVol.toFloat() / maxVolume,
                                    isVisible = true
                                )
                            } else {
                                savedVolumeBeforeMute = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                                isMuted = true
                                gestureHudState = PlayerGestureHudState(
                                    gestureType = GestureType.VOLUME,
                                    valuePercent = 0f,
                                    isVisible = true
                                )
                            }
                        },
                        modifier = Modifier.testTag("btn_quick_mute")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Nyalakan Suara" else "Bisukan Suara",
                            tint = if (isMuted) Color(0xFFF87171) else Color.White
                        )
                    }

                    // Screen Orientation Toggle Button (Landscape / Portrait / Auto)
                    IconButton(
                        onClick = { toggleOrientation() },
                        modifier = Modifier.testTag("btn_orientation_toggle")
                    ) {
                        Icon(
                            imageVector = when (orientationMode) {
                                ScreenOrientationMode.PORTRAIT -> Icons.Default.StayCurrentPortrait
                                ScreenOrientationMode.LANDSCAPE -> Icons.Default.ScreenRotation
                                ScreenOrientationMode.SENSOR -> Icons.Default.ScreenRotation
                            },
                            contentDescription = "Ganti Orientasi Layar (${orientationMode.label})",
                            tint = if (orientationMode == ScreenOrientationMode.PORTRAIT) Color(0xFF38BDF8) else Color.White
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
                            gestureHudState = PlayerGestureHudState(
                                gestureType = GestureType.ASPECT_RATIO,
                                message = "Rasio: ${aspectRatioMode.label}",
                                isVisible = true
                            )
                        },
                        modifier = Modifier.testTag("btn_aspect_ratio")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Rasio Layar",
                            tint = Color.White
                        )
                    }

                    // Sleep Timer (Bedtime) Button
                    IconButton(
                        onClick = { showSleepTimerSheet = true },
                        modifier = Modifier.testTag("btn_sleep_timer")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Timer Tidur",
                            tint = if (sleepTimerState.option != SleepTimerOption.OFF) Color(0xFFFBBF24) else Color.White
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
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s Button
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
                                .size(52.dp)
                                .testTag("btn_rewind_10s")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Mundur 10 Detik",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Play / Pause / Replay Button
                        val isVideoEnded = !isPlaying && currentPositionMs >= totalDurationMs - 1500L && totalDurationMs > 0
                        IconButton(
                            onClick = {
                                mediaPlayer?.let { player ->
                                    if (isVideoEnded) {
                                        player.seekTo(0)
                                        player.start()
                                        isPlaying = true
                                        currentPositionMs = 0L
                                    } else if (player.isPlaying) {
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
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                                    ),
                                    CircleShape
                                )
                                .size(68.dp)
                                .testTag("btn_play_pause")
                        ) {
                            Icon(
                                imageVector = when {
                                    isVideoEnded -> Icons.Default.Replay
                                    isPlaying -> Icons.Default.Pause
                                    else -> Icons.Default.PlayArrow
                                },
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // Forward 10s Button
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
                                .size(52.dp)
                                .testTag("btn_forward_10s")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Maju 10 Detik",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // Bottom Timeline & Quick Action Controls Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
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

                    // Timestamps & Status Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = formatTime(currentPositionMs),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " / ${formatTime(totalDurationMs)}",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            val remainingMs = (totalDurationMs - currentPositionMs).coerceAtLeast(0L)
                            if (remainingMs > 0) {
                                Text(
                                    text = " (-${formatTime(remainingMs)})",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        // Badges (CC / Speed / Ratio)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Subtitle pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selectedSubtitleTrackId != null) Color(0x3338BDF8) else Color(0x22FFFFFF))
                                    .clickable { showSubtitleSheet = true }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (selectedSubtitleTrackId != null) "CC AKTIF" else "CC MATI",
                                    color = if (selectedSubtitleTrackId != null) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Speed pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (playbackSpeed != 1.0f) Color(0x3338BDF8) else Color(0x22FFFFFF))
                                    .clickable { showSpeedMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    color = if (playbackSpeed != 1.0f) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Sleep Timer Active Pill
                            if (sleepTimerState.option != SleepTimerOption.OFF) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x33FBBF24))
                                        .border(1.dp, Color(0x66FBBF24), RoundedCornerShape(6.dp))
                                        .clickable { showSleepTimerSheet = true }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("pill_sleep_timer")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Bedtime,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (sleepTimerState.option == SleepTimerOption.END_OF_VIDEO) {
                                                "TIDUR: SELESAI"
                                            } else {
                                                val mins = (sleepTimerRemainingSeconds / 60).coerceAtLeast(0)
                                                val secs = (sleepTimerRemainingSeconds % 60).coerceAtLeast(0)
                                                "TIDUR: ${mins}m ${secs}s"
                                            },
                                            color = Color(0xFFFBBF24),
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

        // Sleep Timer Bottom Sheet
        if (showSleepTimerSheet) {
            SleepTimerSheet(
                state = sleepTimerState,
                onSetTimer = { newState ->
                    sleepTimerState = newState
                    showSleepTimerSheet = false
                    if (newState.option != SleepTimerOption.OFF) {
                        val msg = if (newState.option == SleepTimerOption.END_OF_VIDEO) {
                            "Timer: Berhenti di akhir video"
                        } else {
                            val mins = ((newState.targetTimestampMs - System.currentTimeMillis()) / 60000L).coerceAtLeast(1)
                            "Timer tidur: $mins menit"
                        }
                        gestureHudState = PlayerGestureHudState(
                            gestureType = GestureType.SLEEP_TIMER,
                            message = msg,
                            isVisible = true
                        )
                    } else {
                        gestureHudState = PlayerGestureHudState(
                            gestureType = GestureType.SLEEP_TIMER,
                            message = "Timer tidur dinonaktifkan",
                            isVisible = true
                        )
                    }
                },
                onDismiss = { showSleepTimerSheet = false }
            )
        }
    }
}

enum class ScreenOrientationMode(val label: String) {
    LANDSCAPE("Landscape"),
    PORTRAIT("Potret (Portrait)"),
    SENSOR("Otomatis (Sensor)")
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

