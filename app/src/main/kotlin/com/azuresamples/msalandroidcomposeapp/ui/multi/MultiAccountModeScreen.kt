@file:OptIn(ExperimentalMaterial3Api::class)

package com.azuresamples.msalandroidcomposeapp.ui.multi

import android.graphics.Typeface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.azuresamples.msalandroidcomposeapp.ui.findActivity


@Composable
fun MultiAccountModeScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val viewModel: MultiViewModel = viewModel()

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context.findActivity()

    LaunchedEffect(Unit) {
        viewModel.initAuth(context)

        viewModel.accountRemovedEvent.collect {
            snackbarHostState.showSnackbar("Account removed")
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        /*
         * Accounts may have been removed from the device (if broker is in use).
         *
         * In shared device mode, accounts might be signed in/out by other apps while this app is not in focus.
         * Therefore, we want to update the accounts state by invoking loadAccounts() here.
         */
        viewModel.loadAccounts()
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        val scopes by viewModel.scopes.collectAsStateWithLifecycle()

        OutlinedTextField(
            label = { Text(text = "Scopes") },
            value = scopes.joinToString(","),
            onValueChange = {
                viewModel.scopes.value = it.split(",")
                    .map { scope -> scope.trim() }
            },
            singleLine = true,
            supportingText = { Text("Comma-separated") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        val msGraphUrl by viewModel.msGraphUrl.collectAsStateWithLifecycle()

        OutlinedTextField(
            value = msGraphUrl,
            onValueChange = { viewModel.msGraphUrl.value = it },
            label = { Text(text = "MSGraph Resource URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        var expanded by remember { mutableStateOf(false) }

        val focusManager = LocalFocusManager.current

        val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                readOnly = true,
                value = selectedAccount?.username ?: "",
                onValueChange = { },
                label = { Text("Account") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    focusManager.clearFocus()
                    expanded = false
                },
                properties = PopupProperties(focusable = false),
                modifier = Modifier.exposedDropdownSize()
            ) {
                accounts?.forEach { account ->
                    DropdownMenuItem(
                        onClick = {
                            viewModel.selectedAccount.value = account
                            focusManager.clearFocus()
                            expanded = false
                        },
                        text = {
                            Text(text = account.username)
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }


        Spacer(Modifier.height(16.dp))

        val isSharedDevice by viewModel.isSharedDevice.collectAsStateWithLifecycle(null)

        OutlinedTextField(
            value = when (isSharedDevice) {
                true -> "Shared"
                false -> "Non-shared"
                null -> ""
            },
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(text = "Device mode") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))

        if (selectedAccount != null) {
            OutlinedButton(
                onClick = { viewModel.removeAccount() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Remove account")
            }

            Spacer(Modifier.height(8.dp))

            FilledTonalButton(
                onClick = { viewModel.callGraphInteractive(activity) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Get Graph Data Interactively")
            }

            Spacer(Modifier.height(8.dp))

            FilledTonalButton(
                onClick = { viewModel.callGraphSilent() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Get Graph Data Silently")
            }
        }

        Spacer(Modifier.height(20.dp))

        val output by viewModel.output.collectAsStateWithLifecycle()
        val isError by viewModel.isError.collectAsStateWithLifecycle()

        output?.let {
            val textColor =
                if (!isError) Color.Unspecified
                else MaterialTheme.colorScheme.error

            Text(
                text = if (!isError) "Result" else "Error",
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = it,
                fontSize = 16.sp,
                fontFamily = FontFamily(Typeface.MONOSPACE),
                color = textColor,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}
