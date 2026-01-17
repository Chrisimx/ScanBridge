package io.github.chrisimx.scanbridge.uicomponents.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.chrisimx.scanbridge.R
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomScannerDialog(onDismiss: () -> Unit, onConnectClicked: (String, HttpUrl, Boolean) -> Unit) {
    var urlErrorState: String? by remember { mutableStateOf(null) }
    var urlText: String by remember { mutableStateOf("") }
    var nameText: String by remember { mutableStateOf("") }

    val context = LocalContext.current

    val validateUrl = fun(): HttpUrl? {
        if (urlText.isEmpty()) {
            urlErrorState = context.getString(R.string.error_state_please_enter_an_url)
            return null
        }

        try {
            return urlText.toHttpUrl()
        } catch (_: IllegalArgumentException) {
            urlErrorState = context.getString(R.string.invalid_url)
            return null
        }
    }

    Dialog(
        onDismissRequest = { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.custom_scanner_dialog_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    modifier = Modifier.testTag("name_input"),
                    value = nameText,
                    onValueChange = {
                        nameText = it
                    },
                    label = { Text(stringResource(R.string.name)) },
                    placeholder = { Text(stringResource(R.string.scanner_name_placeholder)) }
                )
                OutlinedTextField(
                    modifier = Modifier.testTag("url_input"),
                    value = urlText,
                    onValueChange = {
                        urlErrorState = null
                        urlText = it
                    },
                    label = { Text(stringResource(R.string.url_escl_resource)) },
                    placeholder = { Text("http://192.168.178.2/eSCL/") },
                    supportingText = {
                        urlErrorState?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                )
                Button(
                    onClick = {
                        val url = validateUrl() ?: return@Button
                        onConnectClicked(nameText, url, true)
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(stringResource(R.string.connect_and_save))
                }
                Button(
                    onClick = {
                        val url = validateUrl() ?: return@Button
                        onConnectClicked(nameText, url, false)
                    },
                    modifier = Modifier.padding(top = 8.dp).testTag("justconnect")
                ) {
                    Text(stringResource(R.string.connect))
                }
            }
        }
    }
}
