package com.yishenghuang.skry

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yishenghuang.skry.ui.GalleryDeleteViewModel
import com.yishenghuang.skry.util.MediaAccess
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real app dialog and system consent; only deletes a URI created inside this test. */
@RunWith(AndroidJUnit4::class)
class DeleteFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun cancelRotateThenDeleteSyntheticPhoto() {
        check(InstrumentationRegistry.getArguments().getString("skrySyntheticOnly") == "true")
        check(Build.HARDWARE in setOf("ranchu", "goldfish"))
        val context = compose.activity.applicationContext
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "skry_synthetic_delete_${System.nanoTime()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SkrySynthetic")
        }
        val uri = resolver.insert(MediaAccess.imagesCollectionUri(), values)!!
        val image = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        resolver.openOutputStream(uri)!!.use { image.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        image.recycle()
        if (Build.VERSION.SDK_INT >= 30) {
            // Surface platform/provider errors instead of hiding them behind the app's generic error UI.
            assertNotNull(MediaStore.createDeleteRequest(resolver, listOf(uri)).intentSender)
        }
        lateinit var model: GalleryDeleteViewModel
        compose.activityRule.scenario.onActivity { model = ViewModelProvider(it)[GalleryDeleteViewModel::class.java] }
        try {
            compose.runOnIdle { model.request(listOf(uri)) }
            compose.onNodeWithText(context.getString(R.string.action_cancel)).performClick()
            compose.runOnIdle { assertFalse(model.busy.value); assertTrue(model.confirmation.value.isEmpty()) }
            assertTrue(exists(uri))

            compose.runOnIdle {
                model.request(listOf(uri))
                model.request(listOf(Uri.parse("content://must-not-replace/1")))
                assertEquals(listOf(uri.toString()), model.confirmation.value)
            }
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText(context.getString(R.string.action_delete)).performClick()
            if (Build.VERSION.SDK_INT >= 30) {
                compose.runOnIdle { assertTrue("No pending system request; message=${model.message.value}, exists=${exists(uri)}", model.busy.value) }
                clickSystemButton(setOf("Cancel", "Don't allow", "Deny"))
                compose.waitUntil(15_000) { !model.busy.value }
                assertTrue("System cancellation must retain the original", exists(uri))
                compose.waitUntil(30_000) { compose.activity.hasWindowFocus() }
                compose.runOnIdle { model.request(listOf(uri)) }
                compose.onNodeWithText(context.getString(R.string.action_delete))
                    .performSemanticsAction(SemanticsActions.OnClick) { it() }
                compose.waitUntil(15_000) { model.confirmation.value.isEmpty() }
                // Drain Compose recomposition/effects before polling the separate system window.
                // Reading StateFlow alone does not advance the Compose test clock.
                compose.waitForIdle()
                clickSystemButton(setOf("Delete", "Allow"))
            }
            compose.waitUntil(20_000) { !model.busy.value }
            assertFalse("Actual MediaStore deletion must occur", exists(uri))
        } finally {
            if (exists(uri)) resolver.delete(uri, null, null)
        }
    }

    private fun exists(uri: Uri): Boolean =
        compose.activity.contentResolver.query(uri, arrayOf(MediaStore.Images.Media._ID), null, null, null)
            ?.use { it.moveToFirst() } == true

    private fun clickSystemButton(labels: Set<String>) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val deadline = android.os.SystemClock.uptimeMillis() + 30_000
        var lastWindow = "none"
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            val root = automation.rootInActiveWindow
            if (root != null) lastWindow = "${root.packageName}: ${texts(root)}"
            if (root != null && root.packageName?.toString() != "com.yishenghuang.skry") {
                val node = find(root, labels)
                if (node != null) {
                    var target: AccessibilityNodeInfo? = node
                    while (target != null && !target.isClickable) target = target.parent
                    if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return
                }
            }
            android.os.SystemClock.sleep(100)
        }
        error("Expected system consent button was not found: $labels; last window=$lastWindow")
    }

    private fun texts(node: AccessibilityNodeInfo): List<String> = buildList {
        node.text?.let { add(it.toString()) }
        for (index in 0 until node.childCount) node.getChild(index)?.let { addAll(texts(it)) }
    }

    private fun find(node: AccessibilityNodeInfo, labels: Set<String>): AccessibilityNodeInfo? {
        if (node.text?.toString() in labels) return node
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { find(it, labels)?.let { match -> return match } }
        }
        return null
    }
}
