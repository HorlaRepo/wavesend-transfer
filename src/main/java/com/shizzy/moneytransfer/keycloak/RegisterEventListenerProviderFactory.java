package com.shizzy.moneytransfer.keycloak;

import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class RegisterEventListenerProviderFactory implements EventListenerProviderFactory {

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        System.out.println("Creating RegisterEventListenerProvider");
        return new RegisterEventListenerProvider(session);
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        System.out.println("Initializing RegisterEventListenerProviderFactory");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        System.out.println("Post-initializing RegisterEventListenerProviderFactory");
    }

    @Override
    public void close() {
        System.out.println("Closing RegisterEventListenerProviderFactory");
    }

    @Override
    public String getId() {
        return "register-event-listener";
    }
}