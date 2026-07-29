package com.yishenghuang.skry.ui.cleaner

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yishenghuang.skry.R
import com.yishenghuang.skry.ui.components.MonochromeTag
import com.yishenghuang.skry.ui.components.SkryCard
import com.yishenghuang.skry.ui.theme.AppDimensions
import com.yishenghuang.skry.ui.theme.SkryColors
import com.yishenghuang.skry.ui.theme.Typography

data class CleanerDetailState(
    val item: CleanerItem?,
    val related: List<CleanerItem> = emptyList(),
    val reason: String = ""
)

@Composable
fun CleanerDetailScreen(
    state: CleanerDetailState,
    onBack: () -> Unit,
    onOpenRelated: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val item = state.item
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SkryColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.spaceXs, vertical = AppDimensions.spaceXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = SkryColors.OnBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.clean_detail_title), style = Typography.titleLarge)
                Text(
                    stringResource(R.string.clean_detail_subtitle),
                    style = Typography.bodyMedium
                )
            }
        }

        if (item == null) {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.photo_unavailable), style = Typography.bodyMedium)
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimensions.spaceSm)
                .padding(bottom = AppDimensions.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceSm)
        ) {
            ZoomBox(uri = item.uri)
            MonochromeTag(text = item.title)
            Text(item.subtitle, style = Typography.bodyMedium)
            if (item.starred) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        tint = SkryColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(AppDimensions.spaceXs))
                    Text(
                        stringResource(R.string.clean_best_pick),
                        style = Typography.labelLarge,
                        color = SkryColors.Primary
                    )
                }
            }
            if (state.reason.isNotBlank()) {
                SkryCard(minHeight = AppDimensions.spaceXxl) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceXxxs)) {
                        Text(
                            stringResource(R.string.clean_why_here),
                            style = Typography.titleMedium
                        )
                        Text(state.reason, style = Typography.bodyMedium)
                    }
                }
            }
            if (state.related.isNotEmpty()) {
                Text(stringResource(R.string.clean_related), style = Typography.titleMedium)
                Text(
                    stringResource(R.string.clean_related_hint),
                    style = Typography.labelSmall
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.spaceSm),
                    contentPadding = PaddingValues(vertical = AppDimensions.spaceXs)
                ) {
                    items(state.related, key = { it.id }) { related ->
                        RelatedThumb(
                            item = related,
                            onClick = { onOpenRelated(related.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedThumb(item: CleanerItem, onClick: () -> Unit) {
    val context = LocalContext.current
    SkryCard(
        onClick = onClick,
        minHeight = AppDimensions.thumbSize,
        contentPadding = PaddingValues(AppDimensions.spaceXs),
        modifier = Modifier.width(120.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceXxxs)) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.uri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimensions.thumbSize)
                    .clip(RoundedCornerShape(AppDimensions.radiusThumb))
            )
            Text(item.title, style = Typography.labelSmall)
            if (item.starred) {
                Text(
                    stringResource(R.string.action_keep),
                    style = Typography.labelSmall,
                    color = SkryColors.Primary
                )
            }
        }
    }
}

@Composable
private fun ZoomBox(uri: String) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(AppDimensions.radiusCard)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun constrained(raw: Offset, currentScale: Float): Offset {
        if (currentScale <= 1f || containerSize == IntSize.Zero) return Offset.Zero
        val maxX = (containerSize.width * (currentScale - 1f)) / 2f
        val maxY = (containerSize.height * (currentScale - 1f)) / 2f
        return Offset(raw.x.coerceIn(-maxX, maxX), raw.y.coerceIn(-maxY, maxY))
    }

    val transformState = rememberTransformableState { zoom, pan, _ ->
        val next = (scale * zoom).coerceIn(1f, 5f)
        scale = next
        offset = constrained(offset + pan, next)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimensions.spaceXxl * 7)
            .clip(shape)
            .background(SkryColors.Surface, shape),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .onSizeChanged { containerSize = it }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    clip = true
                }
                .transformable(transformState)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        scale = 1f
                        offset = Offset.Zero
                    })
                }
        )
    }
}
