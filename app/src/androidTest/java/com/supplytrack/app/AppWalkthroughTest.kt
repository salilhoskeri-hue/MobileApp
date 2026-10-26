package com.supplytrack.app

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end walkthrough of the app on a fresh install (with its sample data).
 * Checks the main flows and saves a screenshot after each step to
 * files/screenshots in the app's private storage, where CI collects them.
 */
@RunWith(AndroidJUnit4::class)
class AppWalkthroughTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private var shotCount = 0

    @Test
    fun walkThroughMainFlows() {
        // Dashboard: sample data is seeded in the background on first launch.
        waitForText("Hex nuts M8")
        compose.onNodeWithText("Reorder alerts").assertIsDisplayed()
        screenshot("dashboard")

        // Inventory list and search.
        compose.onNodeWithTag("nav_inventory").performClick()
        waitForText("Bubble wrap roll")
        screenshot("inventory")
        compose.onNodeWithTag("inventory_search").performTextInput("bolt")
        waitUntilGone("Bubble wrap roll")
        compose.onNodeWithText("Steel bolts M8").assertIsDisplayed()
        closeSoftKeyboard()
        screenshot("inventory_search")
        compose.onNodeWithTag("inventory_search").performTextClearance()
        closeSoftKeyboard()

        // Product detail: receiving stock updates the count and the history.
        waitForText("Controller board v2")
        compose.onNodeWithText("Controller board v2").performClick()
        waitForText("55 pcs")
        screenshot("product_detail")
        compose.onNodeWithText("Receive").performClick()
        compose.onNodeWithTag("adjust_qty").performTextInput("5")
        compose.onNodeWithTag("adjust_confirm").performClick()
        waitForText("60 pcs")
        compose.onNodeWithText("+5").assertExists()
        closeSoftKeyboard()
        screenshot("product_after_receiving")
        compose.onNodeWithContentDescription("Back").performClick()

        // Shipments: dispatching a sale removes stock, receiving a PO adds it.
        compose.onNodeWithTag("nav_shipments").performClick()
        waitForText("SO-00003")
        screenshot("shipments")
        compose.onNodeWithText("Dispatch").performClick()
        waitForText("SO-00003 dispatched", substring = true)
        screenshot("shipment_dispatched")
        compose.onNodeWithText("Receive").performClick()
        waitForText("PO-00001 received", substring = true)

        // A sale larger than the stock on hand can be created but not dispatched.
        compose.onNodeWithTag("new_shipment").performClick()
        waitForText("New shipment")
        compose.onNodeWithTag("type_outbound").performClick()
        compose.onNodeWithTag("shipment_product").performClick()
        waitForText("Hex nuts M8 (HW-1002) · 18 box")
        compose.onNodeWithText("Hex nuts M8 (HW-1002) · 18 box", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("shipment_quantity").performTextInput("50")
        compose.onNodeWithTag("shipment_partner").performTextInput("Test Retail Co")
        closeSoftKeyboard()
        screenshot("new_shipment_form")
        compose.onNodeWithText("Create sales shipment").performScrollTo().performClick()
        waitForText("To Test Retail Co", substring = true)
        compose.onNodeWithText("Dispatch").performClick()
        waitForText("Not enough stock", substring = true)
        screenshot("dispatch_blocked_low_stock")

        // Suppliers.
        compose.onNodeWithTag("nav_suppliers").performClick()
        waitForText("Acme Components")
        screenshot("suppliers")

        // Dashboard reflects everything above.
        compose.onNodeWithTag("nav_dashboard").performClick()
        waitForText("Reorder alerts")
        screenshot("dashboard_after_changes")
    }

    private fun waitForText(text: String, substring: Boolean = false) {
        compose.waitUntil(TIMEOUT_MS) {
            compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntilGone(text: String) {
        compose.waitUntil(TIMEOUT_MS) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()
        }
    }

    /**
     * Hides the on-screen keyboard through the window's insets controller.
     * (Espresso's closeSoftKeyboard needs window focus, which a slow emulator
     * can briefly lose to system dialogs.)
     */
    private fun closeSoftKeyboard() {
        compose.activityRule.scenario.onActivity { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .hide(WindowInsetsCompat.Type.ime())
        }
        compose.waitForIdle()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        // Give the emulator's software renderer time to put the latest frame on screen.
        Thread.sleep(1_000)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = File(instrumentation.targetContext.filesDir, "screenshots").apply { mkdirs() }
        shotCount++
        File(dir, "%02d_%s.png".format(shotCount, name)).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
