package com.azuresamples.msalandroidkotlinapp.ui.single

import android.graphics.Typeface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.azuresamples.msalandroidkotlinapp.ui.findActivity


@Composable
fun SingleAccountModeScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val viewModel: SingleViewModel = viewModel()

    val account by viewModel.account.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context.findActivity()

    LaunchedEffect(Unit) {
        viewModel.initAuth(context)

        viewModel.signOutEvent.collect {
            snackbarHostState.showSnackbar("Signed out")
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        /*
         * The account may have been removed from the device (if broker is in use).
         *
         * In shared device mode, the account might be signed in/out by other apps while this app is not in focus.
         * Therefore, we want to update the account state by invoking loadAccount() here.
         */
        viewModel.loadAccount()
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

        OutlinedTextField(
            value = account?.username ?: "None",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(text = "Signed-in user") },
            modifier = Modifier.fillMaxWidth(),
        )

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

        if (account == null) {
            Button(
                onClick = { viewModel.signIn(activity) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign In")
            }
        } else {
            OutlinedButton(
                onClick = { viewModel.signOut() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign Out")
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
