package com.azuresamples.msalandroidcomposeapp.ui.single

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azuresamples.msalandroidcomposeapp.MSGraphRequestWrapper
import com.microsoft.identity.client.ktx.PublicClientApplicationKtx
import com.azuresamples.msalandroidcomposeapp.R
import com.microsoft.identity.client.ktx.acquireTokenSilentSuspend
import com.microsoft.identity.client.ktx.acquireTokenSuspend
import com.microsoft.identity.client.ktx.getCurrentAccountSuspend
import com.microsoft.identity.client.ktx.signIn
import com.microsoft.identity.client.ktx.signOutSuspend
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
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
 * Implementation sample for 'Single account' mode.
 *
 * If your app only supports one account being signed-in at a time, this is for you.
 * This requires "account_mode" to be set as "SINGLE" in the configuration file.
 * (Please see res/raw/auth_config_single_account.json for more info).
 */
class SingleAccountViewModel : ViewModel() {

    private val _output = MutableStateFlow<String?>(null)
    val output: StateFlow<String?> = _output

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError

    private val _signOutEvent = Channel<Unit>(Channel.CONFLATED)
    val signOutEvent: Flow<Unit> = _signOutEvent.receiveAsFlow()

    private val app = MutableStateFlow<ISingleAccountPublicClientApplication?>(null)
    val isSharedDevice: Flow<Boolean?> = app.map { it?.isSharedDevice }

    val msGraphUrl = MutableStateFlow(MSGraphRequestWrapper.MS_GRAPH_ROOT_ENDPOINT + "v1.0/me")
    val scopes = MutableStateFlow(listOf("user.read"))

    private val _account = MutableStateFlow<IAccount?>(null)
    val account: StateFlow<IAccount?> = _account

    fun initAuth(context: Context) {
        if (app.value != null) return

        viewModelScope.launch {
            try {
                app.value = PublicClientApplicationKtx.createSingleAccountPublicClientApplication(
                    context, R.raw.auth_config_single_account
                )
            } catch (exception: Exception) {
                showException(exception)
            }

            loadAccount()
        }
    }

    fun loadAccount() {
        /**
         * Load the currently signed-in account, if there's any.
         */
        viewModelScope.launch {
            try {
                _account.value = app.value?.getCurrentAccountSuspend()
            } catch (exception: MsalException) {
                showException(exception)
            }
        }
    }

    fun signIn(activity: Activity) {
        viewModelScope.launch {
            try {
                val authResult = app.value?.signIn(
                    activity = activity,
                    scopes = scopes.value,
                )
                Log.d(TAG, "Successfully authenticated")
                // You can use the account data to update your UI or your app database.
                _account.value = authResult?.account
            } catch (exception: MsalException) {
                showException(exception)
            }
        }
    }

    fun signOut() {
        /**
         * Removes the signed-in account and cached tokens from this app
         * (or device, if the device is in shared mode).
         */
        viewModelScope.launch {
            try {
                app.value?.signOutSuspend()
                _account.value = null
                clearOutput()
                triggerSignOutMessage()
            } catch (exception: MsalException) {
                showException(exception)
            }
        }
    }

    fun callGraphInteractive(activity: Activity) {
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

        clearOutput()

        val parameters = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withScopes(scopes.value)
            .forAccount(account.value)
            .build()

        viewModelScope.launch {
            try {
                val authResult = app.value?.acquireTokenSuspend(parameters)
                    ?: return@launch

                /* Successfully got a token, use it to call a protected resource - MSGraph */
                Log.d(TAG, "Successfully authenticated")
                Log.d(TAG, "ID Token: " + authResult.account.claims?.get("id_token"))

                callGraphAPI(accessToken = authResult.accessToken)

            } catch (exception: MsalException) {
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
        }
    }

    fun callGraphSilent() {
        /*
         * Once you've signed the user in, you can perform acquireTokenSilent
         * to obtain resources without interrupting the user.
         */

        clearOutput()

        val silentParameters = AcquireTokenSilentParameters.Builder()
            .fromAuthority(account.value?.authority)
            .forAccount(account.value)
            .withScopes(scopes.value)
            .build()

        viewModelScope.launch {
            try {
                val authResult = app.value?.acquireTokenSilentSuspend(silentParameters)
                    ?: return@launch

                Log.d(TAG, "Successfully authenticated")

                /* Successfully got a token, use it to call a protected resource - MSGraph */
                callGraphAPI(accessToken = authResult.accessToken)

            } catch (exception: MsalException) {
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
        }
    }

    /**
     * Make an HTTP request to obtain MSGraph data
     *
     * The sample is using the global service cloud as a default.
     * If you're developing an app for sovereign cloud users, please change the Microsoft Graph Resource URL accordingly.
     * https://docs.microsoft.com/en-us/graph/deployments#microsoft-graph-and-graph-explorer-service-root-endpoints
     */
    private fun callGraphAPI(accessToken: String) {
        viewModelScope.launch {
            try {
                val response = MSGraphRequestWrapper.callGraphAPI(
                    graphResourceUrl = msGraphUrl.value,
                    accessToken = accessToken,
                )

                Log.d(TAG, "Response: $response")
                showResult(response)

            } catch (e: Exception) {
                Log.d(TAG, "Error: $e")
                showException(e)
            }
        }
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

    private fun triggerSignOutMessage() {
        viewModelScope.launch {
            _signOutEvent.send(Unit)
        }
    }

    companion object {
        private val TAG = this::class.java.simpleName
    }
}
