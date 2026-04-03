package io.github.chrisimx.scanbridge.screenshot

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.chrisimx.esclmockserver.EsclMockServer
import io.github.chrisimx.scanbridge.BuildConfig
import io.github.chrisimx.scanbridge.MainActivity
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.datastore.lastRouteStore
import io.github.chrisimx.scanbridge.datastore.shownMessagesStore
import io.github.chrisimx.scanbridge.datastore.updateSettings
import io.github.chrisimx.scanbridge.proto.copy
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

class ScanBridgeScreenshotTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.POST_NOTIFICATIONS
    )

    @Rule @JvmField
    val localeTestRule = LocaleTestRule()

    fun startServer(): EsclMockServer {
        val context = InstrumentationRegistry.getInstrumentation().context

        val imageFile = copyAssetToByteArray(context, "scan-1.jpg")

        val server = EsclMockServer {
            servedImage = imageFile
        }

        return server
    }

    fun copyAssetToByteArray(testContext: Context, assetName: String): ByteArray = testContext.assets.open(assetName).use {
        it.readBytes()
    }

    @Before
    fun cleanupForTest() {
        runBlocking {
            composeTestRule.activity.shownMessagesStore.updateData {
                it.copy {
                    thankPlayOne = true
                }
            }
            composeTestRule.activity.lastRouteStore.updateData {
                it.copy {
                    clearLastRoute()
                }
            }
            composeTestRule.activity.appSettingsStore.updateSettings {
                clearLastUsedScanSettings()
                autoCleanup = true
            }
        }
    }

    @Test
    fun discovery() {
        composeTestRule.waitForIdle()

        Screengrab.screenshot("03-discoveryScreen")

        cleanupForTest()
    }

    @Test
    fun settings() {
        composeTestRule.onNodeWithTag("bottombutton1").performClick()

        composeTestRule.waitForIdle()

        Screengrab.screenshot("05-settingsScreen")

        cleanupForTest()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun support() {
        if (BuildConfig.FLAVOR == "play") {
            composeTestRule.waitUntilAtLeastOneExists(hasTestTag("bottombutton2"), 30000)
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("bottombutton2").performClick()

            composeTestRule.waitForIdle()

            Screengrab.screenshot("06-supportScreen")

            cleanupForTest()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scan() {
        val server = startServer()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("custom_scanner_fab"), 30000)
        composeTestRule.onNodeWithTag("custom_scanner_fab").performClick()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("name_input"), 30000)
        composeTestRule.onNodeWithTag("name_input")
            .performTextInput("Brother MFC-L8690CDW series")
        composeTestRule.onNodeWithTag("url_input")
            .performTextInput("http://127.0.0.1:8080/eSCL")
        composeTestRule.onNodeWithTag("justconnect").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasTestTag("full_screen_error_message"), 30000)
        Screengrab.screenshot("04-emptyScanScreen")

        composeTestRule.onNodeWithTag("scanbtn").performClick()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("snackbar_dismiss"), 30000)

        composeTestRule.onNodeWithTag("snackbar_dismiss").performClick()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("scanbtn"), 30000)

        composeTestRule.onNodeWithTag("scanbtn").performClick()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("scanbtn"), 30000)

        composeTestRule.waitForIdle()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("scan_page"), 30000)

        composeTestRule.waitForIdle()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("snackbar_dismiss"), 30000)

        composeTestRule.onNodeWithTag("snackbar_dismiss").performClick()

        composeTestRule.waitForIdle()

        Screengrab.screenshot("01-scannedPageScreen")

        server.close()

        // pressBack()

        // composeTestRule.waitForIdle()

        // composeTestRule.onNodeWithTag("leave_diag_button").performClick()

        cleanupForTest()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scanSettings() {
        val server = startServer()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("custom_scanner_fab"), 30000)
        composeTestRule.onNodeWithTag("custom_scanner_fab").performClick()

        composeTestRule.onNodeWithTag("name_input")
            .performTextInput("Brother MFC-L8690CDW series")
        composeTestRule.onNodeWithTag("url_input")
            .performTextInput("http://127.0.0.1:8080/eSCL")
        composeTestRule.onNodeWithTag("justconnect").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasTestTag("full_screen_error_message"), 30000)

        composeTestRule.onNodeWithTag("scansettings").performClick()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("scsetcolumn"), 30000)

        composeTestRule.onNodeWithTag("scsetcolumn").performTouchInput {
            swipeUp()
        }

        composeTestRule.waitForIdle()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("copyesclkt"), 30000)

        Screengrab.screenshot("02-scanSettings")

        server.close()

        // composeTestRule.waitUntilAtLeastOneExists(hasTestTag("leave_diag_button"), 20000)

        // composeTestRule.onNodeWithTag("leave_diag_button").performClick()

        cleanupForTest()
    }
}
