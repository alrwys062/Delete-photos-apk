package com.example.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.DeletionHistoryEntity
import com.example.data.db.HistoryOutcome
import com.example.data.db.MediaType
import com.example.ui.theme.LocalTimeDeleteColors
import com.example.util.FormatUtils
import com.example.util.LocalAppLocale
import com.example.util.appString

private enum class HistoryFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    DELETED(R.string.history_deleted),
    FAILED(R.string.history_failed),
    CANCELLED(R.string.history_cancelled)
}

@Composable
fun HistoryScreen(
    items: List<DeletionHistoryEntity>,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTimeDeleteColors.current
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }
    var showClearDialog by remember { mutableStateOf(false) }

    val filtered = remember(items, filter) {
        when (filter) {
            HistoryFilter.ALL -> items
            HistoryFilter.DELETED -> items.filter { it.outcome == HistoryOutcome.DELETED }
            HistoryFilter.FAILED -> items.filter { it.outcome == HistoryOutcome.FAILED }
            HistoryFilter.CANCELLED -> items.filter { it.outcome == HistoryOutcome.CANCELLED }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(appString(R.string.history_clear_dialog_title)) },
            text = { Text(appString(R.string.history_clear_dialog_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ext.danger)
                ) {
                    Text(appString(R.string.history_clear_logs), color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDialog = false }) {
                    Text(appString(R.string.action_cancel))
                }
            }
        )
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
            item {
                val deletedCount = items.count { it.outcome == HistoryOutcome.DELETED }
                val failedCount = items.count { it.outcome == HistoryOutcome.FAILED }
                val cancelledCount = items.count { it.outcome == HistoryOutcome.CANCELLED }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HistorySummaryPill(
                        count = deletedCount,
                        label = appString(R.string.history_deleted),
                        color = ext.success,
                        modifier = Modifier.weight(1f)
                    )
                    HistorySummaryPill(
                        count = failedCount,
                        label = appString(R.string.history_failed),
                        color = ext.danger,
                        modifier = Modifier.weight(1f)
                    )
                    HistorySummaryPill(
                        count = cancelledCount,
                        label = appString(R.string.history_cancelled),
                        color = ext.warning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        HistoryFilter.entries.forEach { f ->
                            FilterChip(
                                selected = filter == f,
                                onClick = { filter = f },
                                label = { Text(appString(f.labelRes), style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (items.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(appString(R.string.history_clear))
                        }
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
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = appString(R.string.history_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = appString(R.string.history_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { record ->
                    HistoryRecordCard(record = record)
                }
            }
        }
    }
}

@Composable
private fun HistorySummaryPill(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val ext = LocalTimeDeleteColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ext.elevatedCard,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryRecordCard(record: DeletionHistoryEntity) {
    val ext = LocalTimeDeleteColors.current
    val locale = LocalAppLocale.current
    val (icon, tint, containerColor, labelRes) = when (record.outcome) {
        HistoryOutcome.DELETED -> Quad(Icons.Default.CheckCircle, ext.success, ext.successContainer, R.string.outcome_deleted)
        HistoryOutcome.FAILED -> Quad(Icons.Default.Error, ext.danger, ext.dangerContainer, R.string.outcome_failed)
        HistoryOutcome.CANCELLED -> Quad(Icons.Default.Cancel, ext.warning, ext.warningContainer, R.string.outcome_cancelled)
    }
    val outcomeLabel = appString(labelRes)
    val mediaTypeLabel = appString(
        if (record.mediaType == MediaType.VIDEO) R.string.media_type_video else R.string.media_type_photo
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ext.elevatedCard,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = outcomeLabel, tint = tint, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = containerColor
                    ) {
                        Text(
                            text = outcomeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$mediaTypeLabel • ${FormatUtils.formatFileSize(record.sizeBytes, locale)} • ${record.folderName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.details,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = FormatUtils.formatDateTime(record.executedAtMillis, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
