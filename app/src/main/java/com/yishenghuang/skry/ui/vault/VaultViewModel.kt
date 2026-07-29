package com.yishenghuang.skry.ui.vault

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yishenghuang.skry.R
import com.yishenghuang.skry.SkryApplication
import com.yishenghuang.skry.data.MediaRepository
import com.yishenghuang.skry.data.PhotoEntity
import com.yishenghuang.skry.domain.FindingLabels
import com.yishenghuang.skry.domain.FindingsJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VaultItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val fileName: String,
    val vaultedAt: Long
)

data class VaultUiState(
    val unlocked: Boolean = false,
    val count: Int = 0,
    val items: List<VaultItem> = emptyList(),
    val selectedId: String? = null,
    val preview: Bitmap? = null,
    val loadingPreview: Boolean = false,
    val busy: Boolean = false,
    val message: String? = null
)

class VaultViewModel(
    application: Application,
    private val repository: MediaRepository
) : AndroidViewModel(application) {

    private val app get() = getApplication<Application>()
    private val unlocked = MutableStateFlow(false)
    private val selectedId = MutableStateFlow<String?>(null)
    private val preview = MutableStateFlow<Bitmap?>(null)
    private val loadingPreview = MutableStateFlow(false)
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val session = combine(
        unlocked,
        selectedId,
        preview,
        loadingPreview,
        busy
    ) { isUnlocked, selected, bitmap, loading, isBusy ->
        Session(isUnlocked, selected, bitmap, loading, isBusy)
    }

    val uiState: StateFlow<VaultUiState> = combine(
        session,
        repository.observeVaultPhotos(),
        repository.observeVaultCount(),
        message
    ) { sess, photos, count, msg ->
        VaultUiState(
            unlocked = sess.unlocked,
            count = count,
            items = if (sess.unlocked) {
                photos.map { it.toVaultItem(app) }
            } else {
                emptyList()
            },
            selectedId = sess.selectedId,
            preview = sess.preview,
            loadingPreview = sess.loadingPreview,
            busy = sess.busy,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VaultUiState()
    )

    fun onUnlockSuccess() {
        unlocked.value = true
        message.value = null
    }

    fun lock() {
        unlocked.value = false
        closeDetail()
    }

    fun clearMessage() {
        message.value = null
    }

    fun showMessage(text: String) {
        message.value = text
    }

    fun openItem(id: String) {
        selectedId.value = id
        loadingPreview.value = true
        preview.value?.recycle()
        preview.value = null
        viewModelScope.launch {
            val fileName = getApplication<SkryApplication>().database.photoDao().getById(id)?.vaultFileName
                ?: uiState.value.items.firstOrNull { it.id == id }?.fileName

            if (fileName.isNullOrBlank()) {
                loadingPreview.value = false
                message.value = app.getString(R.string.vault_err_missing_file)
                return@launch
            }
            runCatching {
                val bytes = repository.readVaultBytes(fileName)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.onSuccess { bitmap ->
                preview.value = bitmap
            }.onFailure {
                message.value = it.message ?: app.getString(R.string.vault_err_decrypt)
            }
            loadingPreview.value = false
        }
    }

    fun closeDetail() {
        selectedId.value = null
        preview.value?.recycle()
        preview.value = null
        loadingPreview.value = false
    }

    fun deleteSelected() {
        val id = selectedId.value ?: return
        busy.value = true
        viewModelScope.launch {
            runCatching { repository.deleteFromVault(id) }
                .onSuccess {
                    closeDetail()
                    message.value = app.getString(R.string.vault_msg_removed)
                }
                .onFailure {
                    message.value = it.message ?: app.getString(R.string.vault_err_delete)
                }
            busy.value = false
        }
    }

    private data class Session(
        val unlocked: Boolean,
        val selectedId: String?,
        val preview: Bitmap?,
        val loadingPreview: Boolean,
        val busy: Boolean
    )

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = application as SkryApplication
                    return VaultViewModel(app, app.mediaRepository) as T
                }
            }
    }
}

private fun PhotoEntity.toVaultItem(app: Application): VaultItem {
    val findings = FindingsJson.decode(findingsJson)
    val primary = findings.maxByOrNull { it.confidence }
    val title = primary?.let { app.getString(FindingLabels.titleRes(it.type)) }
        ?: displayName
        ?: app.getString(R.string.vault_item_title)
    return VaultItem(
        id = id,
        title = title,
        subtitle = app.getString(R.string.vault_item_row_sub),
        fileName = vaultFileName.orEmpty(),
        vaultedAt = vaultedAt ?: 0L
    )
}
