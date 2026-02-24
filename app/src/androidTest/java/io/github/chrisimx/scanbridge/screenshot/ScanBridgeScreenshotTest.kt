package io.github.chrisimx.scanbridge.screenshot

import android.util.Log
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import io.github.chrisimx.scanbridge.MainActivity
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.datastore.lastRouteStore
import io.github.chrisimx.scanbridge.datastore.updateSettings
import io.github.chrisimx.scanbridge.proto.copy
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

class ScanBridgeScreenshotTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Rule @JvmField
    val localeTestRule = LocaleTestRule()

    fun startServer(vararg args: String): Process {
        val context = InstrumentationRegistry.getInstrumentation().context

        val mockServer = File(context.applicationInfo.nativeLibraryDir, "lib_escl_mock.so")

        Log.d("ScanBridgeTest", "ESCL mock server: ${mockServer.absolutePath}")

        val process = ProcessBuilder()
            .command(mockServer.absolutePath, *args)
            .start()

        Thread {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        Log.d("ESCLMockServer", line!!) // Log each line from the process
                    }
                }
            } catch (e: Exception) {
                Log.e("ESCLMockServer", "Error reading output", e)
            }
        }.start()

        return process
    }

    @Before
    fun cleanupForTest() {
        runBlocking {
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

        Screengrab.screenshot("discoveryScreen")

        cleanupForTest()
    }

    @Test
    fun settings() {
        composeTestRule.onNodeWithTag("bottombutton1").performClick()

        composeTestRule.waitForIdle()

        Screengrab.screenshot("settingsScreen")

        cleanupForTest()
    }

    @Test
    fun support() {
        composeTestRule.onNodeWithTag("bottombutton2").performClick()

        composeTestRule.waitForIdle()

        Screengrab.screenshot("supportScreen")

        cleanupForTest()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scan() {
        val server = startServer()

        composeTestRule.onNodeWithTag("custom_scanner_fab").performClick()

        composeTestRule.onNodeWithTag("name_input")
            .performTextInput("Brother MFC-L8690CDW series")
        composeTestRule.onNodeWithTag("url_input")
            .performTextInput("http://192.168.178.122/eSCL")
        composeTestRule.onNodeWithTag("justconnect").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasTestTag("full_screen_error_message"), 30000)
        Screengrab.screenshot("emptyScanScreen")

        composeTestRule.onNodeWithTag("scanbtn").performClick()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("scanbtn"), 30000)

        composeTestRule.onNodeWithTag("scanbtn").performClick()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("scanbtn"), 30000)

        composeTestRule.waitForIdle()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("scan_page"), 30000)

        composeTestRule.waitForIdle()

        Screengrab.screenshot("scannedPageScreen")

        server.destroy()

        // pressBack()

        // composeTestRule.waitForIdle()

        // composeTestRule.onNodeWithTag("leave_diag_button").performClick()

        cleanupForTest()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scanSettings() {
        val server = startServer()

        composeTestRule.onNodeWithTag("custom_scanner_fab").performClick()

        composeTestRule.onNodeWithTag("name_input")
            .performTextInput("Brother MFC-L8690CDW series")
        composeTestRule.onNodeWithTag("url_input")
            .performTextInput("http://192.168.178.122/eSCL")
        composeTestRule.onNodeWithTag("justconnect").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasTestTag("full_screen_error_message"), 30000)

        composeTestRule.onNodeWithTag("scansettings").performClick()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("scsetcolumn"), 30000)

        composeTestRule.onNodeWithTag("scsetcolumn").performTouchInput {
            swipeUp()
        }

        composeTestRule.waitForIdle()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("copyesclkt"), 30000)

        Screengrab.screenshot("scanSettings")

        server.destroy()

        // composeTestRule.waitUntilAtLeastOneExists(hasTestTag("leave_diag_button"), 20000)

        // composeTestRule.onNodeWithTag("leave_diag_button").performClick()

        cleanupForTest()
    }
}
