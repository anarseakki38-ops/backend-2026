# Outlook/Exchange SMTP Configuration Guide

Since your error is `535 5.7.139 Basic authentication is disabled`, the Outlook server is blocking the login attempt. In a strict environment like banking, security policies often disable legacy login methods by default.

You must enable **SMTP Authentication** explicitly.

## Scenario A: Personal Account (@outlook.com)
*Use this if you are testing with your personal email.*

1.  **Log in** to [Outlook.com](https://outlook.live.com/).
2.  Click **Settings (Gear icon)** > **Mail** > **Sync email**.
3.  Under **POP and IMAP**, select **Yes** for "Let devices and apps use POP".
    *   *Why?* Enabling POP often inadvertently re-enables SMTP Basic Authentication for the account.
4.  **Save** the changes.
5.  Ensure you are using an **App Password** (generated in Microsoft Account > Security), not your login password.

## Scenario B: Enterprise / Banking (Office 365)
*Use this for the actual banking environment. You (or the IT Admin) must perform these steps.*

### 1. Enable Authenticated SMTP for the Specific User
The bank's global security policy likely disables Basic Auth. You can override this for the specific "Service Account" sending the emails.

**Using Microsoft 365 Admin Center:**
1.  Go to [admin.microsoft.com](https://admin.microsoft.com).
2.  Navigate to **Users** > **Active users**.
3.  Select the user account (e.g., `reports@bank-domain.com`).
4.  In the side panel, go to the **Mail** tab.
5.  Click **Manage email apps**.
6.  **Check** the box for **Authenticated SMTP**.
7.  Click **Save changes**.

**Using PowerShell (Admin):**
```powershell
Connect-ExchangeOnline
Set-CASMailbox -Identity "reports@bank-domain.com" -SmtpClientAuthenticationDisabled $false
```

### 2. Disable Security Defaults (If Global Block is On)
If "Security Defaults" are enabled in Azure Active Directory, Basic Auth is blocked globally.
1.  Go to [Azure Portal](https://portal.azure.com).
2.  Search for **Microsoft Entra ID** (formerly Azure AD).
3.  Go to **Properties** > **Manage Security defaults**.
4.  Set it to **Disabled** (Not recommended for entire bank, better to use Step 1 exception).

## Scenario C: Modern Authentication (OAuth2) - Recommended for Banks
If the bank strictly forbids Basic Auth (no App Passwords allowed), we must switch the code to use **OAuth2**.

**Requirements:**
1.  Register an App in **Azure Active Directory**.
2.  Grant the `Mail.Send` permission.
3.  Generate a **Client ID** and **Client Secret**.
4.  Update the spring boot application to use `spring-cloud-azure-starter-active-directory`.

*If you cannot enable SMTP Auth (Scenario A/B), let me know, and I will rewrite the code for Scenario C (OAuth2).*
