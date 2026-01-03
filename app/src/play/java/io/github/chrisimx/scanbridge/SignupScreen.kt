package io.github.chrisimx.scanbridge

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import io.github.chrisimx.scanbridge.util.snackBarError
import io.github.chrisimx.scanbridge.zammadapi.AccountVerificationClient
import io.github.chrisimx.scanbridge.zammadapi.models.AccountCreationRequest
import io.github.chrisimx.scanbridge.zammadapi.models.VIPCreationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


class SignupViewModel(
    application: Application
) : AndroidViewModel(application) {
    fun signUp(
        name: CharSequence,
        email: CharSequence,
        snackbarHostState: SnackbarHostState,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val api = AccountVerificationClient.RetrofitClient.api
                    val googleChallenge = api.getGoogleChallenge()

                    val ops = OwnershipProofService(application)
                    val ownershipProofResult = ops.requestOwnershipProof(googleChallenge.nonce)

                    if (ownershipProofResult is LicensingRequestResult.Error) {
                        Timber.e(ownershipProofResult.errorCode.asLocalizedString(application))
                        snackBarError(
                            application.getString(R.string.error) + ": " + ownershipProofResult.errorCode.asLocalizedString(application),
                            viewModelScope,
                            application,
                            snackbarHostState)
                        return@withContext
                    } else if (ownershipProofResult is LicensingRequestResult.OwnershipProof) {
                        val rawProof = ownershipProofResult.responseData.originalResponse
                        val signature = ownershipProofResult.signature

                        Timber.d("Ownership proof acquired")

                        val creationRequest = AccountCreationRequest(
                            name.toString(), email.toString(), googleChallenge.token, rawProof, signature
                        )

                        val accountCreationResult = api.createAccount(creationRequest)

                        Timber.d("Account creation request sent")

                        if (accountCreationResult.result != VIPCreationResult.Success) {
                            Timber.e("%s: %s", application.getString(R.string.error), application.getString(accountCreationResult.result.message))
                            snackBarError(
                                application.getString(R.string.error) + ": " + application.getString(accountCreationResult.result.message),
                                viewModelScope,
                                application,
                                snackbarHostState)
                            return@withContext
                        }
                        onSuccess()
                    }
                } catch (error: Exception) {
                    Timber.e(error)
                    snackBarError(
                        application.getString(R.string.error) + ": " + error.message,
                        viewModelScope,
                        application,
                        snackbarHostState)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SignupScreen(modifier: Modifier, onBack: () -> Unit, onSuccess: () -> Unit) {
    val viewModel: SignupViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    var privacyPolicyAccepted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val localTextStyle = LocalTextStyle.current

    val privacyPolicyLink: AnnotatedString = remember {
        buildAnnotatedString {

            val styleCenter = localTextStyle.copy(
                color = Color(0xff64B5F6),
                textDecoration = TextDecoration.Underline
            ).toSpanStyle()

            append(context.getString(R.string.by_joining_i_accept))

            withLink(LinkAnnotation.Url(url = "https://fireamp.eu/privacy-policy-support")) {
                withStyle(
                    style = styleCenter
                ) {
                    append(context.getString(R.string.fireamp_privacy_policy))
                }
            }

            append(context.getString(R.string.last_part_privacy_accept))
        }
    }
    
    Box(modifier = modifier
        .fillMaxSize(), contentAlignment = Alignment.Center) {

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    data,
                    containerColor = if (data.visuals.message.contains(stringResource(R.string.error))) {
                        MaterialTheme.colorScheme.error
                    } else {
                        SnackbarDefaults.color
                    }
                )
            }
        }

        Card(modifier = Modifier.padding(30.dp)) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val emailState = rememberTextFieldState()
                val nameState = rememberTextFieldState()

                Image(
                    painter = painterResource(R.drawable.fireamp_icon),
                    "Fireamp Icon",
                    modifier = Modifier
                        .padding(32.dp)
                        .widthIn(max = 100.dp)
                )

                Text(
                    stringResource(R.string.signup_to_fireamp),
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(nameState,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    placeholder = { Text("Jonathan Steinberg") },
                    label = { Text(stringResource(R.string.name) )},
                )
                OutlinedTextField(
                    emailState,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    placeholder = { Text("example@e-mail.com") },
                    label = { Text(stringResource(R.string.e_mail)) },
                )

                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .toggleable(
                            value = privacyPolicyAccepted,
                            onValueChange = { privacyPolicyAccepted = it },
                            role = Role.Checkbox
                        )
                ) {
                    Checkbox(privacyPolicyAccepted, {
                        privacyPolicyAccepted = it
                    })
                    Text(privacyPolicyLink, textAlign = TextAlign.Justify)
                }


                Row {
                    Button ({
                        if (!privacyPolicyAccepted) {
                            snackBarError(
                                context.getString(R.string.please_accept_the_privacy_policy_to_sign_up),
                                viewModel.viewModelScope, context, snackbarHostState, false)
                            return@Button
                        }

                        viewModel.signUp(nameState.text, emailState.text, snackbarHostState, onSuccess)
                    }, modifier = Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.signup))
                    }
                    OutlinedButton(onBack, modifier = Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.back))
                    }
                }

            }
        }
    }

}

@Composable
@Preview()
fun SignupScreenPreview() {
    ScanBridgeTheme {
        Scaffold() { innerPadding ->
            SignupScreen(Modifier.padding(innerPadding), {}, {})
        }
    }
}
