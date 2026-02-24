package io.github.chrisimx.scanbridge.services

import android.app.Application
import android.net.Uri
import androidx.datastore.core.DataStore
import io.github.chrisimx.scanbridge.BuildConfig
import io.github.chrisimx.scanbridge.logs.FileLogger
import io.github.chrisimx.scanbridge.proto.ScanBridgeSettings
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class FileDebugLogService(
    val appSettings: DataStore<ScanBridgeSettings>,
    val application: Application
) : DebugLogService {

    val scope = CoroutineScope(Dispatchers.Main)

    private var debugWriter: BufferedWriter? = null
    private var tree: Timber.Tree? = null

    private val debugLogActive = appSettings.data.map { it.writeDebug }.distinctUntilChanged()

    init {
        debugLogActive.onEach {
            if (it) {
                start()
            } else {
                stop()
            }
        }.launchIn(scope)
    }

    private fun start() {
        val debugDir = File(application.filesDir, "debug")
        if (!debugDir.exists()) {
            debugDir.mkdir()
        }
        val output = File(debugDir, "debug.txt")
        if (!output.exists()) {
            output.createNewFile()
        }
        debugWriter = BufferedWriter(FileWriter(output, true))
        tree = FileLogger(debugWriter!!)
        Timber.forest().filterIsInstance<FileLogger>().forEach {
            Timber.d("Old tree removed $it")
            it.output.close()
            Timber.uproot(it)
        }
        Timber.plant(tree!!)
        Timber.i(
            "Debug logging starts with ScanBridge (${BuildConfig.VERSION_NAME}, ${BuildConfig.VERSION_CODE}, ${BuildConfig.GIT_COMMIT_HASH}, ${BuildConfig.BUILD_TYPE}, ${BuildConfig.FLAVOR})"
        )
    }

    private fun stop() {
        Timber.i(
            "Debug logging stops with ScanBridge (${BuildConfig.VERSION_NAME}, ${BuildConfig.VERSION_CODE}, ${BuildConfig.GIT_COMMIT_HASH}, ${BuildConfig.BUILD_TYPE}, ${BuildConfig.FLAVOR})"
        )
        tree?.let {
            Timber.uproot(it)
            tree = null
        }
        debugWriter?.close()
        debugWriter = null
    }

    override fun clear() {
        Timber.d("Clearing debug log")
        tree?.let {
            Timber.uproot(it)
            tree = null
        }
        debugWriter?.close()
        debugWriter = null

        val debugDir = File(application.filesDir, "debug")
        if (debugDir.exists()) {
            val debugFile = File(debugDir, "debug.txt")
            if (debugFile.exists()) {
                debugFile.delete()
            }
        }

        start()
    }

    override fun flush() {
        debugWriter?.flush()
    }

    override fun saveToFile(uri: Uri) {
        val debugDir = File(application.filesDir, "debug")
        val debugFile = File(debugDir, "debug.txt")

        application.contentResolver.openOutputStream(uri)?.use { outputStream ->
            debugFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}
