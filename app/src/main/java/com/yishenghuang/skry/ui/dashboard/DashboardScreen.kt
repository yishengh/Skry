package com.yishenghuang.skry.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.yishenghuang.skry.R
import com.yishenghuang.skry.ui.components.HealthProgressRing
import com.yishenghuang.skry.ui.components.SkryAnimatedBackdrop
import com.yishenghuang.skry.ui.components.SkryCard
import com.yishenghuang.skry.ui.components.SkryScreenHeader
import com.yishenghuang.skry.ui.theme.AppDimensions
import com.yishenghuang.skry.ui.theme.SkryColors
import com.yishenghuang.skry.ui.theme.SkryTheme
import com.yishenghuang.skry.ui.theme.Typography

@Composable
fun DashboardScreen(
    state: DashboardViewState,
    onPrivacyClick: () -> Unit = {},
    onDuplicatesClick: () -> Unit = {},
    onBlurryClick: () -> Unit = {},
    onRequestPermission: () -> Unit = {},
    onScanNow: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        SkryAnimatedBackdrop(intensity = 0.35f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimensions.spaceSm, vertical = AppDimensions.spaceMd),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceMd)
        ) {
            SkryScreenHeader(
                title = stringResource(R.string.home_title),
                subtitle = stringResource(R.string.home_subtitle)
            )

            val ringAction = when {
                !state.hasPermission -> null
                state.isScanning -> stringResource(R.string.home_scanning)
                state.pendingCount > 0 -> stringResource(R.string.home_scan_resume)
                else -> stringResource(R.string.home_scan_now)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimensions.spaceSm),
                contentAlignment = Alignment.Center
            ) {
                HealthProgressRing(
                    progress = state.healthScore,
                    label = stringResource(R.string.home_health_label),
                    actionLabel = ringAction,
                    enabled = state.hasPermission && !state.isScanning,
                    onClick = if (state.hasPermission) onScanNow else null
                )
            }

            Text(
                text = if (state.hasPermission) {
                    stringResource(
                        R.string.home_stats_ready,
                        state.libraryCount,
                        state.auditedCount
                    )
                } else {
                    stringResource(R.string.home_stats_need_permission)
                },
                style = Typography.labelSmall,
                color = SkryColors.Accent,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.isScanning || state.pendingCount > 0) {
                SkryCard(minHeight = AppDimensions.spaceXxl) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceXs)) {
                        Text(
                            text = if (state.isScanning) {
                                stringResource(R.string.home_scan_in_progress)
                            } else {
                                stringResource(R.string.home_scan_paused)
                            },
                            style = Typography.titleMedium
                        )
                        LinearProgressIndicator(
                            progress = { state.scanProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppDimensions.spaceXs),
                            color = SkryColors.Primary,
                            trackColor = SkryColors.ProgressTrack,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = stringResource(
                                R.string.home_scan_progress_detail,
                                state.auditedCount,
                                state.pendingCount
                            ),
                            style = Typography.labelSmall
                        )
                    }
                }
            }

            state.lastScanMessage?.let { message ->
                Text(text = message, style = Typography.bodyMedium)
            }

            if (!state.hasPermission) {
                SkryCard(minHeight = AppDimensions.bentoMinHeight) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceSm)) {
                        Text(
                            stringResource(R.string.home_allow_access_title),
                            style = Typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.home_allow_access_body),
                            style = Typography.bodyMedium
                        )
                        Button(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(AppDimensions.radiusButton),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SkryColors.Primary,
                                contentColor = SkryColors.OnBackground
                            )
                        ) {
                            Text(stringResource(R.string.home_grant_access))
                        }
                    }
                }
            }

            SkryCard(
                onClick = onPrivacyClick,
                minHeight = AppDimensions.bentoLargeMinHeight,
                modifier = Modifier.heightIn(min = AppDimensions.bentoLargeMinHeight)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.spaceXs)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = SkryColors.Primary
                        )
                        Text(
                            text = stringResource(R.string.home_privacy_risks),
                            style = Typography.titleLarge
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.home_needs_review),
                            style = Typography.labelSmall,
                            color = SkryColors.Accent
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.spaceXxxs))
                        Text(
                            text = state.highRiskCount.toString(),
                            style = Typography.displayLarge,
                            color = SkryColors.Risk
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.spaceSm)
            ) {
                MetricMiniCard(
                    title = stringResource(R.string.home_duplicates),
                    count = state.duplicateCount,
                    icon = Icons.Outlined.ContentCopy,
                    onClick = onDuplicatesClick,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    title = stringResource(R.string.home_blurry),
                    count = state.blurryCount,
                    icon = Icons.Outlined.BlurOn,
                    onClick = onBlurryClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    count: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SkryCard(
        onClick = onClick,
        modifier = modifier.heightIn(min = AppDimensions.bentoMinHeight)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SkryColors.Accent
            )
            Column {
                Text(text = title, style = Typography.labelSmall)
                Spacer(modifier = Modifier.height(AppDimensions.spaceXxxs))
                Text(text = count.toString(), style = Typography.headlineMedium)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun DashboardPreview() {
    SkryTheme {
        DashboardScreen(
            state = DashboardViewState(
                healthScore = 0.72f,
                highRiskCount = 12,
                duplicateCount = 48,
                blurryCount = 23,
                libraryCount = 1280,
                auditedCount = 900,
                pendingCount = 380,
                scanProgress = 0.7f,
                isScanning = false,
                hasPermission = true,
                lastScanMessage = "Tap the ring to resume"
            )
        )
    }
}
