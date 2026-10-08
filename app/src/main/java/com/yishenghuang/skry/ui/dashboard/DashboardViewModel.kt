package com.yishenghuang.skry.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.yishenghuang.skry.R
import com.yishenghuang.skry.SkryApplication
import com.yishenghuang.skry.data.MediaRepository
import com.yishenghuang.skry.data.ScanPreferences
import com.yishenghuang.skry.worker.FullScanWorker
import com.yishenghuang.skry.util.MediaAccess
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardViewState(
    val healthScore: Float = 1f,
    val highRiskCount: Int = 0,
    val duplicateCount: Int = 0,
    val blurryCount: Int = 0,
    val libraryCount: Int = 0,
    val auditedCount: Int = 0,
    val pendingCount: Int = 0,
    val scanProgress: Float = 0f,
    val isScanning: Boolean = false,
    val lastScanMessage: String? = null,
    val hasPermission: Boolean = false,
    val partialAccess: Boolean = false,
    val locationMetadataUnavailable: Boolean = false
)

class DashboardViewModel(
    application: Application,
    private val repository: MediaRepository,
    private val scanPreferences: ScanPreferences
) : AndroidViewModel(application) {

    private val app get() = getApplication<Application>()
    private val permissionGranted = kotlinx.coroutines.flow.MutableStateFlow(0)
    private val statusMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val indexing = kotlinx.coroutines.flow.MutableStateFlow(false)
    private var scanJob: kotlinx.coroutines.Job? = null

    private val workRunning = WorkManager.getInstance(application)
        .getWorkInfosForUniqueWorkFlow(FullScanWorker.UNIQUE_NAME)
        .map { infos ->
            infos.any {
                it.state == WorkInfo.State.RUNNING ||
                    it.state == WorkInfo.State.ENQUEUED ||
                    it.state == WorkInfo.State.BLOCKED
            }
        }

    private val libraryStats = combine(
        repository.observeCount(),
        repository.observeRiskCount(),
        repository.observeDuplicateCandidateCount(),
        repository.observeBlurryCount(),
        repository.observeAuditedCount(),
        repository.observePendingCount()
    ) { values ->
        LibraryStats(
            library = values[0],
            risk = values[1],
            duplicates = values[2],
            blurry = values[3],
            audited = values[4],
            pending = values[5]
        )
    }

    val uiState: StateFlow<DashboardViewState> = combine(
        libraryStats,
        permissionGranted,
        workRunning,
        statusMessage,
        indexing
    ) { stats, permission, running, message, indexingNow ->
        val permitted = permission != 0
        val scanning = permitted && (running || indexingNow)
        val errors = (stats.library - stats.audited - stats.pending).coerceAtLeast(0)
        val progress = if (stats.library == 0) {
            0f
        } else {
            (stats.audited + errors).toFloat() / stats.library.coerceAtLeast(1)
        }
        val penalty = (stats.risk * 4 + stats.duplicates + stats.blurry).coerceAtMost(80)
        val health = ((100 - penalty) / 100f).coerceIn(0.05f, 1f)
        val computedMessage = when {
            !permitted -> app.getString(R.string.home_msg_need_permission)
            scanning && stats.pending > 0 ->
                app.getString(
                    R.string.home_msg_scanning,
                    stats.audited,
                    stats.audited + stats.pending,
                    stats.pending
                )
            !scanning && stats.pending > 0 && scanPreferences.isScanActive ->
                app.getString(R.string.home_msg_paused, stats.pending)
            errors > 0 -> app.getString(R.string.home_msg_errors, errors)
            message != null -> message
            !scanning && stats.library > 0 && stats.pending == 0 ->
                app.getString(R.string.home_msg_all_audited, stats.audited, stats.risk)
            else -> null
        }
        DashboardViewState(
            healthScore = if (stats.library == 0) 1f else health,
            highRiskCount = stats.risk,
            duplicateCount = stats.duplicates,
            blurryCount = stats.blurry,
            libraryCount = stats.library,
            auditedCount = stats.audited,
            pendingCount = stats.pending,
            scanProgress = progress,
            isScanning = scanning,
            lastScanMessage = computedMessage,
            hasPermission = permitted,
            partialAccess = permitted && !MediaAccess.hasFullGalleryAccess(app),
            locationMetadataUnavailable = permitted && !MediaAccess.canReadLocationMetadata(app)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardViewState()
    )

    fun onPermissionResult(granted: Boolean) {
        permissionGranted.value = if (!granted) 0 else if (MediaAccess.hasFullGalleryAccess(app)) 2 else 1
        if (granted) {
            viewModelScope.launch {
                runCatching { repository.syncGallery() }
                    .onSuccess { statusMessage.value = null }
                    .onFailure { statusMessage.value = app.getString(R.string.home_msg_scan_failed) }
            }
            FullScanWorker.resumeIfNeeded(getApplication())
        } else {
            WorkManager.getInstance(app).cancelUniqueWork(FullScanWorker.UNIQUE_NAME)
            viewModelScope.launch { repository.syncGallery() }
            statusMessage.value = app.getString(R.string.home_msg_need_permission)
        }
    }

    fun scanGallery() {
        if (permissionGranted.value == 0 || indexing.value || uiState.value.isScanning) return
        indexing.value = true
        scanJob = viewModelScope.launch {
            try {
            statusMessage.value = app.getString(R.string.home_msg_indexing)
            runCatching { repository.syncGallery() }
                .onSuccess { indexed ->
                    repository.requeueMissingQuality()
                    val pending = repository.pendingCount()
                    if (pending == 0) {
                        scanPreferences.completeScan()
                        statusMessage.value =
                            app.getString(R.string.home_msg_up_to_date, indexed.totalKnown)
                        return@onSuccess
                    }
                    scanPreferences.beginUserScan()
                    statusMessage.value = null
                    FullScanWorker.enqueue(
                        context = getApplication(),
                        userInitiated = true,
                        replace = true
                    )
                }
                .onFailure {
                    if (it is kotlinx.coroutines.CancellationException) throw it
                    statusMessage.value = app.getString(R.string.home_msg_scan_failed)
                }
            } finally { indexing.value = false }
        }
    }

    fun pauseScan() {
        scanJob?.cancel()
        scanPreferences.completeScan()
        WorkManager.getInstance(app).cancelUniqueWork(FullScanWorker.UNIQUE_NAME)
        statusMessage.value = app.getString(R.string.home_msg_paused, uiState.value.pendingCount)
    }

    private data class LibraryStats(
        val library: Int,
        val risk: Int,
        val duplicates: Int,
        val blurry: Int,
        val audited: Int,
        val pending: Int
    )

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = application as SkryApplication
                    return DashboardViewModel(
                        app,
                        app.mediaRepository,
                        app.scanPreferences
                    ) as T
                }
            }
    }
}
