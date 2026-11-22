package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.db.MediaType
import com.example.scheduler.NotificationHelper
import com.example.ui.components.DeletionConfirmationDialog
import com.example.ui.components.MediaPreviewDialog
import com.example.ui.components.TimerPickerBottomSheet
import com.example.ui.navigation.AppDestination
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MediaLibraryScreen
import com.example.ui.screens.ScheduledScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalTimeDeleteColors
import com.example.ui.viewmodel.TimeDeleteUiEvent
import com.example.ui.viewmodel.TimeDeleteViewModel
import com.example.util.appString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDeleteMainShell(
    viewModel: TimeDeleteViewModel,
    deepLinkDestination: String?,
    onDeepLinkConsumed: () -> Unit
) {
    val context = LocalContext.current
    val ext = LocalTimeDeleteColors.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val scheduledItems by viewModel.scheduledItems.collectAsStateWithLifecycle()
    val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()
    val protectedFolders by viewModel.protectedFolders.collectAsStateWithLifecycle()
    val selectedPhotoUris by viewModel.selectedPhotoUris.collectAsStateWithLifecycle()
    val selectedVideoUris by viewModel.selectedVideoUris.collectAsStateWithLifecycle()
    val timerTarget by viewModel.timerTarget.collectAsStateWithLifecycle()
    val previewPayload by viewModel.previewPayload.collectAsStateWithLifecycle()
    val pendingConfirmationItems by viewModel.pendingConfirmationItems.collectAsStateWithLifecycle()
    val nowMillis by viewModel.nowMillis.collectAsStateWithLifecycle()

    // Activity Result Launchers (100% zero-permission Photo Picker & SAF + MediaStore Delete IntentSender)
    val scheduleBothPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onMediaPickedFromDevice(uris, fallbackType = null, openTimerImmediately = true)
        }
    }

    val photosPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onMediaPickedFromDevice(uris, fallbackType = MediaType.PHOTO, openTimerImmediately = false)
        }
    }

    val videosPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onMediaPickedFromDevice(uris, fallbackType = MediaType.VIDEO, openTimerImmediately = false)
        }
    }

    val safPhotosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onMediaPickedFromDevice(uris, fallbackType = MediaType.PHOTO, openTimerImmediately = false)
        }
    }

    val safVideosLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onMediaPickedFromDevice(uris, fallbackType = MediaType.VIDEO, openTimerImmediately = false)
        }
    }

    val protectedFolderTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            val folderName = docId?.substringAfterLast(':')?.substringAfterLast('/')?.ifBlank { null }
                ?: treeUri.lastPathSegment?.substringAfterLast('/')
                ?: "Protected Folder"
            viewModel.addProtectedFolder(folderName, treeUri.toString())
        }
    }

    val systemDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onSystemDeleteResult(result.resultCode == Activity.RESULT_OK)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Non-blocking notice via UI flow
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is TimeDeleteUiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is TimeDeleteUiEvent.LaunchSystemDelete -> {
                    systemDeleteLauncher.launch(event.request)
                }
            }
        }
    }

    fun navigateTo(destination: AppDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(deepLinkDestination) {
        if (!deepLinkDestination.isNullOrBlank()) {
            val dest = AppDestination.entries.firstOrNull { it.route == deepLinkDestination }
            if (dest != null) {
                navigateTo(dest)
            }
            onDeepLinkConsumed()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.HOME.route
    val currentDestination = AppDestination.entries.firstOrNull { it.route == currentRoute } ?: AppDestination.HOME

    fun launchScheduleNewMedia() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        scheduleBothPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (useRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(ext.gradientStart, ext.gradientEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "TimeDelete",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    AppDestination.entries.forEach { dest ->
                        val selected = currentDestination == dest
                        val label = appString(dest.labelRes)
                        val badgeCount = if (dest == AppDestination.SCHEDULED) {
                            stats.awaitingConfirmation
                        } else 0

                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTo(dest) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (badgeCount > 0) {
                                            Badge(containerColor = ext.danger) {
                                                Text(badgeCount.toString(), color = Color.White)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = label
                                    )
                                }
                            },
                            label = { Text(label, maxLines = 1) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            ),
                            modifier = Modifier.testTag("nav_rail_${dest.route}")
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentDestination == AppDestination.HOME) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Brush.linearGradient(listOf(ext.gradientStart, ext.gradientEnd))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "TimeDelete",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = appString(currentDestination.labelRes),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { launchScheduleNewMedia() },
                                modifier = Modifier.testTag("topbar_quick_schedule")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = appString(R.string.schedule_new_media),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            AppDestination.entries.forEach { dest ->
                                val selected = currentDestination == dest
                                val label = appString(dest.labelRes)
                                val badgeCount = if (dest == AppDestination.SCHEDULED) {
                                    stats.awaitingConfirmation
                                } else 0

                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (badgeCount > 0) {
                                                    Badge(containerColor = ext.danger) {
                                                        Text(badgeCount.toString(), color = Color.White)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = label,
                                                modifier = Modifier.size(21.dp)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Clip
                                        )
                                    },
                                    alwaysShowLabel = true,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.testTag("nav_bar_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = AppDestination.HOME.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(AppDestination.HOME.route) {
                        HomeScreen(
                            stats = stats,
                            settings = settings,
                            nextScheduledItem = scheduledItems.firstOrNull {
                                it.status == com.example.data.db.ScheduleStatus.SCHEDULED
                            },
                            nowMillis = nowMillis,
                            onScheduleNewMedia = { launchScheduleNewMedia() },
                            onNavigate = { navigateTo(it) },
                            onResumeAll = { viewModel.setPausedAll(false) }
                        )
                    }

                    composable(AppDestination.PHOTOS.route) {
                        MediaLibraryScreen(
                            mediaType = MediaType.PHOTO,
                            items = photos,
                            selectedUris = selectedPhotoUris,
                            scheduledItems = scheduledItems,
                            protectedFolders = protectedFolders,
                            nowMillis = nowMillis,
                            onPickFromPhotoPicker = {
                                photosPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onPickFromDocuments = {
                                safPhotosLauncher.launch(arrayOf("image/*"))
                            },
                            onToggleSelect = viewModel::toggleSelection,
                            onSelectAll = { uris -> viewModel.selectAll(MediaType.PHOTO, uris) },
                            onDeselectAll = { viewModel.deselectAll(MediaType.PHOTO) },
                            onPreview = viewModel::openPreview,
                            onScheduleSelected = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                viewModel.openTimerForSelected(MediaType.PHOTO)
                            }
                        )
                    }

                    composable(AppDestination.VIDEOS.route) {
                        MediaLibraryScreen(
                            mediaType = MediaType.VIDEO,
                            items = videos,
                            selectedUris = selectedVideoUris,
                            scheduledItems = scheduledItems,
                            protectedFolders = protectedFolders,
                            nowMillis = nowMillis,
                            onPickFromPhotoPicker = {
                                videosPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            onPickFromDocuments = {
                                safVideosLauncher.launch(arrayOf("video/*"))
                            },
                            onToggleSelect = viewModel::toggleSelection,
                            onSelectAll = { uris -> viewModel.selectAll(MediaType.VIDEO, uris) },
                            onDeselectAll = { viewModel.deselectAll(MediaType.VIDEO) },
                            onPreview = viewModel::openPreview,
                            onScheduleSelected = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                viewModel.openTimerForSelected(MediaType.VIDEO)
                            }
                        )
                    }

                    composable(AppDestination.SCHEDULED.route) {
                        ScheduledScreen(
                            items = scheduledItems,
                            settings = settings,
                            nowMillis = nowMillis,
                            onPreview = viewModel::openPreview,
                            onEditTimer = viewModel::openEditTimer,
                            onCancelSchedule = viewModel::cancelScheduledDeletion,
                            onConfirmDelete = viewModel::requestFinalDeletionConfirmation,
                            onResumeAll = { viewModel.setPausedAll(false) },
                            onScheduleNewMedia = { launchScheduleNewMedia() }
                        )
                    }

                    composable(AppDestination.HISTORY.route) {
                        HistoryScreen(
                            items = historyItems,
                            onClearHistory = viewModel::clearHistory
                        )
                    }

                    composable(AppDestination.SETTINGS.route) {
                        SettingsScreen(
                            settings = settings,
                            protectedFolders = protectedFolders,
                            onToggleBackgroundMonitor = viewModel::setBackgroundMonitorEnabled,
                            onTogglePausedAll = viewModel::setPausedAll,
                            onToggleNotifyScheduled = viewModel::setNotifyScheduled,
                            onToggleNotifyApproaching = viewModel::setNotifyApproaching,
                            onToggleNotifyExpired = viewModel::setNotifyExpired,
                            onToggleNotifyAwaiting = viewModel::setNotifyAwaitingConfirmation,
                            onToggleNotifyCompleted = viewModel::setNotifyCompleted,
                            onToggleNotifyFailed = viewModel::setNotifyFailed,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onSendTestNotification = viewModel::sendTestNotification,
                            onAddProtectedFolderByName = { name -> viewModel.addProtectedFolder(name) },
                            onPickProtectedFolderTree = { protectedFolderTreeLauncher.launch(null) },
                            onRemoveProtectedFolder = viewModel::removeProtectedFolder,
                            onThemeModeChange = viewModel::setThemeMode,
                            onLanguageChange = viewModel::setAppLanguage
                        )
                    }
                }
            }
        }
    }

    // Active Overlays: Timer Picker Sheet, Preview Dialog, Final Confirmation Dialog
    timerTarget?.let { target ->
        TimerPickerBottomSheet(
            target = target,
            onDismiss = viewModel::dismissTimerPicker,
            onConfirm = { deleteAtMillis, label ->
                viewModel.confirmTimerSelection(target, deleteAtMillis, label)
            }
        )
    }

    previewPayload?.let { payload ->
        val alreadyScheduled = scheduledItems.any { it.uriString == payload.uriString }
        MediaPreviewDialog(
            payload = payload,
            isAlreadyScheduled = alreadyScheduled,
            onDismiss = viewModel::dismissPreview,
            onScheduleClick = { media -> viewModel.openTimerForSingle(media) }
        )
    }

    if (pendingConfirmationItems.isNotEmpty()) {
        DeletionConfirmationDialog(
            items = pendingConfirmationItems,
            onDismiss = viewModel::dismissFinalConfirmationDialog,
            onConfirm = viewModel::executeConfirmedDeletion
        )
    }
}
