package io.github.chrisimx.scanbridge

import android.content.ContentValues
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test

class ScanBridgeTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    fun saveBitmapToExternalStorage(bitmap: Bitmap, filename: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val contentResolver = context.contentResolver
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = contentResolver.insert(imageCollection, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }
    }

    private fun saveScreenshot(name: String) {
        val date = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH_mm_ss_SSS"))
        composeTestRule.onNodeWithTag("root_node")
            .captureToImage()
            .asAndroidBitmap()
            .let { bitmap ->
                saveBitmapToExternalStorage(bitmap, "$name-screenshot$date}.png")
            }
    }

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

    @Test
    fun discovery() {
        composeTestRule.activity.getSharedPreferences("scanbridge", MODE_PRIVATE)
            .edit()
            .putBoolean("auto_cleanup", true)
            .apply()

        composeTestRule.onNodeWithText("Discovery").assertIsDisplayed()

        composeTestRule.onNodeWithText("Available scanners").assertIsDisplayed()

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun discoveryScreenshot() {
        composeTestRule.activity.getSharedPreferences("scanbridge", MODE_PRIVATE)
            .edit()
            .putBoolean("auto_cleanup", true)
            .apply()
        saveScreenshot("discovery")
    }

    @Test
    fun settingsScreenshot() {
        composeTestRule.activity.getSharedPreferences("scanbridge", MODE_PRIVATE)
            .edit()
            .putBoolean("auto_cleanup", true)
            .apply()
        composeTestRule.onNodeWithText("Settings").performClick()

        saveScreenshot("settings")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scanningInterface() {
        composeTestRule.activity.getSharedPreferences("scanbridge", MODE_PRIVATE)
            .edit()
            .putBoolean("auto_cleanup", true)
            .apply()

        val server = startServer()

        composeTestRule.onNodeWithTag("custom_scanner_fab").performClick()

        composeTestRule.onNodeWithTag("url_input")
            .performTextInput("http://127.0.0.1:8080/eSCL")
        composeTestRule.onNodeWithText("Just connect").performClick()

        composeTestRule.waitUntilExactlyOneExists(hasText("No pages", substring = true), 1000)
        saveScreenshot("scanning_interface")

        server.destroy()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scan() {
        composeTestRule.activity.getSharedPreferences("scanbridge", MODE_PRIVATE)
            .edit()
            .putBoolean("auto_cleanup", true)
            .apply()

        val server = startServer()

        val url = InstrumentationRegistry.getArguments()
            .getString("escl_server_url") ?: "http://127.0.0.1:8080/eSCL"

        testConnectToScanner(url)
        saveScreenshot("scan")

        server.destroy()
    }

    @OptIn(ExperimentalTestApi::class)
    fun testConnectToScanner(url: String) {
        composeTestRule.activity.getSharedPreferences("scanbridge", MODE_PRIVATE)
            .edit()
            .putBoolean("auto_cleanup", true)
            .apply()

        composeTestRule.onNodeWithTag("custom_scanner_fab").performClick()

        Log.d("ScanBridgeTest", "Trying URL: $url")
        composeTestRule.onNodeWithTag("url_input").performTextInput(url)
        composeTestRule.onNodeWithText("Just connect").performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Scan", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasTestTag("scan_page")).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testRootURLFix() {
        composeTestRule.activity.getSharedPreferences("scanbridge", MODE_PRIVATE)
            .edit()
            .putBoolean("auto_cleanup", true)
            .apply()

        val server = startServer("-s", "")

        testConnectToScanner("http://127.0.0.1:8080")

        server.destroy()
    }
}
