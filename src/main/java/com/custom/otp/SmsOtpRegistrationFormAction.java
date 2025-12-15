package com.custom.otp;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

import java.util.List;

public class SmsOtpRegistrationFormAction implements FormAction {

    public static final String PROVIDER_ID = "afro-sms-registration";

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        // No execution needed for page build
    }

    @Override
    public void validate(ValidationContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String email = formData.getFirst(UserModel.EMAIL);
        String username = formData.getFirst(UserModel.USERNAME);
        String phone = formData.getFirst("phone_number");

        if (phone == null || phone.isBlank()) {
            context.error(Errors.INVALID_REGISTRATION);
            context.validationError(formData, List.of(
                    new FormMessage("phone_number", "Phone number required")));
            return;
        }

        // Cleanup stale unverified users to avoid "User already exists" error
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        UserModel existingUser = null;

        if (email != null && !email.isBlank()) {
            existingUser = session.users().getUserByEmail(realm, email);
        }
        if (existingUser == null && username != null && !username.isBlank()) {
            existingUser = session.users().getUserByUsername(realm, username);
        }

        if (existingUser != null) {
            // Check if this is a stale unverified registration
            // We assume that if "phone_verified" is explicitly "true", they are a valid
            // user.
            // If they are missing the attribute or it is false, we can overwrite/cleanup.
            String isVerified = existingUser.getFirstAttribute("phone_verified");
            if (!"true".equals(isVerified)) {
                // Delete the stale user so registration can proceed
                session.users().removeUser(realm, existingUser);
            }
        }

        context.success();
    }

    @Override
    public void success(FormContext context) {
        var formData = context.getHttpRequest().getDecodedFormParameters();
        String phone = formData.getFirst("phone_number");

        if (context.getUser() != null && phone != null) {
            context.getUser().setSingleAttribute("phone_number", phone);
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }
}
