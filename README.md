# Use MSAL in an Android app to sign-in users and call Microsoft Graph

This project is a rewrite
of [`Azure-Samples/ms-identity-android-kotlin`](https://github.com/Azure-Samples/ms-identity-android-kotlin)
using Jetpack Compose and modern Android development practices.

For additional information, check out the original repo or
the [MSAL library](https://github.com/AzureAD/microsoft-authentication-library-for-android).

## Single Account Mode

In the `Single Account` Mode, only one user can sign into the application at a time.

#### Single Account Mode with Shared Device Mode

`Shared Device` Mode will allow you to configure Android devices to be shared by multiple employees,
while providing Microsoft Identity backed management of the device. Employees will be able to
sign-in to their devices and access customer information quickly. When they are finished with their
shift or task, they will be able to globally Sign-Out of the device and it will be immediately ready
for the next employee to use.

> [!NOTE]
> Applications that run on Shared Devices must be in Single Account Mode. Applications that only
> support Multiple Account Mode will not run on a Shared Device.

In the code, you can use the `isSharedDevice()` flag to determine if an application is in the Shared
Device Mode. Your app can use this flag to modify UX accordingly.

> [!NOTE]
> You can only [put a device in to Shared Mode](https://docs.microsoft.com/azure/active-directory/develop/tutorial-v2-shared-device-mode#set-up-an-android-device-in-shared-mode)
> using the [Authenticator app](https://www.microsoft.com/account/authenticator) and with a user 
> who is in the [Cloud Device Administrator](https://docs.microsoft.com/azure/active-directory/users-groups-roles/directory-assign-admin-roles#cloud-device-administrator) role.
> You can configure the membership of your Organizational Roles by going to the Microsoft Entra
> admin center and selecting:
>
> Microsoft Entra ID -> Roles and Administrators -> Cloud Device Administrator

## Multiple Account Mode

In the `Multiple Account` Mode, the application supports multiple accounts and can switch between
user accounts and get data from that user's account.

## Steps to run the app

> [!NOTE]
> This sample ships with a default `redirect_uri` configured in the `AndroidManifest.xml`. In order
> for the default `redirect_uri` to work, this project must be built with the `debug.keystore`
> located in the `gradle/` directory.

The following steps have been carried out for Android Studio, but you can choose and work with any
editor of your choice.

From the menu, select `Run` -> `Run 'app'`.

Once the app launches,

1. Click on the hamburger icon

    * `Single account`: Select this to explore _Single account mode_

    * `Multiple account`: Select this to explore _Multiple account mode_.

2. Click on `Sign In`, it takes you to the web-based authentication page.

3. Once successfully signed-in, basic user details will be displayed.

To explore more about the application, follow on-screen options.

> [!NOTE]
> This sample application is configured to run out-of-the-box. To register your own application and
> run the sample with those settings, follow below steps.

## Register your own application (optional)

To begin registering your app, start at
the [Microsoft Entra admin center](https://aka.ms/MobileAppReg)

To create an app registration,

1. Click `New Registration`.

2. Name your app, select the audience you're targeting, and click `Register`.

3. In the `Overview` -> `Sign in users in 5 minutes` -> `Android`.
    * Click on `Make these changes for me`.
    * Enter the Package Name from your Android Manifest.
    * Generate a Signature Hash. Follow the instructions in the portal.

4. Hit the `Make updates` button. Note the `MSAL Configuration` as it is used later
   in `AndroidManifest.xml` and `auth_config.json`.

**Configure** the sample application with your app registration by replacing the sample code
in `auth_config.json` and `AndroidManifest.xml`

1. Copy and paste the `MSAL Configuration` JSON from the Microsoft Entra admin center
   into `auth_config.json`.

2. Inside the `AndroidManifest.xml`, replace `android:host` and `android:path` with the same info
   saved in above step.
    - `auth_config.json` contains this information as a reference inside the `redirect_uri` field.
    - The Signature Hash **should NOT be URL-encoded** in `AndroidManifest.xml`.
      Refer to [Microsoft Entra ID Android Quickstart](https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-v2-android)
      for more details.

## About the code

The following code fragments walk through features that MSAL can implement.

### `SingleAccountViewModel` class

- Signing in a user
- Acquiring a token interactively or silently
- Calling Graph API to get basic user details
- Signing out

### `MultiAccountViewModel` class

- Acquiring a token interactively or silently
- Getting signed-in accounts
- Calling Graph API to get basic user details
- Removing an account

> [!NOTE]
> The functionality to add accounts for a multi-account application is missing in
> `ms-identity-android-kotlin` and I didn't see any such methods in the
> [linked API reference](https://javadoc.io/doc/com.microsoft.identity.client/msal/2.2.3/index.html)
> either, so to use the _Multi account mode_ in the demo, start by signing in
> on _Single account mode_.
