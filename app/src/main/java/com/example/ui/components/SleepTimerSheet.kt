package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.SleepTimerOption
import com.example.player.SleepTimerState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    state: SleepTimerState,
    onSetTimer: (SleepTimerState) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedOption by remember { mutableStateOf(state.option) }
    var customMinutes by remember { mutableIntStateOf(state.customMinutes.coerceIn(5, 180)) }
    var isCustomSelected by remember { mutableStateOf(state.isCustom) }
    var fadeOutAudio by remember { mutableStateOf(state.fadeOutAudio) }

    // Live remaining countdown ticker
    var remainingSeconds by remember {
        mutableLongStateOf(
            if (state.option != SleepTimerOption.OFF && !state.isCustom && state.option == SleepTimerOption.END_OF_VIDEO) {
                -1L
            } else if (state.targetTimestampMs > System.currentTimeMillis()) {
                (state.targetTimestampMs - System.currentTimeMillis()) / 1000
            } else 0L
        )
    }

    LaunchedEffect(state.targetTimestampMs, state.option) {
        while (state.option != SleepTimerOption.OFF) {
            if (state.option == SleepTimerOption.END_OF_VIDEO) {
                remainingSeconds = -1L
            } else {
                val rem = (state.targetTimestampMs - System.currentTimeMillis()) / 1000
                remainingSeconds = rem.coerceAtLeast(0)
            }
            delay(1000)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = null,
        modifier = modifier.testTag("sleep_timer_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x3338BDF8))
                            .border(1.dp, Color(0x6638BDF8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Timer Tidur Sinema",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Hentikan video otomatis saat Anda tertidur",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_sleep_timer")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Countdown Card
            if (state.option != SleepTimerOption.OFF) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TIMER SEDANG BERJALAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (remainingSeconds < 0) {
                                "Berhenti saat video ini selesai"
                            } else {
                                val mins = remainingSeconds / 60
                                val secs = remainingSeconds % 60
                                String.format("%02d:%02d", mins, secs)
                            },
                            fontSize = if (remainingSeconds < 0) 18.sp else 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // +5 Menit
                            OutlinedButton(
                                onClick = {
                                    val newTarget = (state.targetTimestampMs.coerceAtLeast(System.currentTimeMillis())) + (5 * 60 * 1000L)
                                    onSetTimer(
                                        state.copy(
                                            option = SleepTimerOption.CUSTOM,
                                            isCustom = true,
                                            customMinutes = ((newTarget - System.currentTimeMillis()) / 60000L).toInt().coerceIn(5, 180),
                                            targetTimestampMs = newTarget
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("btn_add_5m")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF38BDF8)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+5m", color = Color(0xFF38BDF8), fontSize = 12.sp)
                            }

                            // +15 Menit
                            OutlinedButton(
                                onClick = {
                                    val newTarget = (state.targetTimestampMs.coerceAtLeast(System.currentTimeMillis())) + (15 * 60 * 1000L)
                                    onSetTimer(
                                        state.copy(
                                            option = SleepTimerOption.CUSTOM,
                                            isCustom = true,
                                            customMinutes = ((newTarget - System.currentTimeMillis()) / 60000L).toInt().coerceIn(5, 180),
                                            targetTimestampMs = newTarget
                                        )
                                    )
                                },
                                modifier = Modifier.testTag("btn_add_15m")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF38BDF8)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+15m", color = Color(0xFF38BDF8), fontSize = 12.sp)
                            }

                            // Matikan Timer
                            Button(
                                onClick = {
                                    selectedOption = SleepTimerOption.OFF
                                    isCustomSelected = false
                                    onSetTimer(SleepTimerState(option = SleepTimerOption.OFF))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444)
                                ),
                                modifier = Modifier.testTag("btn_turn_off_timer")
                            ) {
                                Text("Matikan", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Options List
            Text(
                text = "Pilih Durasi Waktu Tidur",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE2E8F0)
            )
            Spacer(modifier = Modifier.height(10.dp))

            val options = listOf(
                SleepTimerOption.OFF,
                SleepTimerOption.MIN_15,
                SleepTimerOption.MIN_30,
                SleepTimerOption.MIN_45,
                SleepTimerOption.MIN_60,
                SleepTimerOption.END_OF_VIDEO
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { opt ->
                    val isSelected = !isCustomSelected && selectedOption == opt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0x330284C7) else Color(0xFF1E293B))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                isCustomSelected = false
                                selectedOption = opt
                                if (opt == SleepTimerOption.OFF) {
                                    onSetTimer(SleepTimerState(option = SleepTimerOption.OFF, fadeOutAudio = fadeOutAudio))
                                } else if (opt == SleepTimerOption.END_OF_VIDEO) {
                                    onSetTimer(
                                        SleepTimerState(
                                            option = SleepTimerOption.END_OF_VIDEO,
                                            isCustom = false,
                                            targetTimestampMs = 0L,
                                            fadeOutAudio = fadeOutAudio
                                        )
                                    )
                                } else {
                                    val durationMs = opt.minutes * 60 * 1000L
                                    onSetTimer(
                                        SleepTimerState(
                                            option = opt,
                                            isCustom = false,
                                            targetTimestampMs = System.currentTimeMillis() + durationMs,
                                            fadeOutAudio = fadeOutAudio
                                        )
                                    )
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("opt_timer_${opt.name}"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = opt.label,
                                color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                            if (opt == SleepTimerOption.END_OF_VIDEO) {
                                Text(
                                    text = "Otomatis berhenti tepat saat film atau episode selesai",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Slider Duration Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isCustomSelected) Color(0x330284C7) else Color(0xFF1E293B))
                    .border(
                        width = 1.dp,
                        color = if (isCustomSelected) Color(0xFF38BDF8) else Color(0x22FFFFFF),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        isCustomSelected = true
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kustom (${customMinutes} Menit)",
                    color = if (isCustomSelected) Color(0xFF38BDF8) else Color.White,
                    fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 15.sp
                )
                if (isCustomSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isCustomSelected) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 8.dp, end = 8.dp)
                ) {
                    Slider(
                        value = customMinutes.toFloat(),
                        onValueChange = { customMinutes = it.toInt() },
                        valueRange = 5f..120f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF0284C7),
                            inactiveTrackColor = Color(0x4DFFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_custom_timer")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("5m", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text("${customMinutes} Menit", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("120m", color = Color(0xFF64748B), fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val durationMs = customMinutes * 60 * 1000L
                            onSetTimer(
                                SleepTimerState(
                                    option = SleepTimerOption.CUSTOM,
                                    isCustom = true,
                                    customMinutes = customMinutes,
                                    targetTimestampMs = System.currentTimeMillis() + durationMs,
                                    fadeOutAudio = fadeOutAudio
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_apply_custom_timer")
                    ) {
                        Text("Terapkan $customMinutes Menit", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Gentle Audio Fade-out Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Redupkan Audio Perlahan (Fade Out)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Menurunkan volume secara lembut 60s sebelum video berhenti",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = fadeOutAudio,
                    onCheckedChange = {
                        fadeOutAudio = it
                        if (state.option != SleepTimerOption.OFF) {
                            onSetTimer(state.copy(fadeOutAudio = it))
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF0284C7),
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.testTag("switch_fade_out")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
