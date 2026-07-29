package com.yishenghuang.skry.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.yishenghuang.skry.ui.theme.SkryColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Slow drifting indigo orbs on deep black — splash and ambient brand backdrop.
 */
@Composable
fun SkryAnimatedBackdrop(
    modifier: Modifier = Modifier,
    intensity: Float = 1f
) {
    val transition = rememberInfiniteTransition(label = "skryBackdrop")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "backdropPhase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "backdropPulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(SkryColors.Background)

        val w = size.width
        val h = size.height
        val angle = t * Math.PI.toFloat() * 2f

        val orbs = listOf(
            Triple(
                Offset(
                    x = w * (0.28f + 0.08f * cos(angle)),
                    y = h * (0.32f + 0.06f * sin(angle * 0.9f))
                ),
                w * 0.42f * pulse,
                SkryColors.Primary.copy(alpha = 0.28f * intensity)
            ),
            Triple(
                Offset(
                    x = w * (0.72f + 0.07f * sin(angle * 1.1f)),
                    y = h * (0.58f + 0.05f * cos(angle))
                ),
                w * 0.36f * (2f - pulse),
                SkryColors.PrimaryVariant.copy(alpha = 0.22f * intensity)
            ),
            Triple(
                Offset(
                    x = w * (0.52f + 0.05f * cos(angle * 0.7f)),
                    y = h * (0.78f + 0.04f * sin(angle * 1.3f))
                ),
                w * 0.28f,
                Color(0xFF312E81).copy(alpha = 0.35f * intensity)
            )
        )

        orbs.forEach { (center, radius, color) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        // Soft vignette for depth without flat fill.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    SkryColors.Background.copy(alpha = 0.55f * intensity)
                )
            )
        )
    }
}
