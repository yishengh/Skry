package com.yishenghuang.skry.ui.risk

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yishenghuang.skry.SkryApplication
import com.yishenghuang.skry.R
import com.yishenghuang.skry.data.MediaRepository
import com.yishenghuang.skry.data.PhotoEntity
import com.yishenghuang.skry.data.UserReviewStatus
import com.yishenghuang.skry.domain.DetectableCategories
import com.yishenghuang.skry.domain.DetectableCategory
import com.yishenghuang.skry.domain.Finding
import com.yishenghuang.skry.domain.FindingLabels
import com.yishenghuang.skry.domain.FindingType
import com.yishenghuang.skry.domain.FindingsJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RiskListFilter {
    NeedsReview,
    Confirmed,
    Cleared
}

data class RiskItem(
    val id: String,
    val uri: String,
    val typeLabel: String,
    val subtitle: String,
    val hasSensitiveRegion: Boolean,
    val userReview: UserReviewStatus,
    val findings: List<Finding>
)

data class RiskUiState(
    val filter: RiskListFilter = RiskListFilter.NeedsReview,
    val categoryFilter: FindingType? = null,
    val items: List<RiskItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val categories: List<DetectableCategory> = DetectableCategories.all
)

class RiskViewModel(
    application: Application,
    private val repository: MediaRepository
) : AndroidViewModel(application) {

    private val appContext get() = getApplication<Application>()

    private val filter = MutableStateFlow(RiskListFilter.NeedsReview)
    private val categoryFilter = MutableStateFlow<FindingType?>(null)
    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())

    private val lists = combine(
        repository.observeRiskPhotos(),
        repository.observeConfirmedRiskPhotos(),
        repository.observeClearedRiskPhotos()
    ) { active, confirmed, cleared ->
        Triple(active, confirmed, cleared)
    }

    val uiState: StateFlow<RiskUiState> = combine(
        lists,
        filter,
        categoryFilter,
        selectedIds
    ) { photoLists, currentFilter, category, selected ->
        val source = when (currentFilter) {
            RiskListFilter.NeedsReview ->
                photoLists.first.filter { it.userReview == UserReviewStatus.NONE }
            RiskListFilter.Confirmed -> photoLists.second
            RiskListFilter.Cleared -> photoLists.third
        }
        var items = source.map { it.toRiskItem(appContext) }
        if (category != null) {
            items = items.filter { item -> item.findings.any { it.type == category } }
        }
        RiskUiState(
            filter = currentFilter,
            categoryFilter = category,
            items = items,
            selectedIds = selected.filter { id -> items.any { it.id == id } }.toSet(),
            categories = DetectableCategories.all
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RiskUiState()
    )

    fun setFilter(value: RiskListFilter) {
        filter.value = value
        selectedIds.value = emptySet()
    }

    fun setCategoryFilter(type: FindingType?) {
        categoryFilter.value = type
        selectedIds.value = emptySet()
    }

    fun toggleSelection(id: String) {
        selectedIds.value = selectedIds.value.toMutableSet().also { set ->
            if (!set.add(id)) set.remove(id)
        }
    }

    fun selectAllVisible() {
        selectedIds.value = uiState.value.items.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun confirmLeak(id: String) {
        viewModelScope.launch { repository.setUserReview(id, UserReviewStatus.CONFIRMED_LEAK) }
    }

    fun dismissFalsePositive(id: String) {
        viewModelScope.launch { repository.setUserReview(id, UserReviewStatus.DISMISSED) }
    }

    fun restoreReview(id: String) {
        viewModelScope.launch { repository.setUserReview(id, UserReviewStatus.NONE) }
    }

    fun batchConfirmSelected() {
        val ids = selectedIds.value.toList()
        viewModelScope.launch {
            ids.forEach { repository.setUserReview(it, UserReviewStatus.CONFIRMED_LEAK) }
            selectedIds.value = emptySet()
        }
    }

    fun batchClearSelected() {
        val ids = selectedIds.value.toList()
        viewModelScope.launch {
            ids.forEach { repository.setUserReview(it, UserReviewStatus.DISMISSED) }
            selectedIds.value = emptySet()
        }
    }

    fun batchRestoreSelected() {
        val ids = selectedIds.value.toList()
        viewModelScope.launch {
            ids.forEach { repository.setUserReview(it, UserReviewStatus.NONE) }
            selectedIds.value = emptySet()
        }
    }

    fun moveToVault(id: String, onResult: (Boolean, String?, android.net.Uri?) -> Unit) {
        viewModelScope.launch {
            val outcome = repository.moveToVault(id)
            onResult(
                outcome.success,
                outcome.message,
                outcome.originalUri.takeIf { outcome.success && it != android.net.Uri.EMPTY }
            )
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = application as SkryApplication
                    return RiskViewModel(app, app.mediaRepository) as T
                }
            }
    }
}

private fun PhotoEntity.toRiskItem(context: Application): RiskItem {
    val findings = FindingsJson.decode(findingsJson)
    val primary = findings.maxByOrNull { it.confidence }
    val hasSensitiveRegion = findings.any {
        it.type == FindingType.CREDIT_CARD ||
            it.type == FindingType.PASSPORT ||
            it.type == FindingType.IDENTITY_CARD ||
            it.type == FindingType.SSN_LIKE ||
            it.type == FindingType.PHONE_NUMBER ||
            it.type == FindingType.EMAIL_ADDRESS ||
            it.type == FindingType.PHYSICAL_ADDRESS ||
            it.type == FindingType.IBAN ||
            it.type == FindingType.SECRET_TOKEN ||
            (it.boxLeft != null && it.boxTop != null)
    }
    val typeLabel = primary?.let { context.getString(FindingLabels.titleRes(it.type)) }
        ?: context.getString(R.string.finding_generic)
    return RiskItem(
        id = id,
        uri = uri,
        typeLabel = typeLabel,
        subtitle = primary?.snippet
            ?: findings.joinToString(" · ") {
                context.getString(FindingLabels.titleRes(it.type))
            }.ifBlank { typeLabel },
        hasSensitiveRegion = hasSensitiveRegion,
        userReview = userReview,
        findings = findings
    )
}
