package com.custom.otp;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

public class UnverifiedUserBlockerAction implements RequiredActionProvider {

    public static final String PROVIDER_ID = "unverified-user-block";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        // We do not auto-trigger this based on logic here.
        // It is explicitly added by the Authenticator during registration.
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        UserModel user = context.getUser();
        String isVerified = user.getFirstAttribute("phone_verified");

        if (!"true".equals(isVerified)) {
            // User is unverified. Show blocking error page.
            context.challenge(
                    context.form()
                            // .setHeader("Registration Incomplete") // Method might not exist in this
                            // version
                            .setInfo("You must complete the implementation.")
                            .addError(new FormMessage("form-error",
                                    "Your account verification failed or was abandoned. Please register again to create a new account."))
                            .createForm("info.ftl") // simple info/error page
            );
        } else {
            // If somehow verified (should be removed by authenticator, but safety net)
            context.success();
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // No form to process. Just a blocking page.
        // If they click "back" or try to continue, the challenge reappears.
        requiredActionChallenge(context);
    }

    @Override
    public void close() {
    }
}
