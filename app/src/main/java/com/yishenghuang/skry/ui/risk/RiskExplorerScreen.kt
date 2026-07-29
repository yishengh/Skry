package com.yishenghuang.skry.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HighlightOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yishenghuang.skry.data.UserReviewStatus
import com.yishenghuang.skry.domain.DetectableCategories
import com.yishenghuang.skry.domain.Finding
import com.yishenghuang.skry.domain.FindingType
import com.yishenghuang.skry.ui.components.MonochromeTag
import com.yishenghuang.skry.ui.components.SkryCard
import com.yishenghuang.skry.ui.components.SkryEmptyState
import com.yishenghuang.skry.ui.components.SkryScreenHeader
import com.yishenghuang.skry.ui.theme.AppDimensions
import com.yishenghuang.skry.ui.theme.SkryColors
import com.yishenghuang.skry.ui.theme.SkryTheme
import com.yishenghuang.skry.ui.theme.Typography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskExplorerScreen(
    state: RiskUiState,
    onOpen: (RiskItem) -> Unit = {},
    onToggle: (String) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onFilterChange: (RiskListFilter) -> Unit = {},
    onCategoryChange: (FindingType?) -> Unit = {},
    onBatchConfirm: () -> Unit = {},
    onBatchClear: () -> Unit = {},
    onBatchRestore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    val categoryLabel = state.categories
        .firstOrNull { it.type == state.categoryFilter }
        ?.title
        ?: "All categories"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimensions.spaceSm)
    ) {
        SkryScreenHeader(
            title = "Risk Explorer",
            subtitle = "Select to batch-review · tap a photo to open"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.spaceXs)
        ) {
            RiskFilterChip("Review", state.filter == RiskListFilter.NeedsReview) {
                onFilterChange(RiskListFilter.NeedsReview)
            }
            RiskFilterChip("Confirmed", state.filter == RiskListFilter.Confirmed) {
                onFilterChange(RiskListFilter.Confirmed)
            }
            RiskFilterChip("Cleared", state.filter == RiskListFilter.Cleared) {
                onFilterChange(RiskListFilter.Cleared)
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.spaceSm))

        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it }
        ) {
            OutlinedTextField(
                value = categoryLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SkryColors.Primary,
                    unfocusedBorderColor = SkryColors.Hairline,
                    focusedTextColor = SkryColors.OnBackground,
                    unfocusedTextColor = SkryColors.OnBackground,
                    focusedLabelColor = SkryColors.Accent,
                    unfocusedLabelColor = SkryColors.Accent
                ),
                shape = RoundedCornerShape(AppDimensions.radiusButton)
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
                containerColor = SkryColors.Surface
            ) {
                DropdownMenuItem(
                    text = { Text("All categories", color = SkryColors.OnBackground) },
                    onClick = {
                        onCategoryChange(null)
                        categoryExpanded = false
                    }
                )
                state.categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(category.title, color = SkryColors.OnBackground)
                                Text(category.description, style = Typography.labelSmall)
                            }
                        },
                        onClick = {
                            onCategoryChange(category.type)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        if (state.items.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimensions.spaceSm),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onSelectAll) {
                    Text("Select all", color = SkryColors.Primary)
                }
                if (state.selectedIds.isNotEmpty()) {
                    TextButton(onClick = onClearSelection) {
                        Text("Clear selection", color = SkryColors.Accent)
                    }
                }
            }
        }

        if (state.items.isEmpty()) {
            SkryEmptyState(
                title = when (state.filter) {
                    RiskListFilter.NeedsReview -> "Nothing to review"
                    RiskListFilter.Confirmed -> "No confirmed risks"
                    RiskListFilter.Cleared -> "No cleared items"
                },
                subtitle = "Scan from Home, or switch filters to find reviewed items.",
                icon = Icons.Outlined.VerifiedUser,
                modifier = Modifier.padding(top = AppDimensions.spaceMd)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = AppDimensions.spaceSm),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceSm)
            ) {
                items(state.items, key = { it.id }) { item ->
                    RiskRowCard(
                        item = item,
                        selected = item.id in state.selectedIds,
                        onOpen = { onOpen(item) },
                        onToggle = { onToggle(item.id) }
                    )
                }
            }

            if (state.selectedIds.isNotEmpty()) {
                BatchBar(
                    filter = state.filter,
                    count = state.selectedIds.size,
                    onConfirm = onBatchConfirm,
                    onClear = onBatchClear,
                    onRestore = onBatchRestore
                )
            }
        }
    }
}

@Composable
private fun BatchBar(
    filter: RiskListFilter,
    count: Int,
    onConfirm: () -> Unit,
    onClear: () -> Unit,
    onRestore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimensions.spaceSm),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceXs)
    ) {
        Text("$count selected", style = Typography.labelSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.spaceSm)) {
            when (filter) {
                RiskListFilter.Cleared -> {
                    Button(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(AppDimensions.radiusButton),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkryColors.Primary,
                            contentColor = SkryColors.OnBackground
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Undo, null, Modifier.size(AppDimensions.spaceSm))
                        Spacer(Modifier.width(AppDimensions.spaceXs))
                        Text("Restore")
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(AppDimensions.radiusButton),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SkryColors.Accent)
                    ) {
                        Icon(Icons.Outlined.HighlightOff, null, Modifier.size(AppDimensions.spaceSm))
                        Spacer(Modifier.width(AppDimensions.spaceXs))
                        Text("Not a leak")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(AppDimensions.radiusButton),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkryColors.Risk,
                            contentColor = SkryColors.OnBackground
                        )
                    ) {
                        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(AppDimensions.spaceSm))
                        Spacer(Modifier.width(AppDimensions.spaceXs))
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SkryColors.Surface,
            labelColor = SkryColors.Accent,
            selectedContainerColor = SkryColors.SurfaceLifted,
            selectedLabelColor = SkryColors.Primary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = SkryColors.Hairline,
            selectedBorderColor = SkryColors.Primary
        )
    )
}

@Composable
private fun RiskRowCard(
    item: RiskItem,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit
) {
    val shape = RoundedCornerShape(AppDimensions.radiusCard)
    SkryCard(
        contentPadding = PaddingValues(AppDimensions.spaceSm),
        minHeight = AppDimensions.thumbSize,
        onClick = onOpen,
        modifier = if (selected) {
            Modifier.border(AppDimensions.hairline, SkryColors.Primary, shape)
        } else {
            Modifier
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(AppDimensions.spaceLg)
                    .clip(RoundedCornerShape(AppDimensions.radiusTag))
                    .background(
                        if (selected) SkryColors.Primary.copy(alpha = 0.3f) else SkryColors.SurfaceLifted
                    )
                    .border(
                        AppDimensions.hairline,
                        if (selected) SkryColors.Primary else SkryColors.Hairline,
                        RoundedCornerShape(AppDimensions.radiusTag)
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Selected",
                        tint = SkryColors.Primary,
                        modifier = Modifier.size(AppDimensions.spaceSm)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AppDimensions.spaceSm))
            RiskThumbnail(uri = item.uri, hasSensitiveRegion = item.hasSensitiveRegion)
            Spacer(modifier = Modifier.width(AppDimensions.spaceSm))
            Column(modifier = Modifier.weight(1f)) {
                MonochromeTag(text = item.typeLabel)
                Spacer(modifier = Modifier.height(AppDimensions.spaceXs))
                Text(text = item.subtitle, style = Typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun RiskThumbnail(
    uri: String,
    hasSensitiveRegion: Boolean
) {
    val shape = RoundedCornerShape(AppDimensions.radiusThumb)
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(AppDimensions.thumbSize)
            .clip(shape)
            .background(SkryColors.SurfaceLifted)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (hasSensitiveRegion) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .blur(12.dp)
                    .background(SkryColors.OnBackground.copy(alpha = 0.35f))
            )
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = SkryColors.Primary.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AppDimensions.spaceXxxs)
                    .size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun RiskExplorerPreview() {
    SkryTheme {
        RiskExplorerScreen(
            state = RiskUiState(
                items = listOf(
                    RiskItem(
                        id = "1",
                        uri = "",
                        typeLabel = "Passport",
                        subtitle = "MRZ line detected",
                        hasSensitiveRegion = true,
                        userReview = UserReviewStatus.NONE,
                        findings = listOf(
                            Finding(FindingType.PASSPORT, "Passport", 0.9f, "MRZ")
                        )
                    )
                ),
                categories = DetectableCategories.all,
                selectedIds = setOf("1")
            )
        )
    }
}
