package io.github.chrisimx.scanbridge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import timber.log.Timber

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val error = intent.getStringExtra("error") ?: "Unknown error"
        // val crashLog = intent.getStringExtra("crash_file")

        Timber.plant(Timber.DebugTree())

        setContent {
            CrashDisplay(
                error
            )
        }
    }
}
