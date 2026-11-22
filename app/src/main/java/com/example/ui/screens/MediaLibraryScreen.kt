package com.example.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
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
import com.example.data.db.ProtectedFolderEntity
import com.example.data.db.ScheduledDeletionEntity
import com.example.data.db.TrackedMediaEntity
import com.example.ui.theme.LocalTimeDeleteColors
import com.example.util.FormatUtils
import com.example.util.LocalAppLocale
import com.example.util.appString

private enum class MediaFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    UNSCHEDULED(R.string.filter_unscheduled),
    SCHEDULED(R.string.filter_scheduled)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaLibraryScreen(
    mediaType: MediaType,
    items: List<TrackedMediaEntity>,
    selectedUris: Set<String>,
    scheduledItems: List<ScheduledDeletionEntity>,
    protectedFolders: List<ProtectedFolderEntity>,
    nowMillis: Long,
    onPickFromPhotoPicker: () -> Unit,
    onPickFromDocuments: () -> Unit,
    onToggleSelect: (TrackedMediaEntity) -> Unit,
    onSelectAll: (List<String>) -> Unit,
    onDeselectAll: () -> Unit,
    onPreview: (TrackedMediaEntity) -> Unit,
    onScheduleSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTimeDeleteColors.current
    val locale = LocalAppLocale.current
    var activeFilter by remember { mutableStateOf(MediaFilter.ALL) }

    val scheduledMap = remember(scheduledItems) {
        scheduledItems.associateBy { it.uriString }
    }
    val protectedNames = remember(protectedFolders) {
        protectedFolders.map { it.folderName.lowercase().trim() }
    }

    val filteredItems = remember(items, activeFilter, scheduledMap) {
        when (activeFilter) {
            MediaFilter.ALL -> items
            MediaFilter.UNSCHEDULED -> items.filter { it.uriString !in scheduledMap }
            MediaFilter.SCHEDULED -> items.filter { it.uriString in scheduledMap }
        }
    }

    val visibleUris = remember(filteredItems) { filteredItems.map { it.uriString } }
    val allVisibleSelected = visibleUris.isNotEmpty() && visibleUris.all { it in selectedUris }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPickFromPhotoPicker,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("browse_${mediaType.name.lowercase()}_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = appString(
                            if (mediaType == MediaType.PHOTO) R.string.browse_photos else R.string.browse_videos
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onPickFromDocuments,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("browse_folders_${mediaType.name.lowercase()}_button")
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = appString(R.string.browse_folders), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(appString(R.string.browse_folders))
                }
            }

            if (items.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MediaFilter.entries.forEach { filter ->
                            val count = when (filter) {
                                MediaFilter.ALL -> items.size
                                MediaFilter.UNSCHEDULED -> items.count { it.uriString !in scheduledMap }
                                MediaFilter.SCHEDULED -> items.count { it.uriString in scheduledMap }
                            }
                            FilterChip(
                                selected = activeFilter == filter,
                                onClick = { activeFilter = filter },
                                label = {
                                    Text(
                                        "${appString(filter.labelRes)} ($count)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            if (allVisibleSelected) onDeselectAll() else onSelectAll(visibleUris)
                        },
                        modifier = Modifier.testTag("select_all_${mediaType.name.lowercase()}_button")
                    ) {
                        Icon(
                            imageVector = if (allVisibleSelected) Icons.Default.ClearAll else Icons.Default.SelectAll,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(appString(if (allVisibleSelected) R.string.deselect_all_short else R.string.select_all))
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
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
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (mediaType == MediaType.PHOTO) Icons.Default.PhotoLibrary else Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (items.isEmpty()) {
                                    appString(
                                        if (mediaType == MediaType.PHOTO) R.string.empty_photos_title else R.string.empty_videos_title
                                    )
                                } else {
                                    appString(R.string.empty_filter_title, appString(activeFilter.labelRes))
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = appString(
                                    if (mediaType == MediaType.PHOTO) R.string.empty_photos_desc else R.string.empty_videos_desc
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onPickFromPhotoPicker,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    appString(
                                        if (mediaType == MediaType.PHOTO) R.string.select_photos else R.string.select_videos
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 6.dp,
                        bottom = if (selectedUris.isNotEmpty()) 96.dp else 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems, key = { it.uriString }) { media ->
                        val isSelected = media.uriString in selectedUris
                        val scheduled = scheduledMap[media.uriString]
                        val folderKey = media.folderName.lowercase().trim()
                        val isProtected = protectedNames.any { p ->
                            p.isNotEmpty() && (folderKey == p || folderKey.contains(p))
                        }

                        MediaGridTile(
                            media = media,
                            isSelected = isSelected,
                            isScheduled = scheduled != null,
                            isProtected = isProtected,
                            nowMillis = nowMillis,
                            locale = locale,
                            onTap = {
                                if (selectedUris.isNotEmpty()) {
                                    onToggleSelect(media)
                                } else {
                                    onPreview(media)
                                }
                            },
                            onToggleSelect = { onToggleSelect(media) },
                            onLongClick = { onToggleSelect(media) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedUris.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = appString(R.string.selected_count, selectedUris.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(
                            onClick = onDeselectAll,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(appString(R.string.deselect_all), style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Button(
                        onClick = onScheduleSelected,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("schedule_selected_${mediaType.name.lowercase()}_button")
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(appString(R.string.schedule_count, selectedUris.size))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridTile(
    media: TrackedMediaEntity,
    isSelected: Boolean,
    isScheduled: Boolean,
    isProtected: Boolean,
    nowMillis: Long,
    locale: java.util.Locale,
    onTap: () -> Unit,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 2.5.dp else 0.dp

    Box(
        modifier = Modifier
            .aspectRatio(0.92f)
            .clip(RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = media.uriString,
            contentDescription = media.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                    )
                )
        )

        if (isScheduled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        } else if (isProtected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF059669).copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(40.dp)
                .clickable(onClick = onToggleSelect)
                .padding(6.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f)
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        if (media.mediaType == MediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = FormatUtils.formatMediaTimestamp(media.dateAddedMillis, nowMillis, locale),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (media.mediaType == MediaType.VIDEO && media.durationMillis > 0L) {
                Text(
                    text = FormatUtils.formatVideoDuration(media.durationMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
