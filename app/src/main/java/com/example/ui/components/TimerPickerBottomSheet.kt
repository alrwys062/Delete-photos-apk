package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.db.MediaType
import com.example.ui.theme.LocalTimeDeleteColors
import com.example.ui.viewmodel.TimerTarget
import com.example.util.FormatUtils
import com.example.util.LocalAppLocale
import com.example.util.appString
import java.util.Calendar
import java.util.concurrent.TimeUnit

private data class TimerPreset(
    val id: String,
    @StringRes val labelRes: Int,
    val durationMillis: Long
)

private val timerPresets = listOf(
    TimerPreset("1h", R.string.timer_1h, TimeUnit.HOURS.toMillis(1)),
    TimerPreset("2h", R.string.timer_2h, TimeUnit.HOURS.toMillis(2)),
    TimerPreset("3h", R.string.timer_3h, TimeUnit.HOURS.toMillis(3)),
    TimerPreset("6h", R.string.timer_6h, TimeUnit.HOURS.toMillis(6)),
    TimerPreset("12h", R.string.timer_12h, TimeUnit.HOURS.toMillis(12)),
    TimerPreset("24h", R.string.timer_24h, TimeUnit.HOURS.toMillis(24)),
    TimerPreset("2d", R.string.timer_2d, TimeUnit.DAYS.toMillis(2)),
    TimerPreset("3d", R.string.timer_3d, TimeUnit.DAYS.toMillis(3)),
    TimerPreset("7d", R.string.timer_7d, TimeUnit.DAYS.toMillis(7)),
    TimerPreset("14d", R.string.timer_14d, TimeUnit.DAYS.toMillis(14)),
    TimerPreset("30d", R.string.timer_30d, TimeUnit.DAYS.toMillis(30))
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimerPickerBottomSheet(
    target: TimerTarget,
    onDismiss: () -> Unit,
    onConfirm: (deleteAtMillis: Long, label: String) -> Unit
) {
    val context = LocalContext.current
    val locale = LocalAppLocale.current
    val ext = LocalTimeDeleteColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedPresetId by remember { mutableStateOf("24h") }
    var customDeleteAtMillis by remember {
        mutableLongStateOf(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24))
    }

    val isCustom = selectedPresetId == "custom"
    val previewTargetMillis = remember(selectedPresetId, customDeleteAtMillis) {
        if (isCustom) {
            customDeleteAtMillis
        } else {
            val preset = timerPresets.firstOrNull { it.id == selectedPresetId } ?: timerPresets[5]
            System.currentTimeMillis() + preset.durationMillis
        }
    }

    val selectedLabel = if (isCustom) {
        appString(R.string.timer_custom_selected, FormatUtils.formatShortDateTime(customDeleteAtMillis, locale))
    } else {
        val preset = timerPresets.firstOrNull { it.id == selectedPresetId } ?: timerPresets[5]
        appString(preset.labelRes)
    }

    val headerUri = when (target) {
        is TimerTarget.NewMedia -> target.items.firstOrNull()?.uriString
        is TimerTarget.EditScheduled -> target.item.uriString
    }
    val isVideoHeader = when (target) {
        is TimerTarget.NewMedia -> target.items.firstOrNull()?.mediaType == MediaType.VIDEO
        is TimerTarget.EditScheduled -> target.item.mediaType == MediaType.VIDEO
    }
    val itemCount = when (target) {
        is TimerTarget.NewMedia -> target.items.size
        is TimerTarget.EditScheduled -> 1
    }
    val totalBytes = when (target) {
        is TimerTarget.NewMedia -> target.items.sumOf { it.sizeBytes }
        is TimerTarget.EditScheduled -> target.item.sizeBytes
    }
    val subtitleDate = when (target) {
        is TimerTarget.NewMedia -> {
            val first = target.items.firstOrNull()
            if (target.items.size == 1 && first != null) {
                appString(R.string.timer_added_prefix, FormatUtils.formatShortDateTime(first.dateAddedMillis, locale))
            } else {
                appString(
                    R.string.timer_counts_summary,
                    target.items.count { it.mediaType == MediaType.PHOTO },
                    target.items.count { it.mediaType == MediaType.VIDEO }
                )
            }
        }
        is TimerTarget.EditScheduled -> appString(
            R.string.timer_currently_set,
            FormatUtils.formatShortDateTime(target.item.deleteAtMillis, locale)
        )
    }

    fun launchDateTimePicker() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = maxOf(customDeleteAtMillis, System.currentTimeMillis() + 60_000L)
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                }
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        pickedCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        pickedCal.set(Calendar.MINUTE, minute)
                        val minFuture = System.currentTimeMillis() + 60_000L
                        customDeleteAtMillis = maxOf(pickedCal.timeInMillis, minFuture)
                        selectedPresetId = "custom"
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                ).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000L
        }.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = appString(
                        if (target is TimerTarget.EditScheduled) R.string.timer_edit_title else R.string.timer_setup_title
                    ),
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = appString(R.string.action_cancel))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ext.elevatedCard,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (headerUri != null) {
                            AsyncImage(
                                model = headerUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }
                        if (isVideoHeader) {
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
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (target) {
                                is TimerTarget.EditScheduled -> target.item.displayName
                                is TimerTarget.NewMedia -> if (itemCount == 1) {
                                    target.items.first().displayName
                                } else {
                                    appString(R.string.timer_media_selected, itemCount)
                                }
                            },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = appString(R.string.timer_size_prefix, FormatUtils.formatFileSize(totalBytes, locale)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = subtitleDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = appString(R.string.timer_select_heading),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            val chunked = timerPresets.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                chunked.forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPresets.forEach { preset ->
                            val isSelected = selectedPresetId == preset.id
                            TimerPresetPill(
                                label = appString(preset.labelRes),
                                selected = isSelected,
                                onClick = { selectedPresetId = preset.id },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("timer_preset_${preset.id}")
                            )
                        }
                        repeat(3 - rowPresets.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                onClick = { launchDateTimePicker() },
                shape = RoundedCornerShape(16.dp),
                color = if (isCustom) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else ext.elevatedCard,
                border = BorderStroke(
                    width = if (isCustom) 1.5.dp else 1.dp,
                    color = if (isCustom) MaterialTheme.colorScheme.primary else ext.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timer_custom_datetime_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = if (isCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isCustom) {
                            appString(R.string.timer_custom_selected, FormatUtils.formatDateTime(customDeleteAtMillis, locale))
                        } else {
                            appString(R.string.timer_custom_datetime)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = appString(R.string.timer_quick_custom),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    1 to R.string.timer_1m,
                    5 to R.string.timer_5m,
                    15 to R.string.timer_15m,
                    30 to R.string.timer_30m
                ).forEach { (mins, lblRes) ->
                    val tagId = "custom_${mins}m"
                    FilterChip(
                        selected = isCustom && selectedPresetId == "custom" &&
                            kotlin.math.abs((customDeleteAtMillis - System.currentTimeMillis()) - mins * 60_000L) < 55_000L,
                        onClick = {
                            customDeleteAtMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(mins.toLong())
                            selectedPresetId = "custom"
                        },
                        label = { Text(appString(lblRes), style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag(tagId)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (itemCount == 1) {
                                appString(R.string.timer_preview_single, FormatUtils.formatDateTime(previewTargetMillis, locale))
                            } else {
                                appString(R.string.timer_preview_multi, itemCount, FormatUtils.formatDateTime(previewTargetMillis, locale))
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = appString(R.string.timer_android_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(ext.gradientStart, ext.gradientEnd)
                        )
                    )
                    .clickable {
                        onConfirm(previewTargetMillis, selectedLabel)
                    }
                    .testTag("confirm_schedule_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = appString(
                            if (target is TimerTarget.EditScheduled) R.string.timer_btn_update else R.string.timer_btn_schedule
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TimerPresetPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTimeDeleteColors.current
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else ext.elevatedCard
    val textColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
