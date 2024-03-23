@file:OptIn(ExperimentalMaterial3Api::class)

package com.azuresamples.msalandroidcomposeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.azuresamples.msalandroidcomposeapp.ui.multi.MultiAccountModeScreen
import com.azuresamples.msalandroidcomposeapp.ui.single.SingleAccountModeScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Content()
            }
        }
    }
}

internal enum class AccountMode(val label: String, val iconResource: Int) {
    SingleAccount("Single account", R.drawable.ic_single_account_24dp),
    MultipleAccount("Multiple account", R.drawable.ic_multiple_account_24dp),
}

@Composable
private fun Content() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf(AccountMode.SingleAccount) }

    val snackbarHostState = remember { SnackbarHostState() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.padding(12.dp)) {
                    Text("Account Mode", modifier = Modifier.padding(16.dp))

                    for (mode in AccountMode.entries) {
                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    painterResource(id = mode.iconResource),
                                    contentDescription = null,
                                )
                            },
                            label = { Text(text = mode.label) },
                            selected = mode == selectedMode,
                            onClick = {
                                scope.launch { drawerState.close() }
                                selectedMode = mode
                            }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text("${selectedMode.label} mode")
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Open navigation drawer"
                            )
                        }
                    },
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
        ) { contentPadding ->

            when (selectedMode) {
                AccountMode.SingleAccount -> SingleAccountModeScreen(
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.padding(contentPadding),
                )

                AccountMode.MultipleAccount -> MultiAccountModeScreen(
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier.padding(contentPadding),
                )
            }
        }
    }
}
