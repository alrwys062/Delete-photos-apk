package com.example.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.db.MediaType
import com.example.data.db.ScheduleStatus
import com.example.data.db.ScheduledDeletionEntity
import com.example.data.prefs.AppSettings
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalTimeDeleteColors
import com.example.util.FormatUtils
import com.example.util.LocalAppLocale
import com.example.util.appString

private enum class ScheduledFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    AWAITING(R.string.filter_awaiting),
    PHOTOS(R.string.nav_photos),
    VIDEOS(R.string.nav_videos)
}

@Composable
fun ScheduledScreen(
    items: List<ScheduledDeletionEntity>,
    settings: AppSettings,
    nowMillis: Long,
    onPreview: (ScheduledDeletionEntity) -> Unit,
    onEditTimer: (ScheduledDeletionEntity) -> Unit,
    onCancelSchedule: (ScheduledDeletionEntity) -> Unit,
    onConfirmDelete: (List<ScheduledDeletionEntity>) -> Unit,
    onResumeAll: () -> Unit,
    onScheduleNewMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTimeDeleteColors.current
    var filter by remember { mutableStateOf(ScheduledFilter.ALL) }

    val awaitingItems = remember(items) {
        items.filter { it.status == ScheduleStatus.AWAITING_CONFIRMATION }
    }

    val filtered = remember(items, filter) {
        when (filter) {
            ScheduledFilter.ALL -> items
            ScheduledFilter.AWAITING -> items.filter { it.status == ScheduleStatus.AWAITING_CONFIRMATION }
            ScheduledFilter.PHOTOS -> items.filter { it.mediaType == MediaType.PHOTO }
            ScheduledFilter.VIDEOS -> items.filter { it.mediaType == MediaType.VIDEO }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (settings.isPausedAll) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ext.warningContainer,
                        border = BorderStroke(1.dp, ext.warning.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PauseCircle, contentDescription = null, tint = ext.warning)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = appString(R.string.scheduled_paused_notice),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onResumeAll) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(appString(R.string.action_resume))
                            }
                        }
                    }
                }
            }

            if (awaitingItems.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ext.dangerContainer,
                        border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ext.danger)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = appString(
                                        if (awaitingItems.size == 1) R.string.expired_single else R.string.expired_plural,
                                        awaitingItems.size
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = appString(R.string.expired_android_notice),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onConfirmDelete(awaitingItems) },
                                colors = ButtonDefaults.buttonColors(containerColor = ext.danger),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("authorize_all_expired_button")
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(appString(R.string.authorize_delete_count, awaitingItems.size), color = Color.White)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ScheduledFilter.entries.forEach { f ->
                        val count = when (f) {
                            ScheduledFilter.ALL -> items.size
                            ScheduledFilter.AWAITING -> awaitingItems.size
                            ScheduledFilter.PHOTOS -> items.count { it.mediaType == MediaType.PHOTO }
                            ScheduledFilter.VIDEOS -> items.count { it.mediaType == MediaType.VIDEO }
                        }
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = {
                                Text(
                                    "${appString(f.labelRes)} ($count)",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ext.elevatedCard,
                        border = BorderStroke(1.dp, ext.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = appString(R.string.empty_scheduled_title),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = appString(R.string.empty_scheduled_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onScheduleNewMedia,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(appString(R.string.schedule_new_media))
                            }
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { item ->
                    ScheduledItemCard(
                        item = item,
                        isPausedAll = settings.isPausedAll,
                        nowMillis = nowMillis,
                        onPreview = { onPreview(item) },
                        onEditTimer = { onEditTimer(item) },
                        onCancel = { onCancelSchedule(item) },
                        onConfirmDelete = { onConfirmDelete(listOf(item)) }
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = appString(R.string.workmanager_footer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduledItemCard(
    item: ScheduledDeletionEntity,
    isPausedAll: Boolean,
    nowMillis: Long,
    onPreview: () -> Unit,
    onEditTimer: () -> Unit,
    onCancel: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val ext = LocalTimeDeleteColors.current
    val locale = LocalAppLocale.current
    val remainingMillis = (item.deleteAtMillis - nowMillis).coerceAtLeast(0L)
    val totalDuration = item.originalDurationMillis.coerceAtLeast(1L)
    val progress = ((totalDuration - remainingMillis).toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    val isAwaiting = item.status == ScheduleStatus.AWAITING_CONFIRMATION || (!isPausedAll && remainingMillis == 0L)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ext.elevatedCard,
        border = BorderStroke(
            width = 1.dp,
            color = if (isAwaiting) ext.danger.copy(alpha = 0.6f) else ext.cardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scheduled_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onPreview)
                ) {
                    AsyncImage(
                        model = item.uriString,
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (item.mediaType == MediaType.VIDEO) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(3.dp)
                        ) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (item.mediaType == MediaType.VIDEO) {
                                ext.videoAccent.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            }
                        ) {
                            Text(
                                text = appString(
                                    if (item.mediaType == MediaType.VIDEO) R.string.media_type_video else R.string.media_type_photo
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.mediaType == MediaType.VIDEO) ext.videoAccent else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPausedAll) {
                            appString(R.string.status_paused_prefix, FormatUtils.formatRemainingReadable(remainingMillis, locale))
                        } else if (isAwaiting) {
                            appString(R.string.status_ready_auth)
                        } else {
                            appString(R.string.status_delete_in, FormatUtils.formatRemainingReadable(remainingMillis, locale))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isAwaiting) ext.danger else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = appString(R.string.scheduled_at_prefix, FormatUtils.formatDateTime(item.deleteAtMillis, locale)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAwaiting) "00:00:00" else FormatUtils.formatCountdown(remainingMillis),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = if (isAwaiting) ext.danger else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${FormatUtils.formatFileSize(item.sizeBytes, locale)} • ${item.folderName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (isAwaiting) 1f else progress },
                color = if (isAwaiting) ext.danger else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            if (!item.lastError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.lastError,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.danger
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isAwaiting) {
                    Button(
                        onClick = onConfirmDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = ext.danger),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("confirm_delete_item_${item.id}")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(appString(R.string.action_delete), color = Color.White, maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = onEditTimer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("edit_timer_${item.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(appString(R.string.action_edit), maxLines = 1)
                }

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("cancel_schedule_${item.id}")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(appString(R.string.action_cancel), maxLines = 1)
                }
            }
        }
    }
}
