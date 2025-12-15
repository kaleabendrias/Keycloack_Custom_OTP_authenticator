package com.custom.otp;

import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Random;

public class SmsOtpRegistrationAuthenticator implements Authenticator {

    public static final String OTP_NOTE = "sms_registration_otp";
    public static final String OTP_SENT = "sms_registration_otp_sent";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        UserModel user = context.getUser();

        if (user == null) {
            // Fallback: Try to get user from session if context doesn't have it
            user = authSession.getAuthenticatedUser();
        }

        if (user == null) {
            // Should not happen if placed after Registration User Profile
            context.failure(AuthenticationFlowError.INVALID_USER);
            return;
        }

        // 1. BLOCK USER UNTIL VERIFIED
        // We add unverified-user-block action. This blocks them from accessing the app
        // if they abandon registration using our custom error page.
        // We remove this action immediately when they verify SMS OTP below.
        user.setEnabled(true);
        user.addRequiredAction("unverified-user-block");
        user.setSingleAttribute("phone_verified", "false");

        // 2. CHECK IF RESUMING
        if ("true".equals(authSession.getAuthNote(OTP_SENT))) {
            context.challenge(context.form().createForm("sms-otp-form.ftl"));
            return;
        }

        // 3. SEND OTP
        String phone = user.getFirstAttribute("phone_number");
        if (phone == null || phone.isBlank()) {
            context.failure(AuthenticationFlowError.INVALID_USER);
            return;
        }

        String otp = generateOtp();
        sendOtp(phone, otp);

        authSession.setAuthNote(OTP_NOTE, otp);
        authSession.setAuthNote(OTP_SENT, "true");

        context.challenge(context.form().createForm("sms-otp-form.ftl"));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String inputOtp = formData.getFirst("otp_code");
        String storedOtp = context.getAuthenticationSession().getAuthNote(OTP_NOTE);

        if (storedOtp == null || !storedOtp.equals(inputOtp)) {
            // Challenge again (Retry).
            // Since requiresUser() is false, Keycloak allows this even if user is disabled.
            context.challenge(
                    context.form()
                            .setError("Invalid OTP code")
                            .createForm("sms-otp-form.ftl"));
            return;
        }

        // SUCCESS
        UserModel user = context.getUser();
        if (user == null) {
            user = context.getAuthenticationSession().getAuthenticatedUser();
        }

        if (user != null) {
            user.setSingleAttribute("phone_verified", "true");

            // 4. VERIFIED: REMOVE BLOCKING ACTION
            user.removeRequiredAction("unverified-user-block");
            // user.setEmailVerified(true); // Optional: mark email as verified if you trust
            // the flow
            user.setEnabled(true);
        }

        context.getAuthenticationSession().removeAuthNote(OTP_NOTE);
        context.getAuthenticationSession().removeAuthNote(OTP_SENT);

        context.success();
    }

    // CRITICAL: This allows the authenticator to run even if the user is DISABLED.
    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession s, RealmModel r, UserModel u) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession s, RealmModel r, UserModel u) {
    }

    @Override
    public void close() {
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    private void sendOtp(String phone, String otp) {
        // Log it (or replace with SMS provider)
        org.jboss.logging.Logger.getLogger(SmsOtpRegistrationAuthenticator.class)
                .infof("Sending OTP to %s: %s", phone, otp);
    }
}
