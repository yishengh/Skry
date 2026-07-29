package com.yishenghuang.skry.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yishenghuang.skry.R
import com.yishenghuang.skry.ui.components.SkryAnimatedBackdrop
import com.yishenghuang.skry.ui.theme.AppDimensions
import com.yishenghuang.skry.ui.theme.SkryColors
import com.yishenghuang.skry.ui.theme.Typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SkrySplashGate(
    content: @Composable () -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }
    val brandAlpha = remember { Animatable(0f) }
    val ringProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            brandAlpha.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
        launch {
            ringProgress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        }
        delay(1650)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(tween(0)),
            exit = fadeOut(tween(420, easing = FastOutSlowInEasing))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                SkryAnimatedBackdrop(intensity = 1f)
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .alpha(brandAlpha.value),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SplashMark(progress = ringProgress.value)
                    Spacer(modifier = Modifier.height(AppDimensions.spaceMd))
                    Text(
                        text = "Skry",
                        style = Typography.displayLarge,
                        color = SkryColors.OnBackground
                    )
                    Spacer(modifier = Modifier.height(AppDimensions.spaceXs))
                    Text(
                        text = stringResource(R.string.splash_tagline),
                        style = Typography.bodyMedium,
                        color = SkryColors.Accent
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashMark(progress: Float) {
    Canvas(modifier = Modifier.size(88.dp)) {
        val stroke = 3.dp.toPx()
        val pad = stroke
        drawArc(
            color = SkryColors.ProgressTrack,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(pad, pad),
            size = Size(size.width - pad * 2, size.height - pad * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = SkryColors.Primary,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(pad, pad),
            size = Size(size.width - pad * 2, size.height - pad * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        // Inner shield-like diamond hint
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.18f
        drawCircle(
            color = SkryColors.Primary.copy(alpha = 0.9f * progress),
            radius = r * 0.35f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = SkryColors.PrimaryVariant.copy(alpha = 0.45f * progress),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = stroke * 0.7f)
        )
    }
}
