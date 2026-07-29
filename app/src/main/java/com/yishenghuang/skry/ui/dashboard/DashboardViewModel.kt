package com.yishenghuang.skry.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.yishenghuang.skry.SkryApplication
import com.yishenghuang.skry.data.MediaRepository
import com.yishenghuang.skry.data.ScanPreferences
import com.yishenghuang.skry.worker.FullScanWorker
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
    val hasPermission: Boolean = false
)

class DashboardViewModel(
    application: Application,
    private val repository: MediaRepository,
    private val scanPreferences: ScanPreferences
) : AndroidViewModel(application) {

    private val permissionGranted = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val statusMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

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
        statusMessage
    ) { stats, permitted, running, message ->
        val scanning = running || (scanPreferences.isScanActive && stats.pending > 0)
        val progress = if (stats.library == 0) {
            0f
        } else {
            stats.audited.toFloat() / (stats.audited + stats.pending).coerceAtLeast(1)
        }
        val penalty = (stats.risk * 4 + stats.duplicates + stats.blurry).coerceAtMost(80)
        val health = ((100 - penalty) / 100f).coerceIn(0.05f, 1f)
        val computedMessage = when {
            message != null -> message
            scanning && stats.pending > 0 ->
                "Scanning ${stats.audited} / ${stats.audited + stats.pending} · ${stats.pending} left"
            !scanning && stats.pending > 0 && scanPreferences.isScanActive ->
                "Scan paused · ${stats.pending} left · tap ring to resume"
            !scanning && stats.library > 0 && stats.pending == 0 ->
                "All ${stats.audited} audited · ${stats.risk} risks"
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
            hasPermission = permitted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardViewState()
    )

    fun onPermissionResult(granted: Boolean) {
        permissionGranted.value = granted
        if (granted) {
            // Index only — never start privacy OCR automatically.
            viewModelScope.launch {
                runCatching { repository.syncGallery() }
                    .onSuccess { result ->
                        statusMessage.value =
                            "${result.totalKnown} indexed · tap the ring to audit"
                    }
            }
            FullScanWorker.resumeIfNeeded(getApplication())
        } else {
            statusMessage.value = "Photo access is required to audit your gallery."
        }
    }

    fun scanGallery() {
        if (!permissionGranted.value) return
        viewModelScope.launch {
            statusMessage.value = "Indexing gallery…"
            runCatching { repository.syncGallery() }
                .onSuccess { indexed ->
                    // Backfill quality metrics for photos audited before Phase 3.
                    repository.requeueMissingQuality()
                    val pending = repository.pendingCount()
                    if (pending == 0) {
                        scanPreferences.completeScan()
                        statusMessage.value =
                            "Up to date · ${indexed.totalKnown} already audited"
                        return@onSuccess
                    }
                    scanPreferences.beginUserScan()
                    statusMessage.value = "Scanning · $pending left"
                    FullScanWorker.enqueue(
                        context = getApplication(),
                        userInitiated = true,
                        replace = true
                    )
                }
                .onFailure {
                    statusMessage.value = "Scan failed. Try again."
                }
        }
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
