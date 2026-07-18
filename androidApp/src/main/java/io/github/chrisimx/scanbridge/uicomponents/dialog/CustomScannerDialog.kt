package io.github.chrisimx.scanbridge.uicomponents.dialog

import android.annotation.SuppressLint
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.data.model.EditedCustomScanner
import io.github.chrisimx.scanbridge.scannerdiscovery.ProtocolWithExampleHandleString
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import io.github.chrisimx.scanbridge.uicomponents.SelectionButtonRow
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.scanning_protocol

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomScannerDialog(
    protocolsWithExampleHandle: List<ProtocolWithExampleHandleString>,
    onDismiss: () -> Unit,
    onConnectClicked: (name: String, url: Url, protocol: String, save: Boolean, navigate: Boolean) -> Unit,
    editingType: EditedCustomScanner
) {
    var urlErrorState: String? by remember { mutableStateOf(null) }
    val initializeWith = when (editingType) {
        is EditedCustomScanner.EditingOld -> editingType.scanner
        EditedCustomScanner.New -> null
    }
    val originalIdentifier = protocolsWithExampleHandle
        .firstOrNull { it.protocolIdentifier == initializeWith?.protocolIdentifier }

    var urlText: String by remember {
        mutableStateOf(initializeWith?.url?.toString() ?: "")
    }
    var nameText: String by remember { mutableStateOf(initializeWith?.name ?: "") }

    var selectedProtocol by remember {
        mutableStateOf(originalIdentifier ?: protocolsWithExampleHandle.first())
    }

    val context = LocalContext.current

    val isNewScanner = editingType is EditedCustomScanner.New

    val validateUrl = fun(): Url? {
        if (urlText.isEmpty()) {
            urlErrorState = context.getString(R.string.error_state_please_enter_an_url)
            return null
        }

        try {
            return Url(urlText)
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
                val title = if (isNewScanner) {
                    R.string.new_custom_scanner_dialog_title
                } else {
                    R.string.edit_custom_scanner
                }

                Text(
                    stringResource(title),
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
                    modifier = Modifier.testTag("url_input").padding(top = 16.dp),
                    value = urlText,
                    onValueChange = {
                        urlErrorState = null
                        urlText = it
                    },
                    label = { Text(stringResource(R.string.custom_scanner_url)) },
                    placeholder = { Text(selectedProtocol.exampleScannerIdentifierString) },
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

                SelectionButtonRow(
                    stringResource(Res.string.scanning_protocol),
                    protocolsWithExampleHandle,
                    { selectedProtocol = it!! },
                    { this.protocolIdentifier },
                    selectedProtocol
                )

                if (isNewScanner) {
                    Button(
                        onClick = {
                            val url = validateUrl() ?: return@Button
                            onConnectClicked(
                                nameText,
                                url,
                                selectedProtocol.protocolIdentifier,
                                true,
                                true
                            )
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(stringResource(R.string.connect_and_save))
                    }
                    Button(
                        onClick = {
                            val url = validateUrl() ?: return@Button
                            onConnectClicked(
                                nameText,
                                url,
                                selectedProtocol.protocolIdentifier,
                                false,
                                true
                            )
                        },
                        modifier = Modifier.padding(top = 8.dp).testTag("justconnect")
                    ) {
                        Text(stringResource(R.string.connect))
                    }
                } else {
                    Button(
                        onClick = {
                            val url = validateUrl() ?: return@Button
                            onConnectClicked(
                                nameText,
                                url,
                                selectedProtocol.protocolIdentifier,
                                true,
                                false
                            )
                        },
                        modifier = Modifier.padding(top = 16.dp).testTag("editcustomscanner")
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview
@Composable
fun PreviewCustomScannerDialog() {
    Scaffold {
        ScanBridgeTheme {
            CustomScannerDialog(
                listOf(
                    ProtocolWithExampleHandleString("test", "test")
                ),
                {},
                { _, _, _, _, _ -> Unit },
                EditedCustomScanner.New

            )
        }
    }
}
