package com.azuresamples.msalandroidcomposeapp.msal

import android.app.Activity
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume


suspend fun ISingleAccountPublicClientApplication.getCurrentAccountSuspend(): IAccount? =
    suspendCancellableCoroutine { continuation ->
        this.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                continuation.resume(activeAccount)
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                continuation.resume(currentAccount)
            }

            override fun onError(exception: MsalException) {
                continuation.cancel(exception)
            }
        })
    }

fun ISingleAccountPublicClientApplication.signIn(
    activity: Activity,
    scopes: List<String>,
    loginHint: String? = null,
    prompt: Prompt? = null,
    callback: AuthenticationCallback,
) {
    val signInParameters = SignInParameters.builder()
        .withActivity(activity)
        .withScopes(scopes)
        .withLoginHint(loginHint)
        .withPrompt(prompt)
        .withCallback(callback)
        .build()

    this.signIn(signInParameters)
}

suspend fun ISingleAccountPublicClientApplication.signIn(
    activity: Activity,
    scopes: List<String>,
    loginHint: String? = null,
    prompt: Prompt? = null,
): IAuthenticationResult? =
    suspendCancellableCoroutine { continuation ->
        val callback = object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                continuation.resume(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                continuation.cancel(exception)
            }

            override fun onCancel() {
                continuation.resume(null)
            }
        }

        this.signIn(
            activity = activity,
            scopes = scopes,
            loginHint = loginHint,
            prompt = prompt,
            callback = callback
        )
    }

suspend fun ISingleAccountPublicClientApplication.signOutSuspend() =
    suspendCancellableCoroutine { continuation ->
        this.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
            override fun onSignOut() {
                continuation.resume(Unit)
            }

            override fun onError(exception: MsalException) {
                continuation.cancel(exception)
            }
        })
    }

suspend fun ISingleAccountPublicClientApplication.acquireTokenSilentSuspend(
    acquireTokenSilentParameters: AcquireTokenSilentParameters,
): IAuthenticationResult =
    suspendCancellableCoroutine { continuation ->
        val callback = object : SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                continuation.resume(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                continuation.cancel(exception)
            }
        }

        val paramsWithCallback = AcquireTokenSilentParameters.Builder()
            .fromAuthority(acquireTokenSilentParameters.authority)
            .forAccount(acquireTokenSilentParameters.account)
            .withScopes(acquireTokenSilentParameters.scopes)
            .forceRefresh(acquireTokenSilentParameters.forceRefresh)
            .withAuthenticationScheme(acquireTokenSilentParameters.authenticationScheme)
            .withClaims(acquireTokenSilentParameters.claimsRequest)
            .withCorrelationId(UUID.fromString(acquireTokenSilentParameters.correlationId))
            .withCallback(callback)
            .build()

        this.acquireTokenSilentAsync(paramsWithCallback)
    }

suspend fun ISingleAccountPublicClientApplication.acquireTokenSuspend(
    acquireTokenParameters: AcquireTokenParameters,
): IAuthenticationResult? =
    suspendCancellableCoroutine { continuation ->
        val callback = object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                continuation.resume(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                continuation.cancel(exception)
            }

            override fun onCancel() {
                continuation.resume(null)
            }
        }

        val paramsWithCallback = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(acquireTokenParameters.activity)
            .withFragment(acquireTokenParameters.fragment)
            .withLoginHint(acquireTokenParameters.loginHint)
            .withPreferredAuthMethod(acquireTokenParameters.preferredAuthMethod)!!  // Accidental @Nullable annotation?
            .withPrompt(acquireTokenParameters.prompt)
            .withOtherScopesToAuthorize(acquireTokenParameters.extraScopesToConsent)
            .withAuthorizationQueryStringParameters(acquireTokenParameters.extraQueryStringParameters)
            .fromAuthority(acquireTokenParameters.authority)
            .forAccount(acquireTokenParameters.account)
            .withScopes(acquireTokenParameters.scopes)
            .withAuthenticationScheme(acquireTokenParameters.authenticationScheme)
            .withClaims(acquireTokenParameters.claimsRequest)
            .withCorrelationId(UUID.fromString(acquireTokenParameters.correlationId))
            .withCallback(callback)
            .build()

        this.acquireToken(paramsWithCallback)
    }
