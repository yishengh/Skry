package com.yishenghuang.skry.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp

fun Modifier.hairlineBorder(
    shape: Shape = RoundedCornerShape(AppDimensions.radiusCard),
    color: Color = SkryColors.Hairline,
    width: Dp = AppDimensions.hairline
): Modifier = this.then(
    Modifier.border(width = width, color = color, shape = shape)
)

fun Modifier.pressScale(
    pressedScale: Float = 0.98f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    this
        .scale(scale)
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    try {
                        awaitRelease()
                    } finally {
                        pressed = false
                    }
                },
                onTap = { onClick?.invoke() }
            )
        }
}

fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(AppDimensions.radiusCard)
): Modifier = this
    .clip(shape)
    .hairlineBorder(shape = shape)
