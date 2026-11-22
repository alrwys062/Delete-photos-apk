package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.TimeDeleteApp
import com.example.data.db.DeletionHistoryEntity
import com.example.data.db.HistoryOutcome
import com.example.data.db.MediaType
import com.example.data.db.ProtectedFolderEntity
import com.example.data.db.ScheduleStatus
import com.example.data.db.ScheduledDeletionEntity
import com.example.data.db.TrackedMediaEntity
import com.example.data.media.DeletePreparationResult
import com.example.data.media.MediaPlatformHelper
import com.example.data.prefs.AppLanguage
import com.example.data.prefs.AppSettings
import com.example.data.prefs.ThemeMode
import com.example.scheduler.NotificationHelper
import com.example.util.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class DashboardStats(
    val trackedPhotos: Int = 0,
    val trackedVideos: Int = 0,
    val scheduledPhotos: Int = 0,
    val scheduledVideos: Int = 0,
    val upcoming24h: Int = 0,
    val completedDeletions: Int = 0,
    val activeCountdowns: Int = 0,
    val awaitingConfirmation: Int = 0
)

sealed class TimerTarget {
    data class NewMedia(val items: List<TrackedMediaEntity>) : TimerTarget()
    data class EditScheduled(val item: ScheduledDeletionEntity) : TimerTarget()
}

data class MediaPreviewPayload(
    val uriString: String,
    val displayName: String,
    val mediaType: MediaType,
    val sizeBytes: Long,
    val dateAddedMillis: Long,
    val durationMillis: Long,
    val folderName: String,
    val trackedItem: TrackedMediaEntity? = null
)

sealed class TimeDeleteUiEvent {
    data class ShowMessage(val message: String) : TimeDeleteUiEvent()
    data class LaunchSystemDelete(
        val request: IntentSenderRequest,
        val targetItems: List<ScheduledDeletionEntity>
    ) : TimeDeleteUiEvent()
}

class TimeDeleteViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TimeDeleteApp
    private val repository = app.repository

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val photos: StateFlow<List<TrackedMediaEntity>> = repository.trackedPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videos: StateFlow<List<TrackedMediaEntity>> = repository.trackedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduledItems: StateFlow<List<ScheduledDeletionEntity>> = repository.scheduledDeletions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyItems: StateFlow<List<DeletionHistoryEntity>> = repository.deletionHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val protectedFolders: StateFlow<List<ProtectedFolderEntity>> = repository.protectedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = combine(
        photos,
        videos,
        scheduledItems,
        historyItems,
        nowMillis
    ) { photoList, videoList, scheduledList, historyList, now ->
        val dayAhead = now + TimeUnit.HOURS.toMillis(24)
        DashboardStats(
            trackedPhotos = photoList.size,
            trackedVideos = videoList.size,
            scheduledPhotos = scheduledList.count { it.mediaType == MediaType.PHOTO },
            scheduledVideos = scheduledList.count { it.mediaType == MediaType.VIDEO },
            upcoming24h = scheduledList.count {
                it.status == ScheduleStatus.SCHEDULED && it.deleteAtMillis <= dayAhead
            },
            completedDeletions = historyList.count { it.outcome == HistoryOutcome.DELETED },
            activeCountdowns = scheduledList.count { it.status == ScheduleStatus.SCHEDULED },
            awaitingConfirmation = scheduledList.count { it.status == ScheduleStatus.AWAITING_CONFIRMATION }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    private val _selectedPhotoUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedPhotoUris: StateFlow<Set<String>> = _selectedPhotoUris.asStateFlow()

    private val _selectedVideoUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedVideoUris: StateFlow<Set<String>> = _selectedVideoUris.asStateFlow()

    private val _timerTarget = MutableStateFlow<TimerTarget?>(null)
    val timerTarget: StateFlow<TimerTarget?> = _timerTarget.asStateFlow()

    private val _previewPayload = MutableStateFlow<MediaPreviewPayload?>(null)
    val previewPayload: StateFlow<MediaPreviewPayload?> = _previewPayload.asStateFlow()

    private val _pendingConfirmationItems = MutableStateFlow<List<ScheduledDeletionEntity>>(emptyList())
    val pendingConfirmationItems: StateFlow<List<ScheduledDeletionEntity>> = _pendingConfirmationItems.asStateFlow()

    private val _uiEvents = MutableSharedFlow<TimeDeleteUiEvent>()
    val uiEvents: SharedFlow<TimeDeleteUiEvent> = _uiEvents.asSharedFlow()

    private var activeDeleteBatch: List<ScheduledDeletionEntity> = emptyList()
    private var isEvaluating = false

    private fun lCtx(): Context = LocaleHelper.localizedContext(app, settings.value.appLanguage)

    init {
        viewModelScope.launch {
            repository.refreshTrackedLibraryIntegrity()
            repository.evaluateScheduledCountdowns()
        }
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val now = System.currentTimeMillis()
                _nowMillis.value = now
                val currentSettings = settings.value
                val hasExpired = scheduledItems.value.any {
                    it.status == ScheduleStatus.SCHEDULED && it.deleteAtMillis <= now
                }
                if (!currentSettings.isPausedAll && hasExpired && !isEvaluating) {
                    isEvaluating = true
                    try {
                        repository.evaluateScheduledCountdowns()
                    } finally {
                        isEvaluating = false
                    }
                }
            }
        }
    }

    fun onMediaPickedFromDevice(
        uris: List<Uri>,
        fallbackType: MediaType? = null,
        openTimerImmediately: Boolean = false
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val ctx = lCtx()
            val imported = repository.importMediaUris(uris, fallbackType)
            if (imported.isEmpty()) {
                _uiEvents.emit(TimeDeleteUiEvent.ShowMessage(ctx.getString(R.string.msg_metadata_error)))
                return@launch
            }
            if (openTimerImmediately) {
                _timerTarget.value = TimerTarget.NewMedia(imported)
            } else {
                val label = if (imported.size == 1) imported.first().displayName else imported.size.toString()
                _uiEvents.emit(TimeDeleteUiEvent.ShowMessage(ctx.getString(R.string.msg_loaded_media, label)))
            }
        }
    }

    fun toggleSelection(media: TrackedMediaEntity) {
        if (media.mediaType == MediaType.PHOTO) {
            _selectedPhotoUris.value = _selectedPhotoUris.value.toMutableSet().apply {
                if (!add(media.uriString)) remove(media.uriString)
            }
        } else {
            _selectedVideoUris.value = _selectedVideoUris.value.toMutableSet().apply {
                if (!add(media.uriString)) remove(media.uriString)
            }
        }
    }

    fun selectAll(mediaType: MediaType, visibleUris: List<String>) {
        if (mediaType == MediaType.PHOTO) {
            _selectedPhotoUris.value = visibleUris.toSet()
        } else {
            _selectedVideoUris.value = visibleUris.toSet()
        }
    }

    fun deselectAll(mediaType: MediaType) {
        if (mediaType == MediaType.PHOTO) {
            _selectedPhotoUris.value = emptySet()
        } else {
            _selectedVideoUris.value = emptySet()
        }
    }

    fun openTimerForSelected(mediaType: MediaType) {
        val selectedSet = if (mediaType == MediaType.PHOTO) _selectedPhotoUris.value else _selectedVideoUris.value
        val sourceList = if (mediaType == MediaType.PHOTO) photos.value else videos.value
        val items = sourceList.filter { it.uriString in selectedSet }
        if (items.isNotEmpty()) {
            _timerTarget.value = TimerTarget.NewMedia(items)
        }
    }

    fun openTimerForSingle(item: TrackedMediaEntity) {
        _timerTarget.value = TimerTarget.NewMedia(listOf(item))
    }

    fun openEditTimer(item: ScheduledDeletionEntity) {
        _timerTarget.value = TimerTarget.EditScheduled(item)
    }

    fun dismissTimerPicker() {
        _timerTarget.value = null
    }

    fun openPreview(item: TrackedMediaEntity) {
        _previewPayload.value = MediaPreviewPayload(
            uriString = item.uriString,
            displayName = item.displayName,
            mediaType = item.mediaType,
            sizeBytes = item.sizeBytes,
            dateAddedMillis = item.dateAddedMillis,
            durationMillis = item.durationMillis,
            folderName = item.folderName,
            trackedItem = item
        )
    }

    fun openPreview(item: ScheduledDeletionEntity) {
        _previewPayload.value = MediaPreviewPayload(
            uriString = item.uriString,
            displayName = item.displayName,
            mediaType = item.mediaType,
            sizeBytes = item.sizeBytes,
            dateAddedMillis = item.dateAddedMillis,
            durationMillis = item.durationMillis,
            folderName = item.folderName
        )
    }

    fun dismissPreview() {
        _previewPayload.value = null
    }

    fun confirmTimerSelection(target: TimerTarget, deleteAtMillis: Long, timerLabel: String) {
        viewModelScope.launch {
            val ctx = lCtx()
            when (target) {
                is TimerTarget.NewMedia -> {
                    val outcome = repository.scheduleMediaDeletion(target.items, deleteAtMillis, timerLabel)
                    _selectedPhotoUris.value = _selectedPhotoUris.value - target.items.map { it.uriString }.toSet()
                    _selectedVideoUris.value = _selectedVideoUris.value - target.items.map { it.uriString }.toSet()
                    _timerTarget.value = null

                    val parts = mutableListOf<String>()
                    if (outcome.scheduledCount > 0) {
                        parts += ctx.getString(R.string.msg_scheduled_count, outcome.scheduledCount, timerLabel)
                    }
                    if (outcome.skippedDuplicates > 0) {
                        parts += ctx.getString(R.string.msg_skipped_dups, outcome.skippedDuplicates)
                    }
                    if (outcome.blockedProtected > 0) {
                        parts += ctx.getString(
                            R.string.msg_blocked_protected,
                            outcome.blockedProtected,
                            outcome.blockedFolderNames.joinToString()
                        )
                    }
                    if (parts.isNotEmpty()) {
                        _uiEvents.emit(TimeDeleteUiEvent.ShowMessage(parts.joinToString(" • ")))
                    }
                }

                is TimerTarget.EditScheduled -> {
                    repository.updateScheduledTimer(target.item.id, deleteAtMillis)
                    _timerTarget.value = null
                    _uiEvents.emit(
                        TimeDeleteUiEvent.ShowMessage(
                            ctx.getString(R.string.msg_updated_timer, target.item.displayName, timerLabel)
                        )
                    )
                }
            }
        }
    }

    fun cancelScheduledDeletion(item: ScheduledDeletionEntity) {
        viewModelScope.launch {
            val ctx = lCtx()
            repository.cancelScheduledDeletion(item, logInHistory = true)
            _uiEvents.emit(
                TimeDeleteUiEvent.ShowMessage(
                    ctx.getString(R.string.msg_cancelled_schedule, item.displayName)
                )
            )
        }
    }

    fun requestFinalDeletionConfirmation(items: List<ScheduledDeletionEntity>) {
        if (items.isEmpty()) return
        _pendingConfirmationItems.value = items
    }

    fun dismissFinalConfirmationDialog() {
        _pendingConfirmationItems.value = emptyList()
    }

    fun executeConfirmedDeletion(items: List<ScheduledDeletionEntity>) {
        _pendingConfirmationItems.value = emptyList()
        if (items.isEmpty()) return
        viewModelScope.launch {
            val ctx = lCtx()
            when (val prep = MediaPlatformHelper.prepareDeletionRequest(app, items)) {
                is DeletePreparationResult.RequiresSystemConfirmation -> {
                    activeDeleteBatch = prep.targetItems
                    _uiEvents.emit(TimeDeleteUiEvent.LaunchSystemDelete(prep.intentSenderRequest, prep.targetItems))
                }
                is DeletePreparationResult.DirectAttemptCompleted -> {
                    val deletedItems = items.filter { it.id in prep.deletedIds }
                    if (deletedItems.isNotEmpty()) {
                        val (confirmedCount, _) = repository.finalizeVerifiedDeletions(deletedItems)
                        if (confirmedCount > 0) {
                            _uiEvents.emit(
                                TimeDeleteUiEvent.ShowMessage(
                                    ctx.getString(R.string.msg_deleted_confirmed, confirmedCount)
                                )
                            )
                        }
                    }
                    for ((failedItem, reason) in prep.failedItems) {
                        repository.recordDeletionFailure(listOf(failedItem), reason)
                    }
                    if (prep.failedItems.isNotEmpty()) {
                        _uiEvents.emit(
                            TimeDeleteUiEvent.ShowMessage(prep.failedItems.first().second)
                        )
                    }
                }
                is DeletePreparationResult.Failed -> {
                    repository.recordDeletionFailure(items, prep.reason)
                    _uiEvents.emit(TimeDeleteUiEvent.ShowMessage(prep.reason))
                }
            }
        }
    }

    fun onSystemDeleteResult(granted: Boolean) {
        val batch = activeDeleteBatch
        activeDeleteBatch = emptyList()
        if (batch.isEmpty()) return

        viewModelScope.launch {
            val ctx = lCtx()
            if (granted) {
                val (confirmed, failed) = repository.finalizeVerifiedDeletions(batch)
                val msg = when {
                    confirmed > 0 && failed == 0 -> ctx.getString(R.string.msg_deleted_perm, confirmed)
                    confirmed > 0 -> ctx.getString(R.string.msg_deleted_partial, confirmed, failed)
                    else -> ctx.getString(R.string.msg_deleted_verify_failed)
                }
                _uiEvents.emit(TimeDeleteUiEvent.ShowMessage(msg))
            } else {
                repository.recordDeletionFailure(
                    batch,
                    ctx.getString(R.string.msg_user_declined_delete)
                )
                _uiEvents.emit(
                    TimeDeleteUiEvent.ShowMessage(ctx.getString(R.string.msg_delete_not_authorized))
                )
            }
        }
    }

    fun addProtectedFolder(folderName: String, treeUri: String? = null) {
        viewModelScope.launch {
            val ctx = lCtx()
            val added = repository.addProtectedFolder(folderName, treeUri)
            val res = if (added) R.string.msg_protected_added else R.string.msg_protected_exists
            _uiEvents.emit(TimeDeleteUiEvent.ShowMessage(ctx.getString(res, folderName)))
        }
    }

    fun removeProtectedFolder(folder: ProtectedFolderEntity) {
        viewModelScope.launch {
            val ctx = lCtx()
            repository.removeProtectedFolder(folder.id)
            _uiEvents.emit(
                TimeDeleteUiEvent.ShowMessage(
                    ctx.getString(R.string.msg_protected_removed, folder.folderName)
                )
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val ctx = lCtx()
            repository.clearHistory()
            _uiEvents.emit(TimeDeleteUiEvent.ShowMessage(ctx.getString(R.string.msg_history_cleared)))
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setAppLanguage(language) }
    }

    fun setPausedAll(paused: Boolean) {
        viewModelScope.launch {
            val ctx = lCtx()
            repository.setPausedAll(paused)
            val res = if (paused) R.string.msg_all_paused else R.string.msg_all_resumed
            _uiEvents.emit(TimeDeleteUiEvent.ShowMessage(ctx.getString(res)))
        }
    }

    fun setBackgroundMonitorEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setBackgroundMonitorEnabled(enabled) }
    }

    fun setNotifyScheduled(enabled: Boolean) = viewModelScope.launch { repository.setNotifyScheduled(enabled) }
    fun setNotifyApproaching(enabled: Boolean) = viewModelScope.launch { repository.setNotifyApproaching(enabled) }
    fun setNotifyExpired(enabled: Boolean) = viewModelScope.launch { repository.setNotifyExpired(enabled) }
    fun setNotifyAwaitingConfirmation(enabled: Boolean) = viewModelScope.launch { repository.setNotifyAwaitingConfirmation(enabled) }
    fun setNotifyCompleted(enabled: Boolean) = viewModelScope.launch { repository.setNotifyCompleted(enabled) }
    fun setNotifyFailed(enabled: Boolean) = viewModelScope.launch { repository.setNotifyFailed(enabled) }
    fun setApproachingLeadMinutes(minutes: Int) = viewModelScope.launch { repository.setApproachingLeadMinutes(minutes) }

    fun sendTestNotification() {
        val lang = settings.value.appLanguage
        NotificationHelper.sendTestNotification(app, lang)
        viewModelScope.launch {
            val ctx = lCtx()
            if (NotificationHelper.hasNotificationPermission(app)) {
                _uiEvents.emit(TimeDeleteUiEvent.ShowMessage(ctx.getString(R.string.msg_test_notif_sent)))
            } else {
                _uiEvents.emit(TimeDeleteUiEvent.ShowMessage(ctx.getString(R.string.msg_grant_notif_perm)))
            }
        }
    }
}
