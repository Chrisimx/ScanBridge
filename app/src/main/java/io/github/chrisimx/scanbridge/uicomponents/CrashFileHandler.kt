package io.github.chrisimx.scanbridge.uicomponents

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.github.chrisimx.scanbridge.uicomponents.dialog.CrashFileDialog
import java.io.File
import kotlin.collections.isNotEmpty

@Composable
fun CrashFileHandler() {
    var crash: String? by remember { mutableStateOf(null) }
    var crashFile: File? by remember { mutableStateOf(null) }
    val context = LocalContext.current

    LaunchedEffect(true) {
        Log.d("ScanBridgeApp", "Checking for crash files")
        val files =
            context.filesDir.listFiles { file, name -> name.startsWith("crash") && file.isFile }
        if (files != null && files.isNotEmpty()) {
            crashFile = files[0]
            crash = crashFile!!.readText()
            return@LaunchedEffect
        }
        val crashDir = File(context.filesDir, "crashes")
        if (crashDir.exists() && crashDir.isDirectory) {
            val crashFiles = crashDir.listFiles { _, name -> name.startsWith("crash") }
            if (crashFiles != null && crashFiles.isNotEmpty()) {
                crashFile = crashFiles[0]
                crash = crashFile!!.readText()
            }
        }

    }

    if (crash != null) {
        CrashFileDialog(
            crash = crash!!,
            onDismiss = { crashFile!!.delete(); crash = null; crashFile = null })
    }
}