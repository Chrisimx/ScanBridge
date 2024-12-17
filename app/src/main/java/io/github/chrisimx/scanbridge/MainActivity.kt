/*
 *     Copyright (C) 2024 Christian Nagel and contributors
 *
 *     This file is part of ScanBridge.
 *
 *     ScanBridge is free software: you can redistribute it and/or modify it under the terms of
 *     the GNU General Public License as published by the Free Software Foundation, either
 *     version 3 of the License, or (at your option) any later version.
 *
 *     ScanBridge is distributed in the hope that it will be useful, but WITHOUT ANY
 *     WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *     FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along with eSCLKt.
 *     If not, see <https://www.gnu.org/licenses/>.
 *
 *     SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.chrisimx.scanbridge

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.nsd.NsdManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getSystemService
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    var scannerDiscoveryPair: Pair<NsdManager, ScannerDiscovery>? = null
    val statefulScannerMap = mutableStateMapOf<String,DiscoveredScanner>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var selectedItem = rememberSaveable { mutableIntStateOf(0)}
            val items = listOf("Discovery", "Setttings")
            val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.Settings)
            val unselectedIcons =
                listOf(Icons.Outlined.Home, Icons.Outlined.Settings)

            ScanBridgeTheme {
                Scaffold(modifier = Modifier.fillMaxSize(),
                    topBar = @Composable { ScannerDiscoveryTopBar() },
                    bottomBar = @Composable {
                        NavigationBar {
                            items.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            if (selectedItem.intValue == index) selectedIcons[index] else unselectedIcons[index],
                                            contentDescription = item
                                        )
                                    },
                                    label = { Text(item) },
                                    selected = selectedItem.intValue == index,
                                    onClick = { selectedItem.intValue = index }
                                )
                            }
                        }
                    }) { innerPadding ->

                    when (selectedItem.intValue) {
                        0 -> ScanBrowser(innerPadding, statefulScannerMap)
                        1 -> { Text("Settings")}
                    }

                }
            }
        }

        thread {
            val service = getSystemService(this, NsdManager::class.java)
            if (service == null) {
                Log.e("ScannerDiscovery", "Couldn't get NsdManager service")
                return@thread
            }
            val listener = ScannerDiscovery(service, statefulScannerMap)
            service.discoverServices("_uscan._tcp", NsdManager.PROTOCOL_DNS_SD,listener )
            Log.i("ScannerDiscovery", "Discovery started")
            scannerDiscoveryPair = Pair(service, listener)
        }
    }

    override fun onDestroy() {
        Log.d("ScannerDiscovery", "Activity is destroyed: Service discovery is stopped")
        scannerDiscoveryPair?.first?.stopServiceDiscovery(scannerDiscoveryPair?.second)
        super.onDestroy()
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Changing the theme doesn't recreate the activity, so set the E2E values again
        enableEdgeToEdge()
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerDiscoveryTopBar() {
    TopAppBar(
        title = @Composable { Text("Network-attached scanners") }
    )
}
@Composable
fun ScanBrowser(innerPadding: PaddingValues, statefulScannerMap: SnapshotStateMap<String, DiscoveredScanner>) {
    if (statefulScannerMap.isNotEmpty()) {
        LazyColumn (
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            reverseLayout = true,
        ) {
            statefulScannerMap.forEach {
                val discoveredScanner = it.value
                discoveredScanner.addresses.forEach {
                    item {

                        FoundScannerItem(discoveredScanner.name, it)
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.twotone_wifi_find_24),
                contentDescription = stringResource(R.string.warning_desc),
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(stringResource(R.string.no_scanners_found), modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun FoundScannerItem(name: String, address: String) {
    val activity = LocalContext.current as Activity
    ElevatedCard(modifier = Modifier
        .defaultMinSize(minHeight = 60.dp)
        .padding(10.dp),
        onClick = {
            activity.startActivity(
                Intent(
                    activity.applicationContext,
                    ScanningActivity::class.java
                ).putExtra("name", name).putExtra("address", address)
            )
    }) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 80.dp)
                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(17.dp),
                painter = painterResource(R.drawable.round_print_36),
                tint = MaterialTheme.colorScheme.surfaceTint,
                contentDescription = stringResource(id = R.string.print_symbol_desc)
            )
            Column {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(address)
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Preview(showBackground = true)
@Composable
fun ScanBrowserPreview() {
    ScanBridgeTheme {
        Scaffold { innerPadding ->
            ScanBrowser(innerPadding, mutableStateMapOf())
        }
    }
}