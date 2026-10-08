package com.yishenghuang.skry

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yishenghuang.skry.util.MediaAccess
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Host changes real runtime grants before each run; revoking a grant kills the app by design. */
@RunWith(AndroidJUnit4::class)
class PermissionUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun realPermissionStateIsReflectedInHome() {
        val args = InstrumentationRegistry.getArguments()
        val expected = args.getString("galleryPermission")
        assumeTrue("Run with an explicit permission matrix case", expected != null)
        check(args.getString("skrySyntheticOnly") == "true")
        check(Build.HARDWARE in setOf("ranchu", "goldfish"))
        val context = compose.activity.applicationContext
        when (expected) {
            "denied" -> {
                assertFalse(MediaAccess.hasGalleryAccess(context))
                compose.onNodeWithText(context.getString(R.string.home_allow_access_title))
                    .performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(context.getString(R.string.home_open_settings))
                    .performScrollTo().assertIsDisplayed()
            }
            "partial" -> {
                assertTrue(MediaAccess.hasGalleryAccess(context))
                assertFalse(MediaAccess.hasFullGalleryAccess(context))
                compose.onNodeWithText(context.getString(R.string.home_partial_access))
                    .performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(context.getString(R.string.home_choose_photos))
                    .performScrollTo().assertIsDisplayed()
            }
            "full" -> {
                assertTrue(MediaAccess.hasFullGalleryAccess(context))
                compose.onNodeWithText(context.getString(R.string.home_scan_now)).assertIsDisplayed()
            }
            else -> error("Unknown test case")
        }
    }
}
