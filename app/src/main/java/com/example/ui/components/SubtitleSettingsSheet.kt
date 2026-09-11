package com.example.ui.components

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.SubtitleBackgroundStyle
import com.example.data.model.SubtitleStyleConfig
import com.example.data.model.SubtitleTrack
import com.example.data.model.SubtitleVerticalPosition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsSheet(
    tracks: List<SubtitleTrack>,
    selectedTrackId: String?,
    config: SubtitleStyleConfig,
    onSelectTrack: (String?) -> Unit,
    onUpdateConfig: (SubtitleStyleConfig) -> Unit,
    onPickExternalFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Pengaturan Subtitle",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle Selection
            Text(
                text = "Trek Subtitle",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Off option
            Card(
                onClick = { onSelectTrack(null) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedTrackId == null) Color(0x3338BDF8) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .testTag("track_off")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nonaktifkan Subtitle",
                        color = if (selectedTrackId == null) Color(0xFF38BDF8) else Color.White,
                        fontWeight = if (selectedTrackId == null) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedTrackId == null) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8))
                    }
                }
            }

            // Available Tracks
            tracks.forEach { track ->
                val isSelected = selectedTrackId == track.id
                Card(
                    onClick = { onSelectTrack(track.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0x3338BDF8) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .testTag("track_${track.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.label,
                                color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${track.language.uppercase()} • Format ${track.format}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Button to load external SRT/VTT file
            OutlinedButton(
                onClick = onPickExternalFile,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_load_external_subtitle"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF38BDF8)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Muat File Subtitle Eksternal (.SRT / .VTT)")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subtitle Position
            Text(
                text = "Posisi Vertikal Subtitle",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SubtitleVerticalPosition.entries.forEach { pos ->
                    val label = when (pos) {
                        SubtitleVerticalPosition.BOTTOM -> "Bawah"
                        SubtitleVerticalPosition.CENTER -> "Tengah"
                        SubtitleVerticalPosition.TOP -> "Atas"
                    }
                    FilterChip(
                        selected = config.verticalPosition == pos,
                        onClick = { onUpdateConfig(config.copy(verticalPosition = pos)) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vertical Offset Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Jarak Tepi (Offset)",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "${config.verticalOffsetDp} dp",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }
            Slider(
                value = config.verticalOffsetDp.toFloat(),
                onValueChange = { onUpdateConfig(config.copy(verticalOffsetDp = it.toInt())) },
                valueRange = 10f..120f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF38BDF8),
                    activeTrackColor = Color(0xFF0284C7),
                    inactiveTrackColor = Color(0xFF334155)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ukuran Teks Subtitle",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "${config.fontSizeSp} sp",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }
            Slider(
                value = config.fontSizeSp.toFloat(),
                onValueChange = { onUpdateConfig(config.copy(fontSizeSp = it.toInt())) },
                valueRange = 14f..32f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF38BDF8),
                    activeTrackColor = Color(0xFF0284C7),
                    inactiveTrackColor = Color(0xFF334155)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle Text Color
            Text(
                text = "Warna Teks Subtitle",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val colors = listOf(
                    0xFFFFFFFF to "Putih",
                    0xFFFACC15 to "Kuning",
                    0xFF38BDF8 to "Sian",
                    0xFF4ADE80 to "Hijau"
                )
                colors.forEach { (hex, name) ->
                    val isSelected = config.textColorHex == hex
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF0284C7) else Color(0x66000000),
                                shape = CircleShape
                            )
                            .clickable { onUpdateConfig(config.copy(textColorHex = hex)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Background Style
            Text(
                text = "Latar Belakang Subtitle",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SubtitleBackgroundStyle.entries.forEach { style ->
                    val label = when (style) {
                        SubtitleBackgroundStyle.TRANSLUCENT_BLACK -> "Transparan"
                        SubtitleBackgroundStyle.SOLID_BLACK -> "Pekat"
                        SubtitleBackgroundStyle.SHADOW_OUTLINE -> "Bayangan"
                        SubtitleBackgroundStyle.NONE -> "Polos"
                    }
                    FilterChip(
                        selected = config.backgroundStyle == style,
                        onClick = { onUpdateConfig(config.copy(backgroundStyle = style)) },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle Audio Sync Delay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sinkronisasi Waktu (Offset Audio)",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                val sec = config.timingOffsetMs / 1000f
                Text(
                    text = "${if (sec > 0) "+$sec" else "$sec"}s",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(-2000L, -1000L, -500L, 0L, 500L, 1000L, 2000L).forEach { offset ->
                    val sec = offset / 1000f
                    val label = if (offset == 0L) "0s" else if (offset > 0) "+${sec}s" else "${sec}s"
                    FilterChip(
                        selected = config.timingOffsetMs == offset,
                        onClick = { onUpdateConfig(config.copy(timingOffsetMs = offset)) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
