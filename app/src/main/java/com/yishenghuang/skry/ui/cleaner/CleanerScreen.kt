package com.yishenghuang.skry.ui.cleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yishenghuang.skry.R
import com.yishenghuang.skry.ui.components.MonochromeTag
import com.yishenghuang.skry.ui.components.SkryCard
import com.yishenghuang.skry.ui.components.SkryEmptyState
import com.yishenghuang.skry.ui.components.SkryScreenHeader
import com.yishenghuang.skry.ui.theme.AppDimensions
import com.yishenghuang.skry.ui.theme.SkryColors
import com.yishenghuang.skry.ui.theme.SkryTheme
import com.yishenghuang.skry.ui.theme.Typography

@Composable
fun CleanerScreen(
    state: CleanerUiState,
    onSectionSelected: (CleanerSection) -> Unit = {},
    onOpen: (CleanerItem) -> Unit = {},
    onToggle: (String) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimensions.spaceSm)
    ) {
        SkryScreenHeader(
            title = stringResource(R.string.clean_title),
            subtitle = stringResource(R.string.clean_subtitle)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.spaceXs)
        ) {
            SectionChip(
                label = stringResource(R.string.clean_chip_duplicates, state.duplicateCount),
                selected = state.section == CleanerSection.Duplicates,
                onClick = { onSectionSelected(CleanerSection.Duplicates) }
            )
            SectionChip(
                label = stringResource(R.string.clean_chip_blurry, state.blurryCount),
                selected = state.section == CleanerSection.Blurry,
                onClick = { onSectionSelected(CleanerSection.Blurry) }
            )
            SectionChip(
                label = stringResource(R.string.clean_chip_expired, state.expiredCount),
                selected = state.section == CleanerSection.ExpiredScreenshots,
                onClick = { onSectionSelected(CleanerSection.ExpiredScreenshots) }
            )
            SectionChip(
                label = stringResource(R.string.clean_chip_long, state.longCount),
                selected = state.section == CleanerSection.LongScreenshots,
                onClick = { onSectionSelected(CleanerSection.LongScreenshots) }
            )
        }

        Spacer(modifier = Modifier.height(AppDimensions.spaceSm))

        if (state.items.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSelectAll) {
                    Text(stringResource(R.string.action_select_all), color = SkryColors.Primary)
                }
                if (state.selectedIds.isNotEmpty()) {
                    TextButton(onClick = onClearSelection) {
                        Text(stringResource(R.string.action_clear), color = SkryColors.Accent)
                    }
                }
            }
        }

        if (state.items.isEmpty()) {
            SkryEmptyState(
                title = stringResource(R.string.clean_empty_title),
                subtitle = stringResource(R.string.clean_empty_subtitle),
                icon = Icons.Outlined.AutoAwesome,
                modifier = Modifier.padding(top = AppDimensions.spaceMd)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = AppDimensions.spaceSm),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceSm)
            ) {
                items(state.items, key = { it.id }) { item ->
                    CleanerRow(
                        item = item,
                        selected = item.id in state.selectedIds,
                        onOpen = { onOpen(item) },
                        onToggle = { onToggle(item.id) }
                    )
                }
            }

            if (state.selectedIds.isNotEmpty()) {
                Button(
                    onClick = onDeleteSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppDimensions.spaceSm),
                    shape = RoundedCornerShape(AppDimensions.radiusButton),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkryColors.Risk,
                        contentColor = SkryColors.OnBackground
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.spaceSm)
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.spaceXs))
                    Text(stringResource(R.string.clean_delete_n, state.selectedIds.size))
                }
            }
        }
    }
}

@Composable
private fun SectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
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
private fun CleanerRow(
    item: CleanerItem,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(AppDimensions.radiusCard)
    SkryCard(
        onClick = onOpen,
        minHeight = AppDimensions.thumbSize,
        contentPadding = PaddingValues(AppDimensions.spaceSm),
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
                        contentDescription = stringResource(R.string.clean_selected),
                        tint = SkryColors.Primary,
                        modifier = Modifier.size(AppDimensions.spaceSm)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AppDimensions.spaceSm))
            Box(
                modifier = Modifier
                    .size(AppDimensions.thumbSize)
                    .clip(RoundedCornerShape(AppDimensions.radiusThumb))
                    .background(SkryColors.SurfaceLifted)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (item.starred) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = stringResource(R.string.action_keep),
                        tint = SkryColors.Primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(AppDimensions.spaceXxxs)
                            .size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AppDimensions.spaceSm))
            Column(modifier = Modifier.weight(1f)) {
                MonochromeTag(text = item.title)
                Spacer(modifier = Modifier.height(AppDimensions.spaceXs))
                Text(text = item.subtitle, style = Typography.bodyMedium)
                Text(
                    text = if (selected) {
                        stringResource(R.string.clean_selected)
                    } else {
                        stringResource(R.string.clean_open_detail)
                    },
                    style = Typography.labelSmall,
                    color = if (selected) SkryColors.Risk else SkryColors.Accent
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun CleanerPreview() {
    SkryTheme {
        CleanerScreen(
            state = CleanerUiState(
                duplicateCount = 12,
                blurryCount = 4,
                items = listOf(
                    CleanerItem("1", "", "Duplicate", "Keep starred pick", true),
                    CleanerItem("2", "", "Duplicate", "Safe to remove", false)
                ),
                selectedIds = setOf("2")
            )
        )
    }
}
