package io.github.chrisimx.scanbridge

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import io.github.chrisimx.scanbridge.clipboard.toClipEntry
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.copy
import scanbridge.composeui.generated.resources.crash
import scanbridge.composeui.generated.resources.crash_occurred
import scanbridge.composeui.generated.resources.outline_error_24
import scanbridge.composeui.generated.resources.rounded_content_copy_24

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun CrashDisplay(
    error: String,
    loggerFactory: ScanBridgeLoggerFactory = koinInject()
) {
    val localClipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val logger = loggerFactory.withTag("CrashDisplay")

    ScanBridgeTheme {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton({
                    coroutineScope.launch {
                        try {
                            localClipboard.setClipEntry(
                                error.toClipEntry()
                            )
                        } catch (e: Exception) {
                            logger.error {
                                "Error in CrashActivity while trying to copy crash to clipboard: $e"
                            }
                        }
                    }
                }) {
                    Row(horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(Res.drawable.rounded_content_copy_24),
                            contentDescription = stringResource(Res.string.copy),
                            Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 5.dp)
                        )
                        Text(
                            stringResource(Res.string.copy),
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
                    painterResource(Res.drawable.outline_error_24),
                    stringResource(Res.string.crash),
                    Modifier.padding(20.dp).size(64.dp)
                )

                Column(modifier = Modifier.verticalScroll(scrollState)) {
                    Text(
                        stringResource(Res.string.crash_occurred),
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
