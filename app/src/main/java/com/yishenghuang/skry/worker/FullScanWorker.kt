package com.yishenghuang.skry.worker

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yishenghuang.skry.SkryApplication
import com.yishenghuang.skry.data.ScanPreferences

/**
 * Durable privacy scan over PENDING photos only.
 * Survives backgrounding / process death via WorkManager; already DONE/ERROR are skipped.
 */
class FullScanWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SkryApplication
            ?: return Result.failure()
        val prefs = ScanPreferences(applicationContext)
        val repository = app.mediaRepository

        return runCatching {
            var runProcessed = 0

            while (runProcessed < MAX_PHOTOS_PER_RUN) {
                if (isStopped) {
                    prefs.isScanActive = true
                    return@runCatching Result.success(
                        workDataOf(
                            KEY_PROCESSED to runProcessed,
                            KEY_REMAINING to repository.pendingCount(),
                            KEY_RISKS to prefs.sessionRisksFound,
                            KEY_FINISHED to false
                        )
                    )
                }

                val batchResult = repository.runPrivacyScan(
                    maxPhotos = BATCH_SIZE,
                    batchSize = BATCH_SIZE,
                    forceRescan = false,
                    shouldAbort = { isStopped }
                )
                runProcessed += batchResult.processed
                prefs.addRisks(batchResult.risksFound)

                val pending = batchResult.remaining
                setProgress(
                    workDataOf(
                        KEY_PROCESSED to runProcessed,
                        KEY_REMAINING to pending,
                        KEY_RISKS to prefs.sessionRisksFound,
                        KEY_FINISHED to false
                    )
                )

                if (batchResult.processed == 0 || pending == 0) break
            }

            val remaining = repository.pendingCount()
            if (remaining > 0 && !isStopped) {
                enqueue(applicationContext, userInitiated = true, replace = false)
                prefs.isScanActive = true
            } else if (remaining == 0) {
                prefs.completeScan()
            } else {
                prefs.isScanActive = true
            }

            Result.success(
                workDataOf(
                    KEY_PROCESSED to runProcessed,
                    KEY_REMAINING to remaining,
                    KEY_RISKS to prefs.sessionRisksFound,
                    KEY_FINISHED to (remaining == 0)
                )
            )
        }.getOrElse {
            prefs.isScanActive = true
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "skry_full_privacy_scan"
        const val KEY_PROCESSED = "processed"
        const val KEY_REMAINING = "remaining"
        const val KEY_RISKS = "risks"
        const val KEY_FINISHED = "finished"

        private const val BATCH_SIZE = 10
        private const val MAX_PHOTOS_PER_RUN = 40

        fun enqueue(
            context: Context,
            userInitiated: Boolean = false,
            replace: Boolean = true
        ) {
            val request = OneTimeWorkRequestBuilder<FullScanWorker>()
                .apply {
                    // Expedited without getForegroundInfo() is only safe on API 31+.
                    if (userInitiated && Build.VERSION.SDK_INT >= 31) {
                        setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    }
                }
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }

        fun resumeIfNeeded(context: Context) {
            val prefs = ScanPreferences(context)
            if (!prefs.isScanActive) return
            enqueue(context, userInitiated = true, replace = false)
        }
    }
}
