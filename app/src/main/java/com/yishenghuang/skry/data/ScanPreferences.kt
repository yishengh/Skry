package com.yishenghuang.skry.data

import android.content.Context

/**
 * Persists whether the user started a privacy scan that may still have PENDING work.
 * Survives process death so we can resume via WorkManager.
 */
class ScanPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var isScanActive: Boolean
        get() = prefs.getBoolean(KEY_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_ACTIVE, value).apply()

    var sessionRisksFound: Int
        get() = prefs.getInt(KEY_RISKS, 0)
        set(value) = prefs.edit().putInt(KEY_RISKS, value).apply()

    fun beginUserScan() {
        prefs.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putInt(KEY_RISKS, 0)
            .apply()
    }

    fun addRisks(count: Int) {
        if (count <= 0) return
        prefs.edit().putInt(KEY_RISKS, sessionRisksFound + count).apply()
    }

    fun completeScan() {
        prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
    }

    companion object {
        private const val PREFS = "skry_scan"
        private const val KEY_ACTIVE = "scan_active"
        private const val KEY_RISKS = "session_risks"
    }
}
