package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.ScheduledDeletionEntity
import com.example.data.prefs.AppSettings
import com.example.ui.navigation.AppDestination
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalTimeDeleteColors
import com.example.ui.viewmodel.DashboardStats
import com.example.util.FormatUtils
import com.example.util.appString

@Composable
fun HomeScreen(
    stats: DashboardStats,
    settings: AppSettings,
    nextScheduledItem: ScheduledDeletionEntity?,
    nowMillis: Long,
    onScheduleNewMedia: () -> Unit,
    onNavigate: (AppDestination) -> Unit,
    onResumeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTimeDeleteColors.current
    val totalScheduled = stats.scheduledPhotos + stats.scheduledVideos

    val ringProgress by animateFloatAsState(
        targetValue = when {
            totalScheduled == 0 -> 0.14f
            stats.awaitingConfirmation > 0 -> 1f
            else -> ((stats.activeCountdowns.toFloat() / (totalScheduled + 2f)).coerceIn(0.25f, 0.92f))
        },
        animationSpec = tween(durationMillis = 600),
        label = "ringProgress"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TopStatCard(
                        icon = Icons.Default.PhotoLibrary,
                        iconTint = MaterialTheme.colorScheme.primary,
                        count = stats.scheduledPhotos,
                        label = appString(R.string.stat_photos),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.PHOTOS) }
                    )
                    TopStatCard(
                        icon = Icons.Default.VideoLibrary,
                        iconTint = ext.videoAccent,
                        count = stats.scheduledVideos,
                        label = appString(R.string.stat_videos),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.VIDEOS) }
                    )
                    TopStatCard(
                        icon = Icons.Default.DeleteSweep,
                        iconTint = ext.danger,
                        count = stats.completedDeletions,
                        label = appString(R.string.stat_deleted),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.HISTORY) }
                    )
                }
            }

            item {
                AnimatedVisibility(visible = settings.isPausedAll) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ext.warningContainer,
                        border = BorderStroke(1.dp, ext.warning.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PauseCircle,
                                contentDescription = null,
                                tint = ext.warning
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appString(R.string.paused_banner_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = appString(R.string.paused_banner_desc),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = onResumeAll) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(appString(R.string.action_resume))
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = stats.awaitingConfirmation > 0) {
                    Surface(
                        onClick = { onNavigate(AppDestination.SCHEDULED) },
                        shape = RoundedCornerShape(18.dp),
                        color = ext.dangerContainer,
                        border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_awaiting_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = ext.danger,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (stats.awaitingConfirmation == 1) {
                                        appString(R.string.awaiting_banner_single)
                                    } else {
                                        appString(R.string.awaiting_banner_plural, stats.awaitingConfirmation)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = appString(R.string.awaiting_banner_desc),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = ext.elevatedCard,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 22.dp, horizontal = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val ringTrack = MaterialTheme.colorScheme.outlineVariant
                        val ringGradient = listOf(ext.gradientStart, MaterialTheme.colorScheme.secondary, ext.videoAccent)

                        Box(
                            modifier = Modifier.size(168.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 14.dp.toPx()
                                val inset = stroke / 2f
                                val arcSize = Size(size.width - stroke, size.height - stroke)
                                val topLeft = Offset(inset, inset)

                                drawArc(
                                    color = ringTrack,
                                    startAngle = 140f,
                                    sweepAngle = 260f,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(ringGradient),
                                    startAngle = 140f,
                                    sweepAngle = 260f * ringProgress,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stats.activeCountdowns.toString(),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = appString(R.string.active_countdowns),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (nextScheduledItem != null) {
                            val remaining = (nextScheduledItem.deleteAtMillis - nowMillis).coerceAtLeast(0L)
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = appString(R.string.next_prefix, nextScheduledItem.displayName),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = FormatUtils.formatCountdown(remaining),
                                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(ext.gradientStart, ext.gradientEnd))
                                )
                                .clickable(onClick = onScheduleNewMedia)
                                .testTag("schedule_new_media_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = appString(R.string.cta_schedule_new),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricDetailCard(
                        title = appString(R.string.metric_upcoming_24h),
                        value = stats.upcoming24h.toString(),
                        subtitle = appString(
                            if (stats.upcoming24h == 1) R.string.metric_file_due_soon else R.string.metric_files_due_soon
                        ),
                        icon = Icons.Default.Schedule,
                        accent = ext.warning,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.SCHEDULED) }
                    )
                    MetricDetailCard(
                        title = appString(R.string.metric_completed),
                        value = stats.completedDeletions.toString(),
                        subtitle = appString(R.string.metric_verified_deletions),
                        icon = Icons.Default.CheckCircle,
                        accent = ext.success,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(AppDestination.HISTORY) }
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = appString(R.string.quick_actions),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            icon = Icons.Default.PhotoLibrary,
                            label = appString(R.string.qa_browse_photos),
                            subtitle = appString(R.string.qa_loaded_count, stats.trackedPhotos),
                            onClick = { onNavigate(AppDestination.PHOTOS) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            icon = Icons.Default.VideoLibrary,
                            label = appString(R.string.qa_browse_videos),
                            subtitle = appString(R.string.qa_loaded_count, stats.trackedVideos),
                            onClick = { onNavigate(AppDestination.VIDEOS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            icon = Icons.Default.Schedule,
                            label = appString(R.string.qa_scheduled),
                            subtitle = appString(R.string.qa_in_queue, stats.activeCountdowns + stats.awaitingConfirmation),
                            onClick = { onNavigate(AppDestination.SCHEDULED) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            icon = Icons.Default.History,
                            label = appString(R.string.qa_history),
                            subtitle = appString(R.string.qa_audit_log),
                            onClick = { onNavigate(AppDestination.HISTORY) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    QuickActionCard(
                        icon = Icons.Default.Settings,
                        label = appString(R.string.qa_settings_title),
                        subtitle = appString(R.string.qa_settings_subtitle),
                        onClick = { onNavigate(AppDestination.SETTINGS) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun TopStatCard(
    icon: ImageVector,
    iconTint: Color,
    count: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTimeDeleteColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = ext.elevatedCard,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MetricDetailCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTimeDeleteColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = ext.elevatedCard,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTimeDeleteColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = ext.elevatedCard,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
