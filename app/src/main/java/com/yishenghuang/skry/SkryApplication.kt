package com.yishenghuang.skry

import android.app.Application
import com.yishenghuang.skry.data.MediaRepository
import com.yishenghuang.skry.data.ScanPreferences
import com.yishenghuang.skry.data.SkryDatabase
import com.yishenghuang.skry.domain.VaultService
import com.yishenghuang.skry.worker.FullScanWorker

class SkryApplication : Application() {
    val database by lazy { SkryDatabase.get(this) }
    val vaultService by lazy { VaultService(this) }
    val mediaRepository by lazy {
        MediaRepository(this, database.photoDao(), vaultService)
    }
    val scanPreferences by lazy { ScanPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        // Resume interrupted privacy scan after force-stop / process death.
        FullScanWorker.resumeIfNeeded(this)
    }
}
