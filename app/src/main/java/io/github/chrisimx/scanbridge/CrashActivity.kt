package io.github.chrisimx.scanbridge

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class CrashActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val error = intent.getStringExtra("error") ?: "Unknown error"
        val crashLog = intent.getStringExtra("crash_file")

        Timber.plant(Timber.DebugTree())

        Timber.e("$crashLog")

        try {
            CoroutineScope(Dispatchers.IO).launch {
                crashLog?.let {
                    val crashLogFile = File(it)
                    if (crashLogFile.exists()) {
                        crashLogFile.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("Error in CrashActivity while trying to remove crash log file: $e")
        }

        setContent {
            ScanBridgeTheme {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton({
                            try {
                                val clipboard = getSystemService(ClipboardManager::class.java)
                                val clip = ClipData.newPlainText("Crash log", error)
                                clipboard.setPrimaryClip(clip)
                            } catch (e: Exception) {
                                Timber.e("Error in CrashActivity while trying to copy crash to clipboard: $e")
                            }
                        }) {
                            Row(horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painterResource(R.drawable.rounded_content_copy_24),
                                    contentDescription = stringResource(R.string.copy),
                                    Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 5.dp)
                                )
                                Text(
                                    stringResource(R.string.copy),
                                    Modifier.padding(start = 5.dp, top = 10.dp, bottom = 10.dp, end = 10.dp)
                                )
                            }
                        }
                    }
                )
                { innerPadding ->
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painterResource(R.drawable.outline_error_24),
                            stringResource(R.string.crash),
                            Modifier.padding(20.dp).size(64.dp)
                        )

                        Column(modifier = Modifier.verticalScroll(scrollState)) {
                            Text(
                                stringResource(R.string.crash_occurred),
                                modifier = Modifier.padding(bottom = 14.dp),
                                style = MaterialTheme.typography.bodySmallEmphasized
                            )
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                            )
                        }
                    }
                }
            }
        }
    }
}
