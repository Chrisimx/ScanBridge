package io.github.chrisimx.scanbridge

import android.util.Log
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.datastore.lastRouteStore
import io.github.chrisimx.scanbridge.datastore.shownMessagesStore
import io.github.chrisimx.scanbridge.datastore.updateSettings
import io.github.chrisimx.scanbridge.proto.copy
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ScanBridgeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.POST_NOTIFICATIONS
    )

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
    @After
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
        composeTestRule.onNodeWithText("Discovery").assertIsDisplayed()

        composeTestRule.onNodeWithText("Available scanners").assertIsDisplayed()

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scanningInterface() {
        val server = startServer()

        composeTestRule.onNodeWithTag("custom_scanner_fab").performClick()

        composeTestRule.onNodeWithTag("url_input")
            .performTextInput("http://127.0.0.1:8080/eSCL")
        composeTestRule.onNodeWithText("Just connect").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasText("No pages", substring = true), 1000)

        server.destroy()

        pressBack()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("leave_diag_button").performClick()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scan() {
        val server = startServer()

        val url = InstrumentationRegistry.getArguments()
            .getString("escl_server_url") ?: "http://127.0.0.1:8080/eSCL"

        testConnectToScanner(url)

        server.destroy()
    }

    @OptIn(ExperimentalTestApi::class)
    fun testConnectToScanner(url: String) {
        composeTestRule.onNodeWithTag("custom_scanner_fab").performClick()

        Log.d("ScanBridgeTest", "Trying URL: $url")
        composeTestRule.onNodeWithTag("url_input").performTextInput(url)
        composeTestRule.onNodeWithText("Just connect").performClick()

        composeTestRule.onNodeWithText("Scan", useUnmergedTree = true).performClick()

        composeTestRule.waitUntilAtLeastOneExists(hasTestTag("scan_page"), 5000)

        pressBack()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("leave_diag_button").performClick()

        composeTestRule.onNodeWithTag("bottombutton1").performClick()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRootURLFix() {
        val server = startServer("-s", "")

        testConnectToScanner("http://127.0.0.1:8080")

        server.destroy()
    }
}
