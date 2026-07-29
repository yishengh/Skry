package com.yishenghuang.skry.ui.risk

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HighlightOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yishenghuang.skry.data.UserReviewStatus
import com.yishenghuang.skry.domain.Finding
import com.yishenghuang.skry.domain.FindingType
import com.yishenghuang.skry.ui.components.MonochromeTag
import com.yishenghuang.skry.ui.components.SkryCard
import com.yishenghuang.skry.ui.theme.AppDimensions
import com.yishenghuang.skry.ui.theme.SkryColors
import com.yishenghuang.skry.ui.theme.SkryTheme
import com.yishenghuang.skry.ui.theme.Typography

@Composable
fun RiskDetailScreen(
    item: RiskItem?,
    onBack: () -> Unit,
    onConfirmLeak: () -> Unit,
    onDismiss: () -> Unit,
    onRestore: () -> Unit = {},
    onMoveToVault: () -> Unit = {},
    vaultBusy: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SkryColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppDimensions.spaceXs,
                    vertical = AppDimensions.spaceXs
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = SkryColors.OnBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Review risk", style = Typography.titleLarge)
                Text(
                    text = "Pinch to zoom · confirm if this is a real leak",
                    style = Typography.bodyMedium
                )
            }
        }

        if (item == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Photo unavailable", style = Typography.bodyMedium)
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
            ZoomablePhoto(
                uri = item.uri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimensions.spaceXxl * 7)
            )

            when (item.userReview) {
                UserReviewStatus.CONFIRMED_LEAK ->
                    Text(
                        text = "Marked as confirmed leak",
                        style = Typography.labelLarge,
                        color = SkryColors.Risk
                    )
                UserReviewStatus.DISMISSED ->
                    Text(
                        text = "Marked as not a leak",
                        style = Typography.labelLarge,
                        color = SkryColors.Accent
                    )
                UserReviewStatus.NONE ->
                    Text(
                        text = "Awaiting your confirmation",
                        style = Typography.labelLarge,
                        color = SkryColors.Primary
                    )
            }

            Text("Detected on this photo", style = Typography.titleMedium)
            if (item.findings.isEmpty()) {
                Text("No structured findings", style = Typography.bodyMedium)
            } else {
                item.findings.forEach { finding ->
                    FindingRow(finding)
                }
            }

            ReviewActions(
                review = item.userReview,
                onConfirmLeak = onConfirmLeak,
                onDismiss = onDismiss,
                onRestore = onRestore
            )

            OutlinedButton(
                onClick = onMoveToVault,
                enabled = !vaultBusy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.radiusButton),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SkryColors.Primary)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimensions.spaceSm)
                )
                Spacer(Modifier.width(AppDimensions.spaceXs))
                Text(if (vaultBusy) "Saving to vault…" else "Move to Vault")
            }
        }
    }
}

@Composable
private fun ReviewActions(
    review: UserReviewStatus,
    onConfirmLeak: () -> Unit,
    onDismiss: () -> Unit,
    onRestore: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.spaceSm)
    ) {
        when (review) {
            UserReviewStatus.NONE -> {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppDimensions.radiusButton),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SkryColors.Accent)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HighlightOff,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.spaceSm)
                    )
                    Spacer(Modifier.width(AppDimensions.spaceXs))
                    Text("Not a leak")
                }
                Button(
                    onClick = onConfirmLeak,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppDimensions.radiusButton),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkryColors.Risk,
                        contentColor = SkryColors.OnBackground
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.spaceSm)
                    )
                    Spacer(Modifier.width(AppDimensions.spaceXs))
                    Text("Confirm leak")
                }
            }
            UserReviewStatus.CONFIRMED_LEAK -> {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppDimensions.radiusButton),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SkryColors.Accent)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HighlightOff,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.spaceSm)
                    )
                    Spacer(Modifier.width(AppDimensions.spaceXs))
                    Text("Not a leak")
                }
                Button(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppDimensions.radiusButton),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkryColors.Primary,
                        contentColor = SkryColors.OnBackground
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.spaceSm)
                    )
                    Spacer(Modifier.width(AppDimensions.spaceXs))
                    Text("Restore")
                }
            }
            UserReviewStatus.DISMISSED -> {
                OutlinedButton(
                    onClick = onConfirmLeak,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppDimensions.radiusButton),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SkryColors.Risk)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.spaceSm)
                    )
                    Spacer(Modifier.width(AppDimensions.spaceXs))
                    Text("Confirm leak")
                }
                Button(
                    onClick = onRestore,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppDimensions.radiusButton),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkryColors.Primary,
                        contentColor = SkryColors.OnBackground
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.spaceSm)
                    )
                    Spacer(Modifier.width(AppDimensions.spaceXs))
                    Text("Restore")
                }
            }
        }
    }
}

@Composable
private fun FindingRow(finding: Finding) {
    SkryCard(
        minHeight = AppDimensions.spaceXxl,
        contentPadding = PaddingValues(AppDimensions.spaceSm)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceXxxs)) {
            MonochromeTag(text = finding.label)
            Text(
                text = finding.snippet ?: "Detected by on-device rules",
                style = Typography.bodyMedium
            )
            Text(
                text = "Confidence ${(finding.confidence * 100).toInt()}%",
                style = Typography.labelSmall
            )
        }
    }
}

@Composable
private fun ZoomablePhoto(
    uri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(AppDimensions.radiusCard)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun constrainedOffset(raw: Offset, currentScale: Float): Offset {
        if (currentScale <= 1f || containerSize == IntSize.Zero) return Offset.Zero
        val maxX = (containerSize.width * (currentScale - 1f)) / 2f
        val maxY = (containerSize.height * (currentScale - 1f)) / 2f
        return Offset(
            x = raw.x.coerceIn(-maxX, maxX),
            y = raw.y.coerceIn(-maxY, maxY)
        )
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = nextScale
        offset = constrainedOffset(offset + panChange, nextScale)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(SkryColors.Surface, shape),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = "Risk photo",
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
                .transformable(state = transformState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    )
                }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun RiskDetailPreview() {
    SkryTheme {
        RiskDetailScreen(
            item = RiskItem(
                id = "1",
                uri = "",
                typeLabel = "Credit Card",
                subtitle = "Card-like number",
                hasSensitiveRegion = true,
                userReview = UserReviewStatus.NONE,
                findings = listOf(
                    Finding(
                        type = FindingType.CREDIT_CARD,
                        label = "Credit Card",
                        confidence = 0.88f,
                        snippet = "Card-like 4111"
                    )
                )
            ),
            onBack = {},
            onConfirmLeak = {},
            onDismiss = {}
        )
    }
}
