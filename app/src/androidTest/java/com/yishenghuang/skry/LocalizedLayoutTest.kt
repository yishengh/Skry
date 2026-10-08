package com.yishenghuang.skry

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.yishenghuang.skry.ui.dashboard.DashboardScreen
import com.yishenghuang.skry.ui.dashboard.DashboardViewState
import com.yishenghuang.skry.ui.theme.SkryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class LocalizedLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun chineseLargeTextKeepsPermissionActionsReachable() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList(Locale.SIMPLIFIED_CHINESE)); fontScale = 2f
        }
        val localized = base.createConfigurationContext(configuration)
        var requested = 0
        var settings = 0
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(base.resources.displayMetrics.density, 2f)
            ) {
                SkryTheme {
                    DashboardScreen(DashboardViewState(),
                        onRequestPermission = { requested++ }, onOpenSettings = { settings++ })
                }
            }
        }
        compose.onNodeWithText(localized.getString(R.string.home_grant_access)).performScrollTo().performClick()
        compose.onNodeWithText(localized.getString(R.string.home_open_settings)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, requested); assertEquals(1, settings) }
    }
}
