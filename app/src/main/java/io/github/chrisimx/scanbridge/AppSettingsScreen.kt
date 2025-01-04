package io.github.chrisimx.scanbridge

import android.content.Context.MODE_PRIVATE
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.chrisimx.scanbridge.theme.Poppins
import io.github.chrisimx.scanbridge.theme.Teal1
import io.github.chrisimx.scanbridge.theme.gradientBrush

@Composable
fun VersionComposable() {
    val context = LocalContext.current

    Image(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp),
        painter = painterResource(R.drawable.icon_about_dialog),
        contentDescription = stringResource(id = R.string.app_icon_desc)
    )

    Text(
        stringResource(R.string.app_name),
        modifier = Modifier.padding(PaddingValues(4.dp)),
        fontFamily = Poppins,
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        style = MaterialTheme.typography.labelLarge.copy(
            brush = gradientBrush
        )
    )

    Text(
        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.GIT_COMMIT_HASH})",
        fontStyle = FontStyle.Normal,
        fontFamily = Poppins,
    )

    if (BuildConfig.DEBUG) {
        Text(
            stringResource(R.string.debug_build),
            fontStyle = FontStyle.Italic,
            fontFamily = Poppins
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun AppSettingsScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current

    val sharedPreferences = remember {
        context.getSharedPreferences("scanbridge", MODE_PRIVATE)
    }

    var automaticCleanup by remember {
        mutableStateOf(
            sharedPreferences.getBoolean(
                "auto_cleanup",
                false
            )
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        VersionComposable()

        HorizontalDivider(modifier = Modifier.padding(14.dp))

        var information: String? by remember {
            mutableStateOf(null)
        }

        ElevatedCard(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.settings),
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = Teal1
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = automaticCleanup,
                            onValueChange = {
                                sharedPreferences
                                    .edit()
                                    .putBoolean("auto_cleanup", it)
                                    .apply()
                                automaticCleanup = it
                            },
                            role = Role.Checkbox
                        )
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = automaticCleanup,
                        modifier = Modifier.weight(0.1f),
                        onCheckedChange = null
                    )
                    Text(
                        text = stringResource(R.string.auto_cleanup),
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .weight(0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val context = LocalContext.current
                    Box(modifier = Modifier.weight(0.1f)) {
                        IconButton(onClick = {
                            information = context.getString(
                                R.string.auto_cleanup_explanation,
                                context.getString(R.string.app_name)
                            )
                        }) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = stringResource(R.string.auto_cleanup_info_desc)
                            )
                        }
                    }

                }
            }
        }

        if (information != null) {
            Dialog(onDismissRequest = { information = null }) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            information!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}