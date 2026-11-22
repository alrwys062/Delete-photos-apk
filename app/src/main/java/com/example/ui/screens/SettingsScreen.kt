package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.ProtectedFolderEntity
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.AppSettings
import com.example.data.prefs.ThemeMode
import com.example.scheduler.DeletionScheduler
import com.example.scheduler.NotificationHelper
import com.example.ui.theme.LocalTimeDeleteColors
import com.example.util.appString

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    protectedFolders: List<ProtectedFolderEntity>,
    onToggleBackgroundMonitor: (Boolean) -> Unit,
    onTogglePausedAll: (Boolean) -> Unit,
    onToggleNotifyScheduled: (Boolean) -> Unit,
    onToggleNotifyApproaching: (Boolean) -> Unit,
    onToggleNotifyExpired: (Boolean) -> Unit,
    onToggleNotifyAwaiting: (Boolean) -> Unit,
    onToggleNotifyCompleted: (Boolean) -> Unit,
    onToggleNotifyFailed: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSendTestNotification: () -> Unit,
    onAddProtectedFolderByName: (String) -> Unit,
    onPickProtectedFolderTree: () -> Unit,
    onRemoveProtectedFolder: (ProtectedFolderEntity) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ext = LocalTimeDeleteColors.current

    var showNotificationsDetail by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }

    val hasNotificationPerm = NotificationHelper.hasNotificationPermission(context)
    val canExactAlarm = DeletionScheduler.canScheduleExactAlarms(context)

    if (showAddFolderDialog) {
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text(appString(R.string.settings_protect_folder_title)) },
            text = {
                Column {
                    Text(
                        text = appString(R.string.settings_protect_folder_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        label = { Text(appString(R.string.settings_folder_name_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("protected_folder_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            onAddProtectedFolderByName(folderNameInput.trim())
                            folderNameInput = ""
                            showAddFolderDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_protected_folder_button")
                ) {
                    Text(appString(R.string.settings_protect_btn))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddFolderDialog = false }) {
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
                SettingsGroupCard(title = appString(R.string.settings_sec_scheduling)) {
                    SettingsToggleRow(
                        icon = Icons.Default.Schedule,
                        title = appString(R.string.settings_auto_delete_service),
                        subtitle = appString(R.string.settings_auto_delete_desc),
                        checked = settings.backgroundMonitorEnabled,
                        onCheckedChange = onToggleBackgroundMonitor,
                        testTag = "toggle_background_monitor"
                    )
                    HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                    SettingsToggleRow(
                        icon = Icons.Default.PauseCircle,
                        title = appString(R.string.settings_pause_all),
                        subtitle = appString(R.string.settings_pause_all_desc),
                        checked = settings.isPausedAll,
                        onCheckedChange = onTogglePausedAll,
                        testTag = "toggle_pause_all"
                    )
                }
            }

            item {
                SettingsGroupCard(title = appString(R.string.settings_sec_notifications)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNotificationsDetail = !showNotificationsDetail }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBadge(icon = Icons.Default.Notifications)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(appString(R.string.settings_notif_prefs), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = appString(
                                    if (hasNotificationPerm) R.string.settings_notif_granted else R.string.settings_notif_missing
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasNotificationPerm) MaterialTheme.colorScheme.onSurfaceVariant else ext.warning
                            )
                        }
                        TextButton(onClick = { showNotificationsDetail = !showNotificationsDetail }) {
                            Text(appString(if (showNotificationsDetail) R.string.settings_hide else R.string.settings_customize))
                        }
                    }

                    if (!hasNotificationPerm) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onRequestNotificationPermission,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(appString(R.string.settings_allow_notifications))
                            }
                        }
                    }

                    AnimatedVisibility(visible = showNotificationsDetail) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                            NotificationSubToggle(appString(R.string.settings_notif_scheduled), settings.notifyScheduled, onToggleNotifyScheduled)
                            NotificationSubToggle(
                                appString(R.string.settings_notif_approaching, settings.approachingLeadMinutes),
                                settings.notifyApproaching,
                                onToggleNotifyApproaching
                            )
                            NotificationSubToggle(appString(R.string.settings_notif_expired), settings.notifyExpired, onToggleNotifyExpired)
                            NotificationSubToggle(appString(R.string.settings_notif_awaiting), settings.notifyAwaitingConfirmation, onToggleNotifyAwaiting)
                            NotificationSubToggle(appString(R.string.settings_notif_completed), settings.notifyCompleted, onToggleNotifyCompleted)
                            NotificationSubToggle(appString(R.string.settings_notif_failed), settings.notifyFailed, onToggleNotifyFailed)

                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onSendTestNotification,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(appString(R.string.settings_send_test_notif))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                SettingsGroupCard(title = appString(R.string.settings_sec_protection)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsIconBadge(icon = Icons.Default.FolderOff)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(appString(R.string.settings_excluded_folders), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = appString(R.string.settings_excluded_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (protectedFolders.isEmpty()) {
                            Text(
                                text = appString(R.string.settings_no_protected_folders),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                protectedFolders.forEach { folder ->
                                    InputChip(
                                        selected = true,
                                        onClick = { onRemoveProtectedFolder(folder) },
                                        label = { Text(folder.folderName) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = folder.folderName,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddFolderDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_protected_folder_name_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(appString(R.string.settings_add_name))
                            }
                            OutlinedButton(
                                onClick = onPickProtectedFolderTree,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pick_protected_folder_tree_button")
                            ) {
                                Text(appString(R.string.settings_pick_folder))
                            }
                        }
                    }
                }
            }

            // Appearance & Language
            item {
                SettingsGroupCard(title = appString(R.string.settings_sec_appearance)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsIconBadge(icon = Icons.Default.DarkMode)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(appString(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = appString(
                                        when (settings.themeMode) {
                                            ThemeMode.DARK -> R.string.theme_dark
                                            ThemeMode.LIGHT -> R.string.theme_light
                                            ThemeMode.SYSTEM -> R.string.theme_system
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = settings.themeMode == mode,
                                    onClick = { onThemeModeChange(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = ThemeMode.entries.size
                                    ),
                                    modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                                ) {
                                    Text(
                                        text = appString(
                                            when (mode) {
                                                ThemeMode.DARK -> R.string.theme_dark
                                                ThemeMode.LIGHT -> R.string.theme_light
                                                ThemeMode.SYSTEM -> R.string.theme_system
                                            }
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsIconBadge(icon = Icons.Default.Language)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(appString(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = appString(
                                        when (settings.appLanguage) {
                                            AppLanguage.SYSTEM -> R.string.lang_system
                                            AppLanguage.ENGLISH -> R.string.lang_english
                                            AppLanguage.ARABIC -> R.string.lang_arabic
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            AppLanguage.entries.forEachIndexed { index, lang ->
                                SegmentedButton(
                                    selected = settings.appLanguage == lang,
                                    onClick = { onLanguageChange(lang) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = AppLanguage.entries.size
                                    ),
                                    modifier = Modifier.testTag("language_mode_${lang.name.lowercase()}")
                                ) {
                                    Text(
                                        text = appString(
                                            when (lang) {
                                                AppLanguage.SYSTEM -> R.string.lang_system
                                                AppLanguage.ENGLISH -> R.string.lang_english
                                                AppLanguage.ARABIC -> R.string.lang_arabic
                                            }
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsGroupCard(title = appString(R.string.settings_sec_reliability)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsIconBadge(icon = Icons.Default.Alarm)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    appString(R.string.settings_exact_alarms),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = appString(
                                        if (canExactAlarm) R.string.settings_exact_granted else R.string.settings_exact_optional
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canExactAlarm) {
                                TextButton(onClick = {
                                    runCatching {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    }
                                }) {
                                    Text(appString(R.string.settings_enable))
                                }
                            }
                        }

                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SettingsIconBadge(icon = Icons.Default.BatterySaver)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    appString(R.string.settings_battery_opt),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = appString(R.string.settings_battery_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }
                            }) {
                                Text(appString(R.string.settings_manage))
                            }
                        }

                        HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.5f))

                        Row(verticalAlignment = Alignment.Top) {
                            SettingsIconBadge(icon = Icons.Default.Security)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    appString(R.string.settings_privacy_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = appString(R.string.settings_privacy_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsGroupCard(title = appString(R.string.settings_sec_about)) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIconBadge(icon = Icons.Default.Info)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(appString(R.string.settings_about_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = appString(R.string.settings_about_desc),
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
private fun SettingsGroupCard(
    title: String,
    content: @Composable () -> Unit
) {
    val ext = LocalTimeDeleteColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ext.elevatedCard,
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsIconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBadge(icon = icon)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun NotificationSubToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
