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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.model.EditedCustomScanner
import io.github.chrisimx.scanbridge.model.UrlValidationResult
import io.github.chrisimx.scanbridge.scannerdiscovery.ProtocolWithExampleHandleString
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import io.github.chrisimx.scanbridge.uicomponents.SelectionButtonRow
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.connect
import scanbridge.composeui.generated.resources.connect_and_save
import scanbridge.composeui.generated.resources.custom_scanner_url
import scanbridge.composeui.generated.resources.edit_custom_scanner
import scanbridge.composeui.generated.resources.error_state_please_enter_an_url
import scanbridge.composeui.generated.resources.invalid_url
import scanbridge.composeui.generated.resources.name
import scanbridge.composeui.generated.resources.new_custom_scanner_dialog_title
import scanbridge.composeui.generated.resources.save
import scanbridge.composeui.generated.resources.scanner_name_placeholder
import scanbridge.composeui.generated.resources.scanning_protocol

@Composable
fun UrlValidationResult.toLocalizedString(): String = when (this) {
    UrlValidationResult.Empty -> stringResource(Res.string.error_state_please_enter_an_url)
    UrlValidationResult.InvalidUrl -> stringResource(Res.string.invalid_url)
    is UrlValidationResult.NoError -> ""
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomScannerDialog(
    protocolsWithExampleHandle: List<ProtocolWithExampleHandleString>,
    onDismiss: () -> Unit,
    onConnectClicked: (name: String, url: String, protocol: String, save: Boolean, navigate: Boolean) -> Unit,
    editingType: EditedCustomScanner,
    validateUrl: (String) -> UrlValidationResult
) {
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

    var urlValidationResult: UrlValidationResult by remember { mutableStateOf(validateUrl(urlText)) }

    var selectedProtocol by remember {
        mutableStateOf(originalIdentifier ?: protocolsWithExampleHandle.first())
    }

    val isNewScanner = editingType is EditedCustomScanner.New

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
                    Res.string.new_custom_scanner_dialog_title
                } else {
                    Res.string.edit_custom_scanner
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
                    label = { Text(stringResource(Res.string.name)) },
                    placeholder = { Text(stringResource(Res.string.scanner_name_placeholder)) }
                )
                OutlinedTextField(
                    modifier = Modifier.testTag("url_input").padding(top = 16.dp),
                    value = urlText,
                    onValueChange = {
                        urlValidationResult = validateUrl(it)
                        urlText = it
                    },
                    label = { Text(stringResource(Res.string.custom_scanner_url)) },
                    placeholder = { Text(selectedProtocol.exampleScannerIdentifierString) },
                    supportingText = {
                        Text(
                            urlValidationResult.toLocalizedString(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
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
                            onConnectClicked(
                                nameText,
                                urlText,
                                selectedProtocol.protocolIdentifier,
                                true,
                                true
                            )
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text(stringResource(Res.string.connect_and_save))
                    }
                    Button(
                        onClick = {
                            onConnectClicked(
                                nameText,
                                urlText,
                                selectedProtocol.protocolIdentifier,
                                false,
                                true
                            )
                        },
                        modifier = Modifier.padding(top = 8.dp).testTag("justconnect")
                    ) {
                        Text(stringResource(Res.string.connect))
                    }
                } else {
                    Button(
                        onClick = {
                            onConnectClicked(
                                nameText,
                                urlText,
                                selectedProtocol.protocolIdentifier,
                                true,
                                false
                            )
                        },
                        modifier = Modifier.padding(top = 16.dp).testTag("editcustomscanner")
                    ) {
                        Text(stringResource(Res.string.save))
                    }
                }
            }
        }
    }
}


@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
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
                EditedCustomScanner.New,
                { urlString ->
                     UrlValidationResult.NoError(Url(urlString))
                }
            )
        }
    }
}
