package com.yishenghuang.skry.ui

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yishenghuang.skry.R
import com.yishenghuang.skry.SkryApplication
import com.yishenghuang.skry.util.MediaAccess
import com.yishenghuang.skry.util.SavedUriList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Serial deletion, retained across configuration changes and system consent recreation. */
class GalleryDeleteViewModel(application: Application, private val saved: SavedStateHandle) :
    AndroidViewModel(application) {
    data class ConsentRequest(val sequence: Long, val intentSender: IntentSender)
    private val app get() = getApplication<SkryApplication>()
    val confirmation = MutableStateFlow(SavedUriList.decode(saved["confirmationUris"]))
    val busy = saved.getStateFlow("busy", false)
    val sender = MutableStateFlow<ConsentRequest?>(null)
    private var consentSequence = 0L
    val message = MutableStateFlow<String?>(null)
    private var running = false
    private var cancelRequested = false
    private var queueCache = SavedUriList.decode(saved["queueUris"])
    private var queue: ArrayList<String>
        get() = queueCache
        set(value) { queueCache = value; saved["queueUris"] = SavedUriList.encode(value) }
    private var awaiting: ArrayList<String>
        get() = saved["awaiting"] ?: arrayListOf()
        set(value) { saved["awaiting"] = value }

    init {
        // Never replay a destructive operation automatically after process death.
        // A restored system consent result can still reconcile / resume the saved batch.
        if (busy.value && awaiting.isEmpty()) {
            saved["busy"] = false
            queue = arrayListOf()
            message.value = app.getString(R.string.delete_interrupted)
        }
    }

    fun request(uris: List<Uri>) {
        if (busy.value || confirmation.value.isNotEmpty()) return
        val values = ArrayList(uris.distinct().map(Uri::toString))
        saved["confirmationUris"] = SavedUriList.encode(values)
        confirmation.value = values
    }

    fun cancelConfirmation() {
        saved.remove<ByteArray>("confirmationUris")
        confirmation.value = arrayListOf()
    }

    fun confirm() {
        if (busy.value || confirmation.value.isEmpty()) return
        queue = ArrayList(confirmation.value)
        cancelConfirmation()
        saved["busy"] = true
        cancelRequested = false
        saved["failed"] = 0
        saved["deleted"] = 0
        advance()
    }

    fun senderLaunched() { sender.value = null }
    fun launchFailed() { sender.value = null; finish(R.string.delete_failed) }
    fun clearMessage() { message.value = null }

    fun cancelPending() {
        cancelRequested = true
        if (!running) finish(R.string.delete_cancelled)
    }

    fun onSystemResult(approved: Boolean) {
        if (awaiting.isEmpty()) return
        val batch = awaiting
        awaiting = arrayListOf()
        if (!approved) {
            finish(R.string.delete_cancelled)
        } else if (Build.VERSION.SDK_INT == 29) {
            // Android 10 grants write access; unlike API 30+, it has not deleted yet.
            advance(authorizedRetry = true)
        } else {
            saved["deleted"] = (saved.get<Int>("deleted") ?: 0) + batch.size
            queue = ArrayList(queue.drop(batch.size))
            advance()
        }
    }

    private fun advance(authorizedRetry: Boolean = false) {
        if (running) return
        running = true
        viewModelScope.launch {
            try {
                var retry = authorizedRetry
                while (queue.isNotEmpty() && !cancelRequested) {
                    val batch = queue.take(if (Build.VERSION.SDK_INT >= 30) 100 else 1)
                    val outcome = withContext(Dispatchers.IO) {
                        MediaAccess.deleteMedia(app, batch.map(Uri::parse))
                    }
                    when (outcome) {
                        is MediaAccess.DeleteOutcome.NeedsUserConfirmation -> {
                            if (cancelRequested) {
                                finish(R.string.delete_cancelled)
                                return@launch
                            }
                            if (!retry) {
                                awaiting = ArrayList(batch)
                                sender.value = ConsentRequest(++consentSequence, outcome.intentSender)
                                return@launch
                            }
                            saved["failed"] = (saved.get<Int>("failed") ?: 0) + batch.size
                        }
                        MediaAccess.DeleteOutcome.Deleted ->
                            saved["deleted"] = (saved.get<Int>("deleted") ?: 0) + batch.size
                        MediaAccess.DeleteOutcome.Failed ->
                            saved["failed"] = (saved.get<Int>("failed") ?: 0) + batch.size
                    }
                    retry = false
                    queue = ArrayList(queue.drop(batch.size))
                }
                finish(when {
                    cancelRequested -> R.string.delete_cancelled
                    (saved.get<Int>("failed") ?: 0) > 0 -> R.string.delete_failed
                    else -> R.string.delete_done
                })
            } finally { running = false }
        }
    }

    private fun finish(messageRes: Int) {
        sender.value = null
        queue = arrayListOf()
        awaiting = arrayListOf()
        viewModelScope.launch {
            try {
                app.mediaRepository.syncGallery()
                app.mediaRepository.regroupDuplicates()
                message.value = app.getString(messageRes)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                message.value = app.getString(R.string.delete_refresh_failed)
            } finally { saved["busy"] = false }
        }
    }
}
