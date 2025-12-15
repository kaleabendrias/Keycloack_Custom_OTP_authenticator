package com.custom.otp;

import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;

public class UnverifiedUserBlockerAction implements RequiredActionProvider {

    public static final String PROVIDER_ID = "unverified-user-block";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
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
                            .setInfo("You must complete the implementation.")
                            .addError(new FormMessage("form-error",
                                    "Your account verification failed or was abandoned. Please register again to create a new account."))
                            .createForm("info.ftl"));
        } else {
            // If somehow verified
            context.success();
        }
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // If they click "back" or try to continue, the challenge reappears.
        requiredActionChallenge(context);
    }

    @Override
    public void close() {
    }
}
