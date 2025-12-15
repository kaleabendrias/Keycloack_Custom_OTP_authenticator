# Keycloak Custom SMS OTP Registration

This project provides a custom Keycloak extension to handle **OTP-only Registration** (verifying phone numbers via SMS) while solving common issues like "User Already Exists" for incomplete registrations.

## Features

1.  **Cleaner Logic**: Automatically deletes "stale" unverified users when they try to re-register. usage checks prevent the "User Already Exists" error.
2.  **SMS OTP Authenticator**: Sends an OTP code to the user's phone number during registration.
3.  **Unverified User Blocker**: A custom Blocking Action that prevents unverified users from logging in (if they abandoned registration) and instructs them to register again.

---

## Architecture

The solution uses three Keycloak SPIs involved in the flow:

1.  **FormAction (`SmsOtpRegistrationFormAction`)**
    *   *Type:* `FormAction`
    *   *Role:* **Cleaning Up.**
    *   *Logic:* Runs on the Registration Page validation. Checks if the email/username exists but is unverified. If so, deletes the old user to allow the new registration to proceed.

2.  **Authenticator (`SmsOtpRegistrationAuthenticator`)**
    *   *Type:* `Authenticator`
    *   *Role:* **Sending OTP & Arming the Trap.**
    *   *Logic:*
        *   Adds the `unverified-user-block` Required Action to the user.
        *   Sends the SMS OTP.
        *   Disarms the `unverified-user-block` action ONLY upon successful OTP verification.

3.  **RequiredAction (`UnverifiedUserBlockerAction`)**
    *   *Type:* `RequiredAction`
    *   *Role:* **The Bouncer.**
    *   *Logic:* If a user abandons registration and tries to login later, this action triggers. It checks `phone_verified`. If false, it blocks access and shows: *"Registration Incomplete. Please register again."*

---

## Deployment

### 1. Build
```bash
mvn clean install
```

### 2. Deploy
Copy the jar from `target/keycloak-sms-otp.jar` to your Keycloak `providers/` directory.

**Docker Way:**
```bash
docker compose up -d --build
```

---

## Configuration (Important)

For this valid flow to work, you must configure Keycloak correctly.

### Step 1: Register the Blocking Action
1.  Log in to Keycloak Admin Console.
2.  Go to **Authentication** (left menu).
3.  Click the **Required Actions** tab.
4.  Click **Register**.
5.  Select **Block Unverified User** (`unverified-user-block`).
6.  Ensure it is **Enabled**.

### Step 2: Configure Registration Flow
1.  Go to **Authentication -> Flows**.
2.  Select **Registration**.
3.  Copy the built-in flow to make a custom editable one (e.g., "SMS Registration").
4.  Add the components in this **EXACT Order**:

| Order | Component Name | Type | Requirement | Notes |
| :--- | :--- | :--- | :--- | :--- |
| 1 | Registration Page | Form | REQUIRED | The default registration form. |
| 2 | **Afro SMS OTP (FormAction)** | Step | REQUIRED | **Crucial:** Must be BEFORE "User Creation". |
| 3 | Registration User Creation | Step | REQUIRED | Creates the user in DB. |
| 4 | **SMS OTP (Registration)** | Step | REQUIRED | Sends the OTP & Adds Blocker. |

### Summary
*   **FormAction** cleans up the path before the user is created.
*   **Authenticator** sends the OTP after the user is created.
*   **RequiredAction** protects the system from abandoned accounts.
