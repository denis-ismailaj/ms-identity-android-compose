package com.azuresamples.msalandroidkotlinapp.ui.multi

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuresamples.msalandroidkotlinapp.MSGraphRequestWrapper
import com.azuresamples.msalandroidkotlinapp.PublicClientApplicationExt
import com.azuresamples.msalandroidkotlinapp.R
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication.RemoveAccountCallback
import com.microsoft.identity.client.IPublicClientApplication.LoadAccountsCallback
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


/**
 * Implementation sample for 'Multiple account' mode.
 */
class MultiViewModel : ViewModel() {

    private val _output = MutableStateFlow<String?>(null)
    val output: StateFlow<String?> = _output

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError

    private val _accountRemovedEvent = Channel<Unit>(Channel.CONFLATED)
    val accountRemovedEvent: Flow<Unit> = _accountRemovedEvent.receiveAsFlow()

    private val app = MutableStateFlow<IMultipleAccountPublicClientApplication?>(null)
    val isSharedDevice: Flow<Boolean?> = app.map { it?.isSharedDevice }

    val msGraphUrl = MutableStateFlow(MSGraphRequestWrapper.MS_GRAPH_ROOT_ENDPOINT + "v1.0/me")
    val scopes = MutableStateFlow(listOf("user.read"))

    private val _accounts = MutableStateFlow<List<IAccount>?>(null)
    val accounts: StateFlow<List<IAccount>?> = _accounts

    val selectedAccount = MutableStateFlow<IAccount?>(null)

    fun initAuth(context: Context) {
        viewModelScope.launch {
            try {
                app.value = PublicClientApplicationExt.createMultipleAccountPublicClientApplication(
                    context, R.raw.auth_config_multiple_account
                )
            } catch (exception: Exception) {
                showException(exception)
            }

            loadAccounts()
        }
    }

    /**
     * Load currently signed-in accounts, if there's any.
     */
    fun loadAccounts() {
        app.value?.getAccounts(object : LoadAccountsCallback {
            override fun onTaskCompleted(result: List<IAccount>?) {
                // You can use the account data to update your UI or your app database.
                _accounts.value = result
            }

            override fun onError(exception: MsalException) {
                showException(exception)
            }
        })
    }

    fun removeAccount() {
        /*
         * Removes the selected account and cached tokens from this app
         * (or device, if the device is in shared mode).
         */
        app.value?.removeAccount(selectedAccount.value, object : RemoveAccountCallback {
            override fun onRemoved() {
                selectedAccount.value = null
                clearOutput()
                triggerAccountRemovedMessage()

                // Get the up-to-date list of accounts
                loadAccounts()
            }

            override fun onError(exception: MsalException) {
                showException(exception)
            }
        })
    }


    fun callGraphInteractive(activity: Activity) {
        clearOutput()

        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withScopes(scopes.value)
            .withCallback(object : AuthenticationCallback {
                /**
                 * Callback used for interactive request.
                 * If it succeeds we use the access token to call Microsoft Graph.
                 * Does not check cache.
                 */

                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    /* Successfully got a token, use it to call a protected resource - MSGraph */
                    Log.d(TAG, "Successfully authenticated")
                    Log.d(TAG, "ID Token: " + authenticationResult.account.claims?.get("id_token"))

                    callGraphAPI(
                        context = activity,
                        accessToken = authenticationResult.accessToken,
                    )

                    // Get up-to-date list of accounts
                    loadAccounts()
                }

                override fun onError(exception: MsalException) {
                    /* Failed to acquireToken */
                    Log.d(TAG, "Authentication failed: $exception")

                    showException(exception)

                    @Suppress("ControlFlowWithEmptyBody")
                    if (exception is MsalClientException) {
                        /* Exception inside MSAL, more info inside MsalError.java */
                    } else if (exception is MsalServiceException) {
                        /* Exception when communicating with the STS, likely config issue */
                    }
                }

                override fun onCancel() {
                    /* User canceled the authentication */
                    Log.d(TAG, "User cancelled login.")
                }
            })
            .forAccount(selectedAccount.value)
            .build()
        /*
         * Acquire token interactively. It will also create an account object for the silent call as a result.
         *
         * If acquireTokenSilent() returns an error that requires an interaction (MsalUiRequiredException),
         * invoke acquireToken() to have the user resolve the interrupt interactively.
         *
         * Some example scenarios are
         *  - password change
         *  - the resource you're acquiring a token for has a stricter set of requirement than your Single Sign-On refresh token.
         *  - you're introducing a new scope which the user has never consented for.
         */
        app.value?.acquireToken(parameters)
    }

    fun callGraphSilent(context: Context) {
        clearOutput()

        val silentParameters = AcquireTokenSilentParameters.Builder()
            .fromAuthority(selectedAccount.value?.authority)
            .forAccount(selectedAccount.value)
            .withScopes(scopes.value)
            .withCallback(object : SilentAuthenticationCallback {
                override fun onSuccess(authenticationResult: IAuthenticationResult) {
                    Log.d(TAG, "Successfully authenticated")

                    /* Successfully got a token, use it to call a protected resource - MSGraph */
                    callGraphAPI(
                        context = context,
                        accessToken = authenticationResult.accessToken,
                    )
                }

                override fun onError(exception: MsalException) {
                    /* Failed to acquireToken */
                    Log.d(TAG, "Authentication failed: $exception")

                    showException(exception)

                    when (exception) {
                        is MsalClientException -> {
                            /* Exception inside MSAL, more info inside MsalError.java */
                        }

                        is MsalServiceException -> {
                            /* Exception when communicating with the STS, likely config issue */
                        }

                        is MsalUiRequiredException -> {
                            /* Tokens expired or no session, retry with interactive */
                        }
                    }
                }
            })
            .build()
        /*
         * Perform acquireToken without interrupting the user.
         *
         * This requires an account object of the account you're obtaining a token for.
         */
        app.value?.acquireTokenSilentAsync(silentParameters)
    }

    /**
     * Make an HTTP request to obtain MSGraph data
     *
     * The sample is using the global service cloud as a default.
     * If you're developing an app for sovereign cloud users, please change the Microsoft Graph Resource URL accordingly.
     * https://docs.microsoft.com/en-us/graph/deployments#microsoft-graph-and-graph-explorer-service-root-endpoints
     */
    private fun callGraphAPI(context: Context, accessToken: String) {
        MSGraphRequestWrapper.callGraphAPIUsingVolley(
            context,
            msGraphUrl.value,
            accessToken,
            responseListener = { response ->
                /* Successfully called graph, process data and send to UI */
                Log.d(TAG, "Response: $response")
                showResult(response)
            },
            errorListener = { error ->
                Log.d(TAG, "Error: $error")
                showException(error)
            },
        )
    }

    private fun showException(exception: Exception) {
        _isError.value = true
        _output.value = exception.toString()
    }

    private fun showResult(result: Any) {
        _isError.value = false
        _output.value = result.toString()
    }

    private fun clearOutput() {
        _isError.value = false
        _output.value = null
    }

    private fun triggerAccountRemovedMessage() {
        viewModelScope.launch {
            _accountRemovedEvent.send(Unit)
        }
    }

    companion object {
        private val TAG = this::class.java.simpleName
    }
}
