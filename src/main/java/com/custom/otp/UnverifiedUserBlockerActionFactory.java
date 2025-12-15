package com.custom.otp;

import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UnverifiedUserBlockerActionFactory implements RequiredActionFactory {

    @Override
    public String getId() {
        return UnverifiedUserBlockerAction.PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "Block Unverified User";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return new UnverifiedUserBlockerAction();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}
