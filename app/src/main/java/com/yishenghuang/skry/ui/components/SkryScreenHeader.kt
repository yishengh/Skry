package com.yishenghuang.skry.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yishenghuang.skry.ui.theme.AppDimensions
import com.yishenghuang.skry.ui.theme.Typography

/**
 * Shared page header for visual + copy consistency across tabs.
 */
@Composable
fun SkryScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AppDimensions.spaceMd, bottom = AppDimensions.spaceSm),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceXxxs)
        ) {
            Text(text = title, style = Typography.headlineLarge)
            Text(text = subtitle, style = Typography.bodyMedium)
        }
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = trailing
            )
        }
    }
}
