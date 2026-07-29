package com.yishenghuang.skry.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.yishenghuang.skry.ui.theme.AppDimensions
import com.yishenghuang.skry.ui.theme.SkryColors
import com.yishenghuang.skry.ui.theme.glassSurface
import com.yishenghuang.skry.ui.theme.Typography as SkryTypography

@Composable
fun SkryCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(AppDimensions.spaceSm),
    minHeight: Dp = AppDimensions.bentoMinHeight,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(AppDimensions.radiusCard)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "skryCardScale"
    )
    val surfaceColor = if (pressed) SkryColors.SurfaceLifted else SkryColors.Surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .scale(scale)
            .glassSurface(shape)
            .background(surfaceColor, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun MonochromeTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = SkryColors.TagBackground,
                shape = RoundedCornerShape(AppDimensions.radiusTag)
            )
            .padding(horizontal = AppDimensions.spaceXs, vertical = AppDimensions.spaceXxxs)
    ) {
        Text(
            text = text,
            style = SkryTypography.labelMedium,
            color = SkryColors.TagForeground
        )
    }
}

@Composable
fun SkryEmptyState(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppDimensions.spaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceSm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SkryColors.Accent.copy(alpha = 0.45f),
            modifier = Modifier.size(AppDimensions.spaceXxl)
        )
        Text(
            text = title,
            style = SkryTypography.titleLarge,
            color = SkryColors.OnSurfaceMuted,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = SkryTypography.bodyMedium,
            color = SkryColors.Accent.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HealthProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String,
    actionLabel: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val clamped = progress.coerceIn(0f, 1f)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "healthRingScale"
    )

    Box(
        modifier = modifier
            .size(AppDimensions.progressSize)
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = AppDimensions.progressStroke.toPx()
            drawArc(
                color = SkryColors.ProgressTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
            drawArc(
                brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                    listOf(SkryColors.Primary, SkryColors.PrimaryVariant, SkryColors.Primary)
                ),
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = stroke,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(clamped * 100).toInt()}",
                style = SkryTypography.displayLarge,
                color = SkryColors.OnBackground
            )
            Text(
                text = label,
                style = SkryTypography.labelSmall,
                color = SkryColors.Accent
            )
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(AppDimensions.spaceXxxs))
                Text(
                    text = actionLabel,
                    style = SkryTypography.labelMedium,
                    color = if (enabled) SkryColors.Primary else SkryColors.Accent
                )
            }
        }
    }
}
