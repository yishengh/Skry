package com.yishenghuang.skry.ui.cleaner

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yishenghuang.skry.SkryApplication
import com.yishenghuang.skry.data.MediaRepository
import com.yishenghuang.skry.data.PhotoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CleanerSection {
    Duplicates,
    Blurry,
    ExpiredScreenshots,
    LongScreenshots
}

data class CleanerItem(
    val id: String,
    val uri: String,
    val title: String,
    val subtitle: String,
    val starred: Boolean
)

data class CleanerUiState(
    val duplicateCount: Int = 0,
    val blurryCount: Int = 0,
    val expiredCount: Int = 0,
    val longCount: Int = 0,
    val section: CleanerSection = CleanerSection.Duplicates,
    val items: List<CleanerItem> = emptyList(),
    val selectedIds: Set<String> = emptySet()
)

class CleanerViewModel(
    application: Application,
    private val repository: MediaRepository
) : AndroidViewModel(application) {

    private val section = MutableStateFlow(CleanerSection.Duplicates)
    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val detailId = MutableStateFlow<String?>(null)
    private val relatedItems = MutableStateFlow<List<CleanerItem>>(emptyList())

    private val counts = combine(
        repository.observeDuplicateCandidateCount(),
        repository.observeBlurryCount(),
        repository.observeExpiredScreenshotCount(),
        repository.observeLongScreenshots()
    ) { dup, blur, expired, longPhotos ->
        Counts(dup, blur, expired, longPhotos.size)
    }

    private val lists = combine(
        repository.observeSuggestedDeletes(),
        repository.observeBlurryPhotos(),
        repository.observeExpiredScreenshots(),
        repository.observeLongScreenshots()
    ) { duplicates, blurry, expired, longPhotos ->
        Lists(duplicates, blurry, expired, longPhotos)
    }

    val uiState: StateFlow<CleanerUiState> = combine(
        counts,
        lists,
        section,
        selectedIds
    ) { countState, listState, currentSection, selected ->
        val items = when (currentSection) {
            CleanerSection.Duplicates -> listState.duplicates.map {
                it.toCleanerItem(
                    title = "Duplicate",
                    subtitle = "Keep starred pick · safe to remove extras",
                    starred = it.isStarredPick
                )
            }
            CleanerSection.Blurry -> listState.blurry.map {
                it.toCleanerItem(
                    title = "Blurry",
                    subtitle = "Low sharpness · quality ${it.qualityScore.toInt()}"
                )
            }
            CleanerSection.ExpiredScreenshots -> listState.expired.map {
                it.toCleanerItem(
                    title = "Expired screenshot",
                    subtitle = "Sensitive screenshot older than 7 days"
                )
            }
            CleanerSection.LongScreenshots -> listState.longScreenshots.map {
                it.toCleanerItem(
                    title = "Long screenshot",
                    subtitle = "Unusual aspect ratio"
                )
            }
        }
        CleanerUiState(
            duplicateCount = countState.duplicates,
            blurryCount = countState.blurry,
            expiredCount = countState.expired,
            longCount = countState.longCount,
            section = currentSection,
            items = items,
            selectedIds = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CleanerUiState()
    )

    val detailState: StateFlow<CleanerDetailState> = combine(
        detailId,
        relatedItems,
        uiState
    ) { id, related, cleaner ->
        if (id == null) return@combine CleanerDetailState(null)
        val item = cleaner.items.firstOrNull { it.id == id }
            ?: related.firstOrNull { it.id == id }
        CleanerDetailState(
            item = item,
            related = related.filter { it.id != id },
            reason = item?.let { reasonFor(cleaner.section, it) }.orEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CleanerDetailState(null)
    )

    fun selectSection(value: CleanerSection) {
        section.value = value
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

    fun selectedUris(): List<Uri> {
        val selected = selectedIds.value
        return uiState.value.items
            .filter { it.id in selected }
            .map { Uri.parse(it.uri) }
    }

    fun openDetail(id: String) {
        detailId.value = id
        viewModelScope.launch {
            val relatedPhotos = repository.findRelatedDuplicates(id)
            relatedItems.value = relatedPhotos.map { photo ->
                CleanerItem(
                    id = photo.id,
                    uri = photo.uri,
                    title = if (photo.isStarredPick) "Best pick" else "Similar",
                    subtitle = "Quality ${photo.qualityScore.toInt()}",
                    starred = photo.isStarredPick
                )
            }
        }
    }

    fun closeDetail() {
        detailId.value = null
        relatedItems.value = emptyList()
    }

    private fun reasonFor(section: CleanerSection, item: CleanerItem): String = when (section) {
        CleanerSection.Duplicates ->
            "Looks nearly identical to other shots. ${if (item.starred) "This is the best keep candidate." else "A sharper twin is marked Keep."}"
        CleanerSection.Blurry ->
            "Looks soft or motion-blurred compared with nearby photos."
        CleanerSection.ExpiredScreenshots ->
            "Sensitive screenshot older than 7 days."
        CleanerSection.LongScreenshots ->
            "Very tall screenshot — usually a scrolling capture."
    }

    private data class Counts(
        val duplicates: Int,
        val blurry: Int,
        val expired: Int,
        val longCount: Int
    )

    private data class Lists(
        val duplicates: List<PhotoEntity>,
        val blurry: List<PhotoEntity>,
        val expired: List<PhotoEntity>,
        val longScreenshots: List<PhotoEntity>
    )

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = application as SkryApplication
                    return CleanerViewModel(app, app.mediaRepository) as T
                }
            }
    }
}

private fun PhotoEntity.toCleanerItem(
    title: String,
    subtitle: String,
    starred: Boolean = false
) = CleanerItem(
    id = id,
    uri = uri,
    title = title,
    subtitle = subtitle,
    starred = starred
)
