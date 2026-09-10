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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CloudAccountEntity
import com.example.data.local.entity.MediaItemEntity
import com.example.data.local.entity.WatchPositionEntity
import com.example.ui.components.VideoCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudStorageScreen(
    cloudAccounts: List<CloudAccountEntity>,
    cloudMediaList: List<MediaItemEntity>,
    watchPositions: Map<String, WatchPositionEntity>,
    isSyncing: Boolean,
    onSyncAll: () -> Unit,
    onToggleAccount: (providerId: String, isConnected: Boolean) -> Unit,
    onPlayMedia: (MediaItemEntity) -> Unit,
    onToggleOffline: (MediaItemEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val connectedCount = cloudAccounts.count { it.isConnected }
    val latestSyncTime = cloudAccounts.maxOfOrNull { it.lastSyncTimestamp } ?: 0L

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .testTag("cloud_storage_root"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x3338BDF8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Penyimpanan Awan & Sinkronisasi",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Akses koleksi film dari cloud & sinkronkan riwayat tontonan antar perangkat secara instan.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
        }

        // Fast Device Sync Status Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Sinkronisasi Antar Perangkat Cepat",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (latestSyncTime > 0) {
                                        "Terakhir disinkronkan: ${formatDate(latestSyncTime)}"
                                    } else {
                                        "Belum disinkronkan"
                                    },
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0x2238BDF8), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AKTIF",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Posisi detik tontonan terakhir dan penanda subtitle otomatis disinkronkan ke semua smartphone, tablet, atau TV Anda.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSyncAll,
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_sync_now")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menyinkronkan Antar Perangkat...")
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sinkronkan Sekarang")
                        }
                    }
                }
            }
        }

        // Section: Connected Cloud Accounts
        item {
            Text(
                text = "Layanan Cloud Terhubung ($connectedCount aktif)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(cloudAccounts, key = { it.providerId }) { account ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cloud_card_${account.providerId}")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (account.isConnected) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = if (account.isConnected) Color(0xFF38BDF8) else Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = account.providerName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (account.isConnected) account.userEmail ?: "Terhubung" else "Belum terhubung",
                                    color = if (account.isConnected) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = account.isConnected,
                            onCheckedChange = { onToggleAccount(account.providerId, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0284C7)
                            ),
                            modifier = Modifier.testTag("switch_${account.providerId}")
                        )
                    }

                    if (account.isConnected) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Kapasitas Terpakai",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${account.storageUsedGb} GB / ${account.storageTotalGb} GB",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (account.storageUsedGb / account.storageTotalGb).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF334155)
                        )
                    }
                }
            }
        }

        // Section: Cloud Streamable Media
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pustaka Media Cloud",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${cloudMediaList.size} file",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        items(cloudMediaList, key = { it.id }) { media ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = media.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${media.cloudProvider ?: "Cloud"} • ${media.format} • ${media.fileSizeFormatted ?: "120 MB"}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { onToggleOffline(media) },
                            modifier = Modifier
                                .background(Color(0xFF0F172A), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (media.isOfflineAvailable) Icons.Default.CloudDone else Icons.Default.Download,
                                contentDescription = "Unduh Offline",
                                tint = if (media.isOfflineAvailable) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onPlayMedia(media) },
                            modifier = Modifier
                                .background(Color(0xFF0284C7), CircleShape)
                                .size(36.dp)
                                .testTag("btn_play_cloud_${media.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Putar",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
