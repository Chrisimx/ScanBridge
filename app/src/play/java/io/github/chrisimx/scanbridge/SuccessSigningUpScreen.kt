package io.github.chrisimx.scanbridge

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.NEXUS_5
import androidx.compose.ui.tooling.preview.Devices.TABLET
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SuccessSigningUpScreen(modifier: Modifier,
                       onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(30.dp, 0.dp, 30.dp, 30.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {

        Image(
            painter = painterResource(R.drawable.fireamp_icon),
            "Fireamp Icon",
            modifier = Modifier.padding(32.dp, 0.dp, 32.dp, 32.dp).heightIn(max = 200.dp)
        )

        Text(
            stringResource(R.string.account_created_successfully),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLargeEmphasized
        )

        Text(
            stringResource(R.string.explanation_account_creation_completion),
            modifier = Modifier.padding(20.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )


        FlowRow {
            ElevatedButton ({
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:support@fireamp.eu".toUri() // only email apps handle this
                    putExtra(Intent.EXTRA_SUBJECT, "Problem with ScanBridge")
                    putExtra(Intent.EXTRA_TEXT, "")
                }
                context.startActivity(emailIntent)
            }, modifier = Modifier.padding(horizontal = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon( Icons.Rounded.Email, stringResource(R.string.contact_support_over_email), modifier = Modifier.padding(end = 10.dp))
                    Text(stringResource(R.string.contact_support_over_email))
                }
            }
            ElevatedButton ({
                val url = "https://support.fireamp.eu/#login"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = url.toUri()
                context.startActivity(intent)
            }, modifier = Modifier.padding(horizontal = 10.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon( painterResource(R.drawable.outline_globe_24), stringResource(R.string.contact_support_over_email), modifier = Modifier.padding(end = 10.dp))
                    Text(stringResource(R.string.open_support_website))
                }
            }

            Button (onBack, modifier = Modifier.padding(10.dp)) {
                Text(stringResource(R.string.back))
            }
        }
    }
}

@Composable
@Preview(showBackground = true, device = NEXUS_5, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
fun SuccessSigningUpScreenPreview() {
    ScanBridgeTheme {
        Scaffold() { innerPadding ->
            SuccessSigningUpScreen(Modifier.padding(innerPadding))
        }
    }
}

@Composable
@Preview(showBackground = true, device = TABLET, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
fun SuccessSigningUpScreenTabletPreview() {
    ScanBridgeTheme {
        Scaffold() { innerPadding ->
            SuccessSigningUpScreen(Modifier.padding(innerPadding))
        }
    }
}

